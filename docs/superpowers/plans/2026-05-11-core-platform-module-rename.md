# Core Platform Module Rename Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rename current architecture modules and code from `domain` / `presentation` to `core` / `platform`, and rename `:scaffold:android` to `:scaffold:platform`.

**Architecture:** This is a structural and semantic rename only. The MVI flow, business logic, UI behavior, dependency direction, and test intent stay unchanged. Current code and authoritative docs move to `core/platform`; archived `docs/superpowers/specs` and `docs/superpowers/plans` keep historical wording, with a README note explaining old names.

**Tech Stack:** Kotlin, Gradle Kotlin DSL, Jetpack Compose, AndroidX ViewModel, Koin, JUnit, Android instrumentation tests, repository docs in Markdown.

---

## File Structure

### Rename module directories

- Rename: `scaffold/android` -> `scaffold/platform`
- Rename: `biz/home/domain` -> `biz/home/core`
- Rename: `biz/home/presentation` -> `biz/home/platform`
- Rename: `biz/feed/domain` -> `biz/feed/core`
- Rename: `biz/feed/presentation` -> `biz/feed/platform`
- Rename: `biz/story/domain` -> `biz/story/core`
- Rename: `biz/story/presentation` -> `biz/story/platform`
- Rename: `biz/story/background/domain` -> `biz/story/background/core`
- Rename: `biz/story/background/presentation` -> `biz/story/background/platform`
- Rename: `biz/story/input/domain` -> `biz/story/input/core`
- Rename: `biz/story/input/presentation` -> `biz/story/input/platform`
- Rename: `biz/story/comment-panel/domain` -> `biz/story/comment-panel/core`
- Rename: `biz/story/comment-panel/presentation` -> `biz/story/comment-panel/platform`
- Rename: `biz/story/infobar/domain` -> `biz/story/infobar/core`
- Rename: `biz/story/infobar/presentation` -> `biz/story/infobar/platform`
- Rename: `biz/story/message/domain` -> `biz/story/message/core`
- Rename: `biz/story/message/presentation` -> `biz/story/message/platform`
- Rename: `biz/story/share-panel/domain` -> `biz/story/share-panel/core`
- Rename: `biz/story/share-panel/presentation` -> `biz/story/share-panel/platform`
- Rename: `biz/story/story-panel/domain` -> `biz/story/story-panel/core`
- Rename: `biz/story/story-panel/presentation` -> `biz/story/story-panel/platform`

### Modify build files

- Modify: `settings.gradle.kts`
- Modify: `app/build.gradle.kts`
- Modify: `scaffold/platform/build.gradle.kts`
- Modify: `biz/**/core/build.gradle.kts`
- Modify: `biz/**/platform/build.gradle.kts`

### Modify Kotlin and test source

- Modify: `app/src/main/java/**/*.kt`
- Modify: `scaffold/platform/src/**/*.kt`
- Modify: `biz/**/core/src/**/*.kt`
- Modify: `biz/**/platform/src/**/*.kt`

### Modify authoritative docs and project skills

- Modify: `AGENTS.md`
- Modify: `docs/arch/overview.md`
- Modify: `docs/arch/mvi.md`
- Modify: `docs/arch/di.md`
- Modify: `docs/arch/usecase.md`
- Modify: `docs/rules/module-development.md`
- Modify: `docs/skills/declare-feature/SKILL.md`
- Modify: `docs/skills/doc-sync/SKILL.md` only if its examples mention old module names after the rename.
- Modify: `docs/superpowers/README.md`
- Modify: moved `biz/**/core/feature.md` and `biz/**/platform/feature.md`

---

### Task 1: Rename module and package directories

**Files:**
- Rename all module directories listed in File Structure.
- Rename source package directories named `domain`, `presentation`, and `android` after the module move.

- [ ] **Step 1: Verify clean worktree**

Run:

```bash
git status --short
```

Expected: no output. If there is output, stop and inspect it before moving directories.

