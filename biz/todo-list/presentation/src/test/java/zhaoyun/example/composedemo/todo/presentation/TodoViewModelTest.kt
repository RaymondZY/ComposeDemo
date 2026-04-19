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
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import zhaoyun.example.composedemo.domain.model.TodoEffect
import zhaoyun.example.composedemo.domain.model.TodoEvent
import zhaoyun.example.composedemo.domain.usecase.TodoUseCases

/**
 * TodoViewModel 纯单元测试 —— 完全独立于 Compose UI 框架
 * 通过发送 Event、验证 State 变化与 Effect 输出，覆盖全部 8 个业务用例
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TodoViewModelTest {

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        startKoin {
            modules(
                module { factory { TodoUseCases() } }
            )
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        stopKoin()
    }

    private fun createViewModelWithEffectCollector(): Pair<TodoViewModel, MutableList<TodoEffect>> {
        val viewModel = TodoViewModel()
        val effects = mutableListOf<TodoEffect>()
        val scope = CoroutineScope(Dispatchers.Main)
        scope.launch {
            viewModel.effect.collect { effects.add(it) }
        }
        return viewModel to effects
    }

    
    @Test
    fun 初始状态为空() {
        val viewModel = TodoViewModel()
        val state = viewModel.state.value

        assertTrue(state.todos.isEmpty())
        assertEquals("", state.inputText)
        assertFalse(state.isInputValid)
        assertNull(state.isLoggedIn)
    }

    
    @Test
    fun 添加Todo后状态更新并发送副作用() {
        val (viewModel, effects) = createViewModelWithEffectCollector()

        // 输入 BuyMilk
        viewModel.onEvent(TodoEvent.OnInputTextChanged("BuyMilk"))
        assertEquals("BuyMilk", viewModel.state.value.inputText)
        assertTrue(viewModel.state.value.isInputValid)

        // 点击添加
        viewModel.onEvent(TodoEvent.OnAddTodoClicked)

        assertEquals(1, viewModel.state.value.todos.size)
        assertEquals("BuyMilk", viewModel.state.value.todos[0].title)
        assertFalse(viewModel.state.value.todos[0].isCompleted)
        assertEquals("", viewModel.state.value.inputText)
        assertFalse(viewModel.state.value.isInputValid)
        assertEquals(listOf(TodoEffect.ShowToast("添加成功")), effects)

        // 输入 WriteCode
        viewModel.onEvent(TodoEvent.OnInputTextChanged("WriteCode"))
        viewModel.onEvent(TodoEvent.OnAddTodoClicked)

        assertEquals(2, viewModel.state.value.todos.size)
        assertEquals("WriteCode", viewModel.state.value.todos[1].title)
        assertEquals(
            listOf(
                TodoEffect.ShowToast("添加成功"),
                TodoEffect.ShowToast("添加成功")
            ),
            effects
        )
    }

    
    @Test
    fun 输入验证() {
        val viewModel = TodoViewModel()

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
    fun 勾选与取消勾选() {
        val (viewModel, _) = createViewModelWithEffectCollector()

        // 添加两个 Todo
        viewModel.onEvent(TodoEvent.OnInputTextChanged("TaskA"))
        viewModel.onEvent(TodoEvent.OnAddTodoClicked)
        viewModel.onEvent(TodoEvent.OnInputTextChanged("TaskB"))
        viewModel.onEvent(TodoEvent.OnAddTodoClicked)

        val todoA = viewModel.state.value.todos[0]
        val todoB = viewModel.state.value.todos[1]

        // 勾选 TaskA
        viewModel.onEvent(TodoEvent.OnTodoCheckedChanged(todoA.id, true))
        assertTrue(viewModel.state.value.todos[0].isCompleted)
        assertFalse(viewModel.state.value.todos[1].isCompleted)

        // 取消勾选 TaskA
        viewModel.onEvent(TodoEvent.OnTodoCheckedChanged(todoA.id, false))
        assertFalse(viewModel.state.value.todos[0].isCompleted)
        assertFalse(viewModel.state.value.todos[1].isCompleted)
    }

    
    @Test
    fun 删除单个Todo() {
        val (viewModel, effects) = createViewModelWithEffectCollector()

        // 添加两个 Todo
        viewModel.onEvent(TodoEvent.OnInputTextChanged("TaskA"))
        viewModel.onEvent(TodoEvent.OnAddTodoClicked)
        viewModel.onEvent(TodoEvent.OnInputTextChanged("TaskB"))
        viewModel.onEvent(TodoEvent.OnAddTodoClicked)

        val todoA = viewModel.state.value.todos[0]

        // 删除 TaskA
        viewModel.onEvent(TodoEvent.OnTodoDeleteClicked(todoA.id))

        assertEquals(1, viewModel.state.value.todos.size)
        assertEquals("TaskB", viewModel.state.value.todos[0].title)
        assertEquals(
            listOf(
                TodoEffect.ShowToast("添加成功"),
                TodoEffect.ShowToast("添加成功"),
                TodoEffect.ShowToast("已删除")
            ),
            effects
        )

        // 删除 TaskB
        val todoB = viewModel.state.value.todos[0]
        viewModel.onEvent(TodoEvent.OnTodoDeleteClicked(todoB.id))

        assertTrue(viewModel.state.value.todos.isEmpty())
        assertEquals(
            listOf(
                TodoEffect.ShowToast("添加成功"),
                TodoEffect.ShowToast("添加成功"),
                TodoEffect.ShowToast("已删除"),
                TodoEffect.ShowToast("已删除")
            ),
            effects
        )
    }

    
    @Test
    fun 清除所有已完成() {
        val (viewModel, effects) = createViewModelWithEffectCollector()

        // 添加三个 Todo
        viewModel.onEvent(TodoEvent.OnInputTextChanged("TaskA"))
        viewModel.onEvent(TodoEvent.OnAddTodoClicked)
        viewModel.onEvent(TodoEvent.OnInputTextChanged("TaskB"))
        viewModel.onEvent(TodoEvent.OnAddTodoClicked)
        viewModel.onEvent(TodoEvent.OnInputTextChanged("TaskC"))
        viewModel.onEvent(TodoEvent.OnAddTodoClicked)

        val todoA = viewModel.state.value.todos[0]
        val todoB = viewModel.state.value.todos[1]

        // 勾选 TaskA 和 TaskB
        viewModel.onEvent(TodoEvent.OnTodoCheckedChanged(todoA.id, true))
        viewModel.onEvent(TodoEvent.OnTodoCheckedChanged(todoB.id, true))

        // 清除已完成
        viewModel.onEvent(TodoEvent.OnClearCompletedClicked)

        assertEquals(1, viewModel.state.value.todos.size)
        assertEquals("TaskC", viewModel.state.value.todos[0].title)
        assertFalse(viewModel.state.value.todos[0].isCompleted)

        val expectedEffects = listOf(
            TodoEffect.ShowToast("添加成功"),
            TodoEffect.ShowToast("添加成功"),
            TodoEffect.ShowToast("添加成功"),
            TodoEffect.ShowToast("已清除 2 个已完成任务")
        )
        assertEquals(expectedEffects, effects)
    }

    
    @Test
    fun 长文本输入() {
        val (viewModel, effects) = createViewModelWithEffectCollector()

        val longText = (
            "ThisIsAVeryLongTodoItemTitleToTestLongTextDisplayInTheListAndInputField" +
            "ToEnsureTheAppDoesNotCrashOrHaveLayoutIssuesWithLongContent"
        )

        viewModel.onEvent(TodoEvent.OnInputTextChanged(longText))
        viewModel.onEvent(TodoEvent.OnAddTodoClicked)

        assertEquals(1, viewModel.state.value.todos.size)
        assertEquals(longText, viewModel.state.value.todos[0].title)
        assertEquals(listOf(TodoEffect.ShowToast("添加成功")), effects)
    }

    
    @Test
    fun 综合端到端流程() {
        val (viewModel, effects) = createViewModelWithEffectCollector()

        // 1. 添加 BuyMilk 和 WriteCode
        viewModel.onEvent(TodoEvent.OnInputTextChanged("BuyMilk"))
        viewModel.onEvent(TodoEvent.OnAddTodoClicked)
        viewModel.onEvent(TodoEvent.OnInputTextChanged("WriteCode"))
        viewModel.onEvent(TodoEvent.OnAddTodoClicked)

        val buyMilk = viewModel.state.value.todos[0]
        val writeCode = viewModel.state.value.todos[1]

        // 2. 勾选 BuyMilk
        viewModel.onEvent(TodoEvent.OnTodoCheckedChanged(buyMilk.id, true))
        assertTrue(viewModel.state.value.todos[0].isCompleted)

        // 3. 添加 DoSport
        viewModel.onEvent(TodoEvent.OnInputTextChanged("DoSport"))
        viewModel.onEvent(TodoEvent.OnAddTodoClicked)

        // 4. 取消 BuyMilk，勾选 WriteCode
        viewModel.onEvent(TodoEvent.OnTodoCheckedChanged(buyMilk.id, false))
        viewModel.onEvent(TodoEvent.OnTodoCheckedChanged(writeCode.id, true))
        assertFalse(viewModel.state.value.todos[0].isCompleted)
        assertTrue(viewModel.state.value.todos[1].isCompleted)

        // 5. 清除已完成（WriteCode）
        viewModel.onEvent(TodoEvent.OnClearCompletedClicked)

        assertEquals(2, viewModel.state.value.todos.size)
        assertEquals("BuyMilk", viewModel.state.value.todos[0].title)
        assertEquals("DoSport", viewModel.state.value.todos[1].title)
        assertFalse(viewModel.state.value.todos[0].isCompleted)
        assertFalse(viewModel.state.value.todos[1].isCompleted)

        // 6. 删除 BuyMilk
        viewModel.onEvent(TodoEvent.OnTodoDeleteClicked(viewModel.state.value.todos[0].id))

        // 7. 删除 DoSport
        viewModel.onEvent(TodoEvent.OnTodoDeleteClicked(viewModel.state.value.todos[0].id))

        assertTrue(viewModel.state.value.todos.isEmpty())

        val expectedEffects = listOf(
            TodoEffect.ShowToast("添加成功"),
            TodoEffect.ShowToast("添加成功"),
            TodoEffect.ShowToast("添加成功"),
            TodoEffect.ShowToast("已清除 1 个已完成任务"),
            TodoEffect.ShowToast("已删除"),
            TodoEffect.ShowToast("已删除")
        )
        assertEquals(expectedEffects, effects)
    }

    
    @Test
    fun 清除已完成但无已完成任务时不发送副作用() {
        val (viewModel, effects) = createViewModelWithEffectCollector()

        // 添加一个未完成的 Todo（会产生"添加成功"副作用）
        viewModel.onEvent(TodoEvent.OnInputTextChanged("TaskA"))
        viewModel.onEvent(TodoEvent.OnAddTodoClicked)

        // 清除已完成（没有任何已完成任务）——不应产生新的副作用
        viewModel.onEvent(TodoEvent.OnClearCompletedClicked)

        assertEquals(1, viewModel.state.value.todos.size)
        assertEquals("TaskA", viewModel.state.value.todos[0].title)
        assertFalse(viewModel.state.value.todos[0].isCompleted)
        // 只有"添加成功"一个副作用，没有清除相关的副作用
        assertEquals(listOf(TodoEffect.ShowToast("添加成功")), effects)
    }
}
