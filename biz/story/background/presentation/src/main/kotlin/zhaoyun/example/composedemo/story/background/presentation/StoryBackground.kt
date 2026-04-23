package zhaoyun.example.composedemo.story.background.presentation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import coil.compose.AsyncImage
import zhaoyun.example.composedemo.story.background.domain.BackgroundState

@Composable
fun StoryBackground(state: BackgroundState) {
    AsyncImage(
        model = state.backgroundImageUrl,
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
    )
}
