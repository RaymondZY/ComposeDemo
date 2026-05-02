package zhaoyun.example.composedemo.story.background.domain

import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.scaffold.core.usecase.BaseUseCase

class BackgroundUseCase(
    stateHolder: StateHolder<BackgroundState>,
) : BaseUseCase<BackgroundState, BackgroundEvent, BackgroundEffect>(
    stateHolder,
) {
    override suspend fun onEvent(event: BackgroundEvent) {
        // 当前无交互，占位预留
    }
}
