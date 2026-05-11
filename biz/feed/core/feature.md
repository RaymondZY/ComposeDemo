# Feature — Feed Core（信息流业务）

> 参考：信息流卡片列表的 Paging 数据源和失败提示。

---

## 全局功能约束

- **Paging 适配**：业务层将 `FeedRepository.fetchFeed(page, pageSize)` 适配为 AndroidX Paging 可消费的数据源。
- **固定分页参数**：每页加载 10 条，距离末尾 3 条时由 Paging 触发预加载。
- **分页边界**：当服务返回空列表时，当前页为分页结束，后续不再提供 `nextKey`。
- **失败提示**：刷新或加载更多失败时，业务层产生对应 snackbar 提示。

---

## UC-01 初始状态无错误

**前置条件**：信息流业务尚未接收任何用户或系统事件  
**步骤**：

1. 系统读取信息流初始状态

**预期结果**：错误信息为空

---

## UC-02 加载首页成功

**前置条件**：Paging 发起 refresh 请求且未提供 key  
**步骤**：

1. PagingSource 请求第一页卡片数据
2. 服务返回非空卡片列表

**预期结果**：返回第一页数据，`prevKey` 为空，`nextKey` 指向下一页

---

## UC-03 加载追加页成功

**前置条件**：Paging 发起 append 请求并提供页码 key  
**步骤**：

1. PagingSource 请求对应页卡片数据
2. 服务返回非空卡片列表

**预期结果**：返回该页数据，`prevKey` 指向上一页，`nextKey` 指向下一页

---

## UC-04 加载空页结束分页

**前置条件**：Paging 请求某一页且服务返回空列表  
**步骤**：

1. PagingSource 完成该页加载

**预期结果**：返回空数据，`nextKey` 为空

---

## UC-05 数据请求失败

**前置条件**：Repository 返回失败结果  
**步骤**：

1. PagingSource 请求数据

**预期结果**：返回 Paging error，由 platform 的 load state 处理重试和提示

---

## UC-06 计算刷新 key

**前置条件**：Paging 当前存在 anchor position 和相邻页 key  
**步骤**：

1. Paging 请求刷新 key

**预期结果**：返回 anchor 附近的页码，刷新后尽量保持浏览位置

---

## UC-07 刷新失败提示

**前置条件**：platform 观测到 refresh load state 为失败  
**步骤**：

1. platform 发送刷新失败事件

**预期结果**：业务层产生“刷新失败，请重试” snackbar

---

## UC-08 加载更多失败提示

**前置条件**：platform 观测到 append load state 为失败  
**步骤**：

1. platform 发送加载更多失败事件

**预期结果**：业务层产生“加载失败，请重试” snackbar