- [ ] **Step 2: Move scaffold module directory**

Run:

```bash
git mv scaffold/android scaffold/platform
```

Expected: command exits with code 0.

- [ ] **Step 3: Move top-level biz module directories**

Run:

```bash
git mv biz/home/domain biz/home/core
git mv biz/home/presentation biz/home/platform
git mv biz/feed/domain biz/feed/core
git mv biz/feed/presentation biz/feed/platform
git mv biz/story/domain biz/story/core
git mv biz/story/presentation biz/story/platform
```

Expected: all commands exit with code 0.

- [ ] **Step 4: Move story child module directories**

Run:

```bash
git mv biz/story/background/domain biz/story/background/core
git mv biz/story/background/presentation biz/story/background/platform
git mv biz/story/input/domain biz/story/input/core
git mv biz/story/input/presentation biz/story/input/platform
git mv biz/story/comment-panel/domain biz/story/comment-panel/core
git mv biz/story/comment-panel/presentation biz/story/comment-panel/platform
git mv biz/story/infobar/domain biz/story/infobar/core
git mv biz/story/infobar/presentation biz/story/infobar/platform
git mv biz/story/message/domain biz/story/message/core
git mv biz/story/message/presentation biz/story/message/platform
git mv biz/story/share-panel/domain biz/story/share-panel/core
git mv biz/story/share-panel/presentation biz/story/share-panel/platform
git mv biz/story/story-panel/domain biz/story/story-panel/core
git mv biz/story/story-panel/presentation biz/story/story-panel/platform
```

Expected: all commands exit with code 0.

- [ ] **Step 5: Move package directories inside source sets**

Run:

```bash
find biz scaffold/platform -depth -type d -name domain -print | while read dir; do git mv "$dir" "${dir%/domain}/core"; done
find biz scaffold/platform -depth -type d -name presentation -print | while read dir; do git mv "$dir" "${dir%/presentation}/platform"; done
find scaffold/platform -depth -type d -name android -print | while read dir; do git mv "$dir" "${dir%/android}/platform"; done
```

Expected: command exits with code 0. Directories containing package paths now use `core` or `platform`.

- [ ] **Step 6: Check directory state**

Run:

```bash
find biz -maxdepth 4 -type d \( -name domain -o -name presentation \) -print
find scaffold -maxdepth 4 -type d -name android -print
```

Expected: no output.

- [ ] **Step 7: Commit structural moves**

Run:

```bash
git status --short
git add biz scaffold
git commit -m "refactor: rename modules to core platform"
```

Expected: commit succeeds. The status before commit shows renames and no deleted old module directories left unstaged.

---

### Task 2: Update Gradle modules, dependencies, namespaces, packages, and imports

**Files:**
- Modify: `settings.gradle.kts`
- Modify: `app/build.gradle.kts`
- Modify: `scaffold/platform/build.gradle.kts`
- Modify: `biz/**/core/build.gradle.kts`
- Modify: `biz/**/platform/build.gradle.kts`
- Modify: `app/src/main/java/**/*.kt`
- Modify: `biz/**/*.kt`
- Modify: `scaffold/platform/src/**/*.kt`

- [ ] **Step 1: Apply mechanical text migration to current code and authoritative docs**

Run:

```bash
FILES=$(rg -l '(:biz:[^[:space:]]*:(domain|presentation)|:scaffold:android|scaffold\.android|\.domain|\.presentation|/domain|/presentation|\bdomain\b|\bpresentation\b|Domain|Presentation)' settings.gradle.kts app biz scaffold AGENTS.md docs/arch docs/rules docs/skills docs/superpowers/README.md)
perl -0pi -e 's/:scaffold:android/:scaffold:platform/g; s/scaffold\.android/scaffold.platform/g; s/:domain/:core/g; s/:presentation/:platform/g; s/\.domain/\.core/g; s/\.presentation/\.platform/g; s#/domain#/core#g; s#/presentation#/platform#g; s/\bdomain\b/core/g; s/\bpresentation\b/platform/g; s/Domain/Core/g; s/Presentation/Platform/g; s/domain 层/core 层/g; s/presentation 层/platform 层/g' $FILES
```

