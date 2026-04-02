# 03 Scene Handler 和 PromptPlan 到底是什么

上一章：[02-再看总编排器-AiChatServiceImpl.md](./02-再看总编排器-AiChatServiceImpl.md)  
下一章：[04-Tool-Calling-在这个项目里怎么落地.md](./04-Tool-Calling-在这个项目里怎么落地.md)

## 本章目标

这一章要搞清楚两个项目化设计：

1. 为什么有 `AiSceneHandler`
2. 为什么有 `AiPromptPlan`

如果这两个你吃透了，后面看 order、item、system 都会轻松很多。

## 先看的文件

- `src/main/java/com/epass/food/modules/ai/service/AiSceneHandler.java`
- `src/main/java/com/epass/food/modules/ai/dto/AiPromptPlan.java`
- `src/main/java/com/epass/food/modules/ai/service/GeneralAiSceneService.java`
- `src/main/java/com/epass/food/modules/ai/service/OrderAiSceneService.java`
- `src/main/java/com/epass/food/modules/ai/service/ItemAiSceneService.java`
- `src/main/java/com/epass/food/modules/ai/service/SystemAiSceneService.java`

## 一、先看 `AiSceneHandler`

这个接口的存在，就是为了回答一句话：

`不同场景的 AI 处理逻辑，应该放在哪？`

答案是：

`放在各自的场景处理器里。`

### 这个接口最重要的两个方法

#### `sceneType()`

表示：

`这个 handler 负责哪个场景。`

#### `buildPlan(AiSceneRequestContext context)`

表示：

`给我一轮上下文，我返回这一轮 AI 调用计划。`

注意，不是直接返回模型结果。

而是返回“计划”。

---

## 二、为什么要有 `AiPromptPlan`

很多初学者会想：

`场景处理器直接返回一个 prompt 不就行了吗？`

不行。

因为一轮 AI 调用真正需要的东西，早就不只是 prompt 了。

你现在的项目里，一轮计划至少包括：

- prompt
- answerType
- grounded
- nextAction
- card
- tools
- toolContext
- advisorParams
- advisors

所以才需要 `AiPromptPlan`。

## 三、逐个看 `AiPromptPlan` 的字段

### 1. `prompt`

这一轮真正发给模型的 system prompt。

### 2. `answerType`

这一轮默认的回答类型。

为什么说“默认”？

因为后面还可能被 `toolStatus` 收敛覆盖。

### 3. `grounded`

这轮回答默认是否基于真实事实。

### 4. `nextAction`

前端默认下一步动作。

### 5. `card`

默认卡片。

### 6. `tools`

这一轮允许模型用哪些工具。

### 7. `toolContext`

工具执行时可能需要的后端上下文。

比如：

- 当前用户 ID
- 当前权限信息

### 8. `advisorParams`

给 advisor 的额外参数。

比如：

- structured output 规则
- rag strategy
- filter expression

### 9. `advisors`

这一轮场景特有的 advisor。

比如 system 场景的 `QuestionAnswerAdvisor`。

---

## 四、为什么 `General/Order/Item/System` 都要单独有 scene service

因为它们虽然都叫“AI 回答”，但背后的上下文完全不一样。

### `GeneralAiSceneService`

处理最普通、最泛化的问题。

### `OrderAiSceneService`

处理订单领域的问题。

它会更关注：

- 订单工具
- 订单规则
- 订单详情
- 订单统计

### `ItemAiSceneService`

处理菜品领域的问题。

### `SystemAiSceneService`

处理系统知识类问题。

它会接 RAG。

---

## 五、你现在要建立的工程感觉

这套结构和普通业务模块其实很像。

不是：

`搞 AI，所以随便写个大类`

而是：

`AI 也是业务模块，也需要分职责、分场景、分计划`

这就是为什么你会看到：

- handler
- plan
- context
- advisor

这些东西慢慢出现。

---

## 六、你自己应该能做的最小复刻

你现在自己可以尝试：

1. 写一个最小 `AiSceneHandler`
2. 写一个最小 `AiPromptPlan`
3. 写一个最小 `GeneralAiSceneService`
4. 让主编排器从 map 里找到它

只做最小版本就够。

关键是感受：

`为什么 handler + plan 会比一堆 if else 好维护。`

---

## 本章自查

1. 为什么 scene handler 不能只返回字符串 prompt？
2. `AiPromptPlan` 比 prompt 多解决了什么问题？
3. 为什么 system 场景应该有自己单独的 scene service？

下一章：[04-Tool-Calling-在这个项目里怎么落地.md](./04-Tool-Calling-在这个项目里怎么落地.md)
