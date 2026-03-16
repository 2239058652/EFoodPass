# EFoodPass SpringBoot → NestJS 文件级映射表

## 1. 文档用途

这份文档用于把现有 SpringBoot 项目的代码结构，逐层映射为 NestJS 项目的文件结构。

目标不是只做“模块级”对应，而是进一步下沉到：

- Java 中一个包在 NestJS 中应该拆成哪些目录
- Java 中一个类在 NestJS 中应该变成哪些文件
- 哪些逻辑应该保留在 Controller
- 哪些逻辑应该下放到 Service
- 哪些地方要拆 DTO
- 哪些地方要拆 Entity
- 哪些地方要单独抽 Guard / Decorator / Filter / Interceptor

这份文档适合：

- 自己按模块逐步复刻时作为参照
- 提供给其他大模型做代码生成时作为结构约束
- 记录当前 SpringBoot 与 NestJS 的文件级对应关系

---

## 2. 使用原则

### 2.1 不是机械翻译，而是结构等价翻译

SpringBoot 中的：

- 一个 `Controller`
- 一个 `Service`
- 一个 `ServiceImpl`
- 一组 `DTO`
- 一个 `Entity`

在 NestJS 中不一定对应为完全同名同数量文件。

NestJS 的目标是：

**在不改变业务行为的前提下，用更适合 NestJS 的方式组织同样的功能。**

### 2.2 Controller 保持薄

不论在 Java 还是 NestJS 中，都应坚持：

- Controller 只负责接收请求、参数校验入口、权限声明、调用 Service、返回结果
- 业务逻辑不写在 Controller

### 2.3 Service 是核心业务翻译层

如果说 Java 里的 `ServiceImpl` 是业务核心，NestJS 中的 `xxx.service.ts` 就是它的直接承接者。

所以在做代码翻译时：

- 不要只看 Controller
- 必须重点翻译 ServiceImpl 的业务逻辑

### 2.4 实体与 DTO 必须分离

Java 项目如果已经区分：

- Query
- CreateRequest
- UpdateRequest
- Response

NestJS 也要维持这种分层，不要偷懒直接返回 Entity。

---

## 3. 总体目录映射

## 3.1 SpringBoot 常见目录

SpringBoot 项目通常会有类似结构：

```text
src/main/java/com/xxx/efoodpass/
  common/
  config/
  modules/
    auth/
    system/
      user/
      role/
      permission/
    food/
      category/
      item/
      order/
      stocklog/
```

## 3.2 NestJS 对应目录

NestJS 建议映射为：

```text
src/
  common/
  config/
  shared/
  modules/
    auth/
    system/
      user/
      role/
      permission/
    food/
      category/
      item/
      order/
      stock-log/
```

说明：

- SpringBoot 的 `common` 对应 NestJS 的 `common` + `shared`
- SpringBoot 的 `config` 对应 NestJS 的 `config`
- SpringBoot 的业务模块按模块名 1:1 落到 `modules`
- `stocklog` 在 NestJS 中建议统一命名为 `stock-log`

---

## 4. 基础设施层文件映射

---

## 5. Result / Page / Exception / ErrorCode 映射

### SpringBoot 侧常见文件

```text
common/
  result/
    Result.java
  page/
    PageQuery.java
    PageResult.java
  exception/
    BusinessException.java
    GlobalExceptionHandler.java
  enums/
    BizErrorCode.java
```

### NestJS 对应文件

```text
src/shared/result/
  result.ts

src/shared/page/
  page-query.dto.ts
  page-result.dto.ts

src/common/exceptions/
  business.exception.ts

src/common/filters/
  global-exception.filter.ts

src/common/constants/
  biz-error-code.ts

src/common/interceptors/
  response.interceptor.ts
```

### 文件职责说明

#### `Result.java` → `result.ts` + `response.interceptor.ts`

SpringBoot 常见做法：

- Controller 直接返回 `Result.success(data)`
- 出错时返回统一错误结构

NestJS 建议拆成：

- `result.ts`：定义统一返回结构与构造方法
- `response.interceptor.ts`：拦截所有成功响应并统一包装

