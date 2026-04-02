# 02 再看总编排器 AiChatServiceImpl

上一章：[01-先看请求入口与响应结构.md](./01-先看请求入口与响应结构.md)  
下一章：[03-Scene-Handler-和-PromptPlan-到底是什么.md](./03-Scene-Handler-和-PromptPlan-到底是什么.md)

## 本章目标

这一章只做一件事：

把 `AiChatServiceImpl` 拆开看。

你不用一口气全记住，但你必须知道：

`这个类到底在总编排什么。`

## 先看的文件

- `src/main/java/com/epass/food/modules/ai/service/impl/AiChatServiceImpl.java`
- `src/main/java/com/epass/food/modules/ai/service/AiChatService.java`

---

## 一、这个类为什么叫“总编排器”

你要先明白一件事：

`AiChatServiceImpl` 不是干具体业务细节的。

它不应该亲自决定：

- 订单 prompt 长什么样
- 菜品详情工具怎么写
- system RAG 怎么构造文档

这些都应该交给别的类。

它主要负责把几段能力串起来。

## 二、先看这个类的字段

### 1. `ChatClient chatClient`

作用：

`真正调用模型。`

### 2. `AiSceneClassifier aiSceneClassifier`

作用：

`先把用户问题分到大场景。`

### 3. `AiConversationMemoryService conversationMemoryService`

作用：

`处理 session 和记忆。`

比如：

- 保证 sessionId 存在
- 取 prompt context
- 写回本轮对话

### 4. `Map<AiSceneType, AiSceneHandler> sceneHandlerMap`

作用：

`根据 scene 找到对应场景处理器。`

这就是为什么主编排器不用写一大堆 `if else`。

### 5. `AiStructuredOutputAdvisor structuredOutputAdvisor`

作用：

`统一补充结构化输出规则。`

### 6. `AiConversationMemoryAdvisor conversationMemoryAdvisor`

作用：

`把会话记忆注入到 prompt。`

### 7. `AiMetricsService aiMetricsService`

作用：

`统一记录 chat / stream / fallback / rag 指标。`

---

## 三、先看 `chat(...)`

这个方法是同步聊天主入口。

你可以把它拆成 6 步。

### 第 1 步：准备计时和 session

```java
long startedAt = System.currentTimeMillis();
String resolvedSessionId = conversationMemoryService.ensureSessionId(sessionId);
long userCreatedAt = System.currentTimeMillis();
```

#### `startedAt`

后面给 metrics 算耗时用。

#### `resolvedSessionId`

保证这一轮一定有 sessionId。

#### `userCreatedAt`

记录这一轮用户消息时间。

后面会话记忆落库要用。

### 第 2 步：准备运行时上下文

```java
SceneRuntime runtime = prepareRuntime(...);
```

这个 `runtime` 很重要。

它把几样东西打包好了：

- 当前 message
- 当前 scene 解析结果
- 当前 prompt context
- 当前 prompt plan

你可以把它理解成：

`这一轮 AI 调用的现场施工包。`

### 第 3 步：构建请求

```java
var requestSpec = buildStructuredRequest(runtime);
```

它会做的事包括：

- 放 system prompt
- 放 user prompt
- 挂 advisors
- 挂 tools
- 挂 toolContext

### 第 4 步：真正调用模型

```java
ChatClientResponse chatClientResponse = requestSpec.call().chatClientResponse();
```

这里不是只拿一段纯文本，而是拿 `ChatClientResponse`。

因为后面还要从里面提：

- 模型回答正文
- usage metadata
- advisor context

### 第 5 步：把模型结果转成结构化对象

```java
AiStructuredReply reply = new BeanOutputConverter<>(AiStructuredReply.class)
        .convert(extractAssistantText(chatClientResponse.chatResponse()));
```

这一步很关键。

不是手工 `ObjectMapper.readValue(...)`，而是用了 Spring AI 的结构化输出转换器。

这说明你现在已经不是“自己土法 parse JSON”，而是在用 Spring AI 官方能力。

### 第 6 步：收敛最终响应

模型返回后，还要经过一层系统收敛：

- `resolveAnswerType(...)`
- `normalizeToolStatus(...)`
- `resolveNextAction(...)`
- `resolveCard(...)`
- `extractUsage(...)`
- `extractRetrievalMeta(...)`

最后再组装成 `AiChatResponse`。

这一步最重要的意义是：

`模型结果不会直接裸返回给前端。`

---

## 四、为什么还要做 fallback

你会看到一个 `catch (RuntimeException e)`。

它不是偷懒。

它是在给 AI 系统加安全带。

如果模型调用失败，系统不会直接炸掉，而是走：

- `buildFallbackResponse(...)`

这个 fallback 响应会统一变成：

- `answerType = degraded`
- `toolStatus = degraded`
- `nextAction = ask_more_details`
- `grounded = false`

这是很像真实线上系统的设计。

---

## 五、再看 `streamChat(...)`

这个方法和 `chat(...)` 的区别在于：

它不是一次拿完整回答，而是持续吐 chunk。

### 这里最关键的 3 个对象

#### 1. `prefix`

先发一个空内容 chunk，告诉前端：

`流开始了`

#### 2. `contentFlux`

真正不断往前端吐内容的部分。

#### 3. `suffix`

最后发一个 `done=true` 的 chunk。

表示：

`流结束了`

### 为什么 `doOnComplete(...)` 里还要写会话记忆

因为流式输出时，内容是一点点来的。

但 session 里最好存完整一轮答案。

所以要等流结束，把完整内容拼好后再落库。

---

## 六、看几个关键私有方法

### `prepareRuntime(...)`

作用：

把这一轮真正需要的运行时上下文准备好。

里面会做：

- scene 解析
- prompt context 获取
- scene handler 生成 prompt plan

### `buildStructuredRequest(...)`

作用：

把：

- prompt
- advisors
- memory params

拼成真正的 `ChatClientRequestSpec`。

### `resolveSceneType(...)`

作用：

如果当前消息太短、像追问，就尝试复用上一轮 scene。

这是你多轮对话体验成立的关键之一。

### `resolveAnswerType(...)`

作用：

根据工具返回的 `toolStatus`，进一步决定最终回答类型。

比如：

- `not_found -> NOT_FOUND`
- `restricted -> RESTRICTED`

### `extractUsage(...)`

作用：

把 Spring AI 返回里的模型元数据提取出来。

### `extractRetrievalMeta(...)`

作用：

把 RAG 命中的文档和检索策略提取出来。

---

## 七、这一章最重要的理解

你现在要真正记住：

`AiChatServiceImpl 不负责知道所有细节，它负责把多个能力串起来。`

也就是：

- scene
- memory
- advisors
- tools
- structured output
- usage
- retrieval
- fallback
- metrics

它把这些串起来，形成一轮完整的 AI 调用。

---

## 本章小练习

你自己现在做这件事：

在纸上或者笔记里，把 `chat(...)` 画成 6 步流程。

不要照抄文字，自己写。

至少要写出：

1. 先准备什么
2. 再调用谁
3. 模型返回后怎么收敛
4. 失败时怎么降级

如果你能自己画出来，这一章就算过关。

下一章：[03-Scene-Handler-和-PromptPlan-到底是什么.md](./03-Scene-Handler-和-PromptPlan-到底是什么.md)
