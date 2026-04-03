# EFoodPass 宝宝级接口样例：分类、菜品、购物车、订单、统计、日志

这份文档的目标很直接：

- 让你不用猜字段名
- 让你知道每一步请求体该怎么写
- 让你知道返回结果大概长什么样

在开始之前，先确认两件事：

1. 你已经看过 [baby-start.md](./baby-start.md)
2. 你已经先登录并完成 Swagger 授权

登录和会话相关例子在这里：

- [baby-api-auth-and-user.md](./baby-api-auth-and-user.md)

## 1. 先记住这几个规则

### 1.1 创建接口通常不直接返回 ID

比如这些接口：

- `POST /food/category`
- `POST /food/item`
- `POST /app/order`

成功后通常只返回：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

所以你下一步要立刻调用列表接口，把刚创建出来的真实 ID 查出来。

### 1.2 下面文档里的 ID 都只是示例

比如：

- `categoryId = 1`
- `itemId = 1`
- `orderId = 1001`

你实际操作时，要换成你自己查出来的真实值。

### 1.3 订单状态和支付状态先记一下

订单状态：

- `10`：待确认
- `20`：制作中
- `30`：已完成
- `40`：已取消

支付状态：

- `10`：待支付
- `20`：已支付
- `30`：已退款

### 1.4 支付方式支持这 3 个值

调用 `POST /app/order/pay/{id}` 时，`paymentMethod` 推荐填这几个之一：

- `MOCK`
- `ALIPAY`
- `WECHAT`

## 2. 第一步：创建菜品分类

接口：

```http
POST /food/category
```

请求体：

```json
{
  "name": "热销主食",
  "sortNo": 1,
  "status": 1
}
```

成功返回：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

说明：

- `status = 1` 表示启用
- `sortNo` 是排序号
- 这一步不会直接给你分类 ID

## 3. 第二步：查询分类列表，拿到分类 ID

接口：

```http
GET /food/category/list?pageNum=1&pageSize=10
```

成功返回示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 1,
    "pageNum": 1,
    "pageSize": 10,
    "records": [
      {
        "id": 1,
        "name": "热销主食",
        "sortNo": 1,
        "status": 1
      }
    ]
  }
}
```

这时候你就拿到了：

- `categoryId = 1`

后面创建菜品要用它。

## 4. 第三步：如果分类状态不对，可以改状态

接口：

```http
PUT /food/category/status
```

请求体：

```json
{
  "categoryId": 1,
  "status": 1
}
```

成功返回：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

## 5. 第四步：创建菜品

接口：

```http
POST /food/item
```

请求体：

```json
{
  "categoryId": 1,
  "name": "牛肉炒饭",
  "price": 18.00,
  "stock": 50,
  "isOnSale": 1,
  "description": "少油不辣，适合新手测试"
}
```

成功返回：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

注意：

- 字段名是 `isOnSale`
- 不是 `onSale`

## 6. 第五步：查询菜品列表，拿到菜品 ID

接口：

```http
GET /food/item/list?pageNum=1&pageSize=10&categoryId=1
```

成功返回示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 1,
    "pageNum": 1,
    "pageSize": 10,
    "records": [
      {
        "id": 1,
        "categoryId": 1,
        "categoryName": "热销主食",
        "name": "牛肉炒饭",
        "price": 18.00,
        "stock": 50,
        "isOnSale": 1
      }
    ]
  }
}
```

这时候你就拿到了：

- `itemId = 1`

## 7. 第六步：调整菜品上下架和库存

### 7.1 修改上下架

接口：

```http
PUT /food/item/on-sale
```

请求体：

```json
{
  "itemId": 1,
  "isOnSale": 1
}
```

### 7.2 调整库存

接口：

```http
PUT /food/item/stock
```

请求体：

```json
{
  "itemId": 1,
  "stock": 80,
  "remark": "补货测试"
}
```

这两个接口成功时，返回都是：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

## 8. 第七步：查看用户端菜单分类树

接口：

```http
GET /app/menu/tree
```

