package zhaoyun.example.composedemo.domain.usecase

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
import zhaoyun.example.composedemo.service.usercenter.api.model.UserInfo
import zhaoyun.example.composedemo.service.usercenter.mock.FakeUserRepository

/**
 * CheckLoginUseCase 单元测试 —— 演示如何在纯 Kotlin 模块中测试依赖服务发现的 UseCase
 *
 * 通过 :service:user-center:mock 提供的 FakeUserRepository 模拟真实实现。
 */
class CheckLoginUseCaseTest {

    @Before
    fun setup() {
        startKoin {
            modules(testUserModule)
        }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `初始状态未登录`() {
        val useCase = CheckLoginUseCase()
        assertFalse(useCase())
        assertNull(useCase.getCurrentUser())
    }

    @Test
    fun `登录后返回已登录状态及用户信息`() {
        val useCase = CheckLoginUseCase()
        fakeRepository.setLoggedInUser(UserInfo("u_1", "alice", "Alice"))

        assertTrue(useCase())
        assertEquals("Alice", useCase.getCurrentUser()?.displayName)
        assertEquals("alice", useCase.getCurrentUser()?.username)
    }

    @Test
    fun `登出后恢复未登录状态`() {
        val useCase = CheckLoginUseCase()
        fakeRepository.setLoggedInUser(UserInfo("u_1", "alice", "Alice"))
        assertTrue(useCase())

        fakeRepository.logout()

        assertFalse(useCase())
        assertNull(useCase.getCurrentUser())
    }

    // ========== Test Double ==========

    private val fakeRepository = FakeUserRepository()

    private val testUserModule = module {
        single<UserRepository> { fakeRepository }
    }
}
