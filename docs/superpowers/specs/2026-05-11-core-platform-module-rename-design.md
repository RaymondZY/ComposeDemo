# Core / Platform Module Rename Design

## Goal

Rename the project architecture terminology and module structure from `domain` / `presentation` to `core` / `platform`, and rename the Android scaffold binding module from `scaffold:android` to `scaffold:platform`.

This is a naming and structure migration only. It must not change MVI behavior, business logic, UI behavior, dependency direction, or test intent.

## Approved Scope

The migration is a full semantic rename across current code and current authoritative documentation.

### Module names

- `:scaffold:android` becomes `:scaffold:platform`.
- Every `:biz:*:domain` module becomes `:biz:*:core`.
- Every `:biz:*:presentation` module becomes `:biz:*:platform`.
- `:scaffold:core` keeps its current name.
- `:service:*` modules keep their current names.

Affected business module pairs:

- `home`: `domain -> core`, `presentation -> platform`
- `feed`: `domain -> core`, `presentation -> platform`
- `story`: `domain -> core`, `presentation -> platform`
- `story/background`: `domain -> core`, `presentation -> platform`
- `story/input`: `domain -> core`, `presentation -> platform`
- `story/comment-panel`: `domain -> core`, `presentation -> platform`
- `story/infobar`: `domain -> core`, `presentation -> platform`
- `story/message`: `domain -> core`, `presentation -> platform`
- `story/share-panel`: `domain -> core`, `presentation -> platform`
- `story/story-panel`: `domain -> core`, `presentation -> platform`

### Package names

Package and import names must be renamed consistently:

- `zhaoyun.example.composedemo.scaffold.android` becomes `zhaoyun.example.composedemo.scaffold.platform`.
- `zhaoyun.example.composedemo.<feature>.domain` becomes `zhaoyun.example.composedemo.<feature>.core`.
- `zhaoyun.example.composedemo.<feature>.presentation` becomes `zhaoyun.example.composedemo.<feature>.platform`.

Examples:

- `zhaoyun.example.composedemo.feed.domain` -> `zhaoyun.example.composedemo.feed.core`
- `zhaoyun.example.composedemo.feed.presentation` -> `zhaoyun.example.composedemo.feed.platform`
- `zhaoyun.example.composedemo.story.commentpanel.domain` -> `zhaoyun.example.composedemo.story.commentpanel.core`
- `zhaoyun.example.composedemo.story.commentpanel.presentation` -> `zhaoyun.example.composedemo.story.commentpanel.platform`

## Resulting Architecture

The migrated layering is:

| Layer | Responsibility |
| --- | --- |
| `:scaffold:core` | Pure Kotlin MVI framework core. |
| `:scaffold:platform` | Platform binding layer for Android, Compose, ViewModel, and Koin integration. |
| `:biz:*:core` | Platform-independent business core: State, Event, Effect, UseCase, business rules, repositories, fakes, and JVM tests. |
| `:biz:*:platform` | Platform-specific Android/Compose layer: ViewModel, Composable, Koin module, UI tests, and Android behavior. |
| `:service:*` | Service APIs, implementations, and test fakes. |
| `:app` | Application assembly and module registration. |

Dependency rules after migration:

- `:biz:*:core` may depend on `:scaffold:core` and `:service:*:api`.
- `:biz:*:platform` may depend on AndroidX/Compose, `:scaffold:platform`, and its corresponding `:biz:*:core`.
- `:scaffold:platform` may depend on AndroidX/Compose/Koin and `:scaffold:core`.
- `:app` depends on the platform modules it assembles.

## Code Migration Requirements

### Gradle

Update:

- `settings.gradle.kts` includes.
- `app/build.gradle.kts` dependencies.
- All affected `build.gradle.kts` files under `biz/` and `scaffold/`.
- Android `namespace` values that currently contain `.presentation` or `.android`.

Project dependencies must be renamed:

- `project(":...:domain")` -> `project(":...:core")`
- `project(":...:presentation")` -> `project(":...:platform")`
- `project(":scaffold:android")` -> `project(":scaffold:platform")`

