# EFoodPass NestJS 模块任务拆解与初始化模板

## 1. 文档用途

这份文档是《EFoodPass SpringBoot → NestJS 1:1 复刻说明》的配套执行文档。

作用只有两个：

1. 把整个 NestJS 复刻项目拆成可执行的模块级任务
2. 规定 NestJS 项目的初始化标准，避免开发过程越写越乱

建议使用方式：

- 总体方向看主文档
- 每天开发时看这份任务拆解文档
- 让其他大模型辅助编码时，把这份文档和主文档一起提供

---

## 2. 整体开发顺序

严格按以下顺序推进：

1. 项目初始化
2. 基础公共层
3. 数据库接入
4. Auth 模块
5. RBAC 模块
6. 分类模块
7. 餐品模块
8. 库存日志模块
9. 订单模块
10. 统计模块
11. 联调与验收
12. 文档收尾

不要跳过前置基础设施直接写订单模块。

---

## 3. 项目初始化标准模板

## 3.1 项目名称建议

建议新项目名称：

- `efoodpass-nest`

或者：

- `EFoodPass-nest`

保持与 SpringBoot 项目名称有明显对应关系。

## 3.2 推荐依赖

### 核心依赖

- `@nestjs/common`
- `@nestjs/core`
- `@nestjs/platform-express`
- `@nestjs/config`
- `@nestjs/swagger`
- `@nestjs/passport`
- `@nestjs/jwt`
- `passport`
- `passport-jwt`
- `typeorm`
- `@nestjs/typeorm`
- `mysql2`
- `class-validator`
- `class-transformer`
- `bcrypt`
- `reflect-metadata`
- `rxjs`

### 开发依赖

- `typescript`
- `ts-node`
- `tsconfig-paths`
- `@types/node`
- `@types/passport-jwt`
- `@types/bcrypt`
- `eslint`
- `prettier`

## 3.3 环境变量文件建议

至少包含：

### `.env.development`

- `PORT`
- `APP_NAME`
- `DB_HOST`
- `DB_PORT`
- `DB_USERNAME`
- `DB_PASSWORD`
- `DB_DATABASE`
- `JWT_SECRET`
- `JWT_EXPIRES_IN`
- `SWAGGER_PATH`

### 示例

```env
PORT=3000
APP_NAME=EFoodPass Nest
DB_HOST=127.0.0.1
DB_PORT=3306
DB_USERNAME=root
DB_PASSWORD=123456
DB_DATABASE=efoodpass_nest
JWT_SECRET=replace_with_your_secret
JWT_EXPIRES_IN=7d
SWAGGER_PATH=api-docs
```

## 3.4 tsconfig 基本要求

要求：

- 开启装饰器
- 开启元数据
- 严格模式尽量开启
- 配置路径别名

## 3.5 eslint / prettier 原则

目标不是折腾规则，而是保证：

- 文件风格统一
- import 顺序稳定
- DTO / Entity / Service / Controller 结构清晰

---

## 4. 建议目录结构模板

```text
src/
  main.ts
  app.module.ts

  config/
    app.config.ts
    database.config.ts
    jwt.config.ts
    swagger.config.ts

  common/
    constants/
      biz-error-code.ts
      auth.constants.ts
      order.constants.ts
    decorators/
      current-user.decorator.ts
      require-permissions.decorator.ts
      public.decorator.ts
    dto/
      base-id.dto.ts
    enums/
    exceptions/
      business.exception.ts
    filters/
      global-exception.filter.ts
    guards/
      jwt-auth.guard.ts
      permissions.guard.ts
    interceptors/
      response.interceptor.ts
    interfaces/
      current-user.interface.ts
    utils/
      password.util.ts
      page.util.ts

  shared/
    result/
      result.ts
    page/
      page-query.dto.ts
      page-result.dto.ts

  modules/
    auth/
      dto/
        login.dto.ts
      strategies/
        jwt.strategy.ts
      auth.controller.ts
      auth.module.ts
      auth.service.ts

    system/
      user/
        dto/
        entities/
        user.controller.ts
        user.module.ts
        user.service.ts
      role/
        dto/
        entities/
        role.controller.ts
        role.module.ts
        role.service.ts
      permission/
        dto/
        entities/
        permission.controller.ts
        permission.module.ts
        permission.service.ts

    food/
      category/
        dto/
        entities/
        category.controller.ts
        category.module.ts
        category.service.ts
      item/
        dto/
        entities/
        item.controller.ts
        item.module.ts
        item.service.ts
      stock-log/
        dto/
        entities/
        stock-log.controller.ts
        stock-log.module.ts
        stock-log.service.ts
      order/
        dto/
        entities/
        order.controller.ts
        order.module.ts
        order.service.ts
```

