package zhaoyun.example.composedemo.todo.presentation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import zhaoyun.example.composedemo.domain.model.TodoEvent
import zhaoyun.example.composedemo.domain.usecase.CheckLoginUseCase
import zhaoyun.example.composedemo.domain.usecase.TodoUseCases
import zhaoyun.example.composedemo.scaffold.core.mvi.BaseEffect
import zhaoyun.example.composedemo.service.usercenter.api.model.UserInfo
import zhaoyun.example.composedemo.service.usercenter.mock.FakeUserRepository

/**
 * TodoViewModel 纯单元测试 —— 完全独立于 Compose UI 框架与 DI 容器
 * 通过发送 Event、验证 State 变化与 Effect 输出，覆盖全部 8 个业务用例
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TodoViewModelTest {

    private val fakeRepository = FakeUserRepository()
    private lateinit var viewModel: TodoViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = TodoViewModel(TodoUseCases(), CheckLoginUseCase(fakeRepository))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModelWithEffectCollector(): Pair<TodoViewModel, MutableList<BaseEffect>> {
        val testViewModel = TodoViewModel(TodoUseCases(), CheckLoginUseCase(fakeRepository))
        val effects = mutableListOf<BaseEffect>()
        val scope = CoroutineScope(Dispatchers.Main)
        scope.launch {
            testViewModel.baseEffect.collect { effects.add(it) }
        }
        return testViewModel to effects
    }

    @Test
    fun `初始状态为空`() {
        val state = viewModel.state.value

        assertTrue(state.todos.isEmpty())
        assertEquals("", state.inputText)
        assertFalse(state.isInputValid)
        assertNull(state.isLoggedIn)
    }

    @Test
    fun `添加Todo后状态更新并发送副作用`() {
        val (testViewModel, effects) = createViewModelWithEffectCollector()

        // 输入 BuyMilk
        testViewModel.onEvent(TodoEvent.OnInputTextChanged("BuyMilk"))
        assertEquals("BuyMilk", testViewModel.state.value.inputText)
        assertTrue(testViewModel.state.value.isInputValid)

        // 点击添加
        testViewModel.onEvent(TodoEvent.OnAddTodoClicked)

        assertEquals(1, testViewModel.state.value.todos.size)
        assertEquals("BuyMilk", testViewModel.state.value.todos[0].title)
        assertFalse(testViewModel.state.value.todos[0].isCompleted)
        assertEquals("", testViewModel.state.value.inputText)
        assertFalse(testViewModel.state.value.isInputValid)
        assertEquals(listOf(BaseEffect.ShowToast("添加成功")), effects)

        // 输入 WriteCode
        testViewModel.onEvent(TodoEvent.OnInputTextChanged("WriteCode"))
        testViewModel.onEvent(TodoEvent.OnAddTodoClicked)

        assertEquals(2, testViewModel.state.value.todos.size)
        assertEquals("WriteCode", testViewModel.state.value.todos[1].title)
        assertEquals(
            listOf(
                BaseEffect.ShowToast("添加成功"),
                BaseEffect.ShowToast("添加成功")
            ),
            effects
        )
    }

    @Test
    fun `输入验证`() {
        // 空输入
        assertFalse(viewModel.state.value.isInputValid)

        // 有效输入
        viewModel.onEvent(TodoEvent.OnInputTextChanged("abc"))
        assertTrue(viewModel.state.value.isInputValid)

        // 清空
        viewModel.onEvent(TodoEvent.OnInputTextChanged(""))
        assertFalse(viewModel.state.value.isInputValid)

        // 纯空格（isBlank => false）
        viewModel.onEvent(TodoEvent.OnInputTextChanged("   "))
        assertFalse(viewModel.state.value.isInputValid)
    }

    @Test
    fun `勾选与取消勾选`() {
        val (testViewModel, _) = createViewModelWithEffectCollector()

        // 添加两个 Todo
        testViewModel.onEvent(TodoEvent.OnInputTextChanged("TaskA"))
        testViewModel.onEvent(TodoEvent.OnAddTodoClicked)
        testViewModel.onEvent(TodoEvent.OnInputTextChanged("TaskB"))
        testViewModel.onEvent(TodoEvent.OnAddTodoClicked)

        val todoA = testViewModel.state.value.todos[0]
        val todoB = testViewModel.state.value.todos[1]

        // 勾选 TaskA
        testViewModel.onEvent(TodoEvent.OnTodoCheckedChanged(todoA.id, true))
        assertTrue(testViewModel.state.value.todos[0].isCompleted)
        assertFalse(testViewModel.state.value.todos[1].isCompleted)

        // 取消勾选 TaskA
        testViewModel.onEvent(TodoEvent.OnTodoCheckedChanged(todoA.id, false))
        assertFalse(testViewModel.state.value.todos[0].isCompleted)
        assertFalse(testViewModel.state.value.todos[1].isCompleted)
    }

    @Test
    fun `删除单个Todo`() {
        val (testViewModel, effects) = createViewModelWithEffectCollector()

        // 添加两个 Todo
        testViewModel.onEvent(TodoEvent.OnInputTextChanged("TaskA"))
        testViewModel.onEvent(TodoEvent.OnAddTodoClicked)
        testViewModel.onEvent(TodoEvent.OnInputTextChanged("TaskB"))
        testViewModel.onEvent(TodoEvent.OnAddTodoClicked)

        val todoA = testViewModel.state.value.todos[0]

        // 删除 TaskA
        testViewModel.onEvent(TodoEvent.OnTodoDeleteClicked(todoA.id))

        assertEquals(1, testViewModel.state.value.todos.size)
        assertEquals("TaskB", testViewModel.state.value.todos[0].title)
        assertEquals(
            listOf(
                BaseEffect.ShowToast("添加成功"),
                BaseEffect.ShowToast("添加成功"),
                BaseEffect.ShowToast("已删除")
            ),
            effects
        )

        // 删除 TaskB
        val todoB = testViewModel.state.value.todos[0]
        testViewModel.onEvent(TodoEvent.OnTodoDeleteClicked(todoB.id))

        assertTrue(testViewModel.state.value.todos.isEmpty())
        assertEquals(
            listOf(
                BaseEffect.ShowToast("添加成功"),
                BaseEffect.ShowToast("添加成功"),
                BaseEffect.ShowToast("已删除"),
                BaseEffect.ShowToast("已删除")
            ),
            effects
        )
    }

    @Test
    fun `清除所有已完成`() {
        val (testViewModel, effects) = createViewModelWithEffectCollector()

        // 添加三个 Todo
        testViewModel.onEvent(TodoEvent.OnInputTextChanged("TaskA"))
        testViewModel.onEvent(TodoEvent.OnAddTodoClicked)
        testViewModel.onEvent(TodoEvent.OnInputTextChanged("TaskB"))
        testViewModel.onEvent(TodoEvent.OnAddTodoClicked)
        testViewModel.onEvent(TodoEvent.OnInputTextChanged("TaskC"))
        testViewModel.onEvent(TodoEvent.OnAddTodoClicked)

        val todoA = testViewModel.state.value.todos[0]
        val todoB = testViewModel.state.value.todos[1]

        // 勾选 TaskA 和 TaskB
        testViewModel.onEvent(TodoEvent.OnTodoCheckedChanged(todoA.id, true))
        testViewModel.onEvent(TodoEvent.OnTodoCheckedChanged(todoB.id, true))

        // 清除已完成
        testViewModel.onEvent(TodoEvent.OnClearCompletedClicked)

        assertEquals(1, testViewModel.state.value.todos.size)
        assertEquals("TaskC", testViewModel.state.value.todos[0].title)
        assertFalse(testViewModel.state.value.todos[0].isCompleted)

        val expectedEffects = listOf(
            BaseEffect.ShowToast("添加成功"),
            BaseEffect.ShowToast("添加成功"),
            BaseEffect.ShowToast("添加成功"),
            BaseEffect.ShowToast("已清除 2 个已完成任务")
        )
        assertEquals(expectedEffects, effects)
    }

    @Test
    fun `长文本输入`() {
        val (testViewModel, effects) = createViewModelWithEffectCollector()

        val longText = (
            "ThisIsAVeryLongTodoItemTitleToTestLongTextDisplayInTheListAndInputField" +
                "ToEnsureTheAppDoesNotCrashOrHaveLayoutIssuesWithLongContent"
            )

        testViewModel.onEvent(TodoEvent.OnInputTextChanged(longText))
        testViewModel.onEvent(TodoEvent.OnAddTodoClicked)

        assertEquals(1, testViewModel.state.value.todos.size)
        assertEquals(longText, testViewModel.state.value.todos[0].title)
        assertEquals(listOf(BaseEffect.ShowToast("添加成功")), effects)
    }

    @Test
    fun `综合端到端流程`() {
        val (testViewModel, effects) = createViewModelWithEffectCollector()

        // 1. 添加 BuyMilk 和 WriteCode
        testViewModel.onEvent(TodoEvent.OnInputTextChanged("BuyMilk"))
        testViewModel.onEvent(TodoEvent.OnAddTodoClicked)
        testViewModel.onEvent(TodoEvent.OnInputTextChanged("WriteCode"))
        testViewModel.onEvent(TodoEvent.OnAddTodoClicked)

        val buyMilk = testViewModel.state.value.todos[0]
        val writeCode = testViewModel.state.value.todos[1]

        // 2. 勾选 BuyMilk
        testViewModel.onEvent(TodoEvent.OnTodoCheckedChanged(buyMilk.id, true))
        assertTrue(testViewModel.state.value.todos[0].isCompleted)

        // 3. 添加 DoSport
        testViewModel.onEvent(TodoEvent.OnInputTextChanged("DoSport"))
        testViewModel.onEvent(TodoEvent.OnAddTodoClicked)

        // 4. 取消 BuyMilk，勾选 WriteCode
        testViewModel.onEvent(TodoEvent.OnTodoCheckedChanged(buyMilk.id, false))
        testViewModel.onEvent(TodoEvent.OnTodoCheckedChanged(writeCode.id, true))
        assertFalse(testViewModel.state.value.todos[0].isCompleted)
        assertTrue(testViewModel.state.value.todos[1].isCompleted)

        // 5. 清除已完成（WriteCode）
        testViewModel.onEvent(TodoEvent.OnClearCompletedClicked)

        assertEquals(2, testViewModel.state.value.todos.size)
        assertEquals("BuyMilk", testViewModel.state.value.todos[0].title)
        assertEquals("DoSport", testViewModel.state.value.todos[1].title)
        assertFalse(testViewModel.state.value.todos[0].isCompleted)
        assertFalse(testViewModel.state.value.todos[1].isCompleted)

        // 6. 删除 BuyMilk
        testViewModel.onEvent(TodoEvent.OnTodoDeleteClicked(testViewModel.state.value.todos[0].id))

        // 7. 删除 DoSport
        testViewModel.onEvent(TodoEvent.OnTodoDeleteClicked(testViewModel.state.value.todos[0].id))

        assertTrue(testViewModel.state.value.todos.isEmpty())

        val expectedEffects = listOf(
            BaseEffect.ShowToast("添加成功"),
            BaseEffect.ShowToast("添加成功"),
            BaseEffect.ShowToast("添加成功"),
            BaseEffect.ShowToast("已清除 1 个已完成任务"),
            BaseEffect.ShowToast("已删除"),
            BaseEffect.ShowToast("已删除")
        )
        assertEquals(expectedEffects, effects)
    }

    @Test
    fun `清除已完成但无已完成任务时不发送副作用`() {
        val (testViewModel, effects) = createViewModelWithEffectCollector()

        // 添加一个未完成的 Todo（会产生"添加成功"副作用）
        testViewModel.onEvent(TodoEvent.OnInputTextChanged("TaskA"))
        testViewModel.onEvent(TodoEvent.OnAddTodoClicked)

        // 清除已完成（没有任何已完成任务）——不应产生新的副作用
        testViewModel.onEvent(TodoEvent.OnClearCompletedClicked)

        assertEquals(1, testViewModel.state.value.todos.size)
        assertEquals("TaskA", testViewModel.state.value.todos[0].title)
        assertFalse(testViewModel.state.value.todos[0].isCompleted)
        // 只有"添加成功"一个副作用，没有清除相关的副作用
        assertEquals(listOf(BaseEffect.ShowToast("添加成功")), effects)
    }
}
