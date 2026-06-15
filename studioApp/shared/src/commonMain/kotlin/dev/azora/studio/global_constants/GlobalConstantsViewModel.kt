package dev.azora.studio.global_constants

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.azora.sdk.core.domain.util.Res
import dev.azora.sdk.core.project.domain.*
import dev.azora.sdk.core.project.domain.globalConstants
import dev.azora.sdk.core.project.domain.withGlobalConstants
import dev.azora.sdk.core.project.domain.repository.AzoraProjectRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for managing global constants.
 */
class GlobalConstantsViewModel(
    private val projectPath: String,
    private val projectRepository: AzoraProjectRepository
) : ViewModel() {

    private val _state = MutableStateFlow(GlobalConstantsState())
    val state: StateFlow<GlobalConstantsState> = _state.asStateFlow()

    init {
        loadConstants()
    }

    private fun loadConstants() {
        viewModelScope.launch {
            when (val result = projectRepository.getProject()) {
                is Res.Success -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            constants = result.data.settings.globalConstants
                        )
                    }
                }
                is Res.Failure -> {
                    _state.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun onAction(action: GlobalConstantsAction) {
        when (action) {
            is GlobalConstantsAction.Add -> addConstant(action.type)
            is GlobalConstantsAction.Update -> updateConstant(action.constant)
            is GlobalConstantsAction.Remove -> removeConstant(action.constantId)
            is GlobalConstantsAction.Select -> selectConstant(action.constantId)
        }
    }

    private fun addConstant(type: ConstantType) {
        val existingNames = _state.value.constants.map { it.name }.toSet()
        val newConstant = GlobalConstant.createDefault(type, existingNames)
        _state.update { state ->
            state.copy(
                constants = state.constants + newConstant,
                selectedId = newConstant.id
            )
        }
        saveProject()
    }

    private fun updateConstant(constant: GlobalConstant) {
        _state.update { state ->
            state.copy(
                constants = state.constants.map {
                    if (it.id == constant.id) constant else it
                }
            )
        }
        saveProject()
    }

    private fun removeConstant(constantId: String) {
        _state.update { state ->
            state.copy(
                constants = state.constants.filter { it.id != constantId },
                selectedId = if (state.selectedId == constantId) null else state.selectedId
            )
        }
        saveProject()
    }

    private fun selectConstant(constantId: String?) {
        _state.update { it.copy(selectedId = constantId) }
    }

    private fun saveProject() {
        viewModelScope.launch {
            when (val result = projectRepository.getProject()) {
                is Res.Success -> {
                    val updatedProject = result.data.copy(
                        settings = result.data.settings.withGlobalConstants(_state.value.constants)
                    )
                    projectRepository.updateProject(updatedProject)
                    projectRepository.saveProject(projectPath)
                }
                is Res.Failure -> { /* Handle error */ }
            }
        }
    }
}
