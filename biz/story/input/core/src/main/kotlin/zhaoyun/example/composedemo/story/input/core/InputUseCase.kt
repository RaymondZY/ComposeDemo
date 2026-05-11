package zhaoyun.example.composedemo.story.input.core

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

    // UC-02：coordinator 广播时调用。发出 ClearFocus 命令而不是直接改 state，
    // 让 isFocused 始终由 onFocusChanged 单向上报；接收侧（InputArea）需自行判断
    // TextField 是否真的持有焦点再执行全局 clearFocus，避免离屏 page 误清当前焦点。
    override fun dismissKeyboard() {
        dispatchEffect(InputEffect.ClearFocus)
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
