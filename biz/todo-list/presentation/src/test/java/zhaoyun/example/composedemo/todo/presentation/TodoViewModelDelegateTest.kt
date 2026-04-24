package zhaoyun.example.composedemo.todo.presentation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import zhaoyun.example.composedemo.domain.model.TodoEvent
import zhaoyun.example.composedemo.domain.usecase.CheckLoginUseCase
import zhaoyun.example.composedemo.domain.usecase.TodoUseCases
import zhaoyun.example.composedemo.scaffold.core.mvi.BaseEffect
import zhaoyun.example.composedemo.scaffold.core.mvi.DelegateStateHolder
import zhaoyun.example.composedemo.service.usercenter.mock.FakeUserRepository

/**
 * TodoViewModel注入DelegateStateHolder后的集成测试
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TodoViewModelDelegateTest {

    private val fakeRepository = FakeUserRepository()

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createDelegateViewModel(): Pair<TodoViewModel, MutableStateFlow<GlobalTestState>> {
        val globalState = MutableStateFlow(
            GlobalTestState(
                todo = zhaoyun.example.composedemo.domain.model.TodoState()
            )
        )
        val detailState = MutableStateFlow(globalState.value.todo)

        val stateHolder = DelegateStateHolder(
            state = detailState,
            onUpdate = { transform ->
                val newTodo = transform(globalState.value.todo)
                globalState.value = globalState.value.copy(todo = newTodo)
                detailState.value = newTodo
            }
        )

        val viewModel = TodoViewModel(
            todoUseCases = TodoUseCases(),
            checkLoginUseCase = CheckLoginUseCase(fakeRepository),
            injectedStateHolder = stateHolder
        )
        return viewModel to globalState
    }

    @Test
    fun `注入DelegateStateHolder后Event经代理更新外部状态`() {
        val (viewModel, globalState) = createDelegateViewModel()

        assertEquals(globalState.value.todo, viewModel.state.value)

        viewModel.onEvent(TodoEvent.OnInputTextChanged("DelegateTest"))
        assertEquals("DelegateTest", viewModel.state.value.inputText)
        assertEquals("DelegateTest", globalState.value.todo.inputText)

        viewModel.onEvent(TodoEvent.OnAddTodoClicked)
        assertEquals(1, viewModel.state.value.todos.size)
        assertEquals(1, globalState.value.todo.todos.size)
        assertEquals("DelegateTest", globalState.value.todo.todos[0].title)
    }

    @Test
    fun `StateTransform注入能正确影响Detail看到的State`() {
        val globalState = MutableStateFlow(
            GlobalTestState(
                todo = zhaoyun.example.composedemo.domain.model.TodoState()
            )
        )
        val detailState = MutableStateFlow(
            globalState.value.todo.copy(inputText = "[transformed]${globalState.value.todo.inputText}")
        )

        val stateHolder = DelegateStateHolder(
            state = detailState,
            onUpdate = { transform ->
                val newTodo = transform(globalState.value.todo)
                globalState.value = globalState.value.copy(todo = newTodo)
                detailState.value = newTodo.copy(inputText = "[transformed]${newTodo.inputText}")
            }
        )

        val viewModel = TodoViewModel(
            todoUseCases = TodoUseCases(),
            checkLoginUseCase = CheckLoginUseCase(fakeRepository),
            injectedStateHolder = stateHolder
        )

        viewModel.onEvent(TodoEvent.OnInputTextChanged("hello"))
        assertEquals("[transformed]hello", viewModel.state.value.inputText)
        assertEquals("hello", globalState.value.todo.inputText)
    }

    @Test
    fun `Delegate模式下effect仍然独立发射`() = runTest {
        val (viewModel, _) = createDelegateViewModel()

        val effects = mutableListOf<BaseEffect>()
        val scope = kotlinx.coroutines.CoroutineScope(Dispatchers.Main)
        scope.launch { viewModel.baseEffect.collect { effects.add(it) } }

        viewModel.state.value

        viewModel.onEvent(TodoEvent.OnInputTextChanged("EffectTest"))
        viewModel.onEvent(TodoEvent.OnAddTodoClicked)

        assertTrue(effects.isNotEmpty())
        assertEquals(BaseEffect.ShowToast("添加成功"), effects[0])
    }

    data class GlobalTestState(
        val todo: zhaoyun.example.composedemo.domain.model.TodoState
    )
}
