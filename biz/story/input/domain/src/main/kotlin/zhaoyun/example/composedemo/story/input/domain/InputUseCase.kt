package zhaoyun.example.composedemo.story.input.domain

import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistry
import zhaoyun.example.composedemo.scaffold.core.usecase.BaseUseCase

class InputUseCase(
    stateHolder: StateHolder<InputState>,
    serviceRegistry: MutableServiceRegistry,
) : BaseUseCase<InputState, InputEvent, InputEffect>(
    stateHolder,
    serviceRegistry,
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
