# EFoodPass 宝宝级接口样例：认证、个人资料、会话

这份文档适合这种人：

- 你已经把项目启动起来了
- 你已经能打开 Swagger
- 你现在不想猜字段名，只想照着例子抄

如果你还没启动成功，先看：

- [baby-start.md](./baby-start.md)

如果你还不知道整体流程先点什么，先看：

- [baby-operations.md](./baby-operations.md)

## 1. 先记住这 4 件事

### 1.1 除了登录，别的接口都先授权

先调：

```http
POST /auth/login
```

拿到 `token` 以后，点 Swagger 右上角 `Authorize`，把 token 粘进去。

### 1.2 这个项目的成功返回，外面永远套一层

有数据时，通常长这样：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzUxMiJ9.xxx",
    "userId": 1,
    "username": "admin",
    "nickname": "系统管理员"
  }
}
```

没有数据时，通常长这样：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

### 1.3 分页接口的 `data` 会再包一层

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
        "id": 1
      }
    ]
  }
}
```

### 1.4 下面文档里的值很多只是示例

比如：

- `session-1`
- `13800138000`
- `2026-04-03T10:00:00`

你实际操作时，要换成你自己的真实值。

## 2. 登录

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

成功返回示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzUxMiJ9.xxx",
    "userId": 1,
    "username": "admin",
    "nickname": "系统管理员"
  }
}
```

你登录成功后，马上做这件事：

1. 复制 `data.token`
2. 点 Swagger 的 `Authorize`
3. 粘贴 token
4. 确认授权

## 3. 查看当前登录用户

接口：

```http
GET /auth/me
```

成功返回示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "userId": 1,
    "username": "admin",
    "nickname": "系统管理员",
    "phone": "13800138000",
    "lastLoginAt": "2026-04-03T10:10:10",
    "roleCodes": [
      "ADMIN"
    ],
    "permissionCodes": [
      "food:category:list",
      "food:item:list",
      "food:order:list"
    ]
  }
}
```

这一步的作用很简单：

- 确认你真的已经登录成功
- 顺手看看当前账号有什么角色和权限

## 4. 刷新 token

接口：

```http
POST /auth/refresh
```

这个接口需要你已经带着旧 token 登录。

成功返回示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzUxMiJ9.new-token",
    "userId": 1,
    "username": "admin",
    "nickname": "系统管理员"
  }
}
```

最简单理解：

- 旧 token 快过期了
- 你调用一次刷新
- 系统给你新的 token

## 5. 查看当前会话

接口：

```http
GET /auth/session/current
```

成功返回示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "userId": 1,
    "sessionId": "session-1",
    "username": "admin",
    "nickname": "系统管理员",
    "tokenVersion": 1,
    "requestIp": "127.0.0.1",
    "userAgent": "Mozilla/5.0",
    "loginTime": "2026-04-03T10:00:00",
    "lastAccessTime": "2026-04-03T10:15:00",
    "issuedAt": "2026-04-03T10:00:00",
    "expiresAt": "2026-04-03T12:00:00",
    "remainingSeconds": 6300
  }
}
```

这一步适合看：

- 当前是不是这个设备
- token 什么时候过期
- 登录 IP 和最近访问时间

## 6. 查看我的所有在线会话

接口：

```http
GET /auth/session/list
```

成功返回示例：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "sessionId": "session-1",
      "requestIp": "127.0.0.1",
      "userAgent": "Mozilla/5.0",
      "loginTime": "2026-04-03T10:00:00",
      "lastAccessTime": "2026-04-03T10:15:00",
      "expiresAt": "2026-04-03T12:00:00",
      "current": true
    },
    {
      "sessionId": "session-2",
      "requestIp": "192.168.1.10",
      "userAgent": "PostmanRuntime/7.0",
      "loginTime": "2026-04-03T09:30:00",
      "lastAccessTime": "2026-04-03T09:40:00",
      "expiresAt": "2026-04-03T11:30:00",
      "current": false
    }
  ]
}
```

你可以把它理解成：

- 这个账号当前有哪些设备在线
- 哪个是当前设备
- 哪个会话已经很久没动过

## 7. 退出当前设备

接口：

```http
DELETE /auth/session/current
```

成功返回示例：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

调用完以后：

- 当前这个 token 基本就不能继续用了
- 你要重新登录

## 8. 踢掉指定会话

接口：

```http
DELETE /auth/session/{sessionId}
```

示例：

```http
DELETE /auth/session/session-2
```

成功返回示例：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

这一招适合这种场景：

- 你发现账号在别的设备也登录着
- 你只想踢掉某一个设备
- 你不想把自己当前设备也一起登出

## 9. 退出全部设备

接口：

```http
POST /auth/logout
```

成功返回示例：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

这一步的效果是：

- 当前账号所有会话都失效
- 所有设备都需要重新登录

## 10. 修改个人资料

接口：

```http
PUT /auth/profile
```

请求体：

```json
{
  "nickname": "管理员A",
  "phone": "13800138000"
}
```

成功返回示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "userId": 1,
    "username": "admin",
    "nickname": "管理员A",
    "phone": "13800138000",
    "lastLoginAt": "2026-04-03T10:10:10",
    "roleCodes": [
      "ADMIN"
    ],
    "permissionCodes": [
      "food:category:list",
      "food:item:list",
      "food:order:list"
    ]
  }
}
```

注意两点：

- `nickname` 不能为空
- `phone` 不能和别的账号重复

## 11. 修改密码

接口：

```http
PUT /auth/password
```

请求体：

```json
{
  "oldPassword": "Admin@123",
  "newPassword": "Admin@456"
}
```

成功返回示例：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

这一点非常重要：

- 修改密码成功后
- 这个账号的所有会话都会被踢下线
- 你必须重新登录

## 12. 最常见的 4 个问题

### 12.1 返回 401

最常见原因：

- 你没先登录
- 你没点 Swagger 的 `Authorize`
- token 过期了
- 你刚刚执行了退出当前设备或退出全部设备

### 12.2 返回 403

意思通常是：

- 你登录了
- 但你没有这个接口的权限

### 12.3 修改资料时报手机号重复

意思通常是：

- 这个手机号已经被其他账号占用了

### 12.4 修改密码时报旧密码不对

意思通常是：

- `oldPassword` 填错了

## 13. 下一步最推荐看哪里

如果你已经把登录、资料、会话这些接口跑通了，下一步最适合看：

- [baby-api-food-order.md](./baby-api-food-order.md)

那一份会带你继续走：

- 分类
- 菜品
- 菜单
- 购物车
- 下单
- 支付
- 后台处理订单
- 统计和日志
