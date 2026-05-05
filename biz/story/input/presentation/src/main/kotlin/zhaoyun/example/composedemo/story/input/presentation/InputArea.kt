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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import zhaoyun.example.composedemo.story.input.domain.InputEffect
import zhaoyun.example.composedemo.story.input.domain.InputEvent

@Composable
fun InputArea(
    viewModel: InputViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var textFieldValue by remember { mutableStateOf(TextFieldValue()) }

    // UC-06：收集 InsertBrackets effect，同步含光标位置的 TextFieldValue
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is InputEffect.InsertBrackets ->
                    textFieldValue = TextFieldValue(
                        text = effect.newText,
                        selection = TextRange(effect.cursorPosition),
                    )
            }
        }
    }

    // UC-01/02：isFocused 状态驱动键盘显隐
    LaunchedEffect(state.isFocused) {
        if (state.isFocused) {
            focusRequester.requestFocus()
            keyboardController?.show()
        } else {
            keyboardController?.hide()
            focusManager.clearFocus()
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
            // 消费来自 Row 背景的 tap，防止 FeedScreen.detectTapGestures 误触发 dismiss
            // 同时实现「点击输入框任意区域打开键盘」的 UX（UC-01）
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { focusRequester.requestFocus() },
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
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
