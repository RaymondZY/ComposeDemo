package zhaoyun.example.composedemo.scaffold.android

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import zhaoyun.example.composedemo.scaffold.core.mvi.BaseEffect
import zhaoyun.example.composedemo.scaffold.core.mvi.UiEffect
import zhaoyun.example.composedemo.scaffold.core.mvi.UiEvent
import zhaoyun.example.composedemo.scaffold.core.mvi.UiState

/**
 * MVI 通用屏幕包装器 —— 自动完成 State 收集、BaseEffect 处理与自定义 Effect 分发
 *
 * ### BaseEffect 扩展指南
 * 1. 在 `:scaffold:core` 的 [BaseEffect] 中添加新的 sealed 子类。
 * 2. 在 [defaultHandleBaseEffect] 中补充默认处理逻辑（如 Toast、Snackbar 等无状态操作）。
 * 3. 若副作用需要 Compose 树中的组件（如 Dialog、Sheet）或导航栈（NavigateBack），
 *    应在调用方通过 [onBaseEffect] 拦截并自行处理；返回 `true` 表示已消费，默认逻辑不再执行。
 *
 * 使用示例：
 * ```
 * MviScreen(
 *     viewModel = koinViewModel<LoginViewModel>(),
 *     initEvent = LoginEvent.CheckSession,
 *     onEffect = { effect ->
 *         when (effect) {
 *             is LoginEffect.NavigateToHome -> onLoginSuccess()
 *         }
 *     },
 *     onBaseEffect = { effect ->
 *         when (effect) {
 *             is BaseEffect.NavigateBack -> { navController.popBackStack(); true }
 *             else -> false // 未消费，继续走默认处理
 *         }
 *     }
 * ) { state, onEvent ->
 *     LoginPage(state = state, onEvent = onEvent)
 * }
 * ```
 */
@Composable
fun <S : UiState, E : UiEvent, F : UiEffect> MviScreen(
    viewModel: BaseViewModel<S, E, F>,
    initEvent: E? = null,
    onEffect: suspend (F) -> Unit = {},
    onBaseEffect: suspend (BaseEffect) -> Boolean = { false },
    content: @Composable (state: S, onEvent: (E) -> Unit) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        initEvent?.let { viewModel.onEvent(it) }
    }

    LaunchedEffect(Unit) {
        viewModel.baseEffect.collect { effect ->
            val consumed = onBaseEffect(effect)
            if (!consumed) {
                defaultHandleBaseEffect(context, effect)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            onEffect(effect)
        }
    }

    content(state, viewModel::onEvent)
}

/**
 * [BaseEffect] 的默认处理实现 —— 仅处理无需 Compose 树或导航栈的副作用
 *
 * 当前支持：
 * - [BaseEffect.ShowToast]
 *
 * 其他类型（如 [BaseEffect.ShowDialog]、[BaseEffect.NavigateBack]）
 * 建议在调用方通过 [MviScreen] 的 [onBaseEffect] 参数自行处理。
 */
suspend fun defaultHandleBaseEffect(context: Context, effect: BaseEffect) {
    when (effect) {
        is BaseEffect.ShowToast -> {
            Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
        }
        else -> {
            // 其他 BaseEffect 默认不处理，由调用方通过 onBaseEffect 覆盖
        }
    }
}
