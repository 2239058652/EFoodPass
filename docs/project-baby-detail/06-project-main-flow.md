# 06 站在全项目角度看：一个请求是怎么跑完整条链的

这一章不再按模块拆，而是按“请求链路”拆。

目的是让你把前面的分散知识重新串起来。

## 一 先看最短链路：登录

你可以把登录理解成一条 6 步链：

1. 前端 POST `/auth/login`
2. [AuthController.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/modules/auth/controller/AuthController.java) 接请求
3. [LoginRequest.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/modules/auth/dto/LoginRequest.java) 做参数校验
4. [AuthServiceImpl.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/modules/auth/service/impl/AuthServiceImpl.java) 查用户、验密码、签 token
5. controller 用 [Result.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/common/result/Result.java) 包起来
6. 前端拿到 token

## 二 再看带权限的链路：查用户列表

比如：

- `GET /system/user/list`

### 第 1 步 请求先进入 Security 过滤器链

最关键的是 [JwtAuthenticationFilter.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/config/security/JwtAuthenticationFilter.java)。

它会：

1. 从请求头拿 token
2. 解析 token
3. 查用户
4. 查角色
5. 查权限
6. 组装 `LoginUser + authorities`
7. 放进 `SecurityContextHolder`

### 第 2 步 `@PreAuthorize` 做权限判断

到 [SysUserController.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/modules/system/user/controller/SysUserController.java) 后，先看：

```java
@PreAuthorize("hasAuthority('system:user:list')")
```

没有这个权限，方法不会往下执行。

### 第 3 步 service 组装查询条件并分页

`SysUserServiceImpl.listUsers()` 会：

1. 组 wrapper
2. 查分页
3. 查每个用户的角色
4. 组装返回 DTO

### 第 4 步 返回统一分页结果

最后返回的是：

- `Result<PageResult<UserListResponse>>`

## 三 再看更复杂的链路：创建订单

比如：

- `POST /food/order`

### 第 1 步 安全链先过

先走 JWT 过滤器，确认当前请求是谁发的。

### 第 2 步 controller 接请求体

[FoodOrderController.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/modules/food/order/controller/FoodOrderController.java) 的 `create()` 接收：

- `FoodOrderCreateRequest`

### 第 3 步 DTO 先做基础格式校验

- 用户 id 不为空
- 订单明细不为空
- 每条明细数量大于 0

### 第 4 步 service 执行业务规则

[FoodOrderServiceImpl.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/modules/food/order/service/impl/FoodOrderServiceImpl.java) 会做：

- 查用户
- 查菜品
- 查分类
- 查库存
- 计算金额
- 保存订单
- 保存明细
- 扣库存
- 写库存日志

### 第 5 步 如果某一步失败

会发生两件事：

1. 抛 `BusinessException`
2. 因为有事务，所以整条订单创建回滚

### 第 6 步 全局异常统一返回

最终由 [GlobalExceptionHandler.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/common/exception/GlobalExceptionHandler.java) 包成统一 JSON。

## 四 controller、service、entity、dto、mapper 的关系

### 1. controller

负责“接请求”和“回结果”。

### 2. dto

负责“传数据”。

分成：

- request DTO
- response DTO

### 3. entity

负责“映射数据库表”。

### 4. service

负责“写业务规则”。

### 5. mapper

负责“和数据库打交道”。

## 五 这个项目为什么看起来整齐

因为它坚持了几种统一约定：

1. 返回统一：`Result<T>`
2. 分页统一：`PageQuery + PageResult<T>`
3. 业务异常统一：`BusinessException`
4. 权限统一：`@PreAuthorize`
5. 认证统一：JWT + SecurityContext

这些统一约定就是项目不会越来越乱的关键。

## 六 如果你自己复刻，一定先复刻这些统一约定

不要一上来先写订单。

建议先做：

1. 启动类
2. `Result<T>`
3. `BusinessException + GlobalExceptionHandler`
4. `PageQuery + PageResult<T>`
5. `SecurityConfig + JWT`

因为后面所有业务模块都依赖它们。

## 七 这一章最适合你的复述练习

你自己不用看文档，口头讲一遍下面两条链：

1. `/auth/login` 怎么从请求变成 token
2. `/food/order` 怎么从请求变成订单和库存变更
