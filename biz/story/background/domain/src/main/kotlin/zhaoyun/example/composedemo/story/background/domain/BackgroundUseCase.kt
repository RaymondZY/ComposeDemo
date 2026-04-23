package zhaoyun.example.composedemo.story.background.domain

import zhaoyun.example.composedemo.scaffold.core.mvi.BaseUseCase

class BackgroundUseCase : BaseUseCase<BackgroundState, BackgroundEvent, BackgroundEffect>(
    BackgroundState()
) {
    override suspend fun onEvent(event: BackgroundEvent) {
        // 当前无交互，占位预留
    }
}
