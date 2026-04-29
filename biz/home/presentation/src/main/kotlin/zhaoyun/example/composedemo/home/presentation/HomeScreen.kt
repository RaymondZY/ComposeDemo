package zhaoyun.example.composedemo.home.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import zhaoyun.example.composedemo.scaffold.android.MviScreen

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    MviScreen(viewModel = viewModel) {
        HomePage(
            state = state,
            onEvent = viewModel::onEvent,
            modifier = modifier
        )
    }
}