#### `PageQuery.java` → `page-query.dto.ts`

主要负责：

- 页码
- 页大小
- 排序字段
- 排序方向

#### `PageResult.java` → `page-result.dto.ts`

主要负责：

- 列表数据
- 总数
- 当前页
- 页大小

#### `BusinessException.java` → `business.exception.ts`

保留：

- 业务错误码
- 错误信息

#### `GlobalExceptionHandler.java` → `global-exception.filter.ts`

统一处理：

- 参数校验错误
- 业务异常
- 无权限异常
- 未认证异常
- 系统异常

#### `BizErrorCode.java` → `biz-error-code.ts`

以常量或枚举形式定义：

- 用户相关错误码
- 权限相关错误码
- 分类相关错误码
- 餐品相关错误码
- 订单相关错误码
- 库存相关错误码

---

## 6. Config 文件映射

### SpringBoot 常见文件

```text
config/
  SecurityConfig.java
  MybatisPlusConfig.java
  SwaggerConfig.java
  WebMvcConfig.java
  JwtConfig.java
```

### NestJS 对应文件

```text
src/config/
  app.config.ts
  database.config.ts
  jwt.config.ts
  swagger.config.ts

src/common/guards/
  jwt-auth.guard.ts
  permissions.guard.ts
```

### 说明

#### `SecurityConfig.java`

在 NestJS 中不会只有一个对应文件，而是分散为：

- `jwt.strategy.ts`
- `jwt-auth.guard.ts`
- `permissions.guard.ts`
- `public.decorator.ts`
- `require-permissions.decorator.ts`

也就是说，Spring Security 的集中式配置，在 NestJS 中会拆成多处职责更清晰的文件。

#### `MybatisPlusConfig.java`

NestJS 中没有完全等价文件，主要落到：

- `database.config.ts`
- 各模块 service 的分页查询实现

#### `SwaggerConfig.java`

映射为：

- `swagger.config.ts`
- `main.ts` 中的 swagger 初始化代码

#### `JwtConfig.java`

映射为：

- `jwt.config.ts`
- `auth.module.ts` 中的 JwtModule 注册

---

## 7. 认证与鉴权文件映射

---

## 8. Auth 模块映射

### SpringBoot 常见文件

```text
modules/auth/
  controller/
    AuthController.java
  service/
    AuthService.java
  service/impl/
    AuthServiceImpl.java
  dto/
    LoginRequest.java
    LoginResponse.java
    CurrentUserResponse.java
  security/
    JwtTokenProvider.java
    JwtAuthenticationFilter.java
    LoginUser.java
    SecurityUserDetailsService.java
```

### NestJS 对应文件

```text
src/modules/auth/
  dto/
    login.dto.ts
    login-response.dto.ts
    current-user-response.dto.ts
  strategies/
    jwt.strategy.ts
  auth.controller.ts
  auth.service.ts
  auth.module.ts

src/common/decorators/
  current-user.decorator.ts
  public.decorator.ts

src/common/guards/
  jwt-auth.guard.ts

src/common/interfaces/
  current-user.interface.ts
```

### 逐文件映射说明

#### `AuthController.java` → `auth.controller.ts`

对应：

- `/auth/login`
- `/auth/me`

保留职责：

- 接收请求
- 交给 service
- 返回统一响应

#### `AuthService.java` + `AuthServiceImpl.java` → `auth.service.ts`

把 Java 的 service 接口和实现合并为 NestJS 一个 service 文件。

包含逻辑：

- 根据用户名查询用户
- 校验密码
- 校验用户状态
- 加载角色和权限
- 生成 token
- 处理 `/auth/me`
- 处理 tokenVersion 校验相关逻辑

#### `LoginRequest.java` → `login.dto.ts`

用于登录入参校验。

#### `LoginResponse.java` → `login-response.dto.ts`

用于登录成功返回结构定义。

#### `CurrentUserResponse.java` → `current-user-response.dto.ts`

用于 `/auth/me` 返回结构。

