package zhaoyun.example.composedemo.home.platform

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import zhaoyun.example.composedemo.feed.platform.FeedScreen
import zhaoyun.example.composedemo.home.core.HomeEvent
import zhaoyun.example.composedemo.home.core.HomeState
import zhaoyun.example.composedemo.home.core.Tab

@Composable
fun HomePage(
    state: HomeState,
    onEvent: (HomeEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        bottomBar = {
            BottomNavigationBar(
                selectedTab = state.selectedTab,
                tabBadges = state.tabBadges,
                onEvent = onEvent
            )
        }
    ) { innerPadding ->
        when (state.selectedTab) {
            Tab.HOME -> FeedScreen(modifier = Modifier.padding(innerPadding))
            Tab.DISCOVER -> DiscoverPage(modifier = Modifier.padding(innerPadding))
            Tab.MESSAGE -> MessagePage(modifier = Modifier.padding(innerPadding))
            Tab.PROFILE -> ProfilePage(modifier = Modifier.padding(innerPadding))
        }
    }
}
