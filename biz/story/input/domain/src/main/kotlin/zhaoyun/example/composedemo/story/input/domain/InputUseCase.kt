package zhaoyun.example.composedemo.story.input.domain

import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistry
import zhaoyun.example.composedemo.scaffold.core.spi.findServiceOrNull
import zhaoyun.example.composedemo.scaffold.core.usecase.BaseUseCase

class InputUseCase(
    stateHolder: StateHolder<InputState>,
    serviceRegistry: MutableServiceRegistry,
) : BaseUseCase<InputState, InputEvent, InputEffect>(stateHolder, serviceRegistry),
    InputFocusSpi {

    init {
        // ServiceRegistry → Koin global 兜底链；测试中直接注入 coordinator
        findServiceOrNull<InputKeyboardCoordinator>()?.register(this)
    }

    // UC-02：coordinator 广播时调用，仅修改 isFocused，保留 text
    override fun dismissKeyboard() {
        updateState { it.copy(isFocused = false) }
    }

    override suspend fun onEvent(event: InputEvent) {
        when (event) {
            is InputEvent.OnTextChanged ->
                updateState { it.copy(text = event.text) }

            is InputEvent.OnFocusChanged ->
                updateState { it.copy(isFocused = event.focused) }

            is InputEvent.OnBracketClicked -> {
                val newText = currentState.text + "（）"
                updateState { it.copy(text = newText) }
                dispatchEffect(InputEffect.InsertBrackets(newText, newText.length - 1))
            }

            is InputEvent.OnVoiceClicked -> Unit
            is InputEvent.OnPlusClicked -> Unit
            is InputEvent.OnSendClicked -> Unit
        }
    }
}
