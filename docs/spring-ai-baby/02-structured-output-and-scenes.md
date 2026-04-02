# 02 Structured Output And Scenes

上一章：[01-minimum-openai-chat.md](./01-minimum-openai-chat.md)  
下一章：[03-tools-and-entity-query.md](./03-tools-and-entity-query.md)

## 这一章的目标

你要理解两件非常重要的事：

1. 为什么 AI 返回不能只是一段纯文本
2. 为什么要做场景分类

## 先看的文件

- `src/main/java/com/epass/food/modules/ai/dto/AiChatResponse.java`
- `src/main/java/com/epass/food/modules/ai/dto/AiStructuredReply.java`
- `src/main/java/com/epass/food/modules/ai/dto/AiSceneType.java`
- `src/main/java/com/epass/food/modules/ai/service/AiSceneClassifier.java`
- `src/main/java/com/epass/food/modules/ai/service/AiSceneHandler.java`
- `src/main/java/com/epass/food/modules/ai/service/impl/AiChatServiceImpl.java`

## 为什么不能只返回一段文本

如果只返回文本，前端和后端都很难稳定消费。

比如：

- 这是订单问题还是系统问题？
- 当前回答是不是降级结果？
- 前端应该显示订单卡片还是系统模块卡片？
- 下一步建议去哪个页面？

所以你现在项目里，返回的不只是 `content`，还有：

- `scene`
- `answerType`
- `toolStatus`
- `nextAction`
- `card`
- `usage`
- `retrieval`

这就是“AI 响应结构化”。

## 当前项目里谁负责内容，谁负责控制

这是非常关键的思想。

### 模型主要负责

- `content`
- 一部分结构化内容体

### 后端主要负责

- `scene`
- `nextAction`
- `answerType`
- `card`
- `retrieval`
- `usage`

一句话：

`模型负责生成，系统负责控制。`

## 为什么要做场景分类

因为你的系统不是一个开放聊天机器人，而是一个业务系统。

业务系统里不同问题要走不同处理链。

你现在的场景有：

- `GENERAL`
- `ORDER`
- `ITEM`
- `STOCK`
- `SYSTEM`

这就是 `AiSceneType` 的作用。

## `AiSceneClassifier` 在做什么

它做的是：

`把用户问题粗分类到一个业务场景。`

先不要追求它完美。

它先做到够用、清晰、可扩展就可以。

## `AiSceneHandler` 在做什么

这个接口非常重要。

它代表：

`每个场景都有自己的处理器。`

所以你后面会看到：

- `OrderAiSceneService`
- `ItemAiSceneService`
- `StockAiSceneService`
- `SystemAiSceneService`
- `GeneralAiSceneService`

这比把所有逻辑堆在一个 `if else` 大类里强很多。

## `AiPromptPlan` 是什么

你可以把它理解成：

`一轮 AI 调用前的施工图。`

它告诉主编排器：

- 本轮 prompt 是什么
- 默认 answerType 是什么
- nextAction 是什么
- card 是什么
- tools 是什么
- advisors 是什么

这就是为什么 `AiChatServiceImpl` 没有继续膨胀成巨型类。

## 你现在自己应该能复刻的最小版本

试着自己做一个简化版：

1. 定义 `AiSceneType`
2. 写一个最简单的 `AiSceneClassifier`
3. 定义一个最简单的 `AiPromptPlan`
4. 写一个 `GeneralAiSceneService`
5. 在 `AiChatServiceImpl` 里按 scene 分发

只要你能手写出这个最小结构，就说明你真的吃下了这一章。

## 本章自查

你必须能自己回答：

1. 为什么 `scene` 不应该完全信任模型返回？
2. `AiSceneHandler` 比 `switch + if else` 好在哪里？
3. `AiPromptPlan` 解决的是什么问题？

下一章：[03-tools-and-entity-query.md](./03-tools-and-entity-query.md)
