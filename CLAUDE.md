# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

```bash
# Build the project
./gradlew assembleDebug

# Run all unit tests
./gradlew test

# Run a single module's tests
./gradlew :scaffold:core:test
./gradlew :biz:story:message:domain:test

# Run Android instrumented tests
./gradlew connectedAndroidTest

# Lint
./gradlew lint
```

## Architecture Overview

This is an Android Jetpack Compose app built on a custom **MVI (Model-View-Intent)** framework. The full framework spec is in `MVI_FRAMEWORK.md`.

### Module Structure

```
:scaffold:core          — Pure Kotlin MVI abstractions (no Android deps)
:scaffold:android       — Android/Compose bindings (BaseViewModel, MviScreen, MviScope)
:service:xxx:api        — Repository interfaces + models
:service:xxx:impl       — Real implementations (currently mock)
:service:xxx:mock       — Fake implementations for tests
:biz:xxx:domain         — State/Event/Effect/UseCase (pure Kotlin, unit-testable)
:biz:xxx:presentation   — ViewModel + Composable + Koin DI module
:app                    — Application wiring (Koin initialization)
```

### MVI Data Flow

```
Composable → sendEvent() → BaseViewModel → CombineUseCase → BaseUseCase.onEvent()
                                                                    ↓
                                                             updateState()
                                                                    ↓
                                                            StateHolder → StateFlow → UI
```

### Key Abstractions

- **`StateHolder<S>`** (`scaffold/core/.../StateHolder.kt`): Holds a `StateFlow<S>` with `updateState`. Call `.derive()` to create a child `StateHolder` that slices and writes back to the parent state — this is how `StoryCardViewModel` shares sub-states with child ViewModels.
- **`BaseUseCase<S,E,F>`** (`scaffold/core/.../usecase/BaseUseCase.kt`): All business logic lives here. Takes a `StateHolder` and `MutableServiceRegistry` at construction. Never put logic in ViewModel.
- **`CombineUseCase<S,E,F>`** (`scaffold/core/.../usecase/CombineUseCase.kt`): Fans out events to multiple child UseCases and merges their effects. `BaseViewModel` always wraps its children in one.
- **`BaseViewModel<S,E,F>`** (`scaffold/android/.../BaseViewModel.kt`): Lifecycle bridge only. Delegates everything to `CombineUseCase`. Call `sendEvent()` from UI.
- **`MviScreen<VM>`** (`scaffold/android/.../MviScreen.kt`): Composable that creates an isolated Koin scope + `ServiceRegistry`, instantiates the root ViewModel, and collects `baseEffect`. Every top-level screen must be wrapped in this.
- **`MviScope` / `MviItemScope`** (`scaffold/android/.../MviScope.kt`): Lighter scope for list items or nested sub-screens that need their own registry scope but aren't full screens.
- **`ServiceRegistry`** (`scaffold/core/.../spi/ServiceRegistry.kt`): Allows UseCases to discover sibling UseCases by interface within the same screen scope. Lookup order: current screen registry → parent screen registry → Koin global.

### Nested State Sharing Pattern

The `StoryCardPage` is the canonical example: a parent ViewModel owns the aggregate `StoryCardState`, and creates derived `StateHolder` slices for each sub-component:

```kotlin
// Parent ViewModel
val messageStateHolder: StateHolder<MessageState> by lazy {
    stateHolder.derive(StoryCardState::message) { copy(message = it) }
}

// Child ViewModel receives the derived holder
class MessageViewModel(stateHolder: StateHolder<MessageState>, registry: MutableServiceRegistry)
    : BaseViewModel<MessageState, MessageEvent, MessageEffect>(stateHolder, registry, ...)
```

Sub-Composables call `screenViewModel(key)` with a `parametersOf(stateHolder)` to get their ViewModel within the parent's Koin scope.

### Testing

Domain logic is tested in `:domain` modules with plain JUnit — no ViewModel or Android required:

```kotlin
val useCase = MessageUseCase(MessageState().toStateHolder(), FakeMutableServiceRegistry())
useCase.onEvent(MessageEvent.OnDialogueClicked)
assertTrue(useCase.currentState.isExpanded)
```

ViewModel-level tests inject a `StateHolderImpl` directly. See `scaffold/android/.../BaseViewModelTest.kt` for patterns.

### DI Wiring

All Koin modules are registered in `ComposeDemoApp.onCreate()`. Each `presentation` module exposes a `val xxxPresentationModule`. Child ViewModels that require an injected `StateHolder` declare a factory with `parametersOf`:

```kotlin
// In the DI module
viewModel { (stateHolder: StateHolder<MessageState>, registry: MutableServiceRegistry) ->
    MessageViewModel(stateHolder, registry)
}
```

### Effect Types

- **`UiEffect` subtype** (`sealed class XxxEffect`): business-level one-shot events (navigation, open sheet). Collected per-screen.
- **`BaseEffect`** (`scaffold/core/.../BaseEffect.kt`): framework-level effects (Toast, Dialog, NavigateBack). `MviScreen` enforces that every `BaseEffect` is handled — unhandled effects crash at runtime.
