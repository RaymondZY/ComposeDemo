package zhaoyun.example.composedemo.story.infobar.platform

import zhaoyun.example.composedemo.scaffold.platform.BaseViewModel
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistry
import zhaoyun.example.composedemo.story.infobar.core.InfoBarEffect
import zhaoyun.example.composedemo.story.infobar.core.InfoBarEvent
import zhaoyun.example.composedemo.story.infobar.core.InfoBarState
import zhaoyun.example.composedemo.story.infobar.core.InfoBarUseCase

class InfoBarViewModel(
    cardId: String,
    stateHolder: StateHolder<InfoBarState>,
    serviceRegistry: MutableServiceRegistry,
) : BaseViewModel<InfoBarState, InfoBarEvent, InfoBarEffect>(
    stateHolder,
    serviceRegistry,
    { holder, registry ->
        InfoBarUseCase(
            cardId = cardId,
            stateHolder = holder,
            serviceRegistry = registry,
        )
    },
)
