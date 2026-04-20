package zhaoyun.example.composedemo.todo.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import zhaoyun.example.composedemo.domain.usecase.CheckLoginUseCase
import zhaoyun.example.composedemo.domain.usecase.TodoUseCases
import zhaoyun.example.composedemo.service.usercenter.api.model.UserInfo
import zhaoyun.example.composedemo.service.usercenter.mock.FakeUserRepository

/**
 * TodoList Compose UI 测试 —— 覆盖登录状态与 Todo 交互
 */
@RunWith(AndroidJUnit4::class)
class TodoListScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val fakeRepository = FakeUserRepository()

    @Before
    fun setup() {
        // 直接构造 ViewModel，无需 Koin
    }

    @After
    fun tearDown() {
        fakeRepository.logout()
    }

    private fun createViewModel(): TodoViewModel =
        TodoViewModel(TodoUseCases(CheckLoginUseCase(fakeRepository)))

    @Test
    fun `未登录时显示登录提示`() {
        composeTestRule.setContent {
            TodoListScreen(onNavigateToLogin = {}, viewModel = createViewModel())
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("请先登录").assertIsDisplayed()
        composeTestRule.onNodeWithText("登录后即可使用 Todo List 功能").assertIsDisplayed()
        composeTestRule.onNodeWithTag("login_navigate_button").assertIsDisplayed()
    }

    @Test
    fun `登录后显示Todo列表页面`() {
        fakeRepository.setLoggedInUser(UserInfo("u_1", "alice", "Alice"))

        composeTestRule.setContent {
            TodoListScreen(onNavigateToLogin = {}, viewModel = createViewModel())
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Todo List").assertIsDisplayed()
        composeTestRule.onNodeWithTag("empty_state_text").assertIsDisplayed()
        composeTestRule.onNodeWithTag("todo_input").assertIsDisplayed()
    }

    @Test
    fun `登录后添加Todo完整流程`() {
        fakeRepository.setLoggedInUser(UserInfo("u_1", "alice", "Alice"))

        composeTestRule.setContent {
            TodoListScreen(onNavigateToLogin = {}, viewModel = createViewModel())
        }

        composeTestRule.waitForIdle()

        // 添加第一个 Todo
        composeTestRule.onNodeWithTag("todo_input").performTextInput("BuyMilk")
        composeTestRule.onNodeWithTag("add_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("BuyMilk").assertIsDisplayed()
        composeTestRule.onNodeWithTag("stats_text").assertIsDisplayed()

        // 添加第二个 Todo
        composeTestRule.onNodeWithTag("todo_input").performTextInput("WriteCode")
        composeTestRule.onNodeWithTag("add_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("WriteCode").assertIsDisplayed()
    }
}
