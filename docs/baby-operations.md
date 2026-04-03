# EFoodPass 宝宝级操作教程

这份教程默认你已经完成了启动。

如果你还没启动成功，先看：

- [docs/baby-start.md](/C:/Users/22390/Desktop/EFoodPass/docs/baby-start.md)

这份文档的目标很简单：

- 让你知道先点哪个接口
- 让你知道每一步大概是干什么的
- 让你知道后台管理员和普通用户分别怎么用

## 1. 第一次用 Swagger，要怎么点

先打开：

```text
http://localhost:5603/swagger-ui.html
```

然后按这个顺序做：

1. 找到 `Authentication`
2. 先调用 `POST /auth/login`
3. 复制返回的 `token`
4. 点击右上角 `Authorize`
5. 粘贴 token
6. 再去调别的接口

如果你不先授权，很多接口都会报 `401`。

## 2. 最推荐的新手体验顺序

不要一上来就乱点。照这个顺序最省脑子：

1. 管理员登录
2. 看当前用户信息
3. 新建分类
4. 新建菜品
5. 查看用户端菜单
6. 加购物车
7. 预览订单
8. 创建订单
9. 支付订单
10. 后台处理订单
11. 查看统计、日志

## 3. 管理员先做什么

### 第 1 步：登录管理员

接口：

```text
POST /auth/login
```

请求体：

```json
{
  "username": "admin",
  "password": "Admin@123"
}
```

登录成功后，把 token 放进 Swagger 的 `Authorize`。

### 第 2 步：看当前登录人信息

接口：

```text
GET /auth/me
```

你可以确认自己是不是已经登录成功。

### 第 3 步：新建一个菜品分类

接口：

```text
POST /food/category
```

你可以先试一个很简单的例子：

```json
{
  "name": "热销主食",
  "sortNo": 1,
  "status": 1
}
```

然后再调用：

```text
GET /food/category/list
```

确认分类已经创建出来。

### 第 4 步：新建一个菜品

接口：

```text
POST /food/item
```

这个接口里最重要的是：

- `categoryId`：要填你刚创建的分类 ID
- `price`：菜品价格
- `stock`：库存
- `isOnSale`：是否上架

创建完以后，再调：

```text
GET /food/item/list
```

确认菜品已经存在。

### 第 5 步：如果菜品没法卖，先检查两个地方

- 分类是不是启用状态
- 菜品是不是上架状态

相关接口：

```text
PUT /food/category/status
PUT /food/item/on-sale
PUT /food/item/stock
```

## 4. 普通用户怎么点餐

这个项目里，用户端接口统一是 `/app` 开头。

### 第 1 步：用户登录

接口还是：

```text
POST /auth/login
```

如果你已经有普通用户账号，就用普通用户登录。

如果你手里暂时只有管理员账号，也可以先用管理员账号体验接口流程。

### 第 2 步：看菜单

常用接口：

```text
GET /app/menu/tree
GET /app/menu/items
GET /app/menu/item/{id}
```

推荐顺序：

1. 先调 `GET /app/menu/tree` 看分类树
2. 再调 `GET /app/menu/items` 看菜品列表
3. 最后调 `GET /app/menu/item/{id}` 看单个菜品详情

### 第 3 步：加购物车

先看购物车：

```text
GET /app/cart
```

添加商品：

```text
POST /app/cart/item
```

示例请求体：

```json
{
  "foodItemId": 1,
  "quantity": 2
}
```

如果数量想改，调用：

```text
PUT /app/cart/item/{foodItemId}
```

如果不想要了，调用：

```text
DELETE /app/cart/item/{foodItemId}
```

如果想整车清空，调用：

```text
DELETE /app/cart/clear
```

### 第 4 步：结算购物车

接口：

```text
POST /app/cart/checkout
```

这一步的作用是：

- 重新检查库存
- 重新检查菜品是否还在售卖
- 给你返回一份可下单的结算结果

### 第 5 步：下单前先预览

接口：

```text
POST /app/order/preview
```

这一步很适合前端做“确认订单页”。

你可以在真正下单前先看看：

- 买了哪些菜
- 总金额是多少
- 每个菜的数量和单价对不对

### 第 6 步：真正创建订单

接口：

```text
POST /app/order
```

创建成功后，建议立刻去查订单列表：

```text
GET /app/order/list
```

### 第 7 步：支付订单

接口：

```text
POST /app/order/pay/{id}
```

你需要把 `{id}` 换成真实订单 ID。

支付完成后，再调用：

```text
GET /app/order/{id}
```