---

## 5. main.ts 初始化标准

`main.ts` 至少要完成：

1. 创建应用实例
2. 开启全局前缀（如原项目有）
3. 开启全局 ValidationPipe
4. 注册全局异常过滤器
5. 注册统一响应拦截器
6. 初始化 Swagger
7. 启动端口

### 建议要求

- `transform: true`
- `whitelist: true`
- `forbidNonWhitelisted: false` 可根据原项目调整
- Swagger 标题与项目名保持一致

---

## 6. AppModule 初始化标准

`AppModule` 必须包含：

- `ConfigModule.forRoot()`
- `TypeOrmModule.forRootAsync()`
- 各业务 Module 注册

### 数据库配置硬性要求

- `synchronize: false`
- `logging` 开发环境可开，生产关闭
- 明确配置实体路径

不要为了图省事打开 `synchronize: true`。

---

## 7. 公共层先做什么

在正式写业务模块前，先完成以下公共层：

### 7.1 Result 统一响应

建立统一返回结构，例如：

- `ok(data)`
- `fail(code, message)`

### 7.2 ResponseInterceptor

所有成功返回统一包装，避免 Controller 手工拼结构。

### 7.3 BusinessException

建立业务异常基类，携带：

- `code`
- `message`

### 7.4 GlobalExceptionFilter

统一处理：

- ValidationPipe 参数错误
- BusinessException
- UnauthorizedException
- ForbiddenException
- 未知系统异常

### 7.5 分页 DTO

需要：

- `PageQueryDto`
- `PageResultDto<T>`

建议分页字段与 SpringBoot 保持一致。

### 7.6 公共装饰器

至少需要：

- `@CurrentUser()`
- `@RequirePermissions()`
- `@Public()`

---

## 8. 模块级任务拆解总览

以下模块按顺序逐个完成。

---

## 9. Auth 模块任务拆解

## 9.1 目标

先把登录与当前用户获取打通，作为所有模块基础。

## 9.2 必做文件

```text
modules/auth/
  dto/login.dto.ts
  strategies/jwt.strategy.ts
  auth.controller.ts
  auth.module.ts
  auth.service.ts
```

## 9.3 依赖对象

Auth 模块会依赖：

- 用户实体
- 角色实体
- 权限实体
- bcrypt
- jwt

## 9.4 任务清单

- [ ] 定义 `LoginDto`
- [ ] 编写登录接口
- [ ] 根据用户名查询用户
- [ ] 校验密码
- [ ] 校验用户状态
- [ ] 查询用户角色、权限
- [ ] 生成 JWT
- [ ] 编写 `/auth/me`
- [ ] 编写 `JwtStrategy`
- [ ] 在 `request.user` 中恢复当前用户上下文
- [ ] 校验 `tokenVersion`
- [ ] 处理未认证异常

## 9.5 验收点

- [ ] 正确用户名密码可登录
- [ ] 错误密码返回业务错误
- [ ] 禁用用户不可登录
- [ ] `/auth/me` 返回正确
- [ ] token 过期或伪造能被拦截
- [ ] 修改密码后旧 token 失效

---

## 10. 用户模块任务拆解

## 10.1 必做文件

```text
modules/system/user/
  dto/
    user-query.dto.ts
    create-user.dto.ts
    update-user.dto.ts
    reset-password.dto.ts
    assign-roles.dto.ts
    user-response.dto.ts
  entities/
    user.entity.ts
    user-role.entity.ts
  user.controller.ts
  user.module.ts
  user.service.ts
```

## 10.2 任务清单

- [ ] 映射用户表实体
- [ ] 映射用户角色关联表
- [ ] 用户分页查询
- [ ] 用户详情
- [ ] 新增用户
- [ ] 修改用户
- [ ] 删除用户
- [ ] 启用/禁用用户
- [ ] 重置密码
- [ ] 分配角色

## 10.3 业务规则

