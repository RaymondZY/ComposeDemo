package zhaoyun.example.composedemo.todo.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import android.widget.Toast
import zhaoyun.example.composedemo.scaffold.core.mvi.BaseEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import zhaoyun.example.composedemo.domain.model.TodoEvent
import zhaoyun.example.composedemo.domain.model.TodoItem
import zhaoyun.example.composedemo.domain.model.TodoState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator

/**
 * 完整的 Todo 屏幕 —— 自动创建 ViewModel、收集 State / Effect
 *
 * 该 Composable 属于 Presentation 层，负责：
 * 1. 登录状态检查与登录页面跳转（通过回调解耦）
 * 2. 平台副作用（Toast）处理
 * 3. 无状态的 [TodoListPage] 组合
 */
@Composable
fun TodoListScreen(
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TodoViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.onEvent(TodoEvent.CheckLogin)
    }

    LaunchedEffect(Unit) {
        viewModel.baseEffect.collect { effect ->
            when (effect) {
                is BaseEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    when (state.isLoggedIn) {
        null -> {
            // 登录状态检查中
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        false -> {
            // 未登录 —— 显示登录提示
            LoginPlaceholder(
                onLoginClick = onNavigateToLogin,
                modifier = modifier
            )
        }
        true -> {
            TodoListPage(
                state = state,
                onEvent = viewModel::onEvent,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun LoginPlaceholder(
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "请先登录",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "登录后即可使用 Todo List 功能",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onLoginClick,
                modifier = Modifier.testTag("login_navigate_button")
            ) {
                Text("前往登录")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoListPage(
    state: TodoState,
    onEvent: (TodoEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Todo List") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        floatingActionButton = {
            if (state.isInputValid) {
                FloatingActionButton(
                    onClick = { onEvent(TodoEvent.OnAddTodoClicked) },
                    modifier = Modifier.testTag("add_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "添加"
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // 输入区域
            OutlinedTextField(
                value = state.inputText,
                onValueChange = { onEvent(TodoEvent.OnInputTextChanged(it)) },
                label = { Text("输入待办事项") },
                modifier = Modifier.fillMaxWidth().testTag("todo_input"),
                singleLine = true,
                trailingIcon = {
                    if (state.inputText.isNotEmpty()) {
                        IconButton(onClick = { onEvent(TodoEvent.OnInputTextChanged("")) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "清空"
                            )
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 统计和清除按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val completedCount = state.todos.count { it.isCompleted }
                val totalCount = state.todos.size
                Text(
                    text = "共 $totalCount 项，已完成 $completedCount 项",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("stats_text")
                )
                if (completedCount > 0) {
                    TextButton(
                        onClick = { onEvent(TodoEvent.OnClearCompletedClicked) },
                        modifier = Modifier.testTag("clear_completed_button")
                    ) {
                        Text("清除已完成")
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Todo 列表
            if (state.todos.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无待办事项\n点击右下角 + 添加",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("empty_state_text")
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = state.todos,
                        key = { it.id }
                    ) { todo ->
                        TodoItemCard(
                            todo = todo,
                            onCheckedChange = { isChecked ->
                                onEvent(TodoEvent.OnTodoCheckedChanged(todo.id, isChecked))
                            },
                            onDelete = {
                                onEvent(TodoEvent.OnTodoDeleteClicked(todo.id))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TodoItemCard(
    todo: TodoItem,
    onCheckedChange: (Boolean) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = todo.isCompleted,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag("todo_checkbox_${todo.id}")
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = todo.title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            textDecoration = if (todo.isCompleted) {
                TextDecoration.LineThrough
            } else {
                TextDecoration.None
            },
            color = if (todo.isCompleted) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )

        IconButton(
            onClick = onDelete,
            modifier = Modifier.testTag("todo_delete_${todo.id}")
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "删除",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TodoListPagePreview() {
    MaterialTheme {
        TodoListPage(
            state = TodoState(
                todos = listOf(
                    TodoItem(id = 1, title = "学习 Compose", isCompleted = true),
                    TodoItem(id = 2, title = "完成 MVI 架构", isCompleted = false),
                    TodoItem(id = 3, title = "测试 Todo App", isCompleted = false)
                )
            ),
            onEvent = {}
        )
    }
}
