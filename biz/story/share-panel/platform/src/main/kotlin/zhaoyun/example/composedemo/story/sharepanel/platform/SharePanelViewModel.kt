package zhaoyun.example.composedemo.story.sharepanel.presentation

import zhaoyun.example.composedemo.scaffold.android.BaseViewModel
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistry
import zhaoyun.example.composedemo.story.sharepanel.domain.SharePanelEffect
import zhaoyun.example.composedemo.story.sharepanel.domain.SharePanelEvent
import zhaoyun.example.composedemo.story.sharepanel.domain.SharePanelState
import zhaoyun.example.composedemo.story.sharepanel.domain.SharePanelUseCase

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
