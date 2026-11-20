package com.example.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.EditorPreferences
import com.example.data.model.ProjectTemplate
import com.example.ui.theme.JetBrainsMonoFontFamily

@Composable
fun NewProjectDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, packageName: String, minSdk: Int, template: ProjectTemplate) -> Unit,
) {
    var projectName by remember { mutableStateOf("MyComposeApp") }
    var packageName by remember { mutableStateOf("com.studio.mycomposeapp") }
    var selectedMinSdk by remember { mutableIntStateOf(24) }
    var selectedTemplate by remember { mutableStateOf(ProjectTemplate.EMPTY_COMPOSE) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "New Android Project",
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = projectName,
                    onValueChange = {
                        projectName = it
                        val sanitized = it.lowercase().replace(Regex("[^a-z0-9]"), "")
                        if (sanitized.isNotEmpty()) {
                            packageName = "com.studio.$sanitized"
                        }
                    },
                    label = { Text("Project Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_project_name_input"),
                )

                OutlinedTextField(
                    value = packageName,
                    onValueChange = { packageName = it },
                    label = { Text("Package Name") },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_project_package_input"),
                )

                Text(
                    text = "Minimum SDK",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(24, 26, 28, 31).forEach { sdk ->
                        val selected = sdk == selectedMinSdk
                        Surface(
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedMinSdk = sdk },
                        ) {
                            Text(
                                text = "API $sdk",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            )
                        }
                    }
                }

                Text(
                    text = "Choose Activity Template",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                ProjectTemplate.entries.forEach { template ->
                    val selected = template == selectedTemplate
                    Surface(
                        color = if (selected) {
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                width = if (selected) 1.5.dp else 1.dp,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(12.dp),
                            )
                            .clickable { selectedTemplate = template },
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = template.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f),
                                )
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(6.dp),
                                ) {
                                    Text(
                                        text = template.badge,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = template.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(projectName, packageName, selectedMinSdk, selectedTemplate) },
                enabled = projectName.isNotBlank() && packageName.isNotBlank(),
                modifier = Modifier.testTag("confirm_create_project_button"),
            ) {
                Text("Finish & Open")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
fun NewFileDialog(
    packageName: String,
    onDismiss: () -> Unit,
    onCreate: (folderPath: String, fileName: String) -> Unit,
) {
    val javaFolder = "app/src/main/java/${packageName.replace('.', '/')}"
    val folders = remember(javaFolder) {
        listOf(
            javaFolder,
            "app/src/main/res/values",
            "app/src/main/res/layout",
            "app",
        )
    }
    var selectedFolder by remember { mutableStateOf(javaFolder) }
    var fileName by remember { mutableStateOf("HomeScreen.kt") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New File") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    label = { Text("File Name (.kt, .xml, .kts)") },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_file_name_input"),
                )
                Text(
                    text = "Target Directory",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                folders.forEach { folder ->
                    val selected = folder == selectedFolder
                    Surface(
                        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedFolder = folder },
                    ) {
                        Text(
                            text = folder,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(selectedFolder, fileName) },
                enabled = fileName.isNotBlank(),
                modifier = Modifier.testTag("confirm_create_file_button"),
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
fun EditorSettingsDialog(
    preferences: EditorPreferences,
    onUpdatePreferences: ((EditorPreferences) -> EditorPreferences) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("IDE & Editor Settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SettingToggleRow(
                    title = "Dark IDE Theme",
                    subtitle = "JetBrains Islands Dark vs Light Studio",
                    checked = preferences.darkTheme,
                    onCheckedChange = { checked ->
                        onUpdatePreferences { it.copy(darkTheme = checked) }
                    },
                )
                SettingToggleRow(
                    title = "Show Line Numbers",
                    subtitle = "Display gutter line indices & error markers",
                    checked = preferences.showLineNumbers,
                    onCheckedChange = { checked ->
                        onUpdatePreferences { it.copy(showLineNumbers = checked) }
                    },
                )
                SettingToggleRow(
                    title = "Soft Word Wrap",
                    subtitle = "Wrap long lines within editor viewport",
                    checked = preferences.wordWrap,
                    onCheckedChange = { checked ->
                        onUpdatePreferences { it.copy(wordWrap = checked) }
                    },
                )
                SettingToggleRow(
                    title = "Show Layout Bounds",
                    subtitle = "Draw Compose bounding boxes in Live Preview",
                    checked = preferences.showLayoutBounds,
                    onCheckedChange = { checked ->
                        onUpdatePreferences { it.copy(showLayoutBounds = checked) }
                    },
                )
                Column {
                    Text(
                        text = "Editor Font Size: ${preferences.fontSizeSp}sp",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Slider(
                        value = preferences.fontSizeSp.toFloat(),
                        onValueChange = { value ->
                            onUpdatePreferences { it.copy(fontSizeSp = value.toInt()) }
                        },
                        valueRange = 11f..18f,
                        steps = 6,
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Done")
            }
        },
    )
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
