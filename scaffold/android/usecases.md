# scaffold:android 测试用例

## BaseViewModelTest（单元测试）

- ViewModel 的子 UseCase 共享同一个 registry
- sendEvent 在 viewModelScope 上处理事件
- ViewModel 可以用共享的 StateHolder 构造，更新状态会同步到共享 holder

## ScreenViewModelTest（Instrumented）

- `screenViewModel()` 的默认参数能正确传递给 ViewModel
- 自定义 parameters 能正确接收 payload 和 marker

## ServiceRegistryComposeTest（Instrumented）

- `ServiceRegistryProvider` 能把 registry 暴露给 Composition
- `RegisterService` Composable 在组合时注册服务、在移除时自动注销
