package com.example.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.local.IdeDao
import com.example.data.local.IdeFileEntity
import com.example.data.local.IdeProjectEntity
import com.example.data.local.ProjectTemplates
import com.example.data.local.ProjectWithStats
import com.example.data.model.BuildStatus
import com.example.data.model.EditorPreferences
import com.example.data.model.IdeFileType
import com.example.data.model.ProjectTemplate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

private val Context.studioPrefsDataStore by preferencesDataStore(name = "studiodroid_prefs")

class IdeRepository(
    private val context: Context,
    private val dao: IdeDao,
) {
    private object PrefKeys {
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val FONT_SIZE_SP = intPreferencesKey("font_size_sp")
        val WORD_WRAP = booleanPreferencesKey("word_wrap")
        val SHOW_LINE_NUMBERS = booleanPreferencesKey("show_line_numbers")
        val SHOW_LAYOUT_BOUNDS = booleanPreferencesKey("show_layout_bounds")
    }

    val editorPreferences: Flow<EditorPreferences> = context.studioPrefsDataStore.data.map { prefs ->
        EditorPreferences(
            darkTheme = prefs[PrefKeys.DARK_THEME] ?: true,
            fontSizeSp = prefs[PrefKeys.FONT_SIZE_SP] ?: 13,
            wordWrap = prefs[PrefKeys.WORD_WRAP] ?: false,
            showLineNumbers = prefs[PrefKeys.SHOW_LINE_NUMBERS] ?: true,
            showLayoutBounds = prefs[PrefKeys.SHOW_LAYOUT_BOUNDS] ?: false,
        )
    }

    val allProjectsWithStats: Flow<List<ProjectWithStats>> = combine(
        dao.observeAllProjects(),
        dao.observeAllFiles(),
    ) { projects, files ->
        val counts = files.groupingBy { it.projectId }.eachCount()
        projects.map { project ->
            ProjectWithStats(
                project = project,
                fileCount = counts[project.id] ?: 0,
            )
        }
    }

    fun observeProject(projectId: Long): Flow<IdeProjectEntity?> = dao.observeProject(projectId)

    fun observeFilesForProject(projectId: Long): Flow<List<IdeFileEntity>> =
        dao.observeFilesForProject(projectId)

    suspend fun ensureStarterProjectsSeeded() {
        if (dao.getProjectCount() > 0) return

        createProject(
            name = "ComposeCounter",
            packageName = "com.studio.counter",
            minSdk = 24,
            targetSdk = 36,
            template = ProjectTemplate.EMPTY_COMPOSE,
        )
        createProject(
            name = "TaskPulse",
            packageName = "com.studio.taskpulse",
            minSdk = 26,
            targetSdk = 36,
            template = ProjectTemplate.TASK_PULSE,
        )
        createProject(
            name = "AtmosphereCard",
            packageName = "com.studio.weather",
            minSdk = 24,
            targetSdk = 36,
            template = ProjectTemplate.WEATHER_WIDGET,
        )
    }

    suspend fun createProject(
        name: String,
        packageName: String,
        minSdk: Int,
        targetSdk: Int,
        template: ProjectTemplate,
    ): Long {
        val now = System.currentTimeMillis()
        val projectId = dao.insertProject(
            IdeProjectEntity(
                name = name.trim(),
                packageName = packageName.trim(),
                minSdk = minSdk,
                targetSdk = targetSdk,
                template = template,
                lastBuildStatus = BuildStatus.IDLE,
                updatedAt = now,
            )
        )
        val starterFiles = ProjectTemplates.buildFilesForTemplate(
            projectId = projectId,
            projectName = name.trim(),
            packageName = packageName.trim(),
            minSdk = minSdk,
            targetSdk = targetSdk,
            template = template,
        )
        dao.insertFiles(starterFiles)
        return projectId
    }

    suspend fun deleteProject(projectId: Long) {
        dao.deleteProjectById(projectId)
    }

    suspend fun createFile(
        projectId: Long,
        folderPath: String,
        fileName: String,
        packageName: String,
    ): Long {
        val cleanName = fileName.trim()
        val fileType = IdeFileType.fromFileName(cleanName)
        val starterContent = when (fileType) {
            IdeFileType.KOTLIN -> {
                val baseName = cleanName.removeSuffix(".kt").ifBlank { "NewScreen" }
                """
package $packageName

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Preview
@Composable
fun ${baseName}Screen() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "$baseName",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF3DDC84)
        )
        Text(
            text = "Created in StudioDroid",
            fontSize = 13.sp,
            color = Color(0xFF94A3B8)
        )
    }
}
                """.trimIndent()
            }
            IdeFileType.XML -> """
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="item_label">$cleanName</string>
</resources>
            """.trimIndent()
            IdeFileType.GRADLE -> """
// Additional Gradle configuration
dependencies {
    // implementation("androidx.core:core-ktx:1.18.0")
}
            """.trimIndent()
            IdeFileType.JSON -> "{\n  \"module\": \"$cleanName\"\n}"
            IdeFileType.PROPERTIES -> "org.gradle.jvmargs=-Xmx2048m\n"
        }

        return dao.insertFile(
            IdeFileEntity(
                projectId = projectId,
                folderPath = folderPath.trim('/'),
                name = cleanName,
                fileType = fileType,
                content = starterContent,
                updatedAt = System.currentTimeMillis(),
            )
        )
    }

    suspend fun saveFileContent(fileId: Long, content: String) {
        dao.updateFileContent(fileId, content, System.currentTimeMillis())
    }

    suspend fun deleteFile(fileId: Long) {
        dao.deleteFileById(fileId)
    }

    suspend fun updateBuildStatus(projectId: Long, status: BuildStatus) {
        dao.updateProjectBuildStatus(projectId, status, System.currentTimeMillis())
    }

    suspend fun updatePreferences(transform: (EditorPreferences) -> EditorPreferences) {
        context.studioPrefsDataStore.edit { prefs ->
            val current = EditorPreferences(
                darkTheme = prefs[PrefKeys.DARK_THEME] ?: true,
                fontSizeSp = prefs[PrefKeys.FONT_SIZE_SP] ?: 13,
                wordWrap = prefs[PrefKeys.WORD_WRAP] ?: false,
                showLineNumbers = prefs[PrefKeys.SHOW_LINE_NUMBERS] ?: true,
                showLayoutBounds = prefs[PrefKeys.SHOW_LAYOUT_BOUNDS] ?: false,
            )
            val updated = transform(current)
            prefs[PrefKeys.DARK_THEME] = updated.darkTheme
            prefs[PrefKeys.FONT_SIZE_SP] = updated.fontSizeSp
            prefs[PrefKeys.WORD_WRAP] = updated.wordWrap
            prefs[PrefKeys.SHOW_LINE_NUMBERS] = updated.showLineNumbers
            prefs[PrefKeys.SHOW_LAYOUT_BOUNDS] = updated.showLayoutBounds
        }
    }
}
