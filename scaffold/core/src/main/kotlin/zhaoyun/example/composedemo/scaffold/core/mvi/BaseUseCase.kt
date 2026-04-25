package zhaoyun.example.composedemo.scaffold.core.mvi

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

abstract class BaseUseCase<S : UiState, E : UiEvent, F : UiEffect>(
    initialState: S
) {
    private val _internalState = MutableStateFlow(initialState)
    private var _stateHolder: StateHolder<S>? = null

    private val activeState: StateFlow<S>
        get() = _stateHolder?.state ?: _internalState.asStateFlow()

    val state: StateFlow<S> get() = activeState

    private val _effect = Channel<F>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private val _baseEffect = Channel<BaseEffect>(Channel.BUFFERED)
    val baseEffect = _baseEffect.receiveAsFlow()

    protected val currentState: S get() = activeState.value

    protected fun updateState(transform: (S) -> S) {
        _stateHolder?.update(transform) ?: _internalState.update(transform)
    }

    protected fun sendEffect(effect: F) {
        _effect.trySend(effect)
    }

    protected fun sendBaseEffect(effect: BaseEffect) {
        _baseEffect.trySend(effect)
    }

    /**
     * 将该 UseCase 绑定到外部 [StateHolder]（通常由 [BaseViewModel] 提供）。
     * 绑定后，所有状态读写操作都会路由到 [stateHolder]，实现多个 UseCase 共享同一份 State。
     */
    fun bind(stateHolder: StateHolder<S>) {
        _stateHolder = stateHolder
    }

    // ========== 新增：服务发现 ==========

    private var serviceRegistry: ServiceRegistry? = null

    /**
     * 由 [BaseViewModel] 在初始化时注入
     */
    fun attachServiceRegistry(registry: ServiceRegistry) {
        this.serviceRegistry = registry
    }

    /**
     * 在所在作用域内查找服务实现。
     * 找不到时抛异常，提示开发者检查是否忘记实现 [ServiceProvider]。
     */
    protected inline fun <reified T : Any> findService(): T {
        return serviceRegistry?.find<T>()
            ?: error("Service ${T::class.java.name} not found in current scope. " +
                     "Did you forget to let the providing UseCase implement ServiceProvider?")
    }

    /**
     * 在所在作用域内查找服务实现。
     * 找不到时返回 null，不抛异常。
     */
    protected inline fun <reified T : Any> findServiceOrNull(): T? {
        return serviceRegistry?.find<T>()
    }

    // ========== 新增结束 ==========

    abstract suspend fun onEvent(event: E)
}
