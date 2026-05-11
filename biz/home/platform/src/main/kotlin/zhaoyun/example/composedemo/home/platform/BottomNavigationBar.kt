package zhaoyun.example.composedemo.home.platform

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import zhaoyun.example.composedemo.home.core.HomeEvent
import zhaoyun.example.composedemo.home.core.Tab
import zhaoyun.example.composedemo.home.core.TabBadge

@Composable
fun BottomNavigationBar(
    selectedTab: Tab,
    tabBadges: Map<Tab, TabBadge>,
    onEvent: (HomeEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val tabs = listOf(Tab.HOME, Tab.DISCOVER)
        tabs.forEach { tab ->
            TabItem(
                tab = tab,
                isSelected = tab == selectedTab,
                badge = tabBadges[tab],
                onClick = { onEvent(HomeEvent.OnTabSelected(tab)) }
            )
        }

        // Center button
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .clickable { onEvent(HomeEvent.OnCenterButtonClicked) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "发布",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(28.dp)
            )
        }

        val rightTabs = listOf(Tab.MESSAGE, Tab.PROFILE)
        rightTabs.forEach { tab ->
            TabItem(
                tab = tab,
                isSelected = tab == selectedTab,
                badge = tabBadges[tab],
                onClick = { onEvent(HomeEvent.OnTabSelected(tab)) }
            )
        }
    }
}

@Composable
private fun TabItem(
    tab: Tab,
    isSelected: Boolean,
    badge: TabBadge?,
    onClick: () -> Unit
) {
    val label = when (tab) {
        Tab.HOME -> "首页"
        Tab.DISCOVER -> "发现"
        Tab.MESSAGE -> "消息"
        Tab.PROFILE -> "我的"
    }
    val selectedIcon = when (tab) {
        Tab.HOME -> Icons.Filled.Home
        Tab.DISCOVER -> Icons.Filled.Search
        Tab.MESSAGE -> Icons.Filled.Email
        Tab.PROFILE -> Icons.Filled.AccountCircle
    }
    val unselectedIcon = when (tab) {
        Tab.HOME -> Icons.Outlined.Home
        Tab.DISCOVER -> Icons.Outlined.Search
        Tab.MESSAGE -> Icons.Outlined.Email
        Tab.PROFILE -> Icons.Outlined.AccountCircle
    }
    val icon = if (isSelected) selectedIcon else unselectedIcon
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (badge != null && (badge.showRedDot || badge.unreadCount > 0 || badge.hasBadge)) {
            BadgedBox(
                badge = {
                    if (badge.unreadCount > 0) {
                        Badge { Text(text = badge.unreadCount.toString()) }
                    } else {
                        Badge()
                    }
                }
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = contentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        } else {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = label,
            color = contentColor,
            style = MaterialTheme.typography.labelSmall
        )
    }
}
