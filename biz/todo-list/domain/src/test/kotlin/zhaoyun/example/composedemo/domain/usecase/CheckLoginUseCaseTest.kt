package zhaoyun.example.composedemo.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import zhaoyun.example.composedemo.service.usercenter.api.model.UserInfo
import zhaoyun.example.composedemo.service.usercenter.mock.FakeUserRepository

/**
 * CheckLoginUseCase 单元测试 —— 验证登录状态检查逻辑
 *
 * 通过 :service:user-center:mock 提供的 FakeUserRepository 模拟真实实现。
 * 纯构造函数注入，无需启动 Koin 容器。
 */
class CheckLoginUseCaseTest {

    private val fakeRepository = FakeUserRepository()
    private val useCase = CheckLoginUseCase(fakeRepository)

    @Test
    fun `初始状态未登录`() {
        assertFalse(useCase())
        assertNull(useCase.getCurrentUser())
    }

    @Test
    fun `登录后返回已登录状态及用户信息`() {
        fakeRepository.setLoggedInUser(UserInfo("u_1", "alice", "Alice"))

        assertTrue(useCase())
        assertEquals("Alice", useCase.getCurrentUser()?.displayName)
        assertEquals("alice", useCase.getCurrentUser()?.username)
    }

    @Test
    fun `登出后恢复未登录状态`() {
        fakeRepository.setLoggedInUser(UserInfo("u_1", "alice", "Alice"))
        assertTrue(useCase())

        fakeRepository.logout()

        assertFalse(useCase())
        assertNull(useCase.getCurrentUser())
    }
}
