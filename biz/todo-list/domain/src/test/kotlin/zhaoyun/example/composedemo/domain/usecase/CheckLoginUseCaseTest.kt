package zhaoyun.example.composedemo.domain.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import zhaoyun.example.composedemo.domain.model.TodoEvent
import zhaoyun.example.composedemo.service.usercenter.api.model.UserInfo
import zhaoyun.example.composedemo.service.usercenter.mock.FakeUserRepository

/**
 * CheckLoginUseCase 单元测试 —— 验证作为独立 MVI UseCase 的登录状态检查逻辑
 *
 * 通过发送 [TodoEvent.CheckLogin]、验证 [TodoState.isLoggedIn] 变化来覆盖全部路径。
 * 纯构造函数注入，无需启动 Koin 容器。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CheckLoginUseCaseTest {

    private val fakeRepository = FakeUserRepository()
    private lateinit var useCase: CheckLoginUseCase

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        useCase = CheckLoginUseCase(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `初始状态未登录`() = runTest {
        useCase.onEvent(TodoEvent.CheckLogin)

        assertFalse(useCase.state.value.isLoggedIn!!)
    }

    @Test
    fun `登录后返回已登录状态`() = runTest {
        fakeRepository.setLoggedInUser(UserInfo("u_1", "alice", "Alice"))

        useCase.onEvent(TodoEvent.CheckLogin)

        assertTrue(useCase.state.value.isLoggedIn!!)
    }

    @Test
    fun `登出后恢复未登录状态`() = runTest {
        fakeRepository.setLoggedInUser(UserInfo("u_1", "alice", "Alice"))
        useCase.onEvent(TodoEvent.CheckLogin)
        assertTrue(useCase.state.value.isLoggedIn!!)

        fakeRepository.logout()
        useCase.onEvent(TodoEvent.CheckLogin)

        assertFalse(useCase.state.value.isLoggedIn!!)
    }
}
