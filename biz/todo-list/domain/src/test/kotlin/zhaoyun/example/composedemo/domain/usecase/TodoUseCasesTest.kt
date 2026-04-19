package zhaoyun.example.composedemo.domain.usecase

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
import zhaoyun.example.composedemo.service.usercenter.api.UserRepository
import zhaoyun.example.composedemo.service.usercenter.api.model.UserInfo
import zhaoyun.example.composedemo.service.usercenter.mock.FakeUserRepository

/**
 * TodoUseCases 纯单元测试 —— 完全独立于任何平台框架
 * 通过发送 Event、验证 State 变化与 Effect 输出，覆盖全部 8 个业务用例
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TodoUseCasesTest {

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        startKoin {
            modules(testUserModule)
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        stopKoin()
    }

    private fun createUseCaseWithEffectCollector(): Pair<TodoUseCases, MutableList<TodoEffect>> {
        val useCase = TodoUseCases()
        val effects = mutableListOf<TodoEffect>()
        val scope = CoroutineScope(Dispatchers.Main)
        scope.launch {
            useCase.effect.collect { effects.add(it) }
        }
        return useCase to effects
    }

    
    @Test
    fun 初始状态为空() {
        val useCase = TodoUseCases()
        val state = useCase.state.value

        assertTrue(state.todos.isEmpty())
        assertEquals("", state.inputText)
        assertFalse(state.isInputValid)
        assertNull(state.isLoggedIn)
    }

    
    @Test
    fun 添加Todo后状态更新并发送副作用() {
        val (useCase, effects) = createUseCaseWithEffectCollector()

        // 输入 BuyMilk
        useCase.onEvent(TodoEvent.OnInputTextChanged("BuyMilk"))
        assertEquals("BuyMilk", useCase.state.value.inputText)
        assertTrue(useCase.state.value.isInputValid)

        // 点击添加
        useCase.onEvent(TodoEvent.OnAddTodoClicked)

        assertEquals(1, useCase.state.value.todos.size)
        assertEquals("BuyMilk", useCase.state.value.todos[0].title)
        assertFalse(useCase.state.value.todos[0].isCompleted)
        assertEquals("", useCase.state.value.inputText)
        assertFalse(useCase.state.value.isInputValid)
        assertEquals(listOf(TodoEffect.ShowToast("添加成功")), effects)

        // 输入 WriteCode
        useCase.onEvent(TodoEvent.OnInputTextChanged("WriteCode"))
        useCase.onEvent(TodoEvent.OnAddTodoClicked)

        assertEquals(2, useCase.state.value.todos.size)
        assertEquals("WriteCode", useCase.state.value.todos[1].title)
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
        val useCase = TodoUseCases()

        // 空输入
        assertFalse(useCase.state.value.isInputValid)

        // 有效输入
        useCase.onEvent(TodoEvent.OnInputTextChanged("abc"))
        assertTrue(useCase.state.value.isInputValid)

        // 清空
        useCase.onEvent(TodoEvent.OnInputTextChanged(""))
        assertFalse(useCase.state.value.isInputValid)

        // 纯空格（isBlank => false）
        useCase.onEvent(TodoEvent.OnInputTextChanged("   "))
        assertFalse(useCase.state.value.isInputValid)
    }

    
    @Test
    fun 勾选与取消勾选() {
        val (useCase, _) = createUseCaseWithEffectCollector()

        // 添加两个 Todo
        useCase.onEvent(TodoEvent.OnInputTextChanged("TaskA"))
        useCase.onEvent(TodoEvent.OnAddTodoClicked)
        useCase.onEvent(TodoEvent.OnInputTextChanged("TaskB"))
        useCase.onEvent(TodoEvent.OnAddTodoClicked)

        val todoA = useCase.state.value.todos[0]
        val todoB = useCase.state.value.todos[1]

        // 勾选 TaskA
        useCase.onEvent(TodoEvent.OnTodoCheckedChanged(todoA.id, true))
        assertTrue(useCase.state.value.todos[0].isCompleted)
        assertFalse(useCase.state.value.todos[1].isCompleted)

        // 取消勾选 TaskA
        useCase.onEvent(TodoEvent.OnTodoCheckedChanged(todoA.id, false))
        assertFalse(useCase.state.value.todos[0].isCompleted)
        assertFalse(useCase.state.value.todos[1].isCompleted)
    }

    
    @Test
    fun 删除单个Todo() {
        val (useCase, effects) = createUseCaseWithEffectCollector()

        // 添加两个 Todo
        useCase.onEvent(TodoEvent.OnInputTextChanged("TaskA"))
        useCase.onEvent(TodoEvent.OnAddTodoClicked)
        useCase.onEvent(TodoEvent.OnInputTextChanged("TaskB"))
        useCase.onEvent(TodoEvent.OnAddTodoClicked)

        val todoA = useCase.state.value.todos[0]

        // 删除 TaskA
        useCase.onEvent(TodoEvent.OnTodoDeleteClicked(todoA.id))

        assertEquals(1, useCase.state.value.todos.size)
        assertEquals("TaskB", useCase.state.value.todos[0].title)
        assertEquals(
            listOf(
                TodoEffect.ShowToast("添加成功"),
                TodoEffect.ShowToast("添加成功"),
                TodoEffect.ShowToast("已删除")
            ),
            effects
        )

        // 删除 TaskB
        val todoB = useCase.state.value.todos[0]
        useCase.onEvent(TodoEvent.OnTodoDeleteClicked(todoB.id))

        assertTrue(useCase.state.value.todos.isEmpty())
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
        val (useCase, effects) = createUseCaseWithEffectCollector()

        // 添加三个 Todo
        useCase.onEvent(TodoEvent.OnInputTextChanged("TaskA"))
        useCase.onEvent(TodoEvent.OnAddTodoClicked)
        useCase.onEvent(TodoEvent.OnInputTextChanged("TaskB"))
        useCase.onEvent(TodoEvent.OnAddTodoClicked)
        useCase.onEvent(TodoEvent.OnInputTextChanged("TaskC"))
        useCase.onEvent(TodoEvent.OnAddTodoClicked)

        val todoA = useCase.state.value.todos[0]
        val todoB = useCase.state.value.todos[1]

        // 勾选 TaskA 和 TaskB
        useCase.onEvent(TodoEvent.OnTodoCheckedChanged(todoA.id, true))
        useCase.onEvent(TodoEvent.OnTodoCheckedChanged(todoB.id, true))

        // 清除已完成
        useCase.onEvent(TodoEvent.OnClearCompletedClicked)

        assertEquals(1, useCase.state.value.todos.size)
        assertEquals("TaskC", useCase.state.value.todos[0].title)
        assertFalse(useCase.state.value.todos[0].isCompleted)

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
        val (useCase, effects) = createUseCaseWithEffectCollector()

        val longText = (
            "ThisIsAVeryLongTodoItemTitleToTestLongTextDisplayInTheListAndInputField" +
            "ToEnsureTheAppDoesNotCrashOrHaveLayoutIssuesWithLongContent"
        )

        useCase.onEvent(TodoEvent.OnInputTextChanged(longText))
        useCase.onEvent(TodoEvent.OnAddTodoClicked)

        assertEquals(1, useCase.state.value.todos.size)
        assertEquals(longText, useCase.state.value.todos[0].title)
        assertEquals(listOf(TodoEffect.ShowToast("添加成功")), effects)
    }

    
    @Test
    fun 综合端到端流程() {
        val (useCase, effects) = createUseCaseWithEffectCollector()

        // 1. 添加 BuyMilk 和 WriteCode
        useCase.onEvent(TodoEvent.OnInputTextChanged("BuyMilk"))
        useCase.onEvent(TodoEvent.OnAddTodoClicked)
        useCase.onEvent(TodoEvent.OnInputTextChanged("WriteCode"))
        useCase.onEvent(TodoEvent.OnAddTodoClicked)

        val buyMilk = useCase.state.value.todos[0]
        val writeCode = useCase.state.value.todos[1]

        // 2. 勾选 BuyMilk
        useCase.onEvent(TodoEvent.OnTodoCheckedChanged(buyMilk.id, true))
        assertTrue(useCase.state.value.todos[0].isCompleted)

        // 3. 添加 DoSport
        useCase.onEvent(TodoEvent.OnInputTextChanged("DoSport"))
        useCase.onEvent(TodoEvent.OnAddTodoClicked)

        // 4. 取消 BuyMilk，勾选 WriteCode
        useCase.onEvent(TodoEvent.OnTodoCheckedChanged(buyMilk.id, false))
        useCase.onEvent(TodoEvent.OnTodoCheckedChanged(writeCode.id, true))
        assertFalse(useCase.state.value.todos[0].isCompleted)
        assertTrue(useCase.state.value.todos[1].isCompleted)

        // 5. 清除已完成（WriteCode）
        useCase.onEvent(TodoEvent.OnClearCompletedClicked)

        assertEquals(2, useCase.state.value.todos.size)
        assertEquals("BuyMilk", useCase.state.value.todos[0].title)
        assertEquals("DoSport", useCase.state.value.todos[1].title)
        assertFalse(useCase.state.value.todos[0].isCompleted)
        assertFalse(useCase.state.value.todos[1].isCompleted)

        // 6. 删除 BuyMilk
        useCase.onEvent(TodoEvent.OnTodoDeleteClicked(useCase.state.value.todos[0].id))

        // 7. 删除 DoSport
        useCase.onEvent(TodoEvent.OnTodoDeleteClicked(useCase.state.value.todos[0].id))

        assertTrue(useCase.state.value.todos.isEmpty())

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
        val (useCase, effects) = createUseCaseWithEffectCollector()

        // 添加一个未完成的 Todo（会产生"添加成功"副作用）
        useCase.onEvent(TodoEvent.OnInputTextChanged("TaskA"))
        useCase.onEvent(TodoEvent.OnAddTodoClicked)

        // 清除已完成（没有任何已完成任务）——不应产生新的副作用
        useCase.onEvent(TodoEvent.OnClearCompletedClicked)

        assertEquals(1, useCase.state.value.todos.size)
        assertEquals("TaskA", useCase.state.value.todos[0].title)
        assertFalse(useCase.state.value.todos[0].isCompleted)
        // 只有"添加成功"一个副作用，没有清除相关的副作用
        assertEquals(listOf(TodoEffect.ShowToast("添加成功")), effects)
    }

    
    @Test
    fun 初始未登录状态检查() {
        val useCase = TodoUseCases()

        useCase.onEvent(TodoEvent.CheckLogin)

        assertFalse(useCase.state.value.isLoggedIn!!)
    }

    
    @Test
    fun 登录后状态检查() {
        val useCase = TodoUseCases()
        fakeRepository.setLoggedInUser(UserInfo("u_1", "alice", "Alice"))

        useCase.onEvent(TodoEvent.CheckLogin)

        assertTrue(useCase.state.value.isLoggedIn!!)
    }

    
    @Test
    fun 登出后恢复未登录状态() {
        val useCase = TodoUseCases()
        fakeRepository.setLoggedInUser(UserInfo("u_1", "alice", "Alice"))
        useCase.onEvent(TodoEvent.CheckLogin)
        assertTrue(useCase.state.value.isLoggedIn!!)

        fakeRepository.logout()
        useCase.onEvent(TodoEvent.CheckLogin)

        assertFalse(useCase.state.value.isLoggedIn!!)
    }

    // ========== Test Double ==========

    private val fakeRepository = FakeUserRepository()

    private val testUserModule = module {
        single<UserRepository> { fakeRepository }
        single { CheckLoginUseCase() }
    }
}
