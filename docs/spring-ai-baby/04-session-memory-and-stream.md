# 04 Session Memory And Stream

上一章：[03-tools-and-entity-query.md](./03-tools-and-entity-query.md)  
下一章：[05-rag-and-knowledge-index.md](./05-rag-and-knowledge-index.md)

## 这一章的目标

你要把“单轮 AI”升级成“对话系统”。

这章最核心的两个主题是：

- 会话记忆
- 流式输出

## 先看的文件

- `src/main/java/com/epass/food/modules/ai/service/AiConversationMemoryService.java`
- `src/main/java/com/epass/food/modules/ai/service/AiConversationMemoryAdvisor.java`
- `src/main/java/com/epass/food/modules/ai/dto/AiConversationSessionSummary.java`
- `src/main/java/com/epass/food/modules/ai/dto/AiConversationSessionDetail.java`
- `src/main/java/com/epass/food/modules/ai/dto/AiConversationMessage.java`
- `src/main/java/com/epass/food/modules/ai/dto/AiChatStreamChunk.java`
- `src/main/java/com/epass/food/modules/ai/controller/AiChatController.java`
- `src/main/java/com/epass/food/modules/ai/service/impl/AiChatServiceImpl.java`

## 为什么要做 session

如果没有 session，多轮对话就是假的。

比如上一轮问：

`订单 1 是什么状态？`

下一轮问：

`那金额呢？`

没有 session 和记忆，你根本不知道“那”指什么。

## 当前项目里的会话设计

你现在的会话系统已经不只是一个 `sessionId`。

它分成了几层：

- 当前会话 ID
- prompt memory
- archive history
- session meta
- session index
- summary

这很重要。

## 为什么要区分 prompt memory 和 archive history

这是你项目里一个非常值得学习的点。

### prompt memory

服务模型。

要求：

- 短
- 可裁剪
- 节省 token

### archive history

服务前端和用户。

要求：

- 可恢复
- 可分页
- 适合展示

一句话：

`给模型看的历史，和给用户看的历史，不应该共用同一套语义。`

## `AiConversationMemoryAdvisor` 在做什么

它把这部分事情收进了 Spring AI advisor：

- 摘要注入
- 最近几轮注入

这样主编排器就不用手工拼记忆字符串了。

## 你现在有哪些 session 接口

看 `AiChatController`。

你现在已经有：

- `POST /ai/chat`
- `POST /ai/chat/stream`
- `GET /ai/chat/sessions`
- `GET /ai/chat/session/{sessionId}`
- `PUT /ai/chat/session/{sessionId}/title`
- `DELETE /ai/chat/session/{sessionId}`

这已经是一套完整的会话资源系统了。

## 流式输出在这里解决什么问题

流式输出不是“更高级”，而是更接近聊天体验。

当前项目里，`streamChat(...)` 的关键承诺是：

- 先发一个前缀 chunk
- 再持续发内容 chunk
- 最后发一个 done chunk
- 流结束后写入会话记忆

也就是说，流式不只是“出字”，它和 session 系统是连着的。

## 你自己应该能复刻的最小版本

建议你自己做一个最小 session + stream 练习：

1. 会话里至少保存一轮 `user/assistant`
2. 再做一个简单的 `sessionId` 生成
3. 写一个 `GET /sessions`
4. 再写一个最小 `streamChat()`
5. 流结束后把完整答案写回会话

## 本章自查

你必须能自己回答：

1. 为什么 prompt memory 和 archive history 要分开？
2. 为什么 stream 结束后还要写回会话记忆？
3. `AiConversationMemoryAdvisor` 和 `AiConversationMemoryService` 的职责差别是什么？

下一章：[05-rag-and-knowledge-index.md](./05-rag-and-knowledge-index.md)
