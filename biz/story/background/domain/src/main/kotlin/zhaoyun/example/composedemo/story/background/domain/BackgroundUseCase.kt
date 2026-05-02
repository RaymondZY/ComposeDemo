package zhaoyun.example.composedemo.story.background.domain

import zhaoyun.example.composedemo.scaffold.core.usecase.BaseUseCase
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder

class BackgroundUseCase(
    stateHolder: StateHolder<BackgroundState>? = null,
) : BaseUseCase<BackgroundState, BackgroundEvent, BackgroundEffect>(
    initialState = BackgroundState(),
    stateHolder = stateHolder,
) {
    override suspend fun onEvent(event: BackgroundEvent) {
        // 当前无交互，占位预留
    }
}
