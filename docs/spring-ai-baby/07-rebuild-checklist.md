# 07 Rebuild Checklist

上一章：[06-observability-fallback-and-tests.md](./06-observability-fallback-and-tests.md)

## 这不是新知识章

这是最后的复刻清单。

你的目标不是继续看，而是用这张清单逼自己真正重做。

## 第一阶段：最小链路

你必须能自己重做：

- 配置 Spring AI 模型
- 注入 `ChatClient.Builder`
- 写一个最小 `chat(...)`
- 返回普通文本

如果这一步不会，后面都不要做。

## 第二阶段：结构化 AI

你必须能自己重做：

- `AiChatResponse`
- `AiSceneType`
- `AiSceneClassifier`
- `AiSceneHandler`
- `AiPromptPlan`
- `AiChatServiceImpl` 基础分发

## 第三阶段：工具与实体查询

你必须能自己重做：

- `AiEntityReference`
- `AiQueryIntent`
- 一个最小 `OrderEntityReferenceResolver`
- 一个最小 `OrderAiTools`
- 一个能挂 tools 的 scene service

## 第四阶段：会话系统

你必须能自己重做：

- `sessionId`
- `AiConversationMemoryService`
- session 列表
- session 详情
- rename
- clear

## 第五阶段：stream

你必须能自己重做：

- `AiChatStreamChunk`
- `streamChat(...)`
- 流结束后写回会话

## 第六阶段：RAG

你必须能自己重做：

- `Document` 构建
- `SimpleVectorStore`
- `QuestionAnswerAdvisor`
- system scene 接入 RAG
- knowledge status / rebuild

## 第七阶段：工程收口

你必须能自己重做：

- metrics
- fallback
- 核心回归测试

## 复刻时的顺序建议

严格按这个顺序：

1. 最小聊天
2. 结构化输出
3. scene
4. tools
5. memory
6. stream
7. rag
8. metrics
9. fallback
10. tests

不要颠倒。

## 你什么时候算“真的学会了”

不是现在代码在仓库里就算学会。

而是当你能做到下面 3 件事：

1. 你能自己从空白分支搭出最小版本
2. 你能解释每一层职责
3. 你能自己修一条测试失败

## 你接下来有 3 条路

### 路线 A：自己复刻当前模块

这是最推荐的。

### 路线 B：进入下一阶段

下一阶段可以是：

- AI 评估
- 深化 RAG
- Agent / 多步工具调用
- 前端 AI 页面集成

### 路线 C：写总结

用你自己的话写一份：

- 当前 AI 模块架构
- 关键类职责
- 你最不懂的 3 个点

这是非常有效的复习方式。

## 最后的建议

不要追求“我已经做出了一个很厉害的 AI 模块”。

你现在真正该追求的是：

`我能不能自己再做一遍，并且知道为什么这么做。`

如果你能做到这一点，这一轮学习就真的完成了。
