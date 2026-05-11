package zhaoyun.example.composedemo.scaffold.android

import org.koin.core.qualifier.named

/**
 * Koin scope qualifiers used by the MVI architecture.
 *
 * Presentation modules should use these constants instead of hard-coding
 * scope names, so that they depend on the architecture infrastructure
 * rather than specific usage scenarios.
 */
object MviKoinScopes {
    /** Scope for full-screen MVI screens (e.g. HomeScreen, FeedScreen). */
    val Screen = named("MviScreenScope")

    /** Scope for list-item / nested MVI components (e.g. StoryCard in a pager). */
    val Item = named("MviScope")
}