### Filesystem

Rename module directories:

- `scaffold/android/` -> `scaffold/platform/`
- `biz/**/domain/` -> `biz/**/core/`
- `biz/**/presentation/` -> `biz/**/platform/`

Kotlin source and test package directories should move with the package rename where practical, so path names match declared package names.

### Kotlin symbols

Update package declarations and imports across production, unit test, and Android test sources.

Update DI and test symbols that encode the old layer name:

- `*PresentationModule.kt` -> `*PlatformModule.kt`
- `*PresentationModuleTest` -> `*PlatformModuleTest`
- `*presentationModule` -> `*platformModule`
- Test display names that say `presentation module` -> `platform module`

Module aggregation names such as `feedModules` and `homeModules` can remain because they do not encode the layer name.

Business names should not be renamed unless they contain the old layer name. Keep names such as:

- `UseCase`
- `State`
- `Event`
- `Effect`
- `ViewModel`
- `Screen`

## Documentation Requirements

Update current authoritative documentation:

- `AGENTS.md`
- `docs/arch/*.md`
- `docs/rules/*.md`
- `docs/skills/*/SKILL.md`
- Current `feature.md` files under moved `core` and `platform` modules

The documentation should describe the new terms as the current architecture:

- `core` means platform-independent business core.
- `platform` means platform-specific binding and UI integration.

Historical documents under `docs/superpowers/specs/` and `docs/superpowers/plans/` must not be bulk rewritten. Add a clear historical naming note instead: older archived specs/plans may use `domain` for current `core`, `presentation` for current `platform`, and `scaffold:android` for current `scaffold:platform`.

## Migration Steps

1. Rename directories with Git-aware moves so the diff remains reviewable.
2. Update Gradle includes, dependencies, and Android namespaces.
3. Update package declarations, imports, and source directory names.
4. Rename DI module files, functions, tests, and display names from `Presentation` to `Platform`.
5. Update authoritative documentation and project skills.
6. Add the historical naming note for archived superpowers specs/plans.
7. Run search validation.
8. Run Gradle verification.
9. Run documentation consistency checks.

## Verification

### Search validation

Run repository searches to ensure current code and authoritative docs no longer use old layer names as current architecture:

- `:biz:*:domain`
- `:biz:*:presentation`
- `:scaffold:android`
- `scaffold.android`
- package names ending in `.domain` or `.presentation`
- DI symbols containing `PresentationModule`

Historical archived specs and plans may still contain old names, provided the historical naming note exists.

### Gradle validation

Minimum verification commands:

```bash
./gradlew :scaffold:platform:test
./gradlew :biz:story:comment-panel:core:test
./gradlew :biz:story:platform:testDebugUnitTest
./gradlew :app:compileDebugKotlin
```

If time allows, also run:

```bash
./gradlew test
```

### Documentation validation

Follow the project `doc-sync` skill:

- Check `AGENTS.md` and `docs/**/*.md`.
- Validate internal relative links.
- Check module names, paths, sample commands, and architecture descriptions for consistency.
- Pay special attention to `docs/rules/module-development.md` and `docs/skills/declare-feature/SKILL.md`, because they guide future feature work.

## Risks and Mitigations

| Risk | Mitigation |
| --- | --- |
| Gradle still references old modules. | Treat this as a configuration bug. Fix `settings.gradle.kts` and build scripts instead of recreating old directories. |
| Bulk rename changes historical records incorrectly. | Exclude archived specs/plans from bulk text migration and add a historical naming note. |
| Kotlin packages and source paths diverge. | Move package directories where practical and verify with compilation. |
| DI symbols keep old terminology. | Rename `PresentationModule` files, functions, and tests to `PlatformModule`. |
| Old terminology remains in current docs. | Run search validation and doc-sync review before completion. |

## Non-Goals

- No behavior changes.
- No MVI framework redesign.
- No dependency rule changes beyond renamed module paths.
- No compatibility wrappers for old module names or old package names.
- No bulk rewrite of archived historical specs and plans.