#### `JwtTokenProvider.java` → `jwt.strategy.ts` + `auth.service.ts`

Java 中 JWT 工具类的职责在 NestJS 中一般被拆开：

- 生成 token：放在 `auth.service.ts`
- 解析校验 token：放在 `jwt.strategy.ts`

#### `JwtAuthenticationFilter.java` → `jwt-auth.guard.ts` + `jwt.strategy.ts`

Java 中的 filter 逻辑在 NestJS 中更适合落成：

- `JwtStrategy`：解析 token、加载用户上下文
- `JwtAuthGuard`：保护受限接口

#### `LoginUser.java` → `current-user.interface.ts`

作为当前登录用户上下文结构定义。

#### `SecurityUserDetailsService.java`

其职责一般并入 `auth.service.ts` 或 `jwt.strategy.ts`。

---

## 9. 权限装饰器与守卫映射

### SpringBoot 常见写法

```text
@PreAuthorize("hasAuthority('system:user:list')")
```

### NestJS 对应文件

```text
src/common/decorators/
  require-permissions.decorator.ts

src/common/guards/
  permissions.guard.ts
```

### 说明

#### Java 注解 → NestJS 装饰器

SpringBoot 中的：

- `@PreAuthorize`

在 NestJS 中建议替换为：

- `@RequirePermissions('system:user:list')`

#### 权限判断逻辑

SpringBoot 中权限判断由 Security 表达式完成。

NestJS 中则由：

- 自定义装饰器写入 metadata
- `PermissionsGuard` 读取 metadata 和当前用户权限集合进行判断

---

## 10. 用户模块文件映射

### SpringBoot 常见文件

```text
modules/system/user/
  controller/
    SysUserController.java
  service/
    SysUserService.java
  service/impl/
    SysUserServiceImpl.java
  mapper/
    SysUserMapper.java
    SysUserRoleMapper.java
  entity/
    SysUser.java
    SysUserRole.java
  dto/
    UserQuery.java
    UserCreateRequest.java
    UserUpdateRequest.java
    UserResetPasswordRequest.java
    UserAssignRolesRequest.java
    UserResponse.java
```

### NestJS 对应文件

```text
src/modules/system/user/
  dto/
    user-query.dto.ts
    create-user.dto.ts
    update-user.dto.ts
    reset-user-password.dto.ts
    assign-user-roles.dto.ts
    user-response.dto.ts
  entities/
    user.entity.ts
    user-role.entity.ts
  user.controller.ts
  user.service.ts
  user.module.ts
```

### 逐文件映射说明

#### `SysUserController.java` → `user.controller.ts`

负责：

- 用户列表
- 用户详情
- 新增
- 修改
- 删除
- 启停用
- 重置密码
- 分配角色

#### `SysUserService.java` + `SysUserServiceImpl.java` → `user.service.ts`

Java 的接口和实现合并为一个 service 文件。

#### `SysUserMapper.java` → `user.entity.ts` + repository 调用

MyBatis Mapper 在 TypeORM 中通常不会独立写一个 mapper 文件，而是：

- 用 `user.entity.ts` 描述表
- 通过 repository / query builder 完成查询

#### `SysUserRoleMapper.java` → `user-role.entity.ts`

用户角色关联表用实体映射。

#### `SysUser.java` → `user.entity.ts`

#### `SysUserRole.java` → `user-role.entity.ts`

#### DTO 文件映射

- `UserQuery.java` → `user-query.dto.ts`
- `UserCreateRequest.java` → `create-user.dto.ts`
- `UserUpdateRequest.java` → `update-user.dto.ts`
- `UserResetPasswordRequest.java` → `reset-user-password.dto.ts`
- `UserAssignRolesRequest.java` → `assign-user-roles.dto.ts`
- `UserResponse.java` → `user-response.dto.ts`

---

## 11. 角色模块文件映射

### SpringBoot 常见文件

