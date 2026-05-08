package zhaoyun.example.composedemo.story.commentpanel.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.koin.core.parameter.parametersOf
import zhaoyun.example.composedemo.scaffold.android.MviScreen
import zhaoyun.example.composedemo.scaffold.core.mvi.toStateHolder
import zhaoyun.example.composedemo.story.commentpanel.domain.CommentPanelState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentPanelSheet(
    cardId: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(),
        modifier = modifier,
    ) {
        CommentPanelScreen(cardId = cardId)
    }
}

@Composable
fun CommentPanelScreen(
    cardId: String,
    modifier: Modifier = Modifier,
) {
    val initialState = remember(cardId) {
        CommentPanelState(cardId = cardId)
    }
    MviScreen<CommentPanelViewModel>(
        parameters = { parametersOf(initialState.toStateHolder()) },
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .testTag(CommentPanelTestTags.EmptyScreen),
        )
    }
}

object CommentPanelTestTags {
    const val EmptyScreen = "comment_panel_empty_screen"
}
