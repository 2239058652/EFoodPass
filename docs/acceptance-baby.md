# EFoodPass 宝宝级验收清单

这份文档是最后一公里。

前面的文档是在教你：

- 怎么启动
- 怎么理解接口
- 怎么理解 Flyway

这份文档是在帮你确认：

- 这个项目现在到底有没有真的跑通

如果你已经完成了启动，建议按这份清单一项一项验。

## 1. 启动前验收

先确认这几件事：

- MySQL 已启动
- Redis 已启动
- 数据库 `e_food` 已存在
- [application-dev.yml](/C:/Users/22390/Desktop/EFoodPass/src/main/resources/application-dev.yml) 的数据库和 Redis 配置是对的

## 2. 启动成功验收

启动命令：

```powershell
mvnw.cmd spring-boot:run
```

你最少要确认这两件事：

- 启动日志里没有新的报错
- 日志里最终出现了应用启动成功的信息

如果这里失败，优先先看：

- [baby-start.md](./baby-start.md)
- [flyway-baby.md](./flyway-baby.md)

## 3. Flyway 验收

### 3.1 看数据库里有没有迁移历史表

执行：

```sql
USE e_food;
SHOW TABLES LIKE 'flyway_schema_history';
```

如果能看到，说明 Flyway 已经接上。

### 3.2 看迁移历史

执行：

```sql
SELECT installed_rank,
       version,
       description,
       script,
       success
FROM flyway_schema_history
ORDER BY installed_rank;
```

你至少应该能看到：

- `V1__bootstrap_current_schema.sql`
- `V2__upgrade_legacy_schema.sql`

而且 `success` 应该是成功状态。

### 3.3 看关键字段有没有补齐

执行：

```sql
SELECT column_name
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'food_order'
  AND column_name IN ('payment_status', 'payment_method', 'paid_at', 'close_reason', 'closed_at');
```

如果这几列都查得到，说明之前那类 `Unknown column 'payment_status'` 问题已经解决。

## 4. Swagger 验收

打开：

```text
http://localhost:5603/swagger-ui.html
```

你要确认这两件事：

- 页面能打开
- 页面里能正常看到接口分组，而不是报 OpenAPI 渲染错误

如果这里又出现 “Unable to render this definition”，优先看：

- `/v3/api-docs` 返回的到底是不是正常 OpenAPI JSON
- [pom.xml](/C:/Users/22390/Desktop/EFoodPass/pom.xml) 的 Swagger 依赖是否已经是当前版本

## 5. 认证验收

### 第一步：登录

接口：

```http
POST /auth/login
```

请求体：

```json
{
  "username": "admin",
  "password": "Admin@123"
}
```

验收标准：

- 返回 `code = 200`
- 返回里有 `token`

### 第二步：查看当前用户

接口：

```http
GET /auth/me
```

验收标准：

- 返回 `code = 200`
- 返回里能看到 `username = admin`

## 6. Flyway 接口验收

注意：

- `http://localhost:5603/actuator/flyway` 直接浏览器打开返回 `401` 是正常的
- 因为这个项目要求先登录带 token

正确验收方法：

1. 先登录拿 token
2. 再用带 `Authorization: Bearer token` 的工具访问 `/actuator/flyway`

验收标准：

- 能返回迁移信息
- 不再是 `401`

## 7. 管理端基础功能验收

### 7.1 创建分类

接口：

```http
POST /food/category
```

请求体：

```json
{
  "name": "验收分类",
  "sortNo": 1,
  "status": 1
}
```

### 7.2 查询分类列表

接口：

```http
GET /food/category/list?pageNum=1&pageSize=10
```

验收标准：

- 能查到刚创建的“验收分类”

### 7.3 创建菜品

接口：

```http
POST /food/item
```

请求体示例：

```json
{
  "categoryId": 你刚查到的分类ID,
  "name": "验收炒饭",
  "price": 18.00,
  "stock": 20,
  "isOnSale": 1,
  "description": "用于验收"
}
```

### 7.4 查询菜品列表

接口：

```http
GET /food/item/list?pageNum=1&pageSize=10
```

验收标准：

- 能查到刚创建的“验收炒饭”

## 8. 用户端点餐链路验收

### 8.1 看菜单

接口：

```http
GET /app/menu/tree
GET /app/menu/items?pageNum=1&pageSize=10
```

验收标准：

- 菜单里能看到“验收炒饭”

### 8.2 加购物车

接口：

```http
POST /app/cart/item
```

请求体：

```json
{
  "foodItemId": 你刚查到的菜品ID,
  "quantity": 2
}
```

### 8.3 查看购物车

接口：

```http
GET /app/cart
```

验收标准：

- 购物车里能看到这份菜
- `canCheckout = true`

### 8.4 结算和预览

接口：

```http
POST /app/cart/checkout
POST /app/order/preview
```

验收标准：

- 结算能返回总数量和总金额
- 预览能返回菜品明细

### 8.5 创建订单

接口：

```http
POST /app/order
GET /app/order/list?pageNum=1&pageSize=10
```

验收标准：

- 列表里能看到刚创建的订单
- 订单状态应为待确认
- 支付状态应为待支付

### 8.6 支付订单

接口：

```http
POST /app/order/pay/{id}
```

请求体：

```json
{
  "paymentMethod": "ALIPAY"
}
```

验收标准：

- 订单支付状态变成已支付

## 9. 后台订单处理验收

### 9.1 查询订单列表

接口：

```http
GET /food/order/list?pageNum=1&pageSize=10
```

### 9.2 开始制作

接口：

```http
PUT /food/order/process
```

请求体：

```json
{
  "orderId": 真实订单ID
}
```

### 9.3 完成订单

接口：

```http
PUT /food/order/complete
```

请求体：

```json
{
  "orderId": 真实订单ID
}
```

验收标准：

- 订单状态最终能从待确认走到制作中，再走到已完成

## 10. 统计和日志验收

### 10.1 订单统计

接口：

```http
GET /food/order/stat/overview
GET /food/order/stat/payment-status-count
```

验收标准：

- 能看到刚才产生的订单数据

### 10.2 登录日志

接口：

```http
GET /system/login-log/list?pageNum=1&pageSize=10
```

验收标准：

- 能看到刚才登录产生的日志

### 10.3 操作日志

接口：

```http
GET /system/operation-log/list?pageNum=1&pageSize=10
```

验收标准：

- 能看到创建分类、创建菜品、订单处理等写操作日志

## 11. 最终通过标准

如果下面这些都成立，基本就可以认为项目已经跑通：

- 应用能成功启动
- Flyway 迁移成功
- Swagger 页面正常
- `/auth/login` 正常
- `/auth/me` 正常
- 菜单能查到上架菜品
- 购物车、下单、支付能跑通
- 后台订单处理能跑通
- 统计和日志接口正常

## 12. 推荐配套文件

配合这份验收清单，建议一起用这些文件：

- [baby-api-auth-and-user.md](./baby-api-auth-and-user.md)
- [baby-api-food-order.md](./baby-api-food-order.md)
- [flyway-baby.md](./flyway-baby.md)
- [flyway-practice-baby.md](./flyway-practice-baby.md)
- [troubleshooting-baby.md](./troubleshooting-baby.md)
- [baby-acceptance.http](/C:/Users/22390/Desktop/EFoodPass/scripts/baby-acceptance.http)
- [flyway-check.sql](/C:/Users/22390/Desktop/EFoodPass/scripts/flyway-check.sql)