成功返回示例：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "name": "热销主食",
      "sortNo": 1,
      "items": [
        {
          "id": 1,
          "categoryId": 1,
          "categoryName": "热销主食",
          "name": "牛肉炒饭",
          "price": 18.00,
          "stock": 80,
          "soldOut": false,
          "description": "少油不辣，适合新手测试"
        }
      ]
    }
  ]
}
```

## 9. 第八步：查看用户端菜品列表

接口：

```http
GET /app/menu/items?pageNum=1&pageSize=10&categoryId=1
```

成功返回示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 1,
    "pageNum": 1,
    "pageSize": 10,
    "records": [
      {
        "id": 1,
        "categoryId": 1,
        "categoryName": "热销主食",
        "name": "牛肉炒饭",
        "price": 18.00,
        "stock": 80,
        "soldOut": false,
        "description": "少油不辣，适合新手测试"
      }
    ]
  }
}
```

## 10. 第九步：查看单个菜品详情

接口：

```http
GET /app/menu/item/1
```

成功返回示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "categoryId": 1,
    "categoryName": "热销主食",
    "name": "牛肉炒饭",
    "price": 18.00,
    "stock": 80,
    "soldOut": false,
    "description": "少油不辣，适合新手测试"
  }
}
```

## 11. 第十步：加入购物车

接口：

```http
POST /app/cart/item
```

请求体：

```json
{
  "foodItemId": 1,
  "quantity": 2
}
```

成功返回：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

## 12. 第十一步：查看购物车

接口：

```http
GET /app/cart
```

成功返回示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "items": [
      {
        "foodItemId": 1,
        "categoryId": 1,
        "categoryName": "热销主食",
        "name": "牛肉炒饭",
        "price": 18.00,
        "quantity": 2,
        "amount": 36.00,
        "stock": 80,
        "soldOut": false,
        "available": true,
        "unavailableReason": null
      }
    ],
    "totalQuantity": 2,
    "totalAmount": 36.00,
    "invalidItemCount": 0,
    "canCheckout": true
  }
}
```

如果你想改数量，接口是：

```http
PUT /app/cart/item/{foodItemId}
```

请求体：

```json
{
  "quantity": 3
}
```

如果你想删掉某一项，接口是：

```http
DELETE /app/cart/item/{foodItemId}
```

如果你想清空购物车，接口是：

```http
DELETE /app/cart/clear
```

## 13. 第十二步：购物车结算

接口：

```http
POST /app/cart/checkout
```

请求体可以这样写：

```json
{
  "remark": "少辣，不要香菜"
}
```

成功返回示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "totalQuantity": 2,
    "totalAmount": 36.00
  }
}
```

## 14. 第十三步：下单前预览

接口：

```http
POST /app/order/preview
```

请求体：

```json
{
  "remark": "少辣，不要香菜",
  "items": [
    {
      "foodItemId": 1,
      "quantity": 2
    }
  ]
}
```

成功返回示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "totalQuantity": 2,
    "totalAmount": 36.00,
    "remark": "少辣，不要香菜",
    "items": [
      {
        "foodItemId": 1,
        "foodName": "牛肉炒饭",
        "price": 18.00,
        "quantity": 2,
        "amount": 36.00
      }
    ]
  }
}
```

## 15. 第十四步：真正创建订单

接口：

```http
POST /app/order
```

请求体和预览时一样：

```json
{
  "remark": "少辣，不要香菜",
  "items": [
    {
      "foodItemId": 1,
      "quantity": 2
    }
  ]
}
```

成功返回：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

重点提醒：

- 这一步不会直接返回 `orderId`
- 你要马上调用订单列表接口去查

## 16. 第十五步：查看我的订单列表，拿到订单 ID

接口：

```http
GET /app/order/list?pageNum=1&pageSize=10
```

