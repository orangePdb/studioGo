package com.example.ui.viewmodel

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.IdeFileEntity
import com.example.data.local.IdeProjectEntity
import com.example.data.local.ProjectWithStats
import com.example.data.model.BottomToolTab
import com.example.data.model.BuildOutputLine
import com.example.data.model.BuildStatus
import com.example.data.model.CodeDiagnostic
import com.example.data.model.DiagnosticSeverity
import com.example.data.model.EditorPreferences
import com.example.data.model.IdeFileType
import com.example.data.model.LogLevel
import com.example.data.model.LogcatEntry
import com.example.data.model.PreviewDeviceFrame
import com.example.data.model.ProjectTemplate
import com.example.data.model.TerminalLine
import com.example.data.model.WorkspaceMode
import com.example.data.repository.IdeRepository
import com.example.engine.ComposePreviewParser
import com.example.engine.ParsedPreviewScreen
import com.example.engine.StaticAnalyzer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class IdeUiState(
    val isLoading: Boolean = true,
    val projects: List<ProjectWithStats> = emptyList(),
    val activeProject: IdeProjectEntity? = null,
    val projectFiles: List<IdeFileEntity> = emptyList(),
    val openTabIds: List<Long> = emptyList(),
    val activeFileId: Long? = null,
    val editorValue: TextFieldValue = TextFieldValue(""),
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val diagnostics: List<CodeDiagnostic> = emptyList(),
    val parsedPreview: ParsedPreviewScreen? = null,
    val previewStateMap: Map<String, Any> = emptyMap(),
    val previewDarkTheme: Boolean = true,
    val selectedDeviceFrame: PreviewDeviceFrame = PreviewDeviceFrame.PIXEL_PHONE,
    val buildStatus: BuildStatus = BuildStatus.IDLE,
    val buildOutput: List<BuildOutputLine> = emptyList(),
    val logcatEntries: List<LogcatEntry> = emptyList(),
    val logcatLevelFilter: LogLevel = LogLevel.VERBOSE,
    val logcatSearchQuery: String = "",
    val terminalLines: List<TerminalLine> = listOf(
        TerminalLine("StudioDroid Shell v2.4 (Gradle 8.13 • Kotlin 2.4)", isAccent = true),
        TerminalLine("Type 'help' for available commands (ls, tree, cat, gradle assembleDebug)."),
    ),
    val workspaceMode: WorkspaceMode = WorkspaceMode.SPLIT,
    val bottomToolTab: BottomToolTab = BottomToolTab.NONE,
    val showNewProjectDialog: Boolean = false,
    val showNewFileDialog: Boolean = false,
    val showSettingsDialog: Boolean = false,
    val jumpToLineRequest: Int? = null,
) {
    val activeFile: IdeFileEntity?
        get() = projectFiles.firstOrNull { it.id == activeFileId }

    val activeFileDiagnostics: List<CodeDiagnostic>
        get() = diagnostics.filter { it.fileId == activeFileId }

    val errorCount: Int
        get() = diagnostics.count { it.severity == DiagnosticSeverity.ERROR }

    val warningCount: Int
        get() = diagnostics.count { it.severity == DiagnosticSeverity.WARNING }
}

