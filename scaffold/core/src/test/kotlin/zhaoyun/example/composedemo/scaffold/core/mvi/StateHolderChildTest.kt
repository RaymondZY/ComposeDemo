package zhaoyun.example.composedemo.scaffold.core.mvi

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StateHolderChildTest {

    // ─── 测试数据 ───

    data class ParentState(
        val child: ChildState = ChildState(),
        val name: String = "parent"
    ) : UiState

    data class ChildState(
        val grandChild: GrandChildState = GrandChildState(),
        val value: Int = 0
    ) : UiState

    data class GrandChildState(
        val text: String = ""
    ) : UiState

    // ─── 一级 child 测试 ───

    @Test
    fun `一级 child 创建后 state 正确派生`() = runTest {
        val parent = LocalStateHolder(ParentState())
        val child = parent.createChild(
            scope = backgroundScope,
            selector = { it.child },
            updater = { parent, child -> parent.copy(child = child) }
        )

        assertEquals(ChildState(), child.state.value)
    }

    @Test
    fun `一级 child update 后父状态同步更新`() = runTest {
        val parent = LocalStateHolder(ParentState())
        val child = parent.createChild(
            scope = backgroundScope,
            selector = { it.child },
            updater = { parent, child -> parent.copy(child = child) }
        )

        child.update { it.copy(value = 42) }
        advanceUntilIdle()

        assertEquals(42, parent.state.value.child.value)
        assertEquals(42, child.state.value.value)
    }

    @Test
    fun `一级 child 父状态变化后子状态自动派生`() = runTest {
        val parent = LocalStateHolder(ParentState())
        val child = parent.createChild(
            scope = backgroundScope,
            selector = { it.child },
            updater = { parent, child -> parent.copy(child = child) }
        )

        parent.update { it.copy(child = it.child.copy(value = 99)) }
        advanceUntilIdle()

        assertEquals(99, child.state.value.value)
    }

    // ─── 二级 child 测试 ───

    @Test
    fun `二级 child 创建后 state 正确派生`() = runTest {
        val parent = LocalStateHolder(ParentState())
        val child = parent.createChild(
            scope = backgroundScope,
            selector = { it.child },
            updater = { parent, child -> parent.copy(child = child) }
        )
        val grandChild = child.createChild(
            scope = backgroundScope,
            selector = { it.grandChild },
            updater = { child, grandChild -> child.copy(grandChild = grandChild) }
        )

        assertEquals(GrandChildState(), grandChild.state.value)
    }

    @Test
    fun `二级 child update 穿透两层回到 root`() = runTest {
        val parent = LocalStateHolder(ParentState())
        val child = parent.createChild(
            scope = backgroundScope,
            selector = { it.child },
            updater = { parent, child -> parent.copy(child = child) }
        )
        val grandChild = child.createChild(
            scope = backgroundScope,
            selector = { it.grandChild },
            updater = { child, grandChild -> child.copy(grandChild = grandChild) }
        )

        grandChild.update { it.copy(text = "hello") }
        advanceUntilIdle()

        assertEquals("hello", grandChild.state.value.text)
        assertEquals("hello", child.state.value.grandChild.text)
        assertEquals("hello", parent.state.value.child.grandChild.text)
    }

    @Test
    fun `二级 child 父层变化后自动派生`() = runTest {
        val parent = LocalStateHolder(ParentState())
        val child = parent.createChild(
            scope = backgroundScope,
            selector = { it.child },
            updater = { parent, child -> parent.copy(child = child) }
        )
        val grandChild = child.createChild(
            scope = backgroundScope,
            selector = { it.grandChild },
            updater = { child, grandChild -> child.copy(grandChild = grandChild) }
        )

        parent.update { it.copy(child = it.child.copy(grandChild = GrandChildState("from parent"))) }
        advanceUntilIdle()

        assertEquals("from parent", grandChild.state.value.text)
    }

    // ─── 防循环测试 ───

    @Test
    fun `相同值 update 不会触发重复发射`() = runTest {
        val parent = LocalStateHolder(ParentState())
        val child = parent.createChild(
            scope = backgroundScope,
            selector = { it.child },
            updater = { parent, child -> parent.copy(child = child) }
        )

        // 先收集一次初始值，确保 stateIn 已启动
        advanceUntilIdle()

        val emissions = mutableListOf<Int>()
        val job = launch {
            child.state.collect { emissions.add(it.value) }
        }

        // 等待初始值收集
        advanceUntilIdle()

        // 第一次 update: value = 1
        child.update { it.copy(value = 1) }
        advanceUntilIdle()

        // 相同值 update: value = 1，不应触发发射
        child.update { it.copy(value = 1) }
        advanceUntilIdle()

        // 不同值 update: value = 2
        child.update { it.copy(value = 2) }
        advanceUntilIdle()

        job.cancel()

        // 期望: [0, 1, 2] (初始值 + value=1 + value=2)
        assertEquals(listOf(0, 1, 2), emissions)
    }

    // ─── 独立 StateHolder child 测试 ───

    @Test
    fun `DelegateStateHolder 也能创建 child`() = runTest {
        val root = LocalStateHolder(ParentState())
        val delegate = DelegateStateHolder(
            state = root.state,
            onUpdate = { transform -> root.update(transform) }
        )
        val child = delegate.createChild(
            scope = backgroundScope,
            selector = { it.child },
            updater = { parent, child -> parent.copy(child = child) }
        )

        child.update { it.copy(value = 77) }
        advanceUntilIdle()

        assertEquals(77, root.state.value.child.value)
    }
}
