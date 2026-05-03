package zhaoyun.example.composedemo.story.infobar.presentation

import zhaoyun.example.composedemo.scaffold.android.BaseViewModel
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistry
import zhaoyun.example.composedemo.story.infobar.domain.InfoBarEffect
import zhaoyun.example.composedemo.story.infobar.domain.InfoBarEvent
import zhaoyun.example.composedemo.story.infobar.domain.InfoBarState
import zhaoyun.example.composedemo.story.infobar.domain.InfoBarUseCase

class InfoBarViewModel(
    cardId: String,
    stateHolder: StateHolder<InfoBarState>,
    serviceRegistry: MutableServiceRegistry,
) : BaseViewModel<InfoBarState, InfoBarEvent, InfoBarEffect>(
    stateHolder,
    serviceRegistry,
    { holder, registry -> InfoBarUseCase(cardId = cardId, stateHolder = holder, serviceRegistry = registry) },
)
