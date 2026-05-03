# biz:story:infobar:domain 测试用例

## InfoBarUseCaseTest

- 初始状态同步默认值
- 点赞按钮
    - 即时反馈
        - 点击点赞切换 isLiked 并增加 likes（乐观更新）
        - 再次点击点赞恢复原始状态（乐观更新）
    - 异步请求
        - 点赞后异步请求成功，以接口结果回写状态
        - 取消点赞后异步请求成功，以接口结果回写状态
    - 乐观更新与接口结果冲突时，以接口结果为准
    - 快速点击
        - UI上立即切换状态，需要cancel之前的请求，只保留最新的请求
- 点击作者区域发送 NavigateToCreatorProfile Effect

