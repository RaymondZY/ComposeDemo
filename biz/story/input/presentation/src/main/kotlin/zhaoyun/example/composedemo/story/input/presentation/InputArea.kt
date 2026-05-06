package zhaoyun.example.composedemo.story.input.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.koinInject

import zhaoyun.example.composedemo.story.input.domain.InputEffect
import zhaoyun.example.composedemo.story.input.domain.InputEvent
import zhaoyun.example.composedemo.story.input.domain.InputKeyboardCoordinator
import kotlin.math.abs

@Composable
fun InputArea(
    viewModel: InputViewModel,
    modifier: Modifier = Modifier,
    visualTranslationY: Float = 0f,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val coordinator = koinInject<InputKeyboardCoordinator>()
    var textFieldValue by remember { mutableStateOf(TextFieldValue()) }
    // 跟踪本 TextField 是否真的持有焦点。focusManager.clearFocus() 是 window 级全局操作，
    // 离屏/预加载 page 的 InputArea 也会 collect 到自己的 ClearFocus effect，
    // 必须 guard 住，否则会清掉当前页 TextField 的焦点导致键盘秒收。
    var isTextFieldFocused by remember { mutableStateOf(false) }
    // 缓存最近一次 layout 的坐标，以便在 onFocusChanged 触发时立即上报 bounds
    // （onGloballyPositioned 仅在位置变化时回调，焦点变化本身不会触发它）
    var lastCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var lastLayoutBounds by remember { mutableStateOf<InputKeyboardCoordinator.Bounds?>(null) }

    fun cacheLayoutBounds(coords: LayoutCoordinates) {
        val pos = coords.positionInRoot()
        lastLayoutBounds = InputKeyboardCoordinator.Bounds(
            left = pos.x,
            top = pos.y,
            right = pos.x + coords.size.width,
            bottom = pos.y + coords.size.height,
        )
    }

    fun reportBounds() {
        val bounds = lastLayoutBounds ?: return
        coordinator.setActiveBounds(
            InputKeyboardCoordinator.Bounds(
                left = bounds.left,
                top = bounds.top + visualTranslationY,
                right = bounds.right,
                bottom = bounds.bottom + visualTranslationY,
            )
        )
    }

    LaunchedEffect(isTextFieldFocused, visualTranslationY, lastCoords) {
        if (isTextFieldFocused) {
            lastCoords?.let {
                if (lastLayoutBounds == null) {
                    cacheLayoutBounds(it)
                }
                reportBounds()
            }
        }
    }

    // UC-06：收集 InsertBrackets effect，同步含光标位置的 TextFieldValue
    // UC-02：收到 ClearFocus 命令时，仅当本 TextField 真持有焦点才执行 clearFocus
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is InputEffect.InsertBrackets ->
                    textFieldValue = TextFieldValue(
                        text = effect.newText,
                        selection = TextRange(effect.cursorPosition),
                    )
                InputEffect.ClearFocus -> {
                    if (isTextFieldFocused) {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    }
                }
            }
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .background(
                color = Color.White.copy(alpha = 0.15f),
                shape = RoundedCornerShape(20.dp),
            )
            // 「点击输入框任意区域打开键盘」的 UX（UC-01）；
            // FeedScreen 的 dismiss overlay 会通过 bounds hit-test 排除本区域，无需这里再消费 tap
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { focusRequester.requestFocus() },
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
            // 持焦时把 root 坐标系下的整体 bounds 上报给 coordinator，
            // FeedScreen overlay 用它做 hit-test 排除 InputArea 区域。
            // 即使未持焦也缓存 coords，使下次获焦时能立即上报。
            .onGloballyPositioned { coords ->
                lastCoords = coords
                if (abs(visualTranslationY) < 1f) {
                    cacheLayoutBounds(coords)
                }
                if (isTextFieldFocused) {
                    reportBounds()
                }
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // UC-03：单行，hint 超出以省略号截断
        BasicTextField(
            value = textFieldValue,
            onValueChange = { newValue ->
                textFieldValue = newValue
                viewModel.sendEvent(InputEvent.OnTextChanged(newValue.text))
            },
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
                .onFocusChanged { focusState ->
                    isTextFieldFocused = focusState.isFocused
                    if (focusState.isFocused) {
                        // 用最近一次缓存的 coords 立即上报 bounds，
                        // 避免等到下次 layout 才上报（onGloballyPositioned 不会因焦点变化重跑）
                        lastCoords?.let {
                            if (lastLayoutBounds == null) {
                                cacheLayoutBounds(it)
                            }
                            reportBounds()
                        }
                    } else {
                        coordinator.clearActiveBounds()
                    }
                    viewModel.sendEvent(InputEvent.OnFocusChanged(focusState.isFocused))
                },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            textStyle = LocalTextStyle.current.copy(
                color = Color.White,
                fontSize = 14.sp,
            ),
            cursorBrush = SolidColor(Color.White),
            decorationBox = { innerTextField ->
                if (textFieldValue.text.isEmpty()) {
                    Text(
                        text = state.hintText,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = Color.White.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                innerTextField()
            },
        )

        // UC-05：展开状态（isFocused）额外显示括号按钮
        AnimatedVisibility(visible = state.isFocused) {
            IconButton(onClick = { viewModel.sendEvent(InputEvent.OnBracketClicked) }) {
                Text(
                    text = "( )",
                    color = Color.White,
                    fontSize = 14.sp,
                )
            }
        }

        // UC-04/07：语音按钮，始终显示（占位）
        IconButton(onClick = { viewModel.sendEvent(InputEvent.OnVoiceClicked) }) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "语音输入",
                tint = Color.White,
            )
        }

        // UC-04/08/09：有文字时显示发送，无文字时显示加号
        if (state.text.isNotEmpty()) {
            IconButton(onClick = { viewModel.sendEvent(InputEvent.OnSendClicked) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "发送",
                    tint = Color.White,
                )
            }
        } else {
            IconButton(onClick = { viewModel.sendEvent(InputEvent.OnPlusClicked) }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "更多",
                    tint = Color.White,
                )
            }
        }
    }
}