Expected: command exits with code 0. This intentionally excludes `docs/superpowers/specs` and `docs/superpowers/plans`.

- [ ] **Step 2: Inspect Gradle includes**

Run:

```bash
sed -n '24,56p' settings.gradle.kts
```

Expected output includes these lines and no `:domain`, `:presentation`, or `:scaffold:android` entries:

```kotlin
include(":scaffold:core")
include(":scaffold:platform")
include(":biz:home:core")
include(":biz:home:platform")
include(":biz:feed:core")
include(":biz:feed:platform")
include(":biz:story:core")
include(":biz:story:platform")
include(":biz:story:message:core")
include(":biz:story:message:platform")
include(":biz:story:infobar:core")
include(":biz:story:infobar:platform")
include(":biz:story:comment-panel:core")
include(":biz:story:comment-panel:platform")
include(":biz:story:share-panel:core")
include(":biz:story:share-panel:platform")
include(":biz:story:input:core")
include(":biz:story:input:platform")
include(":biz:story:background:core")
include(":biz:story:background:platform")
include(":biz:story:story-panel:core")
include(":biz:story:story-panel:platform")
```

- [ ] **Step 3: Inspect app dependencies**

Run:

```bash
sed -n '40,58p' app/build.gradle.kts
```

Expected: dependencies point to `:biz:*:platform`, not `:biz:*:presentation`.

- [ ] **Step 4: Inspect representative platform module build files**

Run:

```bash
sed -n '1,48p' biz/feed/platform/build.gradle.kts
sed -n '1,48p' biz/story/platform/build.gradle.kts
sed -n '1,48p' scaffold/platform/build.gradle.kts
```

Expected:

- Android namespaces end in `.platform`.
- Platform modules depend on `project(":scaffold:platform")`.
- Platform modules depend on corresponding `project(":...:core")`.
- `scaffold/platform/build.gradle.kts` namespace is `zhaoyun.example.composedemo.scaffold.platform`.

- [ ] **Step 5: Run Gradle project discovery**

Run:

```bash
./gradlew projects
```

Expected: `BUILD SUCCESSFUL`, with new modules listed and no old `:biz:*:domain`, `:biz:*:presentation`, or `:scaffold:android` projects.

- [ ] **Step 6: Commit Gradle and package text migration**

Run:

```bash
git status --short
git add settings.gradle.kts app biz scaffold AGENTS.md docs/arch docs/rules docs/skills docs/superpowers/README.md
git commit -m "refactor: update core platform package references"
```

Expected: commit succeeds.

---

### Task 3: Rename DI platform symbols and files

**Files:**
- Rename: `biz/home/platform/src/main/kotlin/**/HomePresentationModule.kt` -> `HomePlatformModule.kt`
- Rename: `biz/feed/platform/src/main/kotlin/**/FeedPresentationModule.kt` -> `FeedPlatformModule.kt`
- Rename: `biz/story/platform/src/main/kotlin/**/StoryPresentationModule.kt` -> `StoryPlatformModule.kt`
- Rename: all story child `*PresentationModule.kt` files under `biz/story/*/platform`
- Rename: all `*PresentationModuleTest.kt` files under `biz/**/platform/src/test`
- Modify: all imports and references to `*PresentationModule`, `*presentationModule`, and test display names.

- [ ] **Step 1: Rename DI module and test file names**

Run:

```bash
find biz -path '*/platform/src/*' -name '*PresentationModule.kt' -print | while read file; do git mv "$file" "${file%PresentationModule.kt}PlatformModule.kt"; done
find biz -path '*/platform/src/*' -name '*PresentationModuleTest.kt' -print | while read file; do git mv "$file" "${file%PresentationModuleTest.kt}PlatformModuleTest.kt"; done
```