- [ ] `username` 唯一
- [ ] admin 用户不可删除
- [ ] admin 用户不可禁用
- [ ] 重置密码后要处理 tokenVersion
- [ ] 删除或禁用前要符合原项目逻辑

## 10.4 验收点

- [ ] 用户 CRUD 正常
- [ ] admin 保护生效
- [ ] 重置密码后旧 token 失效
- [ ] 角色分配正确写库

---

## 11. 角色模块任务拆解

## 11.1 必做文件

```text
modules/system/role/
  dto/
    role-query.dto.ts
    create-role.dto.ts
    update-role.dto.ts
    assign-permissions.dto.ts
    role-response.dto.ts
  entities/
    role.entity.ts
    role-permission.entity.ts
  role.controller.ts
  role.module.ts
  role.service.ts
```

## 11.2 任务清单

- [ ] 映射角色表
- [ ] 映射角色权限关联表
- [ ] 角色分页查询
- [ ] 角色详情
- [ ] 新增角色
- [ ] 修改角色
- [ ] 删除角色
- [ ] 启用/禁用角色
- [ ] 分配权限

## 11.3 业务规则

- [ ] `roleCode` 唯一
- [ ] ADMIN 角色不可删除
- [ ] ADMIN 角色不可禁用
- [ ] 核心角色保护逻辑保持一致

## 11.4 验收点

- [ ] 角色 CRUD 正常
- [ ] 分配权限成功
- [ ] 核心角色保护正常

---

## 12. 权限模块任务拆解

## 12.1 必做文件

```text
modules/system/permission/
  dto/
    permission-query.dto.ts
    create-permission.dto.ts
    update-permission.dto.ts
    permission-response.dto.ts
  entities/
    permission.entity.ts
  permission.controller.ts
  permission.module.ts
  permission.service.ts
```

## 12.2 任务清单

- [ ] 映射权限表
- [ ] 权限分页查询
- [ ] 权限详情
- [ ] 新增权限
- [ ] 修改权限
- [ ] 删除权限
- [ ] 启用/禁用权限

## 12.3 业务规则

- [ ] `permCode` 唯一
- [ ] `permCode` 与原项目保持一致
- [ ] 核心权限不可删除
- [ ] 核心权限不可禁用

## 12.4 验收点

- [ ] 权限 CRUD 正常
- [ ] 核心权限保护正常
- [ ] 权限码鉴权前置数据准备完成

---

## 13. 权限守卫实现任务

这一项虽然依赖用户、角色、权限数据，但建议单独当作一个任务完成。

## 13.1 必做文件

```text
common/decorators/require-permissions.decorator.ts
common/guards/permissions.guard.ts
```

## 13.2 任务清单

- [ ] 实现 `@RequirePermissions(...codes)`
- [ ] 从元数据中读取权限码
- [ ] 从 `request.user` 中读取当前用户权限集合
- [ ] 判断是否具备要求权限
- [ ] 无权限时抛出统一异常

## 13.3 验收点

- [ ] 有权限时可访问
- [ ] 无权限时返回一致错误
- [ ] 多权限判断逻辑正确

---

## 14. 分类模块任务拆解

## 14.1 必做文件

```text
modules/food/category/
  dto/
    category-query.dto.ts
    create-category.dto.ts
    update-category.dto.ts
    update-category-status.dto.ts
    category-response.dto.ts
  entities/
    food-category.entity.ts
  category.controller.ts
  category.module.ts
  category.service.ts
```

## 14.2 任务清单

- [ ] 映射分类表
- [ ] 分类分页/列表查询
- [ ] 分类详情
- [ ] 新增分类
- [ ] 修改分类
- [ ] 修改分类状态
- [ ] 删除分类

## 14.3 业务规则

- [ ] 分类名必填
- [ ] 分类名唯一
- [ ] 状态值只能取合法值
- [ ] 被餐品引用的分类不能删除

## 14.4 验收点

- [ ] 分类 CRUD 正常
- [ ] 唯一性校验正常
- [ ] 引用保护正常

---

## 15. 餐品模块任务拆解

## 15.1 必做文件

```text
modules/food/item/
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
  item.module.ts
  item.service.ts
```

## 15.2 任务清单

- [ ] 映射餐品表
- [ ] 餐品分页/列表查询
- [ ] 餐品详情
- [ ] 新增餐品
- [ ] 修改餐品
- [ ] 上下架
- [ ] 手动调整库存
- [ ] 删除餐品

