# 03 Tools And Entity Query

上一章：[02-structured-output-and-scenes.md](./02-structured-output-and-scenes.md)  
下一章：[04-session-memory-and-stream.md](./04-session-memory-and-stream.md)

## 这一章的目标

你要真正理解：

`什么时候应该直接把数据塞进 prompt，什么时候应该让模型调用工具。`

## 先看的文件

- `src/main/java/com/epass/food/modules/ai/service/OrderAiTools.java`
- `src/main/java/com/epass/food/modules/ai/service/ItemAiTools.java`
- `src/main/java/com/epass/food/modules/ai/service/OrderAiSceneService.java`
- `src/main/java/com/epass/food/modules/ai/service/ItemAiSceneService.java`
- `src/main/java/com/epass/food/modules/ai/service/OrderEntityReferenceResolver.java`
- `src/main/java/com/epass/food/modules/ai/service/ItemEntityReferenceResolver.java`
- `src/main/java/com/epass/food/modules/ai/dto/AiEntityReference.java`
- `src/main/java/com/epass/food/modules/ai/dto/AiQueryIntent.java`

## Tool Calling 解决什么问题

如果所有动态数据都由 Java 先查完再塞给模型，会有两个问题：

- 主编排器会越来越重
- 模型没有选择权，所有上下文都被硬塞进去

Tool Calling 的意思是：

`模型先看问题，再决定要不要调用某个后端工具。`

## 当前项目里用工具的场景

你现在主要在两个领域上用了工具：

- 订单
- 菜品

比如：

- `订单 1 是什么状态？`
- `订单 1 的金额是多少？`
- `菜品 3 的库存是多少？`

这些问题都很适合工具调用。

## 为什么还要做实体解析

工具调用不是魔法。

系统仍然要先有一层“问题理解”。

所以你现在还有：

- `OrderEntityReferenceResolver`
- `ItemEntityReferenceResolver`

它们先把一句自然语言解析成：

- 实体类型
- 实体 ID
- 查询意图

这一步很关键，因为后面的工具选择和 prompt 聚焦都依赖它。

## `AiQueryIntent` 的意义

它让“详情查询”继续细分。

比如订单场景里，不同问题虽然都查的是同一个订单，但意图不一样：

- 查状态
- 查金额
- 查菜品
- 查整体详情

你如果不做 intent，模型会很容易答得又散又长。

## Tool Calling 在你的项目里是怎么接进去的

看 `AiPromptPlan` 和 `AiChatServiceImpl`。

现在每个 scene handler 不只是返回 prompt，还可以返回：

- `tools`
- `toolContext`

主编排器再统一调用：

- `requestSpec.tools(...)`
- `requestSpec.toolContext(...)`

这说明 Tool Calling 已经不是 demo，而是进入了你的主编排器架构。

## `toolStatus` 为什么重要

很多人做到 Tool Calling 就停了：

- 工具返回数据
- 模型说一句话

但你现在多做了一层更重要的事情：

`把工具结果映射回结构化响应。`

也就是：

- `success`
- `not_found`
- `restricted`

再进一步影响：

- `answerType`
- `nextAction`
- `card`

这才是真正像业务系统。

## 你自己应该能复刻的最小版本

建议你自己做一个最小 order 工具实验：

1. 定义一个 `OrderAiTools`
2. 提供一个最简单的 `getOrderDetail(orderId)`
3. 在 scene service 里把这个工具挂进 `AiPromptPlan`
4. 在主编排器里让 `ChatClient` 用 `tools(...)`
5. 让工具失败时映射成 `not_found`

## 本章自查

你必须能自己回答：

1. Tool Calling 和“Java 先查完再塞 prompt”有什么区别？
2. 为什么工具结果还要映射成 `answerType`？
3. `AiEntityReference + AiQueryIntent` 分别解决什么问题？

下一章：[04-session-memory-and-stream.md](./04-session-memory-and-stream.md)