Expected: command exits with code 0.

- [ ] **Step 2: Rename symbol names and test display text**

Run:

```bash
FILES=$(rg -l 'PresentationModule|presentationModule|presentation module|Presentation Module|presentation layer|Presentation layer' app biz docs/arch docs/rules docs/skills AGENTS.md)
perl -0pi -e 's/PresentationModule/PlatformModule/g; s/presentationModule/platformModule/g; s/presentation module/platform module/g; s/Presentation Module/Platform Module/g; s/presentation layer/platform layer/g; s/Presentation layer/Platform layer/g' $FILES
```

Expected: command exits with code 0.

- [ ] **Step 3: Inspect app DI registration**

Run:

```bash
sed -n '1,90p' app/src/main/java/zhaoyun/example/composedemo/ComposeDemoApp.kt
```

Expected: imports and module list use `*.platform.di.*PlatformModule` names such as `feedPlatformModule`, `homePlatformModule`, `storyPlatformModule`, and story child platform modules.

- [ ] **Step 4: Search for stale DI symbols**

Run:

```bash
rg -n 'PresentationModule|presentationModule|presentation module' app biz scaffold docs/arch docs/rules docs/skills AGENTS.md
```

Expected: no output.

- [ ] **Step 5: Commit DI symbol rename**

Run:

```bash
git status --short
git add app biz docs/arch docs/rules docs/skills AGENTS.md
git commit -m "refactor: rename platform DI modules"
```

Expected: commit succeeds.

---

### Task 4: Update authoritative documentation and historical archive note

**Files:**
- Modify: `AGENTS.md`
- Modify: `docs/arch/overview.md`
- Modify: `docs/arch/mvi.md`
- Modify: `docs/arch/di.md`
- Modify: `docs/arch/usecase.md`
- Modify: `docs/rules/module-development.md`
- Modify: `docs/skills/declare-feature/SKILL.md`
- Modify: `docs/superpowers/README.md`
- Modify: moved `biz/**/core/feature.md`
- Modify: moved `biz/**/platform/feature.md`

- [ ] **Step 1: Review authoritative docs for old layer names**

Run:

```bash
rg -n 'domain|presentation|Domain|Presentation|:scaffold:android|scaffold/android|scaffold\.android' AGENTS.md docs/arch docs/rules docs/skills biz -g '*.md'
```

Expected: no output except deliberate explanation that older archived docs may use old names. If output appears in current docs or feature files, edit it to `core/platform` language.

- [ ] **Step 2: Add or verify historical naming note**

Open `docs/superpowers/README.md` and ensure it contains this text under `## 使用规则`:

```markdown
- 历史规格和计划中可能使用旧命名：`domain` 对应当前 `core`，`presentation` 对应当前 `platform`，`:scaffold:android` 对应当前 `:scaffold:platform`。
```

If the line is missing, add it after the existing line `- 本目录内容不作为当前架构权威来源。`.

- [ ] **Step 3: Verify key architecture docs mention new layers**

Run:

```bash
rg -n ':biz:\*:core|:biz:\*:platform|:scaffold:platform|core 层|platform 层|平台无关|平台相关' docs/arch/overview.md docs/rules/module-development.md docs/skills/declare-feature/SKILL.md AGENTS.md
```

Expected: output shows the current docs describe `core/platform` modules and feature paths.

- [ ] **Step 4: Run Markdown link smoke check**

Run:

```bash
rg -n '\]\(([^)#]+)\)' AGENTS.md docs -g '*.md'
```

Expected: manually inspect changed links. Relative links in changed files must still point to existing files after directory rename.

- [ ] **Step 5: Commit documentation updates**

Run:

```bash
git status --short
git add AGENTS.md docs biz
git commit -m "docs: update architecture docs for core platform"
```

Expected: commit succeeds.

---

### Task 5: Search validation and compile fixes

**Files:**
- Modify only files reported by search or compiler failures.

