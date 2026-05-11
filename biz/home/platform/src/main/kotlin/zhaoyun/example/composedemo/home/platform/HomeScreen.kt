package zhaoyun.example.composedemo.home.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import zhaoyun.example.composedemo.scaffold.platform.MviScreen

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    MviScreen<HomeViewModel> { viewModel ->
        val state by viewModel.state.collectAsStateWithLifecycle()

        HomePage(
            state = state,
            onEvent = viewModel::sendEvent,
            modifier = modifier
        )
    }
}
