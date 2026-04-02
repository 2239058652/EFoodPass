# Spring AI Baby Tutorial

这是一套给当前 `EFoodPass` 项目准备的“宝宝级复刻教程”。

目标不是让你“看懂我做了什么”，而是让你能自己慢慢重做一遍，并且知道每一步为什么要这么做。

## 你现在要怎么用这套文档

不要跳着看。按顺序来。

1. [00-study-method.md](./00-study-method.md)
2. [01-minimum-openai-chat.md](./01-minimum-openai-chat.md)
3. [02-structured-output-and-scenes.md](./02-structured-output-and-scenes.md)
4. [03-tools-and-entity-query.md](./03-tools-and-entity-query.md)
5. [04-session-memory-and-stream.md](./04-session-memory-and-stream.md)
6. [05-rag-and-knowledge-index.md](./05-rag-and-knowledge-index.md)
7. [06-observability-fallback-and-tests.md](./06-observability-fallback-and-tests.md)
8. [07-rebuild-checklist.md](./07-rebuild-checklist.md)

## 这套教程覆盖什么

它覆盖了你现在项目里已经做出来的这条 Spring AI 主线：

- 模型接入
- Prompt 基础
- 结构化输出
- 场景路由
- Tool Calling
- 会话记忆
- Streaming
- RAG
- Knowledge Index
- Metrics
- Fallback
- Regression Tests

## 这套教程不做什么

它不追求“讲完所有 Spring AI 官方 API”。

它只做一件事：

把你当前项目里已经落地的 AI 模块，拆成一套你能自己复刻的学习路线。

## 建议学习节奏

- 每次只学一章
- 每章都亲手改代码
- 每章都做“自查清单”
- 不要一口气把整套文档读完再动手

## 你最终应该达到的状态

看完这套教程后，你应该能自己说清楚：

- 这个项目里的 AI 主链路从哪里开始
- 为什么有 `scene / answerType / nextAction / card`
- Tool Calling 和 RAG 在这里分别负责什么
- 会话系统为什么不能只存 prompt
- 测试在这里是怎么防回归的

下一章：[00-study-method.md](./00-study-method.md)
