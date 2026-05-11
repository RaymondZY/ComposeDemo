package zhaoyun.example.composedemo.story.sharepanel.platform

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.parameter.parametersOf
import zhaoyun.example.composedemo.scaffold.platform.screenViewModel
import zhaoyun.example.composedemo.scaffold.core.mvi.BaseEffect
import zhaoyun.example.composedemo.scaffold.core.mvi.toStateHolder
import zhaoyun.example.composedemo.story.sharepanel.core.SharePanelEffect
import zhaoyun.example.composedemo.story.sharepanel.core.SharePanelEvent
import zhaoyun.example.composedemo.story.sharepanel.core.SharePanelState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharePanelSheet(
    cardId: String,
    backgroundImageUrl: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val initialState = remember(cardId, backgroundImageUrl) {
        SharePanelState(cardId = cardId, backgroundImageUrl = backgroundImageUrl)
    }
    val viewModel: SharePanelViewModel = screenViewModel("$cardId:$backgroundImageUrl") {
        parametersOf(cardId, backgroundImageUrl, initialState.toStateHolder())
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    LaunchedEffect(viewModel) {
        viewModel.sendEvent(SharePanelEvent.OnPanelShown)
    }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is SharePanelEffect.SaveImageToAlbum -> {
                    val saved = saveImageToAlbum(context, effect.imageUrl)
                    showToast(context, if (saved) "图片已保存" else "图片保存失败")
                }

                is SharePanelEffect.CopyLinkToClipboard -> {
                    clipboardManager.setText(AnnotatedString(effect.shareLink))
                    showToast(context, "链接已复制")
                }

                is SharePanelEffect.ShareLinkWithSystem -> {
                    val opened = shareLinkWithSystem(context, effect.shareLink)
                    if (!opened) showToast(context, "无法打开系统分享")
                }
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.baseEffect.collect { effect ->
            when (effect) {
                is BaseEffect.ShowToast -> showToast(context, effect.message)
                else -> Unit
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(),
        modifier = modifier,
    ) {
        SharePanelContent(
            isLoadingShareLink = state.isLoadingShareLink,
            onSaveImage = { viewModel.sendEvent(SharePanelEvent.OnSaveImageClicked) },
            onCopyLink = { viewModel.sendEvent(SharePanelEvent.OnCopyLinkClicked) },
            onMore = { viewModel.sendEvent(SharePanelEvent.OnMoreClicked) },
        )
    }
}

@Composable
internal fun SharePanelContent(
    isLoadingShareLink: Boolean,
    onSaveImage: () -> Unit,
    onCopyLink: () -> Unit,
    onMore: () -> Unit,
) {
    val items = listOf(
        ShareAction("保存图片", Icons.Outlined.Image, onSaveImage),
        ShareAction("复制链接", Icons.Outlined.ContentCopy, onCopyLink),
        ShareAction("更多", Icons.Outlined.MoreHoriz, onMore),
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 32.dp),
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(items) { item ->
                ShareActionItem(item = item)
            }
        }

        if (isLoadingShareLink) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "正在获取分享链接",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                color = Color.Black.copy(alpha = 0.55f),
            )
        }
    }
}

@Composable
private fun ShareActionItem(item: ShareAction) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = item.onClick),
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Color(0xFFF2F2F2)),
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                tint = Color.Black,
                modifier = Modifier.size(30.dp),
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = item.label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = Color.Black,
        )
    }
}

private data class ShareAction(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

private suspend fun saveImageToAlbum(context: Context, imageUrl: String): Boolean {
    return withContext(Dispatchers.IO) {
        runCatching {
            val request = ImageRequest.Builder(context)
                .data(imageUrl)
                .allowHardware(false)
                .build()
            val result = ImageLoader(context).execute(request) as? SuccessResult ?: return@runCatching false
            val bitmap = result.drawable.toBitmap()
            MediaStore.Images.Media.insertImage(
                context.contentResolver,
                bitmap,
                "story-${System.currentTimeMillis()}",
                null,
            ) != null
        }.getOrDefault(false)
    }
}

private fun shareLinkWithSystem(context: Context, shareLink: String): Boolean {
    return runCatching {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareLink)
        }
        val chooser = Intent.createChooser(sendIntent, null).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
        true
    }.getOrDefault(false)
}

private fun showToast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}
