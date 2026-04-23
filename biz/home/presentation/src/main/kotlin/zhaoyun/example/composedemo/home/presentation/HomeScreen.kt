package zhaoyun.example.composedemo.home.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.koin.androidx.compose.koinViewModel
import zhaoyun.example.composedemo.scaffold.android.MviScreen

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel()
) {
    MviScreen(
        viewModel = viewModel
    ) { state, onEvent ->
        HomePage(
            state = state,
            onEvent = onEvent,
            modifier = modifier
        )
    }
}
