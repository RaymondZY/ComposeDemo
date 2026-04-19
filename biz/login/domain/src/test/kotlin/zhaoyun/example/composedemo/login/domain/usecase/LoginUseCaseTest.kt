package zhaoyun.example.composedemo.login.domain.usecase

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import zhaoyun.example.composedemo.service.usercenter.api.model.LoginResult
import zhaoyun.example.composedemo.service.usercenter.mock.FakeUserRepository

/**
 * LoginUseCase 单元测试 —— 验证登录成功与失败路径
 *
 * 通过 :service:user-center:mock 提供的 FakeUserRepository 模拟真实实现。
 * 纯构造函数注入，无需启动 Koin 容器。
 */
class LoginUseCaseTest {

    private val fakeRepository = FakeUserRepository()
    private val useCase = LoginUseCase(fakeRepository)

    @Test
    fun 正确的用户名密码登录成功() = runTest {
        val result = useCase("admin", "123456")

        assertTrue(result is LoginResult.Success)
        assertEquals("admin", (result as LoginResult.Success).userInfo.username)
    }

    @Test
    fun 错误的用户名密码登录失败() = runTest {
        val result = useCase("wrong", "wrong")

        assertTrue(result is LoginResult.Failure)
        assertEquals("用户名或密码错误", (result as LoginResult.Failure).message)
    }

    @Test
    fun 登录成功后仓库状态更新() = runTest {
        useCase("admin", "123456")

        assertTrue(fakeRepository.isLoggedIn())
        assertEquals("admin", fakeRepository.currentUser.value?.username)
    }

    @Test
    fun 登录失败后仓库保持未登录() = runTest {
        val result = useCase("wrong", "wrong")

        assertTrue(result is LoginResult.Failure)
        assertFalse(fakeRepository.isLoggedIn())
        assertNull(fakeRepository.currentUser.value)
    }
}
