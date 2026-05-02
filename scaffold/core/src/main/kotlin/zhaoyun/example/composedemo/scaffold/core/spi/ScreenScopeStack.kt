package zhaoyun.example.composedemo.scaffold.core.spi

import org.koin.core.scope.Scope

object ScreenScopeStack {
    private val stack = ArrayDeque<Scope>()

    val current: Scope? get() = stack.lastOrNull()

    fun push(scope: Scope) {
        stack.addLast(scope)
    }

    fun pop() {
        stack.removeLastOrNull()
    }

    fun requireCurrent(): Scope {
        return current ?: error(
            "No active Koin Scope found. " +
                "Make sure this is created within a Screen Koin Scope."
        )
    }
}