```text
modules/system/role/
  controller/
    SysRoleController.java
  service/
    SysRoleService.java
  service/impl/
    SysRoleServiceImpl.java
  mapper/
    SysRoleMapper.java
    SysRolePermissionMapper.java
  entity/
    SysRole.java
    SysRolePermission.java
  dto/
    RoleQuery.java
    RoleCreateRequest.java
    RoleUpdateRequest.java
    RoleAssignPermissionsRequest.java
    RoleResponse.java
```

### NestJS 对应文件

```text
src/modules/system/role/
  dto/
    role-query.dto.ts
    create-role.dto.ts
    update-role.dto.ts
    assign-role-permissions.dto.ts
    role-response.dto.ts
  entities/
    role.entity.ts
    role-permission.entity.ts
  role.controller.ts
  role.service.ts
  role.module.ts
```

### 说明

映射逻辑与用户模块相同：

- controller → controller
- service + impl → service
- mapper → entity + repository 查询
- dto 一一拆开

特别注意：

- `roleCode` 在 NestJS 中仍要视为稳定业务标识
- 不能因为 TypeScript 代码更方便就随意改变角色编码规则

---

## 12. 权限模块文件映射

### SpringBoot 常见文件

```text
modules/system/permission/
  controller/
    SysPermissionController.java
  service/
    SysPermissionService.java
  service/impl/
    SysPermissionServiceImpl.java
  mapper/
    SysPermissionMapper.java
  entity/
    SysPermission.java
  dto/
    PermissionQuery.java
    PermissionCreateRequest.java
    PermissionUpdateRequest.java
    PermissionResponse.java
```

### NestJS 对应文件

```text
src/modules/system/permission/
  dto/
    permission-query.dto.ts
    create-permission.dto.ts
    update-permission.dto.ts
    permission-response.dto.ts
  entities/
    permission.entity.ts
  permission.controller.ts
  permission.service.ts
  permission.module.ts
```

### 特别说明

权限模块是整个 RBAC 的基础数据源。

所以：

- `permCode` 必须映射准确
- 任何 controller 上声明的权限码都要与数据库权限码一致

---

## 13. 餐品分类模块文件映射

### SpringBoot 常见文件

```text
modules/food/category/
  controller/
    FoodCategoryController.java
  service/
    FoodCategoryService.java
  service/impl/
    FoodCategoryServiceImpl.java
  mapper/
    FoodCategoryMapper.java
  entity/
    FoodCategory.java
  dto/
    FoodCategoryQuery.java
    FoodCategoryCreateRequest.java
    FoodCategoryUpdateRequest.java
    FoodCategoryStatusRequest.java
    FoodCategoryResponse.java
```

### NestJS 对应文件

```text
src/modules/food/category/
  dto/
    category-query.dto.ts
    create-category.dto.ts
    update-category.dto.ts
    update-category-status.dto.ts
    category-response.dto.ts
  entities/
    food-category.entity.ts
  category.controller.ts
  category.service.ts
  category.module.ts
```

### 说明

- 分类 CRUD 逻辑大多会从 `FoodCategoryServiceImpl.java` 转进 `category.service.ts`
- 分类被餐品引用时不可删除，这类规则要写在 service，不要写在 controller

---

## 14. 餐品模块文件映射

### SpringBoot 常见文件

```text
modules/food/item/
  controller/
    FoodItemController.java
  service/
    FoodItemService.java
  service/impl/
    FoodItemServiceImpl.java
  mapper/
    FoodItemMapper.java
  entity/
    FoodItem.java
  dto/
    FoodItemQuery.java
    FoodItemCreateRequest.java
    FoodItemUpdateRequest.java
    FoodItemSaleStatusRequest.java
    FoodItemStockUpdateRequest.java
    FoodItemResponse.java
```

### NestJS 对应文件

```text
src/modules/food/item/
  dto/
    item-query.dto.ts
    create-item.dto.ts
    update-item.dto.ts
    update-item-sale-status.dto.ts
    update-item-stock.dto.ts
    item-response.dto.ts
  entities/
    food-item.entity.ts
  item.controller.ts
  item.service.ts
  item.module.ts
```

### 说明

#### `FoodItemServiceImpl.java` → `item.service.ts`

