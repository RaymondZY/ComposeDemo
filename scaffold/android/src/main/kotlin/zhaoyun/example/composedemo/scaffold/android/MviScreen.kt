package zhaoyun.example.composedemo.scaffold.android

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import zhaoyun.example.composedemo.scaffold.core.mvi.BaseEffect
import zhaoyun.example.composedemo.scaffold.core.mvi.UiEffect
import zhaoyun.example.composedemo.scaffold.core.mvi.UiEvent
import zhaoyun.example.composedemo.scaffold.core.mvi.UiState

/**
 * MVI 通用屏幕包装器 —— 仅负责 [BaseEffect] 的收集与默认处理。
 *
 * State 收集、Effect 收集、initEvent 发送均由调用方（Screen 层）自行组装。
 *
 * ### BaseEffect 扩展指南
 * 1. 在 `:scaffold:core` 的 [BaseEffect] 中添加新的 sealed 子类。
 * 2. 在 [defaultHandleBaseEffect] 中补充默认处理逻辑（如 Toast、Snackbar 等无状态操作）。
 * 3. 若副作用需要 Compose 树中的组件（如 Dialog、Sheet）或导航栈（NavigateBack），
 *    应在调用方通过 [onBaseEffect] 拦截并自行处理；返回 `true` 表示已消费，默认逻辑不再执行。
 *
 * 使用示例：
 * ```
 * val state by viewModel.state.collectAsStateWithLifecycle()
 *
 * LaunchedEffect(Unit) {
 *     viewModel.onEvent(LoginEvent.CheckSession)
 * }
 *
 * MviScreen(
 *     viewModel = viewModel,
 *     onBaseEffect = { effect ->
 *         when (effect) {
 *             is BaseEffect.NavigateBack -> { navController.popBackStack(); true }
 *             else -> false
 *         }
 *     }
 * ) {
 *     LoginPage(state = state, onEvent = viewModel::onEvent)
 * }
 * ```
 */
@Composable
fun <S : UiState, E : UiEvent, F : UiEffect> MviScreen(
    viewModel: BaseViewModel<S, E, F>,
    onBaseEffect: suspend (BaseEffect) -> Boolean = { false },
    content: @Composable () -> Unit
) {
    ServiceRegistryProvider {
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            viewModel.baseEffect.collect { effect ->
                val consumed = onBaseEffect(effect)
                if (!consumed) {
                    defaultHandleBaseEffect(context, effect)
                }
            }
        }
        content()
    }
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
