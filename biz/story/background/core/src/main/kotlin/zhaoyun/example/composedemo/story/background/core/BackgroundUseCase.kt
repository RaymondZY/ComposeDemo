package zhaoyun.example.composedemo.story.background.core

import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistry
import zhaoyun.example.composedemo.scaffold.core.usecase.BaseUseCase

class BackgroundUseCase(
    stateHolder: StateHolder<BackgroundState>,
    serviceRegistry: MutableServiceRegistry,
) : BaseUseCase<BackgroundState, BackgroundEvent, BackgroundEffect>(
    stateHolder,
    serviceRegistry,
) {
    override suspend fun onEvent(event: BackgroundEvent) {
        // 当前无交互，占位预留
    }
}