通常会包含：

- 分类校验
- 分类启用校验
- 同分类名称唯一校验
- 上下架逻辑
- 手动调库存逻辑
- 删除前订单引用校验

这些都应继续放在 service。

---

## 15. 库存日志模块文件映射

### SpringBoot 常见文件

```text
modules/food/stocklog/
  controller/
    FoodStockLogController.java
  service/
    FoodStockLogService.java
  service/impl/
    FoodStockLogServiceImpl.java
  mapper/
    FoodStockLogMapper.java
  entity/
    FoodStockLog.java
  dto/
    FoodStockLogQuery.java
    FoodStockLogResponse.java
```

### NestJS 对应文件

```text
src/modules/food/stock-log/
  dto/
    stock-log-query.dto.ts
    stock-log-response.dto.ts
  entities/
    food-stock-log.entity.ts
  stock-log.controller.ts
  stock-log.service.ts
  stock-log.module.ts
```

### 说明

除了查询接口外，`stock-log.service.ts` 还应被订单模块、餐品模块调用，用来记录库存变化。

也就是说：

- 这不只是一个“展示型模块”
- 还是一个“被其他业务模块依赖的日志写入模块”

---

## 16. 订单模块文件映射

### SpringBoot 常见文件

```text
modules/food/order/
  controller/
    FoodOrderController.java
    AppFoodOrderController.java
  service/
    FoodOrderService.java
    AppFoodOrderService.java
  service/impl/
    FoodOrderServiceImpl.java
    AppFoodOrderServiceImpl.java
  mapper/
    FoodOrderMapper.java
    FoodOrderItemMapper.java
  entity/
    FoodOrder.java
    FoodOrderItem.java
  dto/
    FoodOrderQuery.java
    FoodOrderCreateRequest.java
    AppFoodOrderCreateRequest.java
    FoodOrderCancelRequest.java
    FoodOrderProcessRequest.java
    FoodOrderFinishRequest.java
    FoodOrderResponse.java
    FoodOrderStatQuery.java
    FoodOrderStatResponse.java
```

### NestJS 对应文件

```text
src/modules/food/order/
  dto/
    order-query.dto.ts
    create-order.dto.ts
    create-current-user-order.dto.ts
    cancel-order.dto.ts
    process-order.dto.ts
    finish-order.dto.ts
    order-stat-query.dto.ts
    order-response.dto.ts
    order-stat-response.dto.ts
  entities/
    food-order.entity.ts
    food-order-item.entity.ts
  order.controller.ts
  order.service.ts
  order.module.ts
```

### 逐文件映射说明

#### `FoodOrderController.java` + `AppFoodOrderController.java` → `order.controller.ts`

可以有两种做法：

### 做法 A：合并到一个 controller

优点：

- 文件少
- 容易集中看逻辑

适合学习项目。

### 做法 B：拆成两个 controller

例如：

```text
order-admin.controller.ts
order-app.controller.ts
```

优点：

- 后台接口和当前用户接口职责更清晰

如果你想严格区分后台订单和用户订单，这种方式更清楚。

### 推荐

学习阶段可以先合并为一个 `order.controller.ts`，但内部方法命名和路由前缀要清晰。

#### `FoodOrderService.java` + `AppFoodOrderService.java` + impl → `order.service.ts`

如果逻辑复杂，也可以拆：

```text
order.service.ts
order-stat.service.ts
```

但第一阶段为了对照 SpringBoot，建议先集中在一个 `order.service.ts` 中。

#### `FoodOrderMapper.java` → `food-order.entity.ts`

#### `FoodOrderItemMapper.java` → `food-order-item.entity.ts`

#### DTO 一一对应

- `FoodOrderQuery.java` → `order-query.dto.ts`
- `FoodOrderCreateRequest.java` → `create-order.dto.ts`
- `AppFoodOrderCreateRequest.java` → `create-current-user-order.dto.ts`
- `FoodOrderCancelRequest.java` → `cancel-order.dto.ts`
- `FoodOrderProcessRequest.java` → `process-order.dto.ts`
- `FoodOrderFinishRequest.java` → `finish-order.dto.ts`
- `FoodOrderResponse.java` → `order-response.dto.ts`
- `FoodOrderStatQuery.java` → `order-stat-query.dto.ts`
- `FoodOrderStatResponse.java` → `order-stat-response.dto.ts`

