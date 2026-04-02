# 06 RAG 知识库与索引管理怎么工作

上一章：[05-会话记忆与流式输出怎么工作.md](./05-会话记忆与流式输出怎么工作.md)  
下一章：[07-指标-降级-测试为什么最后才出现.md](./07-指标-降级-测试为什么最后才出现.md)

## 本章目标

这一章讲细：

- RAG 在这个项目里具体接在哪里
- 文档 metadata 为什么重要
- 知识库为什么还要有索引管理

## 先看的文件

- `src/main/java/com/epass/food/modules/ai/service/SystemKnowledgeDocumentFactory.java`
- `src/main/java/com/epass/food/modules/ai/service/SystemKnowledgeRagProperties.java`
- `src/main/java/com/epass/food/modules/ai/service/SystemKnowledgeRagConfiguration.java`
- `src/main/java/com/epass/food/modules/ai/service/SystemAiSceneService.java`
- `src/main/java/com/epass/food/modules/ai/service/SystemKnowledgeIndexService.java`
- `src/main/java/com/epass/food/modules/ai/controller/AiKnowledgeController.java`

## 一、先看 `SystemKnowledgeDocumentFactory`

这个类不是在“调模型”。

它在做的事是：

`把系统知识整理成可检索文档。`

### 这里最关键的不是 text，而是 metadata

为什么？

因为 text 只解决“能被向量化”。

metadata 才解决：

- 能不能过滤
- 能不能分类
- 命中文档后前端怎么显示标题

你现在会看到文档上有这类 metadata：

- `title`
- `moduleCode`
- `topic`
- `knowledgeBase`

## 二、`SystemKnowledgeRagProperties` 是什么

它是 RAG 检索策略的配置承载类。

这里最重要的字段一般有：

- `topK`
- `similarityThreshold`

### `topK`

最多取几条相似文档。

### `similarityThreshold`

相似度低于这个阈值的，不要。

这说明你已经不是“随便搜一下”，而是在调检索策略。

## 三、`SystemKnowledgeRagConfiguration` 在做什么

这个类一般负责：

- 创建 `SimpleVectorStore`
- 创建 `QuestionAnswerAdvisor`

你要把这两个对象分清楚。

### `SimpleVectorStore`

是向量存储本身。

### `QuestionAnswerAdvisor`

是把“检索结果注入 prompt”这件事接到 Spring AI 请求链上的 advisor。

## 四、为什么 system scene 要单独接 RAG

因为 system 问题更像：

- 知识问答
- 模块说明
- 规则说明

而不是订单实时数据。

所以最适合先接到 RAG。

## 五、看 `SystemAiSceneService`

它最关键的不是“写了 system prompt”。

而是：

### 1. 它会决定是否挂 `QuestionAnswerAdvisor`

这说明不是所有场景都走 RAG。

### 2. 它会决定 advisor params

比如：

- `knowledgeBase`
- `filterExpression`
- `topK`
- `similarityThreshold`

### 3. 它会把这些策略同时暴露到 card / retrieval meta

这说明：

`你的 RAG 不是黑盒。`

## 六、为什么要做 `SystemKnowledgeIndexService`

很多人做 RAG 到这里就停了：

- 启动时塞一次文档

但你这里多做了一层很重要的工程化动作：

- status
- rebuild
- 文档数统计
- 重建时间

也就是：

`把知识库索引当成一个系统资源来管理。`

### `rebuildIndex()` 在做什么

一般会做：

1. 删除旧索引
2. 重新生成文档
3. 重新写入 vector store
4. 更新状态和指标

## 七、`AiKnowledgeController` 为什么值得存在

它不是“可有可无的管理接口”。

它让你可以：

- 看当前 system knowledge index 状态
- 手动重建索引

这对调试和运维都很有用。

## 本章小练习

你现在自己做一个极简 RAG 练习：

1. 准备 3 条系统知识
2. 放进 `SimpleVectorStore`
3. 配一个 `QuestionAnswerAdvisor`
4. 问一句：
   - `系统怎么做权限控制？`

哪怕不做完整 metadata，也先感受一次最小 RAG 链路。

## 本章自查

1. `Document.text` 和 `Document.metadata` 分别解决什么问题？
2. 为什么 `SystemKnowledgeIndexService` 不是多余的？
3. `QuestionAnswerAdvisor` 和 `SimpleVectorStore` 的区别是什么？

下一章：[07-指标-降级-测试为什么最后才出现.md](./07-指标-降级-测试为什么最后才出现.md)
