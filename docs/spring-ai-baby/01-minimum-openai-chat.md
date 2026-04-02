# 01 Minimum OpenAI Chat

上一章：[00-study-method.md](./00-study-method.md)  
下一章：[02-structured-output-and-scenes.md](./02-structured-output-and-scenes.md)

## 这一章的目标

先弄懂最小 AI 调用链。

不是先看复杂功能，而是先回答：

`Spring AI 在这个项目里，最基本是怎么调用模型的？`

## 你要先看的文件

- `pom.xml`
- `src/main/resources/application-dev.yml`
- `src/main/java/com/epass/food/modules/ai/service/impl/AiChatServiceImpl.java`

## 第一步：依赖层在做什么

你要在 `pom.xml` 里看到这些东西：

- `spring-ai-starter-model-openai`

这件事的意思不是“只能接 OpenAI”。

它的真实意思是：

`Spring AI 提供了 OpenAI 风格的模型接入层，你可以接 OpenAI，也可以接兼容 OpenAI 协议的服务。`

你这里实际接的是 DashScope/Qwen 这一类兼容 OpenAI 接口的模型。

## 第二步：配置层在做什么

在 `application-dev.yml` 里，你要找到模型配置。

你现在要理解的不是每个配置项，而是这件事：

`模型地址、密钥、模型名，都应该在配置里，而不是写死在 Java 代码里。`

这是后面能切模型、切环境的前提。

## 第三步：最小调用链长什么样

看 `AiChatServiceImpl` 里的 `chat(...)`。

最小骨架其实就是：

1. 准备 prompt
2. `chatClient.prompt()`
3. `.system(...)`
4. `.user(...)`
5. `.call()`
6. 取结果

你先不要被现在的复杂版本吓到。

最小版本脑子里应该只有这一句：

`ChatClient 就是这条 AI 调用链的起点。`

## 你现在应该理解的 3 个对象

### 1. `ChatClient.Builder`

它是 Spring 注入进来的构建器。

意思是：

`模型客户端由 Spring 管，业务层拿来用。`

### 2. `ChatClient`

它是你真正发起 prompt 调用的对象。

### 3. `AiChatServiceImpl`

它不是模型本身。

它是你这个项目里的 AI 总编排器。

## 你现在不要急着关心的内容

先不要一上来就纠结：

- advisor
- tool calling
- rag
- retrieval
- metrics

本章只要抓一件事：

`模型调用一定是从 ChatClient 开始。`

## 你自己现在应该做的最小复刻

建议你单独写一个最小 demo service，哪怕不提交。

目标只要做到：

- 注入 `ChatClient.Builder`
- `build()` 出 `ChatClient`
- 给一段 system prompt
- 给一段 user prompt
- 返回模型文本

如果你能自己写出来，就说明你真的过了这一章。

## 本章自查

你现在必须能自己回答：

1. 为什么 `AiChatServiceImpl` 里不直接 new 一个 HTTP 客户端？
2. `ChatClient` 和 `ChatClient.Builder` 的区别是什么？
3. 最小 AI 调用链的 6 步是什么？

如果这 3 个问题回答不出来，不要往下一章走。

下一章：[02-structured-output-and-scenes.md](./02-structured-output-and-scenes.md)