### 特别说明

订单模块应优先翻译：

- `ServiceImpl`
- 事务逻辑
- 库存扣减与回补逻辑
- 库存日志联动逻辑

不要只把 controller 翻译出来就算完成。

---

## 17. 统计查询文件映射

如果 SpringBoot 中订单统计单独拆了 service，可在 NestJS 中进一步拆：

### SpringBoot 可能存在

```text
modules/food/order/service/
  FoodOrderStatService.java
modules/food/order/service/impl/
  FoodOrderStatServiceImpl.java
```

### NestJS 可选拆法

```text
src/modules/food/order/
  order-stat.service.ts
```

### 是否必须拆

不是必须。

学习阶段如果你想保持文件少一些，可以先把统计逻辑放进：

- `order.service.ts`

如果后续统计逻辑较多，再拆出：

- `order-stat.service.ts`

---

## 18. Mapper 层整体映射规则

---

## 19. Java Mapper → TypeORM 的统一规则

### SpringBoot 侧

MyBatis 项目中常见：

- `xxxMapper.java`
- XML SQL 或注解 SQL

### NestJS 侧

通常不再保留单独 mapper 文件，而是采用：

- `entity.ts`
- repository
- query builder
- 必要时 custom repository 风格封装

### 映射原则

#### 简单 CRUD

直接在 service 中使用 repository。

#### 复杂分页查询

使用 query builder。

#### 复杂统计 SQL

可以：

- 使用 query builder
- 或适当写原生 SQL

### 注意

不要为了追求“像 Java 一样”硬造一个 `mapper.ts` 层，除非真的有必要。

第一阶段应以“清晰复刻业务”为目标，而不是为了形式对齐做额外抽象。

---

## 20. 实体类映射规则

### SpringBoot 常见文件

```text
entity/
  Xxx.java
```

### NestJS 对应文件

```text
entities/
  xxx.entity.ts
```

### 映射要求

- 表名一致
- 主键名一致
- 字段名一致
- 时间字段一致
- 状态字段一致
- 逻辑含义一致

### 不建议的做法

- 不要因为 NestJS 代码可读性好就随便改字段英文名
- 不要把数据库字段语义重新设计
- 不要省略关键状态字段

---

## 21. DTO 映射规则

### SpringBoot 常见 DTO 分类

- Query
- CreateRequest
- UpdateRequest
- Response
- StatusRequest
- AssignRequest

### NestJS 命名建议

- `xxx-query.dto.ts`
- `create-xxx.dto.ts`
- `update-xxx.dto.ts`
- `update-xxx-status.dto.ts`
- `assign-xxx.dto.ts`
- `xxx-response.dto.ts`

### 规则

一个 Java DTO，原则上对应一个 NestJS DTO 文件。

这样做虽然文件多，但最适合：

- 精确复刻
- 清晰维护
- 让其他模型稳定生成代码

---

## 22. 枚举 / 常量映射规则

### SpringBoot 常见文件

```text
enums/
constants/
```

### NestJS 对应文件

```text
src/common/constants/
  auth.constants.ts
  order.constants.ts
  permission.constants.ts
```

或：

```text
src/common/enums/
  order-status.enum.ts
```

### 建议

如果当前 SpringBoot 已有明确常量定义，NestJS 中要同步抽出来，不要在 service 中散落魔法数字。

例如：

- 订单状态
- 用户状态
- 分类状态
- 商品上架状态
- 核心权限码

---

## 23. 工具类映射规则

### SpringBoot 常见文件

```text
utils/
  JwtUtils.java
  PasswordUtils.java
  SecurityUtils.java
```

### NestJS 对应文件

```text
src/common/utils/
  password.util.ts
```

### 说明

NestJS 中不建议把所有东西都做成工具类。

