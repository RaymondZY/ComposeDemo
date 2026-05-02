package zhaoyun.example.composedemo.story.input.domain

import zhaoyun.example.composedemo.scaffold.core.usecase.BaseUseCase
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder

class InputUseCase(
    stateHolder: StateHolder<InputState>,
) : BaseUseCase<InputState, InputEvent, InputEffect>(
    stateHolder,
) {
    override suspend fun onEvent(event: InputEvent) {
        when (event) {
            is InputEvent.OnFocused -> {
                updateState { it.copy(isFocused = true) }
            }
            is InputEvent.OnInputClicked -> {
                // Placeholder for future implementation
            }
            is InputEvent.OnSendClicked -> {
                // Placeholder for future implementation
            }
        }
    }
}
