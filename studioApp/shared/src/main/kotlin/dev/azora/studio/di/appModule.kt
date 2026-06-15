package org.azora.studio.di

import org.azora.studio.assets.OpenAzScriptFilesManager
import org.azora.studio.assets.OpenAzoraNodesFilesManager
import org.azora.studio.assets.OpenAzoraSceneFilesManager
import org.azora.studio.assets.OpenAzoraTileMapFilesManager
import org.azora.studio.az_script.DiagnosticsManager
import org.azora.studio.azora_nodes.AzoraNodesViewModel
import org.azora.studio.editor.StudioViewModel
import org.azora.studio.global_constants.GlobalConstantsViewModel
import org.azora.studio.project_manager.ProjectManagerViewModel
import org.azora.studio.settings.SettingsViewModel
import org.azora.sdk.docking.data.di.dockingDataModule
import org.azora.sdk.core.project.domain.AzoraProjectModel
import kotlinx.coroutines.*
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Main application dependency injection module using Koin.
 *
 * This module provides core application-level dependencies that are available
 * throughout the app's lifecycle.
 */
val appModule = module {
    includes(dockingDataModule)

    /**
     * Provides a singleton application-level CoroutineScope.
     *
     * This scope uses:
     * - SupervisorJob: Allows child coroutines to fail independently without
     *   cancelling the parent scope or sibling coroutines
     * - Dispatchers.Default: Optimized for CPU-intensive work, uses a shared
     *   pool of background threads
     *
     * Use this scope for long-running operations that should survive beyond
     * individual screen lifecycles but should be cancelled when the app terminates.
     */
    single {
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    single { OpenAzoraNodesFilesManager(get()) }
    single { OpenAzoraSceneFilesManager(get()) }
    single { OpenAzoraTileMapFilesManager(get()) }
    single { OpenAzScriptFilesManager(get()) }
    single { DiagnosticsManager() }

    viewModelOf(::ProjectManagerViewModel)

    viewModel { (project: AzoraProjectModel, projectPath: String) ->
        StudioViewModel(project, projectPath, get(), get(), get())
    }

    viewModel { (projectPath: String) ->
        SettingsViewModel(projectPath, get(), get())
    }

    viewModel { (projectPath: String) ->
        GlobalConstantsViewModel(projectPath, get())
    }

    single { AzoraNodesViewModel(get(), get(), get()) }
}
