package com.example.data.local

import com.example.data.model.IdeFileType
import com.example.data.model.ProjectTemplate

object ProjectTemplates {

    fun buildFilesForTemplate(
        projectId: Long,
        projectName: String,
        packageName: String,
        minSdk: Int,
        targetSdk: Int,
        template: ProjectTemplate,
    ): List<IdeFileEntity> {
        val packageFolder = "app/src/main/java/${packageName.replace('.', '/')}"
        val now = System.currentTimeMillis()

        val mainActivityContent = when (template) {
            ProjectTemplate.EMPTY_COMPOSE -> emptyComposeMainActivity(projectName, packageName)
            ProjectTemplate.TASK_PULSE -> taskPulseMainActivity(projectName, packageName)
            ProjectTemplate.WEATHER_WIDGET -> weatherWidgetMainActivity(projectName, packageName)
            ProjectTemplate.BLANK_MODULE -> blankMainActivity(projectName, packageName)
        }

        val themeContent = """
package $packageName.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AppDarkColors = darkColorScheme(
    primary = Color(0xFF3DDC84),
    secondary = Color(0xFF4C8BF5),
    surface = Color(0xFF1B1E26),
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppDarkColors,
        content = content
    )
}
        """.trimIndent()

        val manifestContent = """
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <application
        android:allowBackup="true"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.App">
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
        """.trimIndent()

        val stringsContent = """
<resources>
    <string name="app_name">$projectName</string>
    <string name="welcome_subtitle">Built with StudioDroid IDE</string>
</resources>
        """.trimIndent()

        val gradleContent = """
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "$packageName"
    compileSdk = $targetSdk

    defaultConfig {
        applicationId = "$packageName"
        minSdk = $minSdk
        targetSdk = $targetSdk
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.10.1")
}
        """.trimIndent()

        return listOf(
            IdeFileEntity(
                projectId = projectId,
                folderPath = packageFolder,
                name = "MainActivity.kt",
                fileType = IdeFileType.KOTLIN,
                content = mainActivityContent,
                updatedAt = now,
            ),
            IdeFileEntity(
                projectId = projectId,
                folderPath = "$packageFolder/ui/theme",
                name = "Theme.kt",
                fileType = IdeFileType.KOTLIN,
                content = themeContent,
                updatedAt = now,
            ),
            IdeFileEntity(
                projectId = projectId,
                folderPath = "app/src/main",
                name = "AndroidManifest.xml",
                fileType = IdeFileType.XML,
                content = manifestContent,
                updatedAt = now,
            ),
            IdeFileEntity(
                projectId = projectId,
                folderPath = "app/src/main/res/values",
                name = "strings.xml",
                fileType = IdeFileType.XML,
                content = stringsContent,
                updatedAt = now,
            ),
            IdeFileEntity(
                projectId = projectId,
                folderPath = "app",
                name = "build.gradle.kts",
                fileType = IdeFileType.GRADLE,
                content = gradleContent,
                updatedAt = now,
            ),
        )
    }

    private fun emptyComposeMainActivity(projectName: String, packageName: String): String = """
package $packageName

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CounterAppScreen()
        }
    }
}

@Preview
@Composable
fun CounterAppScreen() {
    var count by remember { mutableStateOf(3) }
    var turboMode by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier.padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Bolt,
            contentDescription = "App Badge",
            tint = Color(0xFF3DDC84)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "$projectName",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFE6EAF2)
        )
        Text(
            text = "Interactive Jetpack Compose Preview",
            fontSize = 13.sp,
            color = Color(0xFF94A3B8)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            modifier = Modifier.padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Build Iterations: ${'$'}count",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF3DDC84)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row {
                    Button(onClick = { count++ }) {
                        Text(text = "+1 Build")
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(onClick = { count = 0 }) {
                        Text(text = "Reset")
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Switch(
            checked = turboMode,
            onCheckedChange = { turboMode = it },
            label = "Gradle Daemon Active"
        )
    }
}
    """.trimIndent()

    private fun taskPulseMainActivity(projectName: String, packageName: String): String = """
package $packageName

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TaskBoardScreen()
        }
    }
}

@Preview
@Composable
fun TaskBoardScreen() {
    var completed by remember { mutableStateOf(4) }
    var notifications by remember { mutableStateOf(true) }

    Column(modifier = Modifier.padding(18.dp)) {
        Text(
            text = "$projectName",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFA78BFA)
        )
        Text(
            text = "Sprint Overview • ${'$'}completed tasks shipped",
            fontSize = 13.sp,
            color = Color(0xFF94A3B8)
        )
        Spacer(modifier = Modifier.height(14.dp))
        Card(modifier = Modifier.padding(4.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Done",
                        tint = Color(0xFF3DDC84)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Configure Material 3 Theme",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Edge-to-edge WindowInsets verified on API 36",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = { completed++ }) {
            Text(text = "Complete Next Task")
        }
        Spacer(modifier = Modifier.height(10.dp))
        Switch(
            checked = notifications,
            onCheckedChange = { notifications = it },
            label = "CI/CD Webhook Alerts"
        )
    }
}
    """.trimIndent()

    private fun weatherWidgetMainActivity(projectName: String, packageName: String): String = """
package $packageName

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WeatherCardScreen()
        }
    }
}

@Preview
@Composable
fun WeatherCardScreen() {
    var tempC by remember { mutableStateOf(22) }
    var autoRefresh by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier.padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(modifier = Modifier.padding(6.dp)) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.WbSunny,
                    contentDescription = "Sunny",
                    tint = Color(0xFFF59E0B)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "San Francisco • ${'$'}tempC°C",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE6EAF2)
                )
                Text(
                    text = "Clear Skies • Wind 11 km/h",
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Row {
            Button(onClick = { tempC-- }) {
                Text(text = "-1°C")
            }
            Spacer(modifier = Modifier.width(12.dp))
            Button(onClick = { tempC++ }) {
                Text(text = "+1°C")
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Switch(
            checked = autoRefresh,
            onCheckedChange = { autoRefresh = it },
            label = "Hourly Forecast Sync"
        )
    }
}
    """.trimIndent()

    private fun blankMainActivity(projectName: String, packageName: String): String = """
package $packageName

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HelloScreen()
        }
    }
}

@Preview
@Composable
fun HelloScreen() {
    Column(modifier = Modifier.padding(20.dp)) {
        Text(
            text = "Hello $projectName!",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF3DDC84)
        )
        Text(
            text = "Edit MainActivity.kt to see live Compose updates.",
            fontSize = 14.sp,
            color = Color(0xFF94A3B8)
        )
    }
}
    """.trimIndent()
}
