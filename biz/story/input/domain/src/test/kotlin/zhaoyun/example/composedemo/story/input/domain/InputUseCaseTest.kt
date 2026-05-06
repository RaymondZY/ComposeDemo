package zhaoyun.example.composedemo.story.input.domain

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import zhaoyun.example.composedemo.scaffold.core.mvi.toStateHolder
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistryImpl

@OptIn(ExperimentalCoroutinesApi::class)
class InputUseCaseTest {

    private lateinit var coordinator: InputKeyboardCoordinator
    private lateinit var useCase: InputUseCase

    @Before
    fun setup() {
        coordinator = InputKeyboardCoordinator()
        val registry = MutableServiceRegistryImpl()
        // 手动注册 coordinator，模拟 Koin global 兜底的效果
        registry.register(InputKeyboardCoordinator::class.java, coordinator)
        useCase = InputUseCase(InputState().toStateHolder(), registry)
    }

    // UC-01
    @Test
    fun `test_UC01_点击后isFocused变为true`() = runTest {
        useCase.receiveEvent(InputEvent.OnFocusChanged(true))
        assertTrue(useCase.state.value.isFocused)
    }

    // UC-02：dismissKeyboard 不再直接改 state，而是发出 ClearFocus 命令
    @Test
    fun `test_UC02_coordinator调用后发出ClearFocus_effect`() = runTest {
        useCase.receiveEvent(InputEvent.OnFocusChanged(true))

        val effects = mutableListOf<InputEffect>()
        val collectJob = launch { useCase.effect.collect { effects += it } }

        coordinator.requestDismiss()
        advanceUntilIdle()
        collectJob.cancel()

        assertTrue(effects.any { it is InputEffect.ClearFocus })
    }

    // UC-02：收起命令不影响已输入文字
    @Test
    fun `test_UC02_收起后文字内容保留`() = runTest {
        useCase.receiveEvent(InputEvent.OnTextChanged("hello"))
        useCase.receiveEvent(InputEvent.OnFocusChanged(true))
        coordinator.requestDismiss()
        assertEquals("hello", useCase.state.value.text)
    }

    // UC-06
    @Test
    fun `test_UC06_点击括号在末尾追加全角括号`() = runTest {
        useCase.receiveEvent(InputEvent.OnTextChanged("你好"))
        useCase.receiveEvent(InputEvent.OnBracketClicked)
        assertEquals("你好（）", useCase.state.value.text)
    }

    // UC-06
    @Test
    fun `test_UC06_光标位置在括号中间`() = runTest {
        useCase.receiveEvent(InputEvent.OnTextChanged("你好"))

        val effects = mutableListOf<InputEffect>()
        val collectJob = launch { useCase.effect.collect { effects += it } }

        useCase.receiveEvent(InputEvent.OnBracketClicked)
        advanceUntilIdle()
        collectJob.cancel()

        val effect = effects.filterIsInstance<InputEffect.InsertBrackets>().first()
        assertEquals("你好（）", effect.newText)
        // cursorPosition 指向 ）之前，即 length - 1
        assertEquals("你好（）".length - 1, effect.cursorPosition)
    }

    // UC-09
    @Test
    fun `test_UC09_有文字时text非空`() = runTest {
        useCase.receiveEvent(InputEvent.OnTextChanged("x"))
        assertTrue(useCase.state.value.text.isNotEmpty())
    }

    // UC-09
    @Test
    fun `test_UC09_清空文字后text为空`() = runTest {
        useCase.receiveEvent(InputEvent.OnTextChanged("x"))
        useCase.receiveEvent(InputEvent.OnTextChanged(""))
        assertTrue(useCase.state.value.text.isEmpty())
    }

    // UC-07
    @Test
    fun `test_UC07_语音点击无状态副作用`() = runTest {
        val stateBefore = useCase.state.value
        useCase.receiveEvent(InputEvent.OnVoiceClicked)
        assertEquals(stateBefore, useCase.state.value)
    }

    // UC-08
    @Test
    fun `test_UC08_加号点击无状态副作用`() = runTest {
        val stateBefore = useCase.state.value
        useCase.receiveEvent(InputEvent.OnPlusClicked)
        assertEquals(stateBefore, useCase.state.value)
    }

    // UC-09
    @Test
    fun `test_UC09_send_click_has_no_state_side_effect`() = runTest {
        useCase.receiveEvent(InputEvent.OnTextChanged("x"))
        val stateBefore = useCase.state.value

        useCase.receiveEvent(InputEvent.OnSendClicked)

        assertEquals(stateBefore, useCase.state.value)
    }
}
