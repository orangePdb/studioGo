package com.example.data.model

enum class IdeFileType(val extension: String, val displayLabel: String) {
    KOTLIN("kt", "Kotlin"),
    XML("xml", "XML"),
    GRADLE("kts", "Gradle KTS"),
    JSON("json", "JSON"),
    PROPERTIES("properties", "Properties");

    companion object {
        fun fromFileName(name: String): IdeFileType {
            val lower = name.lowercase()
            return when {
                lower.endsWith(".gradle.kts") -> GRADLE
                lower.endsWith(".kt") -> KOTLIN
                lower.endsWith(".xml") -> XML
                lower.endsWith(".json") -> JSON
                lower.endsWith(".properties") -> PROPERTIES
                else -> KOTLIN
            }
        }
    }
}

enum class ProjectTemplate(
    val title: String,
    val subtitle: String,
    val badge: String,
) {
    EMPTY_COMPOSE(
        title = "Empty Compose Activity",
        subtitle = "Jetpack Compose Material 3 interactive starter with live state preview",
        badge = "Compose M3",
    ),
    TASK_PULSE(
        title = "TaskPulse Dashboard",
        subtitle = "Card-based productivity layout with status toggles and action buttons",
        badge = "UI Kit",
    ),
    WEATHER_WIDGET(
        title = "Atmosphere Weather Card",
        subtitle = "Sleek gradient weather card with temperature slider and forecast rows",
        badge = "Surface Lab",
    ),
    BLANK_MODULE(
        title = "Blank Kotlin Module",
        subtitle = "Minimal MainActivity.kt, AndroidManifest.xml, and Gradle script",
        badge = "Minimal",
    ),
}

enum class BuildStatus {
    IDLE,
    BUILDING,
    SUCCESS,
    FAILED,
}

enum class DiagnosticSeverity {
    ERROR,
    WARNING,
    INFO,
}

data class CodeDiagnostic(
    val fileId: Long,
    val fileName: String,
    val line: Int,
    val message: String,
    val severity: DiagnosticSeverity,
)

enum class LogLevel(val shortCode: String, val label: String) {
    VERBOSE("V", "Verbose"),
    DEBUG("D", "Debug"),
    INFO("I", "Info"),
    WARN("W", "Warn"),
    ERROR("E", "Error"),
}

data class LogcatEntry(
    val id: Long,
    val timestamp: String,
    val pid: Int,
    val level: LogLevel,
    val tag: String,
    val message: String,
)

data class BuildOutputLine(
    val text: String,
    val isError: Boolean = false,
    val isSuccess: Boolean = false,
)

data class TerminalLine(
    val text: String,
    val isCommand: Boolean = false,
    val isError: Boolean = false,
    val isAccent: Boolean = false,
)

enum class WorkspaceMode(val label: String) {
    CODE("Code"),
    SPLIT("Split"),
    PREVIEW("Preview"),
}

enum class BottomToolTab(val label: String) {
    NONE("Hidden"),
    PROJECT("Project"),
    BUILD("Build"),
    LOGCAT("Logcat"),
    PROBLEMS("Problems"),
    TERMINAL("Terminal"),
}

enum class PreviewDeviceFrame(val label: String) {
    PIXEL_PHONE("Pixel 9"),
    TABLET_COMPACT("Medium Tablet"),
}

data class EditorPreferences(
    val darkTheme: Boolean = true,
    val fontSizeSp: Int = 13,
    val wordWrap: Boolean = false,
    val showLineNumbers: Boolean = true,
    val showLayoutBounds: Boolean = false,
)
