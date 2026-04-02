# 05 food 模块后半段：订单和库存为什么最值得你慢慢看

订单模块是这个项目里最适合训练业务理解能力的模块。

因为它会同时碰到：

- 用户
- 菜品
- 分类
- 库存
- 订单状态
- 订单明细
- 库存日志

重点文件：

- [FoodOrderController.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/modules/food/order/controller/FoodOrderController.java)
- [FoodOrderServiceImpl.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/modules/food/order/service/impl/FoodOrderServiceImpl.java)
- [FoodOrder.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/modules/food/order/entity/FoodOrder.java)
- [FoodOrderCreateRequest.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/modules/food/order/dto/FoodOrderCreateRequest.java)
- [FoodOrderItemRequest.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/modules/food/order/dto/FoodOrderItemRequest.java)
- [FoodStockLogServiceImpl.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/modules/food/stock/service/impl/FoodStockLogServiceImpl.java)

## 一 `FoodOrder` 这个实体的字段怎么理解

看 [FoodOrder.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/modules/food/order/entity/FoodOrder.java)。

### 关键字段

- `id`：数据库主键
- `orderNo`：业务订单号
- `userId`：下单用户
- `totalAmount`：订单总金额
- `orderStatus`：订单状态
- `remark`：备注
- `createdAt` / `updatedAt`：创建和更新时间

### `orderStatus` 为什么特别重要

因为它决定订单能不能进入下一步动作。

这个项目里已经抽成 `FoodOrderStatus` 枚举，常见值是：

- `10` 待确认
- `20` 制作中
- `30` 已完成
- `40` 已取消

## 二 `FoodOrderCreateRequest` 为什么是“外层订单 + 内层明细”

看 [FoodOrderCreateRequest.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/modules/food/order/dto/FoodOrderCreateRequest.java)。

它有：

- `userId`
- `remark`
- `items`

### 为什么 `items` 上面有 `@Valid`

因为 `items` 是对象列表，不是普通字符串。

列表里的每个元素都是 [FoodOrderItemRequest.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/modules/food/order/dto/FoodOrderItemRequest.java)。

`@Valid` 的作用是：

- 外层校验生效
- 内层每条明细的校验也继续生效

### `@NotEmpty` 的作用

表示订单明细不能为空。

也就是不允许“空订单”。

## 三 `FoodOrderItemRequest` 为什么只有两个字段

它只有：

- `foodItemId`
- `quantity`

为什么没有价格和名称？

因为这些信息应该从数据库里真实查询，不能信前端传值。

## 四 `FoodOrderController` 是怎么设计的

看 [FoodOrderController.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/modules/food/order/controller/FoodOrderController.java)。

它暴露的动作是：

- 列表
- 详情
- 创建
- 开始制作
- 取消
- 完成

这里最值得注意的是：

状态变化不是开放成“随便传一个状态值”，而是拆成明确动作。

这样业务语义更清楚。

## 五 `FoodOrderServiceImpl` 是整条业务链的核心

看 [FoodOrderServiceImpl.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/modules/food/order/service/impl/FoodOrderServiceImpl.java)。

建议你重点看：

- `createOrder`
- `processOrder`
- `cancelOrder`
- `completeOrder`
- `getOrderStatOverview`

## 六 `createOrder` 到底做了多少事

你把它拆成小步骤就不难了。

### 第 1 步 校验用户

用户必须：

- 存在
- 启用

### 第 2 步 校验订单明细不能为空

没有明细，不能下单。

### 第 3 步 合并重复菜品数量

先把明细整理成：

- `foodItemId -> 总数量`

这一步是为了后面正确校验库存。

### 第 4 步 逐个校验菜品

会检查：

1. 菜品存在
2. 菜品已上架
3. 所属分类存在且启用
4. 库存足够

### 第 5 步 计算订单总金额

每条明细按：

```java
价格 * 数量
```

累计成订单总金额。

### 第 6 步 保存订单主表

这里会设置：

- `orderNo`
- `userId`
- `totalAmount`
- `orderStatus = PENDING`
- `remark`

### 第 7 步 保存订单明细

把每条 `FoodOrderItem` 插进去。

### 第 8 步 更新库存并记录库存日志

这里不是只改库存，还会调用：

```java
foodStockLogService.recordOrderDeduct(...)
```

也就是：

- 下单扣库存
- 同时留日志

## 七 为什么 `createOrder` 要加 `@Transactional`

看方法上的：

```java
@Transactional(rollbackFor = Exception.class)
```

表示：

- 这个方法放在事务里执行
- 中间任何一步失败，前面的数据库操作也回滚

为什么这里一定要事务？

因为这个方法会同时操作：

- 订单主表
- 订单明细表
- 菜品库存
- 库存日志

## 八 订单状态流转为什么分三个方法

### `processOrder`

只允许：

- 待确认 -> 制作中

### `completeOrder`

只允许：

- 制作中 -> 已完成

### `cancelOrder`

不允许取消：

- 已完成订单
- 已取消订单

取消时还会：

- 回补库存
- 写库存日志

## 九 `FoodStockLogServiceImpl` 是怎么配合订单的

看 [FoodStockLogServiceImpl.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/modules/food/stock/service/impl/FoodStockLogServiceImpl.java)。

它维护了三种库存变更类型：

- 下单扣减
- 取消回补
- 手动调整

### 为什么库存日志要单独做 service

因为库存变更不是只发生在一个地方：

- 下单会改库存
- 取消订单会改库存
- 后台手工调整也会改库存

单独抽 service 后，库存变更记录逻辑就能统一管理。

## 十 订单模块为什么最值得反复看

因为它同时体现了很多业务系统能力：

- 参数校验
- 业务校验
- 状态机思维
- 事务
- 金额计算
- 多表联动
- 库存联动
- 日志留痕

## 十一 这一章最建议你的练习

自己画出 `createOrder` 的流程图，至少包含：

1. 校验用户
2. 校验订单明细
3. 合并重复菜品数量
4. 校验菜品/分类/库存
5. 计算总金额
6. 保存订单主表
7. 保存订单明细
8. 更新库存
9. 记录库存日志
