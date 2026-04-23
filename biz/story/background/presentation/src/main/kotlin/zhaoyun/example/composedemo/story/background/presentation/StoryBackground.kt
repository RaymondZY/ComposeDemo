package zhaoyun.example.composedemo.story.background.presentation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage

@Composable
fun StoryBackground(viewModel: BackgroundViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    AsyncImage(
        model = state.backgroundImageUrl,
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
    )
}
