package zhaoyun.example.composedemo.story.input.domain

import zhaoyun.example.composedemo.scaffold.core.mvi.BaseUseCase

class InputUseCase : BaseUseCase<InputState, InputEvent, InputEffect>(
    InputState()
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