成功返回示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 1,
    "pageNum": 1,
    "pageSize": 10,
    "records": [
      {
        "id": 1001,
        "orderNo": "FO202604030001",
        "userId": 1,
        "totalAmount": 36.00,
        "orderStatus": 10,
        "orderStatusLabel": "待确认",
        "paymentStatus": 10,
        "paymentStatusLabel": "待支付",
        "paymentMethod": null,
        "paidAt": null,
        "closeReason": null,
        "closeReasonLabel": null,
        "closedAt": null,
        "remark": "少辣，不要香菜",
        "createdAt": "2026-04-03T10:30:00"
      }
    ]
  }
}
```

现在你拿到了：

- `orderId = 1001`

## 17. 第十六步：查看订单详情

接口：

```http
GET /app/order/1001
```

成功返回示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1001,
    "orderNo": "FO202604030001",
    "userId": 1,
    "totalAmount": 36.00,
    "orderStatus": 10,
    "orderStatusLabel": "待确认",
    "paymentStatus": 10,
    "paymentStatusLabel": "待支付",
    "paymentMethod": null,
    "paidAt": null,
    "closeReason": null,
    "closeReasonLabel": null,
    "closedAt": null,
    "remark": "少辣，不要香菜",
    "createdAt": "2026-04-03T10:30:00",
    "updatedAt": "2026-04-03T10:30:00",
    "items": [
      {
        "foodItemId": 1,
        "foodNameSnapshot": "牛肉炒饭",
        "priceSnapshot": 18.00,
        "quantity": 2,
        "amount": 36.00
      }
    ]
  }
}
```

## 18. 第十七步：支付订单

接口：

```http
POST /app/order/pay/1001
```

请求体：

```json
{
  "paymentMethod": "ALIPAY"
}
```

成功返回：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

再查一次订单详情，你会看到：

- `paymentStatus` 变成 `20`
- `paymentStatusLabel` 变成 `已支付`
- `paymentMethod` 变成 `ALIPAY`
- `paidAt` 会有值

## 19. 第十八步：取消订单

接口：

```http
PUT /app/order/cancel/1001
```

成功返回：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

注意：

- 不是所有状态都能取消
- 已完成订单通常不能取消

## 20. 第十九步：后台查看订单列表

接口：

```http
GET /food/order/list?pageNum=1&pageSize=10&paymentStatus=20
```

成功返回示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 1,
    "pageNum": 1,
    "pageSize": 10,
    "records": [
      {
        "id": 1001,
        "orderNo": "FO202604030001",
        "userId": 1,
        "totalAmount": 36.00,
        "orderStatus": 10,
        "orderStatusLabel": "待确认",
        "paymentStatus": 20,
        "paymentStatusLabel": "已支付",
        "paymentMethod": "ALIPAY",
        "paidAt": "2026-04-03T10:32:00",
        "closeReason": null,
        "closeReasonLabel": null,
        "closedAt": null,
        "remark": "少辣，不要香菜",
        "createdAt": "2026-04-03T10:30:00"
      }
    ]
  }
}
```

这个接口常用筛选参数有：

- `orderNo`
- `userId`
- `orderStatus`
- `paymentStatus`
- `createdAtStart`
- `createdAtEnd`

## 21. 第二十步：后台开始处理订单

接口：

```http
PUT /food/order/process
```

请求体：

```json
{
  "orderId": 1001
}
```

成功返回：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

## 22. 第二十一步：后台完成订单

接口：

```http
PUT /food/order/complete
```

请求体：

```json
{
  "orderId": 1001
}
```

成功返回：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

## 23. 第二十二步：后台退款

接口：

```http
PUT /food/order/refund
```

请求体：

```json
{
  "orderId": 1001,
  "closeReason": "用户反馈下错单"
}
```

成功返回：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

退款后你可能会在订单详情里看到：

- `paymentStatus = 30`
- `paymentStatusLabel = 已退款`
- `closeReason` 或 `closeReasonLabel` 有值

## 24. 第二十三步：导出订单 CSV

接口：

```http
GET /food/order/export?pageNum=1&pageSize=10&paymentStatus=20
```

这个接口和别的不一样：

- 它返回的是文件
- 不是 JSON
- 浏览器会下载一个 `.csv`

所以你看到下载行为，就是正常现象。

## 25. 第二十四步：查看订单统计总览

接口：

```http
GET /food/order/stat/overview
```

Swagger 里参数这样填：

- `createdAtStart`：`2026-04-03 00:00:00`
- `createdAtEnd`：`2026-04-03 23:59:59`

成功返回示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "totalOrderCount": 12,
    "pendingOrderCount": 2,
    "processingOrderCount": 3,
    "completedOrderCount": 6,
    "canceledOrderCount": 1,
    "totalAmount": 356.00,
    "completedAmount": 280.00
  }
}
```