## 15.3 业务规则

- [ ] 分类必须存在
- [ ] 分类必须启用
- [ ] 同分类下餐品名唯一
- [ ] 价格不能小于 0
- [ ] 库存不能小于 0
- [ ] 上架状态必须合法
- [ ] 分类禁用时不能上架餐品
- [ ] 被订单引用的餐品不能删除

## 15.4 验收点

- [ ] 餐品 CRUD 正常
- [ ] 上下架逻辑正确
- [ ] 调库存成功
- [ ] 删除保护正常

---

## 16. 库存日志模块任务拆解

## 16.1 必做文件

```text
modules/food/stock-log/
  dto/
    stock-log-query.dto.ts
    stock-log-response.dto.ts
  entities/
    food-stock-log.entity.ts
  stock-log.controller.ts
  stock-log.module.ts
  stock-log.service.ts
```

## 16.2 任务清单

- [ ] 映射库存日志表
- [ ] 库存日志查询接口
- [ ] 封装写日志的 service 方法
- [ ] 与手动调库存联动
- [ ] 与订单扣库存联动
- [ ] 与订单取消回库存联动

## 16.3 验收点

- [ ] 手动调库存后有日志
- [ ] 下单后有日志
- [ ] 取消订单后有日志
- [ ] 日志列表查询正常

---

## 17. 订单模块任务拆解

这是最复杂模块，必须放在后面做。

## 17.1 必做文件

```text
modules/food/order/
  dto/
    order-query.dto.ts
    create-order.dto.ts
    create-current-user-order.dto.ts
    cancel-order.dto.ts
    process-order.dto.ts
    finish-order.dto.ts
    order-stat-query.dto.ts
    order-response.dto.ts
  entities/
    food-order.entity.ts
    food-order-item.entity.ts
  order.controller.ts
  order.module.ts
  order.service.ts
```

## 17.2 任务清单

- [ ] 映射订单主表
- [ ] 映射订单明细表
- [ ] 后台订单列表
- [ ] 后台订单详情
- [ ] 后台创建订单
- [ ] 后台处理订单
- [ ] 后台取消订单
- [ ] 后台完成订单
- [ ] 当前用户订单列表
- [ ] 当前用户订单详情
- [ ] 当前用户创建订单
- [ ] 当前用户取消订单

## 17.3 事务任务拆解

### 创建订单事务

- [ ] 校验用户存在且启用
- [ ] 校验商品存在
- [ ] 校验商品上架状态
- [ ] 校验分类启用状态
- [ ] 聚合同一请求中重复商品数量
- [ ] 校验库存充足
- [ ] 写订单主表
- [ ] 写订单明细表
- [ ] 扣减库存
- [ ] 写库存日志
- [ ] 任一步失败整体回滚

### 取消订单事务

- [ ] 校验订单状态是否允许取消
- [ ] 回补库存
- [ ] 写库存日志
- [ ] 更新订单状态
- [ ] 任一步失败整体回滚

## 17.4 用户侧安全规则

- [ ] 用户只能查看自己的订单
- [ ] 用户只能取消自己允许取消的订单

## 17.5 状态流转规则

- [ ] 待处理 → 处理中
- [ ] 待处理 / 处理中 → 已取消（按原项目规则）
- [ ] 处理中 → 已完成
- [ ] 非法状态流转必须拦截

## 17.6 验收点

- [ ] 下单成功扣库存
- [ ] 库存不足无法下单
- [ ] 重复商品聚合正确
- [ ] 取消订单回库存
- [ ] 日志联动正确
- [ ] 用户归属校验正确
- [ ] 事务失败能回滚

---

## 18. 统计模块任务拆解

## 18.1 能力要求

至少复刻：

- 订单概览
- 订单状态统计
- 热销商品统计
- 每日金额统计

## 18.2 任务清单

- [ ] 梳理原 SpringBoot 统计 SQL / 逻辑
- [ ] 在 NestJS 中复刻查询
- [ ] 确保字段名和返回结构尽量一致

## 18.3 验收点

- [ ] 统计结果和原项目基本一致
- [ ] 日期维度统计正确
- [ ] 热销数据排序正确

---

## 19. 联调与对照检查任务

