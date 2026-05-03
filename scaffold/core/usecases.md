# scaffold:core 测试用例

## StateHolderTest

- StateHolder 的初始状态/当前状态暴露
- 派生 StateHolder（derive）能正确切片子状态
- 更新派生 holder 会写回父状态
- 父状态更新会传播到派生 holder
- 派生 state flow 只在子切片变化时才发射

## EventReceiverImplTest

- EventReceiver 能把收到的事件正确分发给注册的 handler

## EffectDispatcherImplTest

- EffectDispatcher 能把 UI Effect 和 Base Effect 分发到各自的 Flow
- 缓冲通道能在收集前先暂存分发的值

## MviFacadeDelegationTest

- MviFacade 的默认实现（stateHolder/eventReceiver/effectDispatcher 组合）能正确委托：
    - initialState/currentState 读取
    - updateState 更新
    - receiveEvent 接收
    - dispatchEffect/dispatchBaseEffect 分发

## CombineUseCaseStateBindingTest

- UseCase 可以用共享的 StateHolder 构造
- CombineUseCase 能用同一个共享 StateHolder 构建子 UseCase，事件处理后所有子 UseCase 和父 holder 状态同步

## CombineUseCaseBehaviorTest

- CombineUseCase 能把单个事件扇出（fan-out）给所有子 UseCase
- 能合并子 UseCase 的 effect 和自身的 effect
- 能合并子 UseCase 的 base effect 和自身的 base effect

## CombineUseCaseRegistryTest

- 子 UseCase 在共享的 Screen registry 中自动注册（autoRegister）
- 创建后可通过 registry.find 查找到

## BaseUseCaseDynamicRegistrationTest

- UseCase 能在运行时动态注册/注销服务
- findServiceOrNull 在注册前返回 null
- 重复注册会快速失败（fail fast）

## UseCaseServiceAutoRegistrationTest

- CombineUseCase 自动注册子 UseCase 的 MviService 接口供兄弟 UseCase 查找
- 支持 TaggedMviService 按 tag 注册
- 支持层级接口（父接口也注册）
- 同一 scope 内重复自动注册会 fail fast

## MutableServiceRegistryImplTest

- 同一 scope 内重复注册（无 tag/有 tag）会抛异常
- 不同 tag 的同一类型可共存
- 子 scope 回退到父 scope 查找
- 子 scope 本地注册优先于父 scope
- 按类型注销只移除本地注册，露出父 scope 回退
- 按实例注销移除该对象的所有别名
- clear 只清空当前 scope，保留父 scope 回退

## ServiceRegistryExtTest

- autoRegister 只注册 MviService 接口（不注册普通接口）
- 支持层级接口注册
- 支持 TaggedMviService 带 tag 注册
- autoUnregister 能移除实例及其所有别名
