# 05 RAG And Knowledge Index

上一章：[04-session-memory-and-stream.md](./04-session-memory-and-stream.md)  
下一章：[06-observability-fallback-and-tests.md](./06-observability-fallback-and-tests.md)

## 这一章的目标

理解两个问题：

1. 为什么系统类问题适合用 RAG
2. 为什么知识库不仅要“能查”，还要“能管理”

## 先看的文件

- `src/main/java/com/epass/food/modules/ai/service/SystemKnowledgeDocumentFactory.java`
- `src/main/java/com/epass/food/modules/ai/service/SystemKnowledgeRagConfiguration.java`
- `src/main/java/com/epass/food/modules/ai/service/SystemKnowledgeRagProperties.java`
- `src/main/java/com/epass/food/modules/ai/service/SystemKnowledgeIndexService.java`
- `src/main/java/com/epass/food/modules/ai/service/SystemAiSceneService.java`
- `src/main/java/com/epass/food/modules/ai/controller/AiKnowledgeController.java`

## 为什么 system 场景适合先上 RAG

因为这类问题通常是：

- 模块职责说明
- 权限控制方式
- auth / system / order / stock 模块之间关系

这类内容很像知识库，不像实时交易数据。

所以它比订单详情、菜品库存更适合优先做 RAG。

## 当前项目里的 RAG 结构

你现在走的是：

- 文档工厂
- `SimpleVectorStore`
- `QuestionAnswerAdvisor`
- scene service 注入 advisor
- 主编排器统一挂载 advisor

这就是最小可运行的 Spring AI RAG 链路。

## `SystemKnowledgeDocumentFactory` 在做什么

它负责把系统知识整理成 `Document`。

这里最值得你注意的不是文档正文，而是 metadata。

你现在文档 metadata 里有：

- `title`
- `moduleCode`
- `topic`
- `knowledgeBase`

这很关键，因为后面过滤检索靠的就是这些 metadata。

## 为什么要做 metadata filter

如果系统知识全放在一个库里，不做过滤，检索会越来越散。

你现在在 `SystemAiSceneService` 里已经按问题生成过滤表达式，比如：

- 登录相关 -> `auth`
- 权限相关 -> `system`
- 订单相关 -> `food/order`

这说明你已经不是“有 RAG 就行”，而是在开始做可控检索。

## 为什么要做 index service

这是另一个很重要的工程点。

很多 RAG demo 都只做：

- 启动时 add 一次文档

但你的项目已经进一步做了：

- status
- rebuild
- index metrics

这说明知识库已经被当成一个系统资源，而不是一段 demo 代码。

## 你现在的知识库管理接口

- `GET /ai/knowledge/system/status`
- `POST /ai/knowledge/system/rebuild`

这两个接口很值得保留，因为它们让你的 RAG 可运维。

## 你自己应该能复刻的最小版本

建议你自己做一个最小 system-rag 实验：

1. 手工构造 3 到 5 个 `Document`
2. 放进 `SimpleVectorStore`
3. 用 `QuestionAnswerAdvisor`
4. 问一个系统类问题
5. 再给文档加 metadata
6. 最后加一个 filter expression

## 本章自查

你必须能自己回答：

1. 为什么 system 场景比 order detail 更适合先做 RAG？
2. 文档 metadata 在你的项目里解决了什么问题？
3. 为什么知识库要有 `status / rebuild` 接口？

下一章：[06-observability-fallback-and-tests.md](./06-observability-fallback-and-tests.md)
