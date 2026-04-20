package zhaoyun.example.composedemo.domain.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import zhaoyun.example.composedemo.domain.model.TodoEvent
import zhaoyun.example.composedemo.domain.model.TodoItem
import zhaoyun.example.composedemo.scaffold.core.mvi.BaseEffect
import zhaoyun.example.composedemo.service.usercenter.api.model.UserInfo
import zhaoyun.example.composedemo.service.usercenter.mock.FakeUserRepository

/**
 * TodoUseCases 纯单元测试 —— 完全独立于任何平台框架与 DI 容器
 * 通过发送 Event、验证 State 变化与 Effect 输出，覆盖全部 8 个业务用例
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TodoUseCasesTest {

    private val fakeRepository = FakeUserRepository()
    private lateinit var useCase: TodoUseCases

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        useCase = TodoUseCases(CheckLoginUseCase(fakeRepository))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.createUseCaseWithEffectCollector(): Pair<TodoUseCases, MutableList<BaseEffect>> {
        val testUseCase = TodoUseCases(CheckLoginUseCase(fakeRepository))
        val effects = mutableListOf<BaseEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            testUseCase.baseEffect.collect { effects.add(it) }
        }
        return testUseCase to effects
    }

    @Test
    fun 初始状态为空() {
        val state = useCase.state.value

        assertTrue(state.todos.isEmpty())
        assertEquals("", state.inputText)
        assertFalse(state.isInputValid)
        assertNull(state.isLoggedIn)
    }

    @Test
    fun 添加Todo后状态更新并发送副作用() = runTest {
        val (testUseCase, effects) = createUseCaseWithEffectCollector()

        // 输入 BuyMilk
        testUseCase.onEvent(TodoEvent.OnInputTextChanged("BuyMilk"))
        assertEquals("BuyMilk", testUseCase.state.value.inputText)
        assertTrue(testUseCase.state.value.isInputValid)

        // 点击添加
        testUseCase.onEvent(TodoEvent.OnAddTodoClicked)

        assertEquals(1, testUseCase.state.value.todos.size)
        assertEquals("BuyMilk", testUseCase.state.value.todos[0].title)
        assertFalse(testUseCase.state.value.todos[0].isCompleted)
        assertEquals("", testUseCase.state.value.inputText)
        assertFalse(testUseCase.state.value.isInputValid)
        assertEquals(listOf(BaseEffect.ShowToast("添加成功")), effects)

        // 输入 WriteCode
        testUseCase.onEvent(TodoEvent.OnInputTextChanged("WriteCode"))
        testUseCase.onEvent(TodoEvent.OnAddTodoClicked)

        assertEquals(2, testUseCase.state.value.todos.size)
        assertEquals("WriteCode", testUseCase.state.value.todos[1].title)
        assertEquals(
            listOf(
                BaseEffect.ShowToast("添加成功"),
                BaseEffect.ShowToast("添加成功")
            ),
            effects
        )
    }

    @Test
    fun 输入验证() = runTest {
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
    fun 勾选与取消勾选() = runTest {
        val (testUseCase, _) = createUseCaseWithEffectCollector()

        // 添加两个 Todo
        testUseCase.onEvent(TodoEvent.OnInputTextChanged("TaskA"))
        testUseCase.onEvent(TodoEvent.OnAddTodoClicked)
        testUseCase.onEvent(TodoEvent.OnInputTextChanged("TaskB"))
        testUseCase.onEvent(TodoEvent.OnAddTodoClicked)

        val todoA = testUseCase.state.value.todos[0]
        val todoB = testUseCase.state.value.todos[1]

        // 勾选 TaskA
        testUseCase.onEvent(TodoEvent.OnTodoCheckedChanged(todoA.id, true))
        assertTrue(testUseCase.state.value.todos[0].isCompleted)
        assertFalse(testUseCase.state.value.todos[1].isCompleted)

        // 取消勾选 TaskA
        testUseCase.onEvent(TodoEvent.OnTodoCheckedChanged(todoA.id, false))
        assertFalse(testUseCase.state.value.todos[0].isCompleted)
        assertFalse(testUseCase.state.value.todos[1].isCompleted)
    }

    @Test
    fun 删除单个Todo() = runTest {
        val (testUseCase, effects) = createUseCaseWithEffectCollector()

        // 添加两个 Todo
        testUseCase.onEvent(TodoEvent.OnInputTextChanged("TaskA"))
        testUseCase.onEvent(TodoEvent.OnAddTodoClicked)
        testUseCase.onEvent(TodoEvent.OnInputTextChanged("TaskB"))
        testUseCase.onEvent(TodoEvent.OnAddTodoClicked)

        val todoA = testUseCase.state.value.todos[0]

        // 删除 TaskA
        testUseCase.onEvent(TodoEvent.OnTodoDeleteClicked(todoA.id))

        assertEquals(1, testUseCase.state.value.todos.size)
        assertEquals("TaskB", testUseCase.state.value.todos[0].title)
        assertEquals(
            listOf(
                BaseEffect.ShowToast("添加成功"),
                BaseEffect.ShowToast("添加成功"),
                BaseEffect.ShowToast("已删除")
            ),
            effects
        )

        // 删除 TaskB
        val todoB = testUseCase.state.value.todos[0]
        testUseCase.onEvent(TodoEvent.OnTodoDeleteClicked(todoB.id))

        assertTrue(testUseCase.state.value.todos.isEmpty())
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
    fun 清除所有已完成() = runTest {
        val (testUseCase, effects) = createUseCaseWithEffectCollector()

        // 添加三个 Todo
        testUseCase.onEvent(TodoEvent.OnInputTextChanged("TaskA"))
        testUseCase.onEvent(TodoEvent.OnAddTodoClicked)
        testUseCase.onEvent(TodoEvent.OnInputTextChanged("TaskB"))
        testUseCase.onEvent(TodoEvent.OnAddTodoClicked)
        testUseCase.onEvent(TodoEvent.OnInputTextChanged("TaskC"))
        testUseCase.onEvent(TodoEvent.OnAddTodoClicked)

        val todoA = testUseCase.state.value.todos[0]
        val todoB = testUseCase.state.value.todos[1]

        // 勾选 TaskA 和 TaskB
        testUseCase.onEvent(TodoEvent.OnTodoCheckedChanged(todoA.id, true))
        testUseCase.onEvent(TodoEvent.OnTodoCheckedChanged(todoB.id, true))

        // 清除已完成
        testUseCase.onEvent(TodoEvent.OnClearCompletedClicked)

        assertEquals(1, testUseCase.state.value.todos.size)
        assertEquals("TaskC", testUseCase.state.value.todos[0].title)
        assertFalse(testUseCase.state.value.todos[0].isCompleted)

        val expectedEffects = listOf(
            BaseEffect.ShowToast("添加成功"),
            BaseEffect.ShowToast("添加成功"),
            BaseEffect.ShowToast("添加成功"),
            BaseEffect.ShowToast("已清除 2 个已完成任务")
        )
        assertEquals(expectedEffects, effects)
    }

    @Test
    fun 长文本输入() = runTest {
        val (testUseCase, effects) = createUseCaseWithEffectCollector()

        val longText = (
            "ThisIsAVeryLongTodoItemTitleToTestLongTextDisplayInTheListAndInputField" +
                "ToEnsureTheAppDoesNotCrashOrHaveLayoutIssuesWithLongContent"
            )

        testUseCase.onEvent(TodoEvent.OnInputTextChanged(longText))
        testUseCase.onEvent(TodoEvent.OnAddTodoClicked)

        assertEquals(1, testUseCase.state.value.todos.size)
        assertEquals(longText, testUseCase.state.value.todos[0].title)
        assertEquals(listOf(BaseEffect.ShowToast("添加成功")), effects)
    }

    @Test
    fun 综合端到端流程() = runTest {
        val (testUseCase, effects) = createUseCaseWithEffectCollector()

        // 1. 添加 BuyMilk 和 WriteCode
        testUseCase.onEvent(TodoEvent.OnInputTextChanged("BuyMilk"))
        testUseCase.onEvent(TodoEvent.OnAddTodoClicked)
        testUseCase.onEvent(TodoEvent.OnInputTextChanged("WriteCode"))
        testUseCase.onEvent(TodoEvent.OnAddTodoClicked)

        val buyMilk = testUseCase.state.value.todos[0]
        val writeCode = testUseCase.state.value.todos[1]

        // 2. 勾选 BuyMilk
        testUseCase.onEvent(TodoEvent.OnTodoCheckedChanged(buyMilk.id, true))
        assertTrue(testUseCase.state.value.todos[0].isCompleted)

        // 3. 添加 DoSport
        testUseCase.onEvent(TodoEvent.OnInputTextChanged("DoSport"))
        testUseCase.onEvent(TodoEvent.OnAddTodoClicked)

        // 4. 取消 BuyMilk，勾选 WriteCode
        testUseCase.onEvent(TodoEvent.OnTodoCheckedChanged(buyMilk.id, false))
        testUseCase.onEvent(TodoEvent.OnTodoCheckedChanged(writeCode.id, true))
        assertFalse(testUseCase.state.value.todos[0].isCompleted)
        assertTrue(testUseCase.state.value.todos[1].isCompleted)

        // 5. 清除已完成（WriteCode）
        testUseCase.onEvent(TodoEvent.OnClearCompletedClicked)

        assertEquals(2, testUseCase.state.value.todos.size)
        assertEquals("BuyMilk", testUseCase.state.value.todos[0].title)
        assertEquals("DoSport", testUseCase.state.value.todos[1].title)
        assertFalse(testUseCase.state.value.todos[0].isCompleted)
        assertFalse(testUseCase.state.value.todos[1].isCompleted)

        // 6. 删除 BuyMilk
        testUseCase.onEvent(TodoEvent.OnTodoDeleteClicked(testUseCase.state.value.todos[0].id))

        // 7. 删除 DoSport
        testUseCase.onEvent(TodoEvent.OnTodoDeleteClicked(testUseCase.state.value.todos[0].id))

        assertTrue(testUseCase.state.value.todos.isEmpty())

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
    fun 清除已完成但无已完成任务时不发送副作用() = runTest {
        val (testUseCase, effects) = createUseCaseWithEffectCollector()

        // 添加一个未完成的 Todo（会产生"添加成功"副作用）
        testUseCase.onEvent(TodoEvent.OnInputTextChanged("TaskA"))
        testUseCase.onEvent(TodoEvent.OnAddTodoClicked)

        // 清除已完成（没有任何已完成任务）——不应产生新的副作用
        testUseCase.onEvent(TodoEvent.OnClearCompletedClicked)

        assertEquals(1, testUseCase.state.value.todos.size)
        assertEquals("TaskA", testUseCase.state.value.todos[0].title)
        assertFalse(testUseCase.state.value.todos[0].isCompleted)
        // 只有"添加成功"一个副作用，没有清除相关的副作用
        assertEquals(listOf(BaseEffect.ShowToast("添加成功")), effects)
    }

    @Test
    fun 初始未登录状态检查() = runTest {
        useCase.onEvent(TodoEvent.CheckLogin)

        assertFalse(useCase.state.value.isLoggedIn!!)
    }

    @Test
    fun 登录后状态检查() = runTest {
        fakeRepository.setLoggedInUser(UserInfo("u_1", "alice", "Alice"))

        useCase.onEvent(TodoEvent.CheckLogin)

        assertTrue(useCase.state.value.isLoggedIn!!)
    }

    @Test
    fun 登出后恢复未登录状态() = runTest {
        fakeRepository.setLoggedInUser(UserInfo("u_1", "alice", "Alice"))
        useCase.onEvent(TodoEvent.CheckLogin)
        assertTrue(useCase.state.value.isLoggedIn!!)

        fakeRepository.logout()
        useCase.onEvent(TodoEvent.CheckLogin)

        assertFalse(useCase.state.value.isLoggedIn!!)
    }
}
