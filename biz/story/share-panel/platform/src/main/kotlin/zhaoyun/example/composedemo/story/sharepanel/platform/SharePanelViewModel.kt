package zhaoyun.example.composedemo.story.sharepanel.platform

import zhaoyun.example.composedemo.scaffold.platform.BaseViewModel
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistry
import zhaoyun.example.composedemo.story.sharepanel.core.SharePanelEffect
import zhaoyun.example.composedemo.story.sharepanel.core.SharePanelEvent
import zhaoyun.example.composedemo.story.sharepanel.core.SharePanelState
import zhaoyun.example.composedemo.story.sharepanel.core.SharePanelUseCase

class SharePanelViewModel(
    cardId: String,
    backgroundImageUrl: String,
    stateHolder: StateHolder<SharePanelState>,
    serviceRegistry: MutableServiceRegistry,
) : BaseViewModel<SharePanelState, SharePanelEvent, SharePanelEffect>(
    stateHolder,
    serviceRegistry,
    { holder, registry ->
        SharePanelUseCase(
            cardId = cardId,
            backgroundImageUrl = backgroundImageUrl,
            stateHolder = holder,
            serviceRegistry = registry,
        )
    },
)