看看支付状态有没有变化。

### 第 8 步：如果不想要了，可以取消订单

接口：

```text
PUT /app/order/cancel/{id}
```

注意：

- 不是所有状态都能取消
- 未支付订单如果超时，系统也可能自动关单

## 5. 后台管理员怎么处理订单

后台常用订单接口：

```text
GET /food/order/list
GET /food/order/{id}
PUT /food/order/process
PUT /food/order/complete
PUT /food/order/cancel
PUT /food/order/refund
GET /food/order/export
```

### 最简单的体验顺序

1. 先调 `GET /food/order/list`
2. 找到你刚才创建的订单 ID
3. 调 `GET /food/order/{id}` 看详情
4. 调 `PUT /food/order/process` 让订单进入处理中
5. 调 `PUT /food/order/complete` 把订单完成

如果遇到退款场景，可以调用：

```text
PUT /food/order/refund
```

如果想导出订单表格，可以调用：

```text
GET /food/order/export
```

导出结果是 CSV 文件。

## 6. 后台统计要怎么看

统计接口都在这里：

```text
/food/order/stat/*
```

常用接口：

```text
GET /food/order/stat/overview
GET /food/order/stat/status-count
GET /food/order/stat/payment-status-count
GET /food/order/stat/top-item
GET /food/order/stat/daily-amount
```

你可以给这些接口带时间参数，比如：

- `createdAtStart`
- `createdAtEnd`

适合看某一段时间内的订单情况。

## 7. 日志功能怎么用

### 登录日志

接口：

```text
GET /system/login-log/list
```

你可以看到：

- 谁登录了
- 登录成功还是失败
- 登录时间
- 登录 IP

### 操作日志

接口：

```text
GET /system/operation-log/list
```

你可以看到后台做过哪些写操作，比如：

- 新建菜品
- 修改分类
- 处理订单
- 退款

## 8. 当前用户资料和会话怎么管理

### 看当前会话

```text
GET /auth/session/current
```

### 看我的所有在线会话

```text
GET /auth/session/list
```

### 退出当前设备

```text
DELETE /auth/session/current
```

### 踢掉某个会话

```text
DELETE /auth/session/{sessionId}
```

### 退出全部设备

```text
POST /auth/logout
```

### 修改个人资料

```text
PUT /auth/profile
```

### 修改密码

```text
PUT /auth/password
```

注意：

修改密码后，系统会把这个账号的所有会话都踢下线。你需要重新登录。

## 9. 新手最容易卡住的几个地方

### 菜单里看不到菜品

优先检查：

- 分类是否启用
- 菜品是否上架
- 菜品库存是否大于 0

### 调接口一直 401

优先检查：

- 有没有先登录
- 有没有点 Swagger 的 `Authorize`
- token 有没有过期

### 调接口返回 403

说明你登录了，但权限不够。

最常见情况是：

- 你拿普通用户 token 去调管理员接口

### 下单失败

优先检查：

- 菜品是否下架
- 分类是否禁用
- 库存是否不足

### 修改资料时报手机号重复

说明这个手机号已经被别的账号占用了。

## 10. 推荐你真正走通一次的完整流程

如果你只想验证“这个项目是不是完整可用”，最推荐照着下面做一遍：

1. `POST /auth/login`
2. `POST /food/category`
3. `POST /food/item`
4. `GET /app/menu/items`
5. `POST /app/cart/item`
6. `POST /app/cart/checkout`
7. `POST /app/order/preview`
8. `POST /app/order`
9. `GET /app/order/list`
10. `POST /app/order/pay/{id}`
11. `GET /food/order/list`
12. `PUT /food/order/process`
13. `PUT /food/order/complete`
14. `GET /food/order/stat/overview`
15. `GET /system/login-log/list`
16. `GET /system/operation-log/list`

你只要这 16 步都能正常跑完，说明这套项目的核心链路已经通了。

如果你希望每一步都有现成请求体和返回体可以直接照抄，继续看这两份：

- [docs/baby-api-auth-and-user.md](/C:/Users/22390/Desktop/EFoodPass/docs/baby-api-auth-and-user.md)
- [docs/baby-api-food-order.md](/C:/Users/22390/Desktop/EFoodPass/docs/baby-api-food-order.md)

## 11. 接下来再学什么

当你已经能把接口点通，就可以开始看代码了。

最推荐的顺序：

1. 先看控制器 `controller`
2. 再看服务层 `service`
3. 再看实体 `entity` 和 DTO
4. 最后看安全、日志、AI 这些横切模块