class IdeViewModel(
    private val repository: IdeRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(IdeUiState())
    val uiState: StateFlow<IdeUiState> = _uiState.asStateFlow()

    val editorPreferences: StateFlow<EditorPreferences> = repository.editorPreferences.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = EditorPreferences(),
    )

    private var projectObserverJob: Job? = null
    private var filesObserverJob: Job? = null
    private var saveDebounceJob: Job? = null
    private var buildJob: Job? = null

    private val undoStack = ArrayDeque<TextFieldValue>()
    private val redoStack = ArrayDeque<TextFieldValue>()
    private var nextLogId = 1L

    init {
        viewModelScope.launch {
            repository.ensureStarterProjectsSeeded()
            repository.allProjectsWithStats.collectLatest { projects ->
                _uiState.update { it.copy(isLoading = false, projects = projects) }
            }
        }
    }

    fun openProject(projectId: Long) {
        projectObserverJob?.cancel()
        filesObserverJob?.cancel()
        undoStack.clear()
        redoStack.clear()

        _uiState.update {
            it.copy(
                openTabIds = emptyList(),
                activeFileId = null,
                editorValue = TextFieldValue(""),
                buildOutput = emptyList(),
                buildStatus = BuildStatus.IDLE,
                logcatEntries = emptyList(),
                bottomToolTab = BottomToolTab.NONE,
            )
        }

        projectObserverJob = viewModelScope.launch {
            repository.observeProject(projectId).collectLatest { project ->
                _uiState.update { it.copy(activeProject = project) }
            }
        }

        filesObserverJob = viewModelScope.launch {
            repository.observeFilesForProject(projectId).collectLatest { files ->
                val current = _uiState.value
                val validIds = files.map { it.id }.toSet()
                val cleanedTabs = current.openTabIds.filter { it in validIds }.toMutableList()

                val targetActiveId = when {
                    current.activeFileId != null && current.activeFileId in validIds -> current.activeFileId
                    else -> {
                        val preferred = files.firstOrNull { it.name == "MainActivity.kt" }
                            ?: files.firstOrNull()
                        preferred?.id
                    }
                }
                if (targetActiveId != null && targetActiveId !in cleanedTabs) {
                    cleanedTabs.add(targetActiveId)
                }

                val activeFile = files.firstOrNull { it.id == targetActiveId }
                val shouldLoadEditor =
                    current.activeFileId != targetActiveId ||
                        (current.editorValue.text.isEmpty() && !activeFile?.content.isNullOrEmpty())

                val nextEditorVal = if (shouldLoadEditor && activeFile != null) {
                    TextFieldValue(activeFile.content)
                } else {
                    current.editorValue
                }

                val diagnostics = StaticAnalyzer.analyzeProject(files)
                val previewTarget = pickPreviewSourceFile(activeFile, files)
                val parsed = previewTarget?.let { ComposePreviewParser.parse(it.content) }

                _uiState.update { state ->
                    val mergedPreviewState = if (state.activeFileId != targetActiveId || state.previewStateMap.isEmpty()) {
                        parsed?.initialStates ?: emptyMap()
                    } else {
                        state.previewStateMap
                    }
                    state.copy(
                        projectFiles = files,
                        openTabIds = cleanedTabs,
                        activeFileId = targetActiveId,
                        editorValue = nextEditorVal,
                        diagnostics = diagnostics,
                        parsedPreview = parsed,
                        previewStateMap = mergedPreviewState,
                    )
                }
            }
        }

        appendLogcat(LogLevel.INFO, "StudioIDE", "Opened project workspace (id=$projectId)")
    }

    fun closeProject() {
        flushPendingSave()
        projectObserverJob?.cancel()
        filesObserverJob?.cancel()
        _uiState.update {
            it.copy(
                activeProject = null,
                projectFiles = emptyList(),
                openTabIds = emptyList(),
                activeFileId = null,
                editorValue = TextFieldValue(""),
                parsedPreview = null,
                previewStateMap = emptyMap(),
                bottomToolTab = BottomToolTab.NONE,
            )
        }
    }

    fun createProject(
        name: String,
        packageName: String,
        minSdk: Int,
        template: ProjectTemplate,
    ) {
        viewModelScope.launch {
            val id = repository.createProject(
                name = name.ifBlank { "MyAndroidApp" },
                packageName = packageName.ifBlank { "com.studio.myapp" },
                minSdk = minSdk,
                targetSdk = 36,
                template = template,
            )
            _uiState.update { it.copy(showNewProjectDialog = false) }
            openProject(id)
        }
    }

    fun deleteProject(projectId: Long) {
        viewModelScope.launch {
            repository.deleteProject(projectId)
        }
    }

    fun openFile(fileId: Long, jumpLine: Int? = null) {
        flushPendingSave()
        val file = _uiState.value.projectFiles.firstOrNull { it.id == fileId } ?: return
        undoStack.clear()
        redoStack.clear()

        val text = file.content
        val targetOffset = if (jumpLine != null && jumpLine > 1) {
            lineStartOffset(text, jumpLine)
        } else {
            0
        }

        val tabs = _uiState.value.openTabIds.toMutableList()
        if (fileId !in tabs) tabs.add(fileId)

        val previewTarget = pickPreviewSourceFile(file, _uiState.value.projectFiles)
        val parsed = previewTarget?.let { ComposePreviewParser.parse(it.content) }

        _uiState.update {
            it.copy(
                openTabIds = tabs,
                activeFileId = fileId,
                editorValue = TextFieldValue(text = text, selection = TextRange(targetOffset)),
                canUndo = false,
                canRedo = false,
                parsedPreview = parsed,
                previewStateMap = parsed?.initialStates ?: emptyMap(),
                jumpToLineRequest = jumpLine,
            )
        }
    }

    fun closeTab(fileId: Long) {
        val current = _uiState.value
        val remaining = current.openTabIds.filter { it != fileId }
        if (remaining.isEmpty()) {
            _uiState.update {
                it.copy(
                    openTabIds = emptyList(),
                    activeFileId = null,
                    editorValue = TextFieldValue(""),
                )
            }
            return
        }
        if (current.activeFileId == fileId) {
            openFile(remaining.last())
            _uiState.update { it.copy(openTabIds = remaining) }
        } else {
            _uiState.update { it.copy(openTabIds = remaining) }
        }
    }

    fun createFile(folderPath: String, fileName: String) {
        val project = _uiState.value.activeProject ?: return
        viewModelScope.launch {
            val newId = repository.createFile(
                projectId = project.id,
                folderPath = folderPath,
                fileName = fileName,
                packageName = project.packageName,
            )
            _uiState.update { it.copy(showNewFileDialog = false) }
            delay(80)
            openFile(newId)
            appendLogcat(LogLevel.DEBUG, "ProjectTree", "Created file $folderPath/$fileName")
        }
    }

    fun deleteFile(fileId: Long) {
        viewModelScope.launch {
            val file = _uiState.value.projectFiles.firstOrNull { it.id == fileId }
            closeTab(fileId)
            repository.deleteFile(fileId)
            if (file != null) {
                appendLogcat(LogLevel.WARN, "ProjectTree", "Deleted ${file.name}")
            }
        }
    }

    fun onEditorValueChange(newValue: TextFieldValue) {
        val current = _uiState.value
        val activeId = current.activeFileId ?: return

        if (newValue.text != current.editorValue.text) {
            undoStack.addLast(current.editorValue)
            if (undoStack.size > 40) undoStack.removeFirst()
            redoStack.clear()
        }

        val updatedFiles = current.projectFiles.map { file ->
            if (file.id == activeId) file.copy(content = newValue.text) else file
        }
        val activeFile = updatedFiles.firstOrNull { it.id == activeId }
        val diagnostics = StaticAnalyzer.analyzeProject(updatedFiles)
        val previewTarget = pickPreviewSourceFile(activeFile, updatedFiles)
        val parsed = previewTarget?.let { ComposePreviewParser.parse(it.content) }

        _uiState.update { state ->
            val mergedStates = if (parsed != null) {
                parsed.initialStates.mapValues { (k, initial) ->
                    state.previewStateMap[k] ?: initial
                }
            } else {
                emptyMap()
            }
            state.copy(
                editorValue = newValue,
                projectFiles = updatedFiles,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
                diagnostics = diagnostics,
                parsedPreview = parsed,
                previewStateMap = mergedStates,
                jumpToLineRequest = null,
            )
        }

        if (newValue.text != current.editorValue.text) {
            saveDebounceJob?.cancel()
            saveDebounceJob = viewModelScope.launch {
                delay(350)
                repository.saveFileContent(activeId, newValue.text)
            }
        }
    }

    fun insertSnippet(snippet: String) {
        val current = _uiState.value.editorValue
        val start = current.selection.min
        val end = current.selection.max
        val newText = buildString {
            append(current.text.substring(0, start))
            append(snippet)
            append(current.text.substring(end))
        }
        val cursorOffset = if (snippet == "{}" || snippet == "()" || snippet == "\"\"") {
            start + 1
        } else {
            start + snippet.length
        }
        onEditorValueChange(
            TextFieldValue(
                text = newText,
                selection = TextRange(cursorOffset),
            )
        )
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        val current = _uiState.value.editorValue
        redoStack.addLast(current)
        val prev = undoStack.removeLast()
        applyUndoRedo(prev)
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val current = _uiState.value.editorValue
        undoStack.addLast(current)
        val next = redoStack.removeLast()
        applyUndoRedo(next)
    }

    private fun applyUndoRedo(target: TextFieldValue) {
        val activeId = _uiState.value.activeFileId ?: return
        val updatedFiles = _uiState.value.projectFiles.map { file ->
            if (file.id == activeId) file.copy(content = target.text) else file
        }
        val diagnostics = StaticAnalyzer.analyzeProject(updatedFiles)
        val activeFile = updatedFiles.firstOrNull { it.id == activeId }
        val parsed = pickPreviewSourceFile(activeFile, updatedFiles)?.let {
            ComposePreviewParser.parse(it.content)
        }

        _uiState.update {
            it.copy(
                editorValue = target,
                projectFiles = updatedFiles,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
                diagnostics = diagnostics,
                parsedPreview = parsed,
            )
        }
        saveDebounceJob?.cancel()
        saveDebounceJob = viewModelScope.launch {
            repository.saveFileContent(activeId, target.text)
        }
    }

    fun formatActiveFile() {
        val file = _uiState.value.activeFile ?: return
        val lines = file.content.lines()
        var indent = 0
        val formatted = lines.joinToString("\n") { raw ->
            val trimmed = raw.trim()
            if (trimmed.startsWith("}") || trimmed.startsWith(")") || trimmed.startsWith("</")) {
                indent = (indent - 1).coerceAtLeast(0)
            }
            val result = if (trimmed.isEmpty()) "" else "    ".repeat(indent) + trimmed
            val opens = trimmed.count { it == '{' } - trimmed.count { it == '}' }
            if (opens > 0) indent += opens
            result
        }
        onEditorValueChange(TextFieldValue(formatted, TextRange(0)))
        appendLogcat(LogLevel.INFO, "Formatter", "Formatted ${file.name}")
    }

    fun runProject() {
        val project = _uiState.value.activeProject ?: return
        flushPendingSave()
        buildJob?.cancel()

        buildJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    buildStatus = BuildStatus.BUILDING,
                    bottomToolTab = BottomToolTab.BUILD,
                    buildOutput = listOf(
                        BuildOutputLine("> Configure project :app (${project.packageName})"),
                    ),
                )
            }
            repository.updateBuildStatus(project.id, BuildStatus.BUILDING)
            appendLogcat(LogLevel.INFO, "GradleBuild", "Starting assembleDebug for ${project.name}...")

            delay(220)
            appendBuildLine("> Task :app:preBuild UP-TO-DATE")
            appendBuildLine("> Task :app:mergeDebugResources")
            delay(220)
            appendBuildLine("> Task :app:processDebugManifest")
            delay(200)
            appendBuildLine("> Task :app:compileDebugKotlin")

            val files = _uiState.value.projectFiles
            val diagnostics = StaticAnalyzer.analyzeProject(files)
            val errors = diagnostics.filter { it.severity == DiagnosticSeverity.ERROR }

            delay(240)
            if (errors.isNotEmpty()) {
                errors.forEach { err ->
                    appendBuildLine(
                        "e: ${err.fileName}:${err.line}: ${err.message}",
                        isError = true,
                    )
                    appendLogcat(LogLevel.ERROR, "kotlinc", "${err.fileName}:${err.line} - ${err.message}")
                }
                appendBuildLine("FAILURE: Build failed with ${errors.size} error(s).", isError = true)
                _uiState.update { it.copy(buildStatus = BuildStatus.FAILED) }
                repository.updateBuildStatus(project.id, BuildStatus.FAILED)
            } else {
                appendBuildLine("> Task :app:dexBuilderDebug")
                delay(180)
                appendBuildLine("> Task :app:packageDebug")
                appendBuildLine("BUILD SUCCESSFUL in 1s • 5 tasks executed", isSuccess = true)
                appendLogcat(
                    LogLevel.INFO,
                    "ActivityManager",
                    "Launched ${project.packageName}/.MainActivity in Live Preview",
                )
                _uiState.update { state ->
                    val nextMode = if (state.workspaceMode == WorkspaceMode.CODE) {
                        WorkspaceMode.SPLIT
                    } else {
                        state.workspaceMode
                    }
                    state.copy(
                        buildStatus = BuildStatus.SUCCESS,
                        workspaceMode = nextMode,
                    )
                }
                repository.updateBuildStatus(project.id, BuildStatus.SUCCESS)
            }
        }
    }

    private fun appendBuildLine(text: String, isError: Boolean = false, isSuccess: Boolean = false) {
        _uiState.update {
            it.copy(buildOutput = it.buildOutput + BuildOutputLine(text, isError = isError, isSuccess = isSuccess))
        }
    }

    fun onPreviewActionClick(actionCode: String) {
        val currentStates = _uiState.value.previewStateMap
        val (updated, logMsg) = ComposePreviewParser.applyClickAction(actionCode, currentStates)
        _uiState.update { it.copy(previewStateMap = updated) }
        if (logMsg != null) {
            appendLogcat(LogLevel.DEBUG, "ComposePreview", logMsg)
        }
    }

    fun onPreviewStateChange(varName: String, newValue: Any) {
        _uiState.update { state ->
            state.copy(previewStateMap = state.previewStateMap + (varName to newValue))
        }
        appendLogcat(LogLevel.DEBUG, "ComposePreview", "$varName = $newValue")
    }

    fun resetPreviewState() {
        val parsed = _uiState.value.parsedPreview
        _uiState.update { it.copy(previewStateMap = parsed?.initialStates ?: emptyMap()) }
        appendLogcat(LogLevel.INFO, "ComposePreview", "Preview state reset to initial defaults")
    }

    fun togglePreviewTheme() {
        _uiState.update { it.copy(previewDarkTheme = !it.previewDarkTheme) }
    }

    fun setDeviceFrame(frame: PreviewDeviceFrame) {
        _uiState.update { it.copy(selectedDeviceFrame = frame) }
    }

    fun setWorkspaceMode(mode: WorkspaceMode) {
        _uiState.update { it.copy(workspaceMode = mode) }
    }

    fun toggleBottomToolTab(tab: BottomToolTab) {
        _uiState.update { state ->
            val next = if (state.bottomToolTab == tab) BottomToolTab.NONE else tab
            state.copy(bottomToolTab = next)
        }
    }

    fun setLogcatLevelFilter(level: LogLevel) {
        _uiState.update { it.copy(logcatLevelFilter = level) }
    }

    fun setLogcatSearchQuery(query: String) {
        _uiState.update { it.copy(logcatSearchQuery = query) }
    }

    fun clearLogcat() {
        _uiState.update { it.copy(logcatEntries = emptyList()) }
    }

    fun setShowNewProjectDialog(show: Boolean) {
        _uiState.update { it.copy(showNewProjectDialog = show) }
    }

    fun setShowNewFileDialog(show: Boolean) {
        _uiState.update { it.copy(showNewFileDialog = show) }
    }

    fun setShowSettingsDialog(show: Boolean) {
        _uiState.update { it.copy(showSettingsDialog = show) }
    }

    fun updatePreferences(transform: (EditorPreferences) -> EditorPreferences) {
        viewModelScope.launch { repository.updatePreferences(transform) }
    }

    fun executeTerminalCommand(rawCommand: String) {
        val cmd = rawCommand.trim()
        if (cmd.isEmpty()) return

        val project = _uiState.value.activeProject
        val files = _uiState.value.projectFiles
        val outLines = mutableListOf(TerminalLine("$ $cmd", isCommand = true))

        when {
            cmd == "clear" -> {
                _uiState.update { it.copy(terminalLines = emptyList()) }
                return
            }
            cmd == "help" -> {
                outLines += TerminalLine("Available StudioDroid commands:", isAccent = true)
                outLines += TerminalLine("  ls                 List files in current project")
                outLines += TerminalLine("  tree               Display hierarchical project structure")
                outLines += TerminalLine("  cat <filename>     Print source contents of a file")
                outLines += TerminalLine("  gradle assembleDebug  Run static build & launch preview")
                outLines += TerminalLine("  sdk                Show configured minSdk/targetSdk info")
                outLines += TerminalLine("  clear              Clear terminal buffer")
            }
            cmd == "ls" -> {
                if (files.isEmpty()) {
                    outLines += TerminalLine("(no files in workspace)")
                } else {
                    files.forEach { outLines += TerminalLine("  ${it.fullPath}") }
                }
            }
            cmd == "tree" -> {
                outLines += TerminalLine(project?.name ?: "workspace", isAccent = true)
                files.forEach { outLines += TerminalLine("├── ${it.fullPath}") }
            }
            cmd.startsWith("cat ") -> {
                val targetName = cmd.removePrefix("cat ").trim()
                val match = files.firstOrNull {
                    it.name.equals(targetName, ignoreCase = true) ||
                        it.fullPath.equals(targetName, ignoreCase = true)
                }
                if (match == null) {
                    outLines += TerminalLine("cat: $targetName: No such file in project", isError = true)
                } else {
                    match.content.lines().take(28).forEach { line ->
                        outLines += TerminalLine(line)
                    }
                    if (match.content.lines().size > 28) {
                        outLines += TerminalLine("... (${match.content.lines().size - 28} more lines)", isAccent = true)
                    }
                }
            }
            cmd == "sdk" -> {
                outLines += TerminalLine(
                    "Project: ${project?.name ?: "-"} | Package: ${project?.packageName ?: "-"}",
                    isAccent = true,
                )
                outLines += TerminalLine(
                    "minSdk=${project?.minSdk ?: 24}  targetSdk=${project?.targetSdk ?: 36}  compileSdk=36",
                )
            }
            cmd.startsWith("gradle") || cmd.startsWith("./gradlew") -> {
                outLines += TerminalLine("Delegating '$cmd' to Gradle Daemon...", isAccent = true)
                _uiState.update { it.copy(terminalLines = it.terminalLines + outLines) }
                runProject()
                return
            }
            else -> {
                outLines += TerminalLine("zsh: command not found: $cmd (try 'help')", isError = true)
            }
        }

        _uiState.update { it.copy(terminalLines = it.terminalLines + outLines) }
    }

    private fun appendLogcat(level: LogLevel, tag: String, message: String) {
        val formatter = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
        val entry = LogcatEntry(
            id = nextLogId++,
            timestamp = formatter.format(Date()),
            pid = 4812,
            level = level,
            tag = tag,
            message = message,
        )
        _uiState.update { state ->
            state.copy(logcatEntries = (state.logcatEntries + entry).takeLast(120))
        }
    }

    private fun pickPreviewSourceFile(
        activeFile: IdeFileEntity?,
        allFiles: List<IdeFileEntity>,
    ): IdeFileEntity? {
        if (activeFile != null && activeFile.fileType == IdeFileType.KOTLIN &&
            activeFile.content.contains("@Composable")
        ) {
            return activeFile
        }
        return allFiles.firstOrNull { it.name == "MainActivity.kt" }
            ?: allFiles.firstOrNull { it.fileType == IdeFileType.KOTLIN && it.content.contains("@Composable") }
    }

    private fun lineStartOffset(text: String, oneBasedLine: Int): Int {
        var line = 1
        var index = 0
        while (index < text.length && line < oneBasedLine) {
            if (text[index] == '\n') line++
            index++
        }
        return index.coerceIn(0, text.length)
    }

    private fun flushPendingSave() {
        val activeId = _uiState.value.activeFileId ?: return
        val text = _uiState.value.editorValue.text
        saveDebounceJob?.cancel()
        viewModelScope.launch {
            repository.saveFileContent(activeId, text)
        }
    }

    class Factory(private val repository: IdeRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return IdeViewModel(repository) as T
        }
    }
}
