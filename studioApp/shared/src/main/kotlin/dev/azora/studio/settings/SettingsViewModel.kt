package org.azora.studio.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.azora.sdk.core.domain.util.Res
import org.azora.sdk.core.project.domain.*
import org.azora.sdk.core.project.domain.repository.AzoraProjectRepository
import org.azora.sdk.core.project.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for managing project settings.
 */
class SettingsViewModel(
    private val projectPath: String,
    private val projectRepository: AzoraProjectRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            when (val result = projectRepository.getProject()) {
                is Res.Success -> {
                    val projectSettings = result.data.settings
                    val projectId = result.data.id
                    val settingsResult = settingsRepository.getSettings(projectId)
                    val settingsModel = if (settingsResult is Res.Success) settingsResult.data else SettingsModel.default(projectId)
                    val langPath = settingsModel.azoraLangPath.ifEmpty { detectAzoraLangPath() }
                    _state.update {
                        it.copy(
                            isLoading = false,
                            editorTooltipsEnabled = projectSettings.editorTooltipsEnabled,
                            tooltipDelaySeconds = projectSettings.tooltipDelaySeconds,
                            preferredColorPicker = projectSettings.preferredColorPicker,
                            paletteColors = projectSettings.paletteColors,
                            useKmpRenderer = projectSettings.sceneStudioUseKmp,
                            azScriptUsePastel = settingsModel.azScriptUsePastel,
                            azScriptBoldKeywords = settingsModel.azScriptBoldKeywords,
                            azScriptItalicPreprocessor = settingsModel.azScriptItalicPreprocessor,
                            azScriptUnderlineVariables = settingsModel.azScriptUnderlineVariables,
                            azoraLangPath = langPath,
                            azoraLangDetected = langPath.isNotEmpty(),
                            showRuntimeWarnings = settingsModel.showRuntimeWarnings
                        )
                    }
                }
                is Res.Failure -> {
                    _state.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.SetStudioTooltipsEnabled -> setStudioTooltipsEnabled(action.enabled)
            is SettingsAction.SetTooltipDelaySeconds -> setTooltipDelaySeconds(action.delay)
            is SettingsAction.SetPreferredColorPicker -> setPreferredColorPicker(action.mode)
            is SettingsAction.AddPaletteColor -> addPaletteColor(action.color)
            is SettingsAction.UpdatePaletteColor -> updatePaletteColor(action.color)
            is SettingsAction.RemovePaletteColor -> removePaletteColor(action.colorId)
            is SettingsAction.SetUseKmpRenderer -> setUseKmpRenderer(action.enabled)
            is SettingsAction.SetAzScriptUsePastel -> setAzScriptUsePastel(action.usePastel)
            is SettingsAction.SetAzScriptBoldKeywords -> setAzScriptBoldKeywords(action.boldKeywords)
            is SettingsAction.SetAzScriptItalicPreprocessor -> setAzScriptItalicPreprocessor(action.italic)
            is SettingsAction.SetAzScriptUnderlineVariables -> setAzScriptUnderlineVariables(action.underline)
            is SettingsAction.SetAzoraLangPath -> setAzoraLangPath(action.path)
            is SettingsAction.SetShowRuntimeWarnings -> setShowRuntimeWarnings(action.show)
        }
    }

    private fun setStudioTooltipsEnabled(enabled: Boolean) {
        _state.update { it.copy(editorTooltipsEnabled = enabled) }
        saveSettings()
    }

    private fun setTooltipDelaySeconds(delay: Int) {
        _state.update { it.copy(tooltipDelaySeconds = delay) }
        saveSettings()
    }

    private fun setPreferredColorPicker(mode: ColorPickerMode) {
        _state.update { it.copy(preferredColorPicker = mode) }
        saveSettings()
    }

    private fun addPaletteColor(color: PaletteColor) {
        _state.update { it.copy(paletteColors = it.paletteColors + color) }
        saveSettings()
    }

    private fun updatePaletteColor(color: PaletteColor) {
        _state.update { state ->
            state.copy(
                paletteColors = state.paletteColors.map {
                    if (it.id == color.id) color else it
                }
            )
        }
        saveSettings()
    }

    private fun setUseKmpRenderer(enabled: Boolean) {
        _state.update { it.copy(useKmpRenderer = enabled) }
        saveSettings()
    }

    private fun setAzScriptUsePastel(usePastel: Boolean) {
        _state.update { it.copy(azScriptUsePastel = usePastel) }
        saveSettings()
    }

    private fun setAzScriptBoldKeywords(boldKeywords: Boolean) {
        _state.update { it.copy(azScriptBoldKeywords = boldKeywords) }
        saveSettings()
    }

    private fun setAzScriptItalicPreprocessor(italic: Boolean) {
        _state.update { it.copy(azScriptItalicPreprocessor = italic) }
        saveSettings()
    }

    private fun setAzScriptUnderlineVariables(underline: Boolean) {
        _state.update { it.copy(azScriptUnderlineVariables = underline) }
        saveSettings()
    }

    private fun setAzoraLangPath(path: String) {
        val valid = path.isNotEmpty() && java.io.File(path, "lib").isDirectory
        _state.update { it.copy(azoraLangPath = path, azoraLangDetected = valid) }
        saveSettings()
    }

    private fun setShowRuntimeWarnings(show: Boolean) {
        _state.update { it.copy(showRuntimeWarnings = show) }
        saveSettings()
    }

    private fun removePaletteColor(colorId: String) {
        _state.update { state ->
            state.copy(paletteColors = state.paletteColors.filter { it.id != colorId })
        }
        saveSettings()
    }

    private fun detectAzoraLangPath(): String {
        val envHome = System.getenv("AZORA_HOME")
        if (envHome != null && java.io.File(envHome, "lib").isDirectory) return envHome
        val defaultHome = java.io.File(System.getProperty("user.home"), ".azoralang")
        if (defaultHome.isDirectory && java.io.File(defaultHome, "lib").isDirectory) return defaultHome.absolutePath
        return ""
    }

    private fun saveSettings() {
        viewModelScope.launch {
            when (val result = projectRepository.getProject()) {
                is Res.Success -> {
                    val currentState = _state.value
                    val projectId = result.data.id

                    // Save project-level settings
                    val updatedSettings = result.data.settings.copy(
                        editorTooltipsEnabled = currentState.editorTooltipsEnabled,
                        tooltipDelaySeconds = currentState.tooltipDelaySeconds,
                        preferredColorPicker = currentState.preferredColorPicker,
                        paletteColors = currentState.paletteColors
                    ).withSceneStudioUseKmp(currentState.useKmpRenderer)
                    val updatedProject = result.data.copy(settings = updatedSettings)
                    projectRepository.updateProject(updatedProject)
                    projectRepository.saveProject(projectPath)

                    // Save AzScript settings to SettingsRepository (observable)
                    val settingsResult = settingsRepository.getSettings(projectId)
                    val existing = if (settingsResult is Res.Success) settingsResult.data else SettingsModel.default(projectId)
                    settingsRepository.saveSettings(
                        existing.copy(
                            azScriptUsePastel = currentState.azScriptUsePastel,
                            azScriptBoldKeywords = currentState.azScriptBoldKeywords,
                            azScriptItalicPreprocessor = currentState.azScriptItalicPreprocessor,
                            azScriptUnderlineVariables = currentState.azScriptUnderlineVariables,
                            azoraLangPath = currentState.azoraLangPath,
                            showRuntimeWarnings = currentState.showRuntimeWarnings
                        )
                    )
                }
                is Res.Failure -> { /* Handle error */ }
            }
        }
    }
}