## 26. 第二十五步：查看支付状态统计

接口：

```http
GET /food/order/stat/payment-status-count
```

Swagger 里参数这样填：

- `createdAtStart`：`2026-04-03 00:00:00`
- `createdAtEnd`：`2026-04-03 23:59:59`

成功返回示例：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "paymentStatus": 10,
      "paymentStatusLabel": "待支付",
      "orderCount": 2
    },
    {
      "paymentStatus": 20,
      "paymentStatusLabel": "已支付",
      "orderCount": 8
    },
    {
      "paymentStatus": 30,
      "paymentStatusLabel": "已退款",
      "orderCount": 2
    }
  ]
}
```

## 27. 第二十六步：查看登录日志

接口：

```http
GET /system/login-log/list?pageNum=1&pageSize=10
```

成功返回示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 1,
    "pageNum": 1,
    "pageSize": 10,
    "records": [
      {
        "id": 1,
        "userId": 1,
        "username": "admin",
        "requestIp": "127.0.0.1",
        "success": 1,
        "message": "login success",
        "loginTime": "2026-04-03T10:00:00"
      }
    ]
  }
}
```

## 28. 第二十七步：查看操作日志

接口：

```http
GET /system/operation-log/list?pageNum=1&pageSize=10
```

成功返回示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 1,
    "pageNum": 1,
    "pageSize": 10,
    "records": [
      {
        "id": 1,
        "requestId": "req-202604031001",
        "userId": 1,
        "username": "admin",
        "module": "FOOD_ITEM",
        "action": "CREATE",
        "method": "POST",
        "path": "/food/item",
        "requestIp": "127.0.0.1",
        "success": 1,
        "errorMessage": null,
        "costMs": 28,
        "operateTime": "2026-04-03T10:05:00"
      }
    ]
  }
}
```

## 29. 最适合新手照抄的一条完整链路

如果你现在只想验证项目是不是通的，最推荐直接照这个顺序：

1. `POST /auth/login`
2. `POST /food/category`
3. `GET /food/category/list`
4. `POST /food/item`
5. `GET /food/item/list`
6. `GET /app/menu/tree`
7. `POST /app/cart/item`
8. `GET /app/cart`
9. `POST /app/cart/checkout`
10. `POST /app/order/preview`
11. `POST /app/order`
12. `GET /app/order/list`
13. `POST /app/order/pay/{id}`
14. `GET /food/order/list`
15. `PUT /food/order/process`
16. `PUT /food/order/complete`
17. `GET /food/order/stat/overview`
18. `GET /system/login-log/list`
19. `GET /system/operation-log/list`

## 30. 新手最容易犯的错误

### 30.1 把 `sortNo` 写成 `sort`

分类创建接口要用：

```json
{
  "sortNo": 1
}
```

### 30.2 把 `isOnSale` 写成 `onSale`

菜品创建和修改上下架时要用：

```json
{
  "isOnSale": 1
}
```

### 30.3 刚创建完分类、菜品、订单，就想直接拿到 ID

这个项目很多创建接口成功时都只返回：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

所以你要自己再查一次列表。

### 30.4 用错支付方式

推荐直接填：

- `MOCK`
- `ALIPAY`
- `WECHAT`

### 30.5 菜单里看不到菜

优先检查：

- 分类是不是启用
- 菜品是不是上架
- 库存是不是大于 0
