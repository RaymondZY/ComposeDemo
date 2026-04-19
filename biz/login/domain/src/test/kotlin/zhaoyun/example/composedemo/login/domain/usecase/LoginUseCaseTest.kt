package zhaoyun.example.composedemo.login.domain.usecase

import kotlinx.coroutines.test.runTest
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
import zhaoyun.example.composedemo.service.usercenter.api.UserRepository
import zhaoyun.example.composedemo.service.usercenter.api.model.LoginResult
import zhaoyun.example.composedemo.service.usercenter.mock.FakeUserRepository

/**
 * LoginUseCase 单元测试 —— 验证登录成功与失败路径
 *
 * 通过 :service:user-center:mock 提供的 FakeUserRepository 模拟真实实现。
 */
class LoginUseCaseTest {

    @Before
    fun setup() {
        startKoin { modules(testUserModule) }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun 正确的用户名密码登录成功() = runTest {
        val useCase = LoginUseCase()
        val result = useCase("admin", "123456")

        assertTrue(result is LoginResult.Success)
        assertEquals("admin", (result as LoginResult.Success).userInfo.username)
    }

    @Test
    fun 错误的用户名密码登录失败() = runTest {
        val useCase = LoginUseCase()
        val result = useCase("wrong", "wrong")

        assertTrue(result is LoginResult.Failure)
        assertEquals("用户名或密码错误", (result as LoginResult.Failure).message)
    }

    @Test
    fun 登录成功后仓库状态更新() = runTest {
        val useCase = LoginUseCase()
        useCase("admin", "123456")

        assertTrue(fakeRepository.isLoggedIn())
        assertEquals("admin", fakeRepository.currentUser.value?.username)
    }

    @Test
    fun 登录失败后仓库保持未登录() = runTest {
        val useCase = LoginUseCase()
        val result = useCase("wrong", "wrong")

        assertTrue(result is LoginResult.Failure)
        assertFalse(fakeRepository.isLoggedIn())
        assertNull(fakeRepository.currentUser.value)
    }

    // ========== Test Double ==========

    private val fakeRepository = FakeUserRepository()

    private val testUserModule = module {
        single<UserRepository> { fakeRepository }
    }
}
