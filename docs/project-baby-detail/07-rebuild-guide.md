# 07 如果你要自己认真复刻，这一套该怎么重写

这一章讲的不是现有代码，而是：

如果你想真正把这个项目学到自己手里，应该按什么顺序重新做。

## 一 先说结论：不要全量照抄

如果你一口气从头照抄全项目，效果通常很差。

因为你会进入这种状态：

- 手在打字
- 脑子没跟上
- 一停下来就不会

正确做法是：

- 每次只复刻一个很小的闭环
- 复刻完就自己跑通
- 再进入下一块

## 二 最推荐的复刻顺序

### 第 1 阶段 只做项目骨架

先自己做出：

1. Spring Boot 启动类
2. `Result<T>`
3. `BusinessException`
4. `GlobalExceptionHandler`
5. `PageQuery`
6. `PageResult<T>`

### 第 2 阶段 只做认证登录

再做：

1. `LoginRequest`
2. `LoginResponse`
3. `AuthController`
4. `AuthService`
5. `AuthServiceImpl`
6. `PasswordConfig`
7. `JwtTokenProvider`
8. `SecurityConfig`
9. `JwtAuthenticationFilter`
10. `LoginUser`

### 第 3 阶段 只做用户模块

只做：

1. `SysUser` 实体
2. `SysUserMapper`
3. `SysUserService`
4. `SysUserServiceImpl`
5. `SysUserController`

先别急着连角色权限，先把用户 CRUD 跑通。

### 第 4 阶段 再接角色和权限

然后再补：

1. `SysRole`
2. `SysPermission`
3. `SysUserRole`
4. `SysRolePermission`
5. 角色 service
6. 权限 service
7. 登录链里查角色与权限
8. `@PreAuthorize`

### 第 5 阶段 做分类和菜品

先做：

1. 分类 CRUD
2. 菜品 CRUD
3. 上下架
4. 手动调整库存

### 第 6 阶段 最后做订单和库存日志

最后再做：

1. 下单
2. 订单状态流转
3. 取消回补库存
4. 库存日志
5. 简单统计

## 三 每个阶段你都只做这 3 件事

1. 先写最小代码
2. 先跑一个接口
3. 再补校验和边界

## 四 你在复刻时最容易犯的 6 个错误

### 错误 1 直接从最长的 service 开始抄

比如一上来就抄订单 service。

### 错误 2 把实体、请求 DTO、响应 DTO 混用

请强行记住：

- entity 映射数据库
- request DTO 接前端输入
- response DTO 回前端输出

### 错误 3 把 controller 写成业务中心

controller 最好只做：

- 接请求
- 调 service
- 返回结果

### 错误 4 校验全堆在 DTO 或全堆在 service

更合理的分工是：

- DTO：基础格式校验
- service：业务规则校验

### 错误 5 不写统一异常处理

这样接口错误格式会越来越乱。

### 错误 6 不理解事务就直接写订单

订单这种多表联动功能，不理解事务很容易写出脏数据。

## 五 你自己复刻时建议准备一个“理解清单”

每写完一个类，都问自己：

1. 这个类处于哪一层
2. 这个类为什么要存在
3. 这里的字段是数据库字段还是传输字段
4. 这里的注解影响的是哪一层
5. 这个方法是在做数据查询，还是在做业务规则

## 六 你现在最不需要做的事情

1. 不要追求代码和原项目完全一致
2. 不要追求一次全会
3. 不要沉浸在“我好像做完了”的假成就感里

真正有效的是：

- 你能解释
- 你能重写
- 你能改动

## 七 你可以怎么用这套文档

最推荐的方法是：

### 第 1 天

只读：

- [01-boot-common-config.md](/C:/Users/22390/Desktop/EFoodPass/docs/project-baby-detail/01-boot-common-config.md)
- [02-auth-security.md](/C:/Users/22390/Desktop/EFoodPass/docs/project-baby-detail/02-auth-security.md)

### 第 2 天

只读：

- [03-system-user-role-permission.md](/C:/Users/22390/Desktop/EFoodPass/docs/project-baby-detail/03-system-user-role-permission.md)

### 第 3 天

只读：

- [04-food-category-item.md](/C:/Users/22390/Desktop/EFoodPass/docs/project-baby-detail/04-food-category-item.md)

### 第 4 天

只读：

- [05-food-order-stock.md](/C:/Users/22390/Desktop/EFoodPass/docs/project-baby-detail/05-food-order-stock.md)

### 第 5 天

重读：

- [06-project-main-flow.md](/C:/Users/22390/Desktop/EFoodPass/docs/project-baby-detail/06-project-main-flow.md)
- 本章

## 八 最后给你的建议

你现在最需要做的事情，是开始建立这种节奏：

1. 读一小章
2. 打开对应类
3. 自己解释字段和方法
4. 调一个接口
5. 第二天再继续

你要追求的不是快，而是扎实。