这是最后阶段，但非常重要。

## 19.1 接口对照清单

每完成一个模块，都要对照：

- [ ] URL 是否一致
- [ ] HTTP 方法是否一致
- [ ] Query 参数是否一致
- [ ] Body 字段是否一致
- [ ] 响应字段是否一致
- [ ] 错误码是否一致
- [ ] 错误 message 是否接近

## 19.2 数据库对照清单

- [ ] 主表写入字段一致
- [ ] 关联表写入字段一致
- [ ] 状态字段更新一致
- [ ] 时间字段行为一致
- [ ] 日志记录一致

## 19.3 权限对照清单

- [ ] 接口权限码一致
- [ ] SQL 初始化中的权限码一致
- [ ] 无权限场景返回一致

---

## 20. 推荐开发节奏

建议每次只做一个小任务包，不要贪多。

### 节奏示例

#### 第一轮

- 项目初始化
- 公共层
- 数据库连接

#### 第二轮

- 用户实体
- Auth 登录
- JwtStrategy
- `/auth/me`

#### 第三轮

- 角色、权限实体
- PermissionsGuard
- 用户/角色/权限基础查询

#### 第四轮

- 分类模块
- 餐品模块
- 手动库存调整

#### 第五轮

- 库存日志
- 订单模块
- 统计模块

#### 第六轮

- 联调
- 修错
- 文档补全

---

## 21. 每次让其他大模型协助时的提问模板

建议你以后固定这么提：

### 模板 1：生成某个模块代码

请根据《EFoodPass SpringBoot → NestJS 1:1 复刻说明》和《EFoodPass NestJS 模块任务拆解与初始化模板》，帮我实现 NestJS 的 XXX 模块。

要求：

1. 严格按 1:1 复刻思路实现
2. 不要擅自改数据库字段
3. 不要擅自改业务规则
4. 输出完整目录结构和代码文件
5. DTO、Entity、Controller、Service 分层清晰
6. 若涉及权限，请保留权限码校验入口
7. 若涉及订单、库存、日志，请考虑事务

### 模板 2：对照 SpringBoot 代码翻译

下面我会提供 SpringBoot 的 Controller、Service、Entity、DTO 代码。
请你把它按 1:1 复刻方式翻译成 NestJS 版本。

要求：

1. 保留业务行为一致
2. 保留字段语义一致
3. 不做额外设计重构
4. 输出 TypeORM Entity、DTO、Controller、Service 代码
5. 如果有业务校验，请完整保留

### 模板 3：只做检查

下面是我已经写好的 NestJS 代码，请你从“是否符合 1:1 复刻原则”的角度检查。

重点检查：

1. 是否擅自改了数据库语义
2. 是否漏掉了业务校验
3. 是否权限控制不完整
4. 是否事务边界不完整
5. 是否响应结构与原项目不一致

---

## 22. 进度看板模板

你可以直接复制下面这个模板长期记录。

```md
# EFoodPass NestJS 复刻进度

## 当前阶段
- 阶段：
- 状态：未开始 / 进行中 / 已完成 / 阻塞
- 日期：

## 今日完成
- [ ]
- [ ]
- [ ]

## 当前完成总览
- [ ] 项目初始化
- [ ] 公共响应结构
- [ ] 全局异常处理
- [ ] Swagger
- [ ] 数据库连接
- [ ] Auth 登录
- [ ] Auth 当前用户
- [ ] Jwt Guard
- [ ] Permissions Guard
- [ ] 用户管理
- [ ] 角色管理
- [ ] 权限管理
- [ ] 分类模块
- [ ] 餐品模块
- [ ] 手动调库存
- [ ] 库存日志
- [ ] 后台订单
- [ ] 用户订单
- [ ] 订单统计
- [ ] 联调检查
- [ ] 文档补全

## 当前问题
-
-
-

## 下一步
-
-
-
```

---

## 23. 最终执行原则

整个 NestJS 复刻项目始终坚持下面这几条：

1. 以 SpringBoot 当前真实行为为准
2. 先复刻一致，再考虑优化
3. 先搭基础，再做复杂业务
4. 订单模块最后做
5. 权限码绝不乱改
6. 数据库结构绝不乱改
7. 事务逻辑绝不拆散
8. 每完成一个模块都要做对照验证

只要按这份文档推进，项目就不会乱。

