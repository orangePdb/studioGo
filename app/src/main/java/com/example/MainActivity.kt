package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.IdeDatabase
import com.example.data.repository.IdeRepository
import com.example.ui.components.EditorSettingsDialog
import com.example.ui.components.NewFileDialog
import com.example.ui.components.NewProjectDialog
import com.example.ui.screens.IdeWorkspaceScreen
import com.example.ui.screens.WelcomeHubScreen
import com.example.ui.theme.StudioDroidTheme
import com.example.ui.viewmodel.IdeViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = IdeDatabase.getInstance(applicationContext)
        val repository = IdeRepository(applicationContext, database.ideDao())

        setContent {
            val viewModel: IdeViewModel = viewModel(
                factory = remember { IdeViewModel.Factory(repository) },
            )
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val preferences by viewModel.editorPreferences.collectAsStateWithLifecycle()

            StudioDroidTheme(darkTheme = preferences.darkTheme) {
                Crossfade(
                    targetState = uiState.activeProject != null,
                    label = "screenCrossfade",
                    modifier = Modifier.fillMaxSize(),
                ) { inWorkspace ->
                    if (inWorkspace) {
                        IdeWorkspaceScreen(
                            uiState = uiState,
                            preferences = preferences,
                            viewModel = viewModel,
                        )
                    } else {
                        WelcomeHubScreen(
                            projects = uiState.projects,
                            onOpenProject = viewModel::openProject,
                            onNewProjectClick = { viewModel.setShowNewProjectDialog(true) },
                            onQuickCreateTemplate = { template ->
                                val cleanTitle = template.title.replace(" ", "")
                                viewModel.createProject(
                                    name = cleanTitle,
                                    packageName = "com.studio.${cleanTitle.lowercase()}",
                                    minSdk = 24,
                                    template = template,
                                )
                            },
                            onDeleteProject = viewModel::deleteProject,
                            onOpenSettings = { viewModel.setShowSettingsDialog(true) },
                        )
                    }
                }

                if (uiState.showNewProjectDialog) {
                    NewProjectDialog(
                        onDismiss = { viewModel.setShowNewProjectDialog(false) },
                        onCreate = { name, pkg, minSdk, template ->
                            viewModel.createProject(name, pkg, minSdk, template)
                        },
                    )
                }

                if (uiState.showNewFileDialog && uiState.activeProject != null) {
                    NewFileDialog(
                        packageName = uiState.activeProject!!.packageName,
                        onDismiss = { viewModel.setShowNewFileDialog(false) },
                        onCreate = viewModel::createFile,
                    )
                }

                if (uiState.showSettingsDialog) {
                    EditorSettingsDialog(
                        preferences = preferences,
                        onUpdatePreferences = viewModel::updatePreferences,
                        onDismiss = { viewModel.setShowSettingsDialog(false) },
                    )
                }
            }
        }
    }
}
