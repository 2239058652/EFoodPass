# 04 Tool Calling 在这个项目里怎么落地

上一章：[03-Scene-Handler-和-PromptPlan-到底是什么.md](./03-Scene-Handler-和-PromptPlan-到底是什么.md)  
下一章：[05-会话记忆与流式输出怎么工作.md](./05-会话记忆与流式输出怎么工作.md)

## 本章目标

真正看懂：

- 工具是怎么暴露给模型的
- 实体 ID 是怎么被识别出来的
- 为什么工具结果还要影响 `answerType`

## 先看的文件

- `src/main/java/com/epass/food/modules/ai/service/OrderAiTools.java`
- `src/main/java/com/epass/food/modules/ai/service/ItemAiTools.java`
- `src/main/java/com/epass/food/modules/ai/service/OrderEntityReferenceResolver.java`
- `src/main/java/com/epass/food/modules/ai/service/ItemEntityReferenceResolver.java`
- `src/main/java/com/epass/food/modules/ai/dto/AiEntityReference.java`
- `src/main/java/com/epass/food/modules/ai/dto/AiQueryIntent.java`
- `src/main/java/com/epass/food/modules/ai/service/OrderAiSceneService.java`
- `src/main/java/com/epass/food/modules/ai/service/ItemAiSceneService.java`

## 一、为什么要有工具

有两类数据不能靠 prompt 静态写死：

- 当前订单详情
- 当前菜品详情

这些数据是动态的。

如果全部由 Java 先查完再塞给模型，也能做，但会越来越重。

Tool Calling 让流程变成：

1. 模型先理解问题
2. 模型决定要不要调用工具
3. 工具返回真实数据
4. 模型基于工具结果继续回答

## 二、先看 `OrderAiTools`

这里最关键的是 `@Tool`。

它的意思不是“普通注解”。

它的真实作用是：

`把这个方法暴露成可供模型调用的工具。`

所以被 `@Tool` 标记的方法，模型就有机会使用。

### 工具方法一般长什么样

你会看到这种特征：

- 入参里有 `orderId`
- 可能还会用到 `ToolContext`
- 返回的是一个专门 DTO

### 为什么返回 DTO，而不是直接返回字符串

因为 DTO 更稳定。

比如订单详情工具可以返回：

- 订单 ID
- 状态
- 金额
- 菜品明细

这种结构化数据比一段字符串更容易被模型消费，也更容易调试。

## 三、`ToolContext` 是干什么的

这是很关键的一层安全设计。

模型虽然可以触发工具，但不能绕过后端权限。

所以你会把这些信息放进 `toolContext`：

- 当前用户 ID
- 当前用户是否能看任意订单

然后工具里按这个上下文去判断权限。

这说明：

`模型可以决定是否调用工具，但不能决定权限。`

## 四、为什么还要有 `AiEntityReference`

工具不是凭空调用的。

系统得先知道用户问的是哪个实体。

所以才有：

- `entityType`
- `entityId`
- `intent`

例如一句：

`订单 1 的金额是多少`

会被拆成类似：

- entityType = order
- entityId = 1
- intent = amount

## 五、`AiQueryIntent` 有什么用

它解决的是：

`同一个实体，不同问题应该聚焦不同答案。`

比如订单 1：

- 查状态
- 查金额
- 查菜品
- 查详情

虽然都是订单 1，但回答方式应该不同。

## 六、SceneService 和 Tool 的关系

你不要把 Tool 和 SceneService 混了。

### SceneService 负责

- 判断这轮需不需要工具
- 挂哪些工具
- 组织 prompt

### Tool 负责

- 真的去拿数据

一句话：

`SceneService 决定给模型什么能力，Tool 决定怎么拿真实数据。`

## 七、为什么 `toolStatus` 还要回到主响应里

这一步非常重要。

很多人只做到：

- 模型调用工具
- 工具返回数据
- 模型说一句话

但你的项目继续往前做了：

- `toolStatus = success`
- `toolStatus = not_found`
- `toolStatus = restricted`

然后主编排器再根据它决定：

- `answerType`
- `nextAction`
- `card`

这说明工具结果已经变成系统控制逻辑的一部分。

## 本章小练习

你现在自己试着写一个最小工具：

假设写一个：

- `getMockOrderStatus(Long orderId)`

要求只做三件事：

1. 加 `@Tool`
2. 返回一个小 DTO
3. 在 `OrderAiSceneService` 里把它挂进 `AiPromptPlan`

先不要追求真实数据库。

只要跑通“挂工具”这个感觉就行。

## 本章自查

1. `@Tool` 的作用是什么？
2. 为什么工具里还需要 `toolContext`？
3. 为什么 `toolStatus` 不能只停留在模型内部？

下一章：[05-会话记忆与流式输出怎么工作.md](./05-会话记忆与流式输出怎么工作.md)
