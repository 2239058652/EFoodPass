# 00 Study Method

上一章：[README.md](./README.md)  
下一章：[01-minimum-openai-chat.md](./01-minimum-openai-chat.md)

## 先把心态摆正

你前面是在我的协助下把模块做出来的，这没问题。

现在真正重要的不是“我已经做出来了”，而是：

`我能不能自己再做一遍。`

这套教程就是为这个目的准备的。

## 正确的学习方式

不要这样学：

- 只看效果
- 只看我解释
- 觉得“懂了”
- 不自己敲

要这样学：

1. 先看本章目标
2. 打开对应文件
3. 用自己的话复述
4. 自己删掉一小部分再重写
5. 跑一下
6. 做本章自查

## 推荐复刻策略

最稳的方式不是重新开新项目，而是在当前仓库里“影子复刻”。

你可以这样做：

- 新建一个自己的分支
- 按本教程顺序回看
- 每章选 1 到 2 个核心文件，尝试自己重写
- 不会时再回来对照现有实现

## 每章固定回答 4 个问题

学完每章后，自己必须能回答：

1. 这一章解决了什么问题？
2. 如果不做这一步，会出什么问题？
3. 当前代码里哪几个文件是核心？
4. 我自己能不能重新写出最小版本？

## 你现在项目里 AI 模块的总入口

最重要的入口文件是：

- `src/main/java/com/epass/food/modules/ai/controller/AiChatController.java`
- `src/main/java/com/epass/food/modules/ai/service/impl/AiChatServiceImpl.java`

后面所有章节，都会不断回到这两处。

## 学习原则

### 原则 1：先抓主链路

不要一开始就被所有 DTO 和工具类吓到。

先看：

- Controller 怎么进
- ServiceImpl 怎么编排
- SceneHandler 怎么分发

### 原则 2：先懂职责，再懂细节

比如你先知道：

- `AiChatController` 负责 HTTP
- `AiChatServiceImpl` 负责总编排
- `AiSceneHandler` 负责按场景出 plan

然后再去看：

- card 怎么拼
- retrieval 怎么映射
- metrics 怎么打点

### 原则 3：每次只追一条链

比如你今天只追：

`/ai/chat -> AiChatServiceImpl -> OrderAiSceneService`

不要同一天把 tool、rag、memory、metrics 全追完。

## 这一套文档的章节结构

### 第 1 段

先学最小可用：

- 模型接入
- 最小聊天
- 结构化输出
- 场景分类

### 第 2 段

再学项目化能力：

- Tool Calling
- Entity Query
- Scene Handler

### 第 3 段

再学系统能力：

- Session Memory
- Streaming
- RAG
- Knowledge Index

### 第 4 段

最后学工程收口：

- Metrics
- Fallback
- Tests

## 本章动手任务

现在只做一件事：

打开下面 3 个文件，别改，先读：

- `src/main/java/com/epass/food/modules/ai/controller/AiChatController.java`
- `src/main/java/com/epass/food/modules/ai/service/impl/AiChatServiceImpl.java`
- `src/main/java/com/epass/food/modules/ai/service/AiSceneHandler.java`

然后用你自己的话写下 3 句话：

- 请求从哪里进
- 谁在总编排
- 谁在按场景处理

如果这 3 句话说不清，先不要进入下一章。

下一章：[01-minimum-openai-chat.md](./01-minimum-openai-chat.md)
