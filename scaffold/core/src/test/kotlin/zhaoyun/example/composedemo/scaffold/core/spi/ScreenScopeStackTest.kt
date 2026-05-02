package zhaoyun.example.composedemo.scaffold.core.spi

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module

class ScreenScopeStackTest {

    @After
    fun tearDown() {
        while (ScreenScopeStack.current != null) {
            ScreenScopeStack.pop()
        }
        stopKoin()
    }

    @Test(expected = IllegalStateException::class)
    fun `requireCurrent throws when stack is empty`() {
        ScreenScopeStack.requireCurrent()
    }

    @Test
    fun `push and pop maintain LIFO order`() {
        startKoin {
            modules(module {
                scope(named("test")) { }
            })
        }

        val koin = org.koin.core.context.GlobalContext.get()
        val scope1 = koin.createScope("scope1", named("test"))
        val scope2 = koin.createScope("scope2", named("test"))
        val scope3 = koin.createScope("scope3", named("test"))

        assertNull(ScreenScopeStack.current)

        ScreenScopeStack.push(scope1)
        assertSame(scope1, ScreenScopeStack.current)

        ScreenScopeStack.push(scope2)
        assertSame(scope2, ScreenScopeStack.current)

        ScreenScopeStack.push(scope3)
        assertSame(scope3, ScreenScopeStack.current)

        ScreenScopeStack.pop()
        assertSame(scope2, ScreenScopeStack.current)

        ScreenScopeStack.pop()
        assertSame(scope1, ScreenScopeStack.current)

        ScreenScopeStack.pop()
        assertNull(ScreenScopeStack.current)
    }

    @Test
    fun `requireCurrent returns current scope when stack is not empty`() {
        startKoin {
            modules(module {
                scope(named("test")) { }
            })
        }

        val koin = org.koin.core.context.GlobalContext.get()
        val scope = koin.createScope("scope1", named("test"))

        ScreenScopeStack.push(scope)
        assertSame(scope, ScreenScopeStack.requireCurrent())
    }
}
