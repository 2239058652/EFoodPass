# 06 Observability Fallback And Tests

上一章：[05-rag-and-knowledge-index.md](./05-rag-and-knowledge-index.md)  
下一章：[07-rebuild-checklist.md](./07-rebuild-checklist.md)

## 这一章的目标

理解最后这三件工程收口动作：

- metrics
- fallback
- regression tests

这三件事决定了你的 AI 模块是不是一个“能上线的系统”，而不是一个“能跑的 demo”。

## 先看的文件

- `src/main/java/com/epass/food/modules/ai/service/AiMetricsService.java`
- `src/main/java/com/epass/food/modules/ai/service/impl/AiChatServiceImpl.java`
- `src/test/java/com/epass/food/modules/ai/service/impl/AiChatServiceImplTest.java`
- `src/test/java/com/epass/food/modules/ai/service/SystemAiSceneServiceTest.java`
- `src/test/java/com/epass/food/modules/ai/service/SystemKnowledgeIndexServiceTest.java`
- `src/test/java/com/epass/food/modules/ai/service/AiConversationMemoryServiceTest.java`
- `src/test/java/com/epass/food/modules/ai/controller/AiChatControllerTest.java`
- `src/test/java/com/epass/food/modules/ai/controller/AiKnowledgeControllerTest.java`

## 为什么要做 metrics

没有指标，你只能凭感觉调 AI。

你现在已经有的指标大概覆盖：

- chat 请求数
- chat 延迟
- stream 请求数
- stream 延迟
- fallback 次数
- rag 命中文档数
- knowledge rebuild 次数

这说明你已经开始用“服务”视角看 AI，而不是只用“功能”视角。

## 为什么要做 fallback

模型失败不是小概率事件。

会出问题的地方很多：

- 模型超时
- 结构化解析失败
- 配置有误
- 上游服务异常

所以你项目里做了 `DEGRADED`。

这意味着：

- 不直接炸 500
- 前端拿到的结构还是稳定的
- 会话也不会断
- metrics 里还能看见降级次数

一句话：

`fallback 是 AI 系统的安全带。`

## 为什么这些测试不是“仅仅测试”

因为现在你的 AI 模块已经有很多层：

- scene
- tool
- rag
- session
- stream
- metrics
- fallback

没有测试的话，每继续改一步，都可能把前面的东西悄悄改坏。

所以这些测试的作用有两个：

1. 查缺补漏
2. 防回归

## 你现在已经补到哪些测试

### 编排层

- degraded fallback
- tool status 映射
- scene reuse
- usage / retrieval 富响应
- stream chunk 顺序、落库、metrics

### 会话层

- session detail 分页
- rename
- clear
- appendTurn 更新 summary 和索引
- listSessions 顺序
- 异常边界

### controller 层

- chat
- stream
- sessions
- session detail
- rename
- clear
- knowledge status
- knowledge rebuild

## 你现在自己应该做的事情

不要只是“知道有测试”。

你要自己挑 1 条测试，试着从头写一遍。

推荐最先自己写的是：

- `AiChatServiceImplTest` 里的 degraded fallback

因为它最能帮你理解：

- 主编排器怎么工作
- 为什么需要 fallback
- 为什么 metrics 和 memory 也要一起验证

## 本章自查

你必须能自己回答：

1. 为什么 AI 模块必须要有 fallback？
2. 为什么这里的测试不是可有可无？
3. metrics 在 AI 项目里解决的是什么问题？

下一章：[07-rebuild-checklist.md](./07-rebuild-checklist.md)