映射原则：

- 真正纯函数类逻辑，放 `utils`
- 依赖业务上下文的逻辑，放 service
- 依赖 request 的逻辑，放 decorator / guard / strategy

### 示例

#### `PasswordUtils.java` → `password.util.ts`

负责：

- bcrypt hash
- bcrypt compare

#### `SecurityUtils.java`

如果是获取当前用户这类逻辑，更适合拆成：

- `current-user.decorator.ts`
- `jwt.strategy.ts`

而不是直接做一个大工具类。

---

## 24. 文件级生成优先级

给其他模型分任务时，建议按下面顺序生成。

### 第一批：基础层

1. `result.ts`
2. `page-query.dto.ts`
3. `page-result.dto.ts`
4. `business.exception.ts`
5. `global-exception.filter.ts`
6. `response.interceptor.ts`
7. `database.config.ts`
8. `swagger.config.ts`
9. `main.ts`
10. `app.module.ts`

### 第二批：认证层

1. `login.dto.ts`
2. `current-user.interface.ts`
3. `current-user.decorator.ts`
4. `public.decorator.ts`
5. `jwt.strategy.ts`
6. `jwt-auth.guard.ts`
7. `auth.service.ts`
8. `auth.controller.ts`
9. `auth.module.ts`

### 第三批：RBAC 数据层

1. `user.entity.ts`
2. `role.entity.ts`
3. `permission.entity.ts`
4. `user-role.entity.ts`
5. `role-permission.entity.ts`

### 第四批：RBAC 业务层

1. `require-permissions.decorator.ts`
2. `permissions.guard.ts`
3. `user.service.ts`
4. `user.controller.ts`
5. `role.service.ts`
6. `role.controller.ts`
7. `permission.service.ts`
8. `permission.controller.ts`

### 第五批：餐饮基础业务层

1. `food-category.entity.ts`
2. `category.service.ts`
3. `category.controller.ts`
4. `food-item.entity.ts`
5. `item.service.ts`
6. `item.controller.ts`
7. `food-stock-log.entity.ts`
8. `stock-log.service.ts`
9. `stock-log.controller.ts`

### 第六批：订单与统计

1. `food-order.entity.ts`
2. `food-order-item.entity.ts`
3. `order.service.ts`
4. `order.controller.ts`
5. `order-stat-response.dto.ts`

---

## 25. 给其他大模型的直接任务模板

### 模板 A：按 Java 文件翻译

下面我会给你一组 SpringBoot 文件，请你严格按照《EFoodPass SpringBoot → NestJS 文件级映射表》把它们翻译成 NestJS 文件。

要求：

1. 保持 1:1 复刻思路
2. 输出完整文件路径
3. 输出对应代码
4. 不擅自改数据库字段语义
5. 不擅自改业务规则
6. Controller、Service、DTO、Entity 分层清晰

### 模板 B：按模块生成

请根据《EFoodPass SpringBoot → NestJS 文件级映射表》，帮我生成 NestJS 的 XXX 模块。

我需要你输出：

1. 目录结构
2. 所有 DTO 文件
3. Entity 文件
4. Controller 文件
5. Service 文件
6. Module 文件
7. 若涉及权限或认证，补充 Guard / Decorator / Strategy

### 模板 C：做文件映射核对

下面是我 SpringBoot 的文件列表和我已经写好的 NestJS 文件列表。
请你根据《EFoodPass SpringBoot → NestJS 文件级映射表》帮我检查：

1. 哪些文件漏了
2. 哪些职责放错位置了
3. 哪些 DTO 需要继续拆分
4. 哪些 Java 逻辑还没有真正迁移

---

## 26. 最终原则

文件级映射的核心不是“文件名长得像”，而是：

1. Java 的职责在 NestJS 中有明确承接者
2. 业务核心逻辑都被迁移
3. DTO / Entity / Service / Controller 的边界清楚
4. 权限与事务逻辑没有丢
5. 后续可以继续稳定增量开发

如果你以后分模块推进，只要始终对照这份文件级映射表，项目结构就不会乱。