- [ ] **Step 1: Search for stale module paths and scaffold package names**

Run:

```bash
rg -n ':biz:[^[:space:]`")]*:(domain|presentation)|:scaffold:android|scaffold\.android' settings.gradle.kts app biz scaffold docs/arch docs/rules docs/skills AGENTS.md
```

Expected: no output.

- [ ] **Step 2: Search for stale Kotlin package names**

Run:

```bash
rg -n 'zhaoyun\.example\.composedemo\.[A-Za-z0-9_.]*(domain|presentation)|package .*\.domain|package .*\.presentation|import .*\.domain|import .*\.presentation' app biz scaffold
```

Expected: no output.

- [ ] **Step 3: Search for stale source directories**

Run:

```bash
find biz scaffold -type d \( -name domain -o -name presentation \) -print
find scaffold/platform/src -type d -path '*/scaffold/android' -print
```

Expected: no output. This avoids matching valid Android source sets such as `src/androidTest`.

- [ ] **Step 4: Compile scaffold platform**

Run:

```bash
./gradlew :scaffold:platform:compileDebugKotlin :scaffold:platform:compileDebugAndroidTestKotlin
```

Expected: `BUILD SUCCESSFUL`. If it fails, fix package/import/build references reported by the compiler and rerun this step.

- [ ] **Step 5: Compile representative business platform modules**

Run:

```bash
./gradlew :biz:feed:platform:compileDebugKotlin :biz:story:platform:compileDebugKotlin :biz:story:comment-panel:platform:compileDebugKotlin
```

Expected: `BUILD SUCCESSFUL`. If it fails, fix package/import/build references reported by the compiler and rerun this step.

- [ ] **Step 6: Compile app**

Run:

```bash
./gradlew :app:compileDebugKotlin
```

Expected: `BUILD SUCCESSFUL`. If it fails, fix package/import/build references reported by the compiler and rerun this step.

- [ ] **Step 7: Commit compile fixes**

Run:

```bash
git status --short
git add app biz scaffold settings.gradle.kts docs AGENTS.md
git commit -m "fix: resolve core platform migration references"
```

Expected: commit succeeds if Step 4 through Step 6 required fixes. If there are no changes, skip this commit.

---

### Task 6: Required tests and final verification

**Files:**
- No planned file changes. Only fix files if verification reveals stale references.

- [ ] **Step 1: Run required unit tests**

Run:

```bash
./gradlew :scaffold:platform:test :biz:story:comment-panel:core:test :biz:story:platform:testDebugUnitTest
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 2: Run required app compile**

Run:

```bash
./gradlew :app:compileDebugKotlin
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Run broader JVM tests if time allows**

Run:

```bash
./gradlew test
```

Expected: `BUILD SUCCESSFUL`. If this is too slow or environment-blocked, record the reason in the final handoff.

- [ ] **Step 4: Run final stale-name search with archive exclusion**

Run:

```bash
rg -n ':biz:[^[:space:]`")]*:(domain|presentation)|:scaffold:android|scaffold\.android|PresentationModule|presentationModule' settings.gradle.kts app biz scaffold AGENTS.md docs/arch docs/rules docs/skills
rg -n 'package .*\.domain|package .*\.presentation|import .*\.domain|import .*\.presentation' app biz scaffold
```

Expected: no output.

- [ ] **Step 5: Confirm historical archive note exists**

Run:

```bash
rg -n 'domain.*core|presentation.*platform|scaffold:android.*scaffold:platform' docs/superpowers/README.md
```

Expected: output includes the historical naming note.

- [ ] **Step 6: Review final diff**

Run:

```bash
git status --short
git diff --stat HEAD
```

Expected: no unstaged changes if all commits were made. If changes remain from verification fixes, inspect, test, and commit them with:

```bash
git add app biz scaffold settings.gradle.kts docs AGENTS.md
git commit -m "chore: finish core platform rename"
```

Expected: final worktree is clean.
