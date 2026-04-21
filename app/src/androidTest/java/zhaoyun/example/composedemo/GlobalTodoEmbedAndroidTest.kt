package zhaoyun.example.composedemo

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import zhaoyun.example.composedemo.domain.model.TodoEvent
import zhaoyun.example.composedemo.domain.usecase.CheckLoginUseCase
import zhaoyun.example.composedemo.domain.usecase.TodoUseCases
import zhaoyun.example.composedemo.scaffold.android.BaseViewModel
import zhaoyun.example.composedemo.service.usercenter.api.model.UserInfo
import zhaoyun.example.composedemo.service.usercenter.mock.FakeUserRepository
import zhaoyun.example.composedemo.todo.presentation.TodoListPage
import zhaoyun.example.composedemo.todo.presentation.TodoViewModel

/**
 * Global页面嵌入Todo的集成测试
 */
@RunWith(AndroidJUnit4::class)
class GlobalTodoEmbedAndroidTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val fakeRepository = FakeUserRepository()

    data class GlobalState(val todoCount: Int = 0)

    class GlobalViewModel : ViewModel() {
        private val _state = MutableStateFlow(GlobalState())
        val state: StateFlow<GlobalState> = _state.asStateFlow()

        fun createTodoReducer(): zhaoyun.example.composedemo.scaffold.core.mvi.Reducer<zhaoyun.example.composedemo.domain.model.TodoState> {
            val todoStateFlow = MutableStateFlow(zhaoyun.example.composedemo.domain.model.TodoState())
            return BaseViewModel.createDelegateReducer(
                stateFlow = todoStateFlow,
                onReduce = { transform ->
                    val newTodo = transform(todoStateFlow.value)
                    todoStateFlow.value = newTodo
                    _state.update { it.copy(todoCount = newTodo.todos.size) }
                }
            )
        }
    }

    @Composable
    fun GlobalWithTodoScreen(globalVm: GlobalViewModel, todoVm: TodoViewModel) {
        val globalState by globalVm.state.collectAsStateWithLifecycle()

        Column {
            Text(text = "Total: ${globalState.todoCount}")
            TodoListPage(state = todoVm.state.value, onEvent = todoVm::onEvent)
        }
    }

    @Test
    fun `Global嵌入Todo后添加操作同步更新全局计数`() {
        fakeRepository.setLoggedInUser(UserInfo("u_1", "alice", "Alice"))

        val globalVm = GlobalViewModel()
        val reducer = globalVm.createTodoReducer()
        val todoVm = TodoViewModel(
            todoUseCases = TodoUseCases(),
            checkLoginUseCase = CheckLoginUseCase(fakeRepository),
            injectedReducer = reducer
        )

        composeTestRule.setContent {
            GlobalWithTodoScreen(globalVm = globalVm, todoVm = todoVm)
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Total: 0").assertIsDisplayed()

        composeTestRule.onNodeWithTag("todo_input").performTextInput("EmbeddedTodo")
        composeTestRule.onNodeWithTag("add_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("EmbeddedTodo").assertIsDisplayed()
        composeTestRule.onNodeWithText("Total: 1").assertIsDisplayed()
    }
}
