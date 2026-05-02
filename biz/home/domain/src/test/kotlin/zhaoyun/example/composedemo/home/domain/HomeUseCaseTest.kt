package zhaoyun.example.composedemo.home.domain

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import zhaoyun.example.composedemo.scaffold.core.mvi.toStateHolder

class HomeUseCaseTest {

    private val useCase = HomeUseCase(HomeState().toStateHolder())

    @Test
    fun `初始状态默认选中HOME`() {
        assertEquals(Tab.HOME, useCase.state.value.selectedTab)
        assertTrue(useCase.state.value.tabBadges.isEmpty())
    }

    @Test
    fun `切换Tab状态更新`() = runTest {
        useCase.receiveEvent(HomeEvent.OnTabSelected(Tab.DISCOVER))
        assertEquals(Tab.DISCOVER, useCase.state.value.selectedTab)
    }

    @Test
    fun `重复选择同一Tab不触发状态变更`() = runTest {
        val before = useCase.state.value
        useCase.receiveEvent(HomeEvent.OnTabSelected(Tab.HOME))
        val after = useCase.state.value
        assertEquals(before, after)
    }

    @Test
    fun `点击中间按钮不改变selectedTab`() = runTest {
        useCase.receiveEvent(HomeEvent.OnCenterButtonClicked)
        assertEquals(Tab.HOME, useCase.state.value.selectedTab)
    }

    @Test
    fun `更新角标后tabBadges包含数据`() = runTest {
        useCase.receiveEvent(HomeEvent.OnBadgeUpdated(Tab.MESSAGE, TabBadge(unreadCount = 3)))
        assertEquals(3, useCase.state.value.tabBadges[Tab.MESSAGE]?.unreadCount)
    }

    @Test
    fun `更新不同Tab角标互不覆盖`() = runTest {
        useCase.receiveEvent(HomeEvent.OnBadgeUpdated(Tab.MESSAGE, TabBadge(unreadCount = 3)))
        useCase.receiveEvent(HomeEvent.OnBadgeUpdated(Tab.DISCOVER, TabBadge(showRedDot = true)))
        assertEquals(3, useCase.state.value.tabBadges[Tab.MESSAGE]?.unreadCount)
        assertTrue(useCase.state.value.tabBadges[Tab.DISCOVER]?.showRedDot == true)
    }
}
