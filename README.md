# EFoodPass

EFoodPass 是一个基于 Spring Boot 3、Spring Security、MyBatis-Plus、MySQL 的后端项目。  
它的定位不是单纯的示例工程，而是一套带认证、权限、系统管理、餐品管理、订单管理、统计和库存日志的后台服务。

这份 README 的目标不是逐行解释代码，而是帮助接手者快速理解：

- 这个项目是怎么分层的
- 每个目录是做什么的
- 每个模块在系统里承担什么职责
- 阅读源码时应该从哪里开始
- 接手维护时要注意什么

---

## 1. 项目概览

当前项目已经覆盖的核心能力：

- JWT 登录认证
- 基于权限码的接口级鉴权
- 用户、角色、权限管理
- 菜品分类管理
- 菜品管理
- 后台订单管理
- 用户端订单接口
- 订单统计
- 库存调整与库存日志

从业务角度看，主链路大致是：

1. 管理员登录系统
2. 维护分类和菜品
3. 创建或管理订单
4. 跟踪订单状态流转
5. 查看订单统计
6. 查看库存变化日志

---

## 2. 技术栈

主要技术依赖定义在 [pom.xml](C:\Users\22390\Desktop\EFoodPass\pom.xml)。

当前技术栈包括：

- Java 17
- Spring Boot 3
- Spring Web
- Spring Validation
- Spring Security
- MyBatis-Plus
- MySQL
- JWT
- SpringDoc OpenAPI / Swagger
- Redis 依赖已引入

说明：

- Redis 依赖已经在项目中引入，但当前核心业务并不强依赖 Redis 才能运行。
- Swagger 用于接口调试和文档查看。
- MyBatis-Plus 负责大部分单表 CRUD 和分页。

---

## 3. 顶层目录说明

项目根目录下值得重点关注的目录和文件：

### `src`

主代码目录。所有 Java 源码和配置文件都在这里。

### `sql`

数据库脚本目录。

当前最重要的是：

- [init-rbac.sql](C:\Users\22390\Desktop\EFoodPass\sql\init-rbac.sql)

它不仅包含表结构，也包含管理员账号、角色、权限初始化，是新库初始化的核心脚本。

### `scripts`

辅助脚本目录。是否使用要看实际脚本内容，但它不是理解主业务的第一入口。

### `PROJECT_HANDOFF.md`

项目交接记录，偏任务上下文，不是系统设计说明，但对理解项目演进过程有帮助。

### `FRONTEND_LLM_GUIDE.md`

给大模型生成前端时使用的详细说明文档。

### `FRONTEND_LLM_PROMPT.md`

给大模型生成前端时使用的短版约束提示词。

---

## 4. Java 源码主目录结构

主源码根目录：

- [src/main/java/com/epass/food](C:\Users\22390\Desktop\EFoodPass\src\main\java\com\epass\food)

这里主要分成三块：

- `common`
- `config`
- `modules`

再加一个启动入口：

- [EFoodPassApplication.java](C:\Users\22390\Desktop\EFoodPass\src\main\java\com\epass\food\EFoodPassApplication.java)

---

## 5. `common` 目录说明

目录：

- [common](C:\Users\22390\Desktop\EFoodPass\src\main\java\com\epass\food\common)

这一层放的是全项目通用能力，不属于任何具体业务模块。

### `common/result`

主要负责统一响应结构和错误码。

关键文件：

- [Result.java](C:\Users\22390\Desktop\EFoodPass\src\main\java\com\epass\food\common\result\Result.java)
- [ResultCode.java](C:\Users\22390\Desktop\EFoodPass\src\main\java\com\epass\food\common\result\ResultCode.java)
- [BizErrorCode.java](C:\Users\22390\Desktop\EFoodPass\src\main\java\com\epass\food\common\result\BizErrorCode.java)

作用：

- 统一成功/失败返回格式
- 统一业务错误码常量

接手时要注意：

- 新增业务异常时，优先在 `BizErrorCode` 里扩展，不要直接写裸数字
- 前端对接时，统一解析 `Result<T>`

### `common/page`

主要负责分页请求与分页返回。

关键文件：

- [PageQuery.java](C:\Users\22390\Desktop\EFoodPass\src\main\java\com\epass\food\common\page\PageQuery.java)
- [PageResult.java](C:\Users\22390\Desktop\EFoodPass\src\main\java\com\epass\food\common\page\PageResult.java)

作用：

- 统一列表查询的分页参数
- 统一分页响应结构

### `common/exception`

主要负责全局异常处理。

关键文件：

- [BusinessException.java](C:\Users\22390\Desktop\EFoodPass\src\main\java\com\epass\food\common\exception\BusinessException.java)
- [GlobalExceptionHandler.java](C:\Users\22390\Desktop\EFoodPass\src\main\java\com\epass\food\common\exception\GlobalExceptionHandler.java)

作用：

- 让业务层可以抛出可控异常
- 统一把异常转换成标准响应

接手时要注意：

- 大部分业务校验失败都应抛 `BusinessException`
- 不要在 Controller 里堆业务判断

---

## 6. `config` 目录说明

目录：

- [config](C:\Users\22390\Desktop\EFoodPass\src\main\java\com\epass\food\config)

这一层负责全局配置。

### `MybatisPlusConfig.java`

作用：

- 配置 MyBatis-Plus
- 通常包含分页相关能力

### `PasswordConfig.java`

作用：

- 配置密码加密器
- 登录和密码重置都会依赖它

### `OpenApiConfig.java`

作用：

- 配置 Swagger / OpenAPI 文档

### `config/security`

目录：

- [config/security](C:\Users\22390\Desktop\EFoodPass\src\main\java\com\epass\food\config\security)

这一层是整个认证和鉴权体系的核心。

关键文件：

- [SecurityConfig.java](C:\Users\22390\Desktop\EFoodPass\src\main\java\com\epass\food\config\security\SecurityConfig.java)
- [JwtAuthenticationFilter.java](C:\Users\22390\Desktop\EFoodPass\src\main\java\com\epass\food\config\security\JwtAuthenticationFilter.java)
- [JwtTokenProvider.java](C:\Users\22390\Desktop\EFoodPass\src\main\java\com\epass\food\config\security\JwtTokenProvider.java)
- [JwtProperties.java](C:\Users\22390\Desktop\EFoodPass\src\main\java\com\epass\food\config\security\JwtProperties.java)
- [LoginUser.java](C:\Users\22390\Desktop\EFoodPass\src\main\java\com\epass\food\config\security\LoginUser.java)
- [AuthenticationEntryPointImpl.java](C:\Users\22390\Desktop\EFoodPass\src\main\java\com\epass\food\config\security\AuthenticationEntryPointImpl.java)
- [AccessDeniedHandlerImpl.java](C:\Users\22390\Desktop\EFoodPass\src\main\java\com\epass\food\config\security\AccessDeniedHandlerImpl.java)

作用：

- 配置哪些接口放行、哪些需要登录
- 在请求进入 Controller 前解析 JWT
- 构建当前登录用户上下文
- 处理未登录和无权限异常

接手时要注意：

- 公开接口必须在 `SecurityConfig` 里显式放行
- 业务接口的权限控制主要依赖 `@PreAuthorize`
- 用户端 `/app/order/**` 目前没有单独权限码，而是依赖“登录后可访问”

---

## 7. `modules` 目录说明

目录：

- [modules](C:\Users\22390\Desktop\EFoodPass\src\main\java\com\epass\food\modules)

这里按业务域拆模块，是阅读项目的主入口。

当前主要有：

- `auth`
- `system`
- `food`
- `admin`
- `test`

---

## 8. `auth` 模块说明

目录：

- [modules/auth](C:\Users\22390\Desktop\EFoodPass\src\main\java\com\epass\food\modules\auth)

作用：

- 登录
- 获取当前登录用户信息

关键文件：

- [AuthController.java](C:\Users\22390\Desktop\EFoodPass\src\main\java\com\epass\food\modules\auth\controller\AuthController.java)
- [AuthServiceImpl.java](C:\Users\22390\Desktop\EFoodPass\src\main\java\com\epass\food\modules\auth\service\impl\AuthServiceImpl.java)
- DTO 目录

提供的接口：

- `POST /auth/login`
- `GET /auth/me`

接手时要注意：

- `/auth/login` 是整个前后端登录流程的起点
- `/auth/me` 是前端权限和用户信息初始化的核心接口

---

## 9. `system` 模块说明

目录：

- [modules/system](C:\Users\22390\Desktop\EFoodPass\src\main\java\com\epass\food\modules\system)

这一层是后台系统管理域，不是餐饮业务本身。

包含三个子模块：

- `user`
- `role`
- `permission`

### `system/user`

作用：

- 管理后台用户
- 分配角色
- 重置密码
- 启停用户

关键文件：

- [SysUserController.java](C:\Users\22390\Desktop\EFoodPass\src\main\java\com\epass\food\modules\system\user\controller\SysUserController.java)
- [SysUserServiceImpl.java](C:\Users\22390\Desktop\EFoodPass\src\main\java\com\epass\food\modules\system\user\service\impl\SysUserServiceImpl.java)

### `system/role`

作用：

- 管理角色
- 给角色分配权限

关键文件：

- [SysRoleController.java](C:\Users\22390\Desktop\EFoodPass\src\main\java\com\epass\food\modules\system\role\controller\SysRoleController.java)
- [SysRoleServiceImpl.java](C:\Users\22390\Desktop\EFoodPass\src\main\java\com\epass\food\modules\system\role\service\impl\SysRoleServiceImpl.java)

### `system/permission`

作用：

- 管理权限定义
- 维护接口权限元数据

关键文件：

- [SysPermissionController.java](C:\Users\22390\Desktop\EFoodPass\src\main\java\com\epass\food\modules\system\permission\controller\SysPermissionController.java)
- [SysPermissionServiceImpl.java](C:\Users\22390\Desktop\EFoodPass\src\main\java\com\epass\food\modules\system\permission\service\impl\SysPermissionServiceImpl.java)

接手时要注意：

- `system` 模块是整个 RBAC 的基础层
- 业务模块里的权限码需要在 SQL 初始化脚本里同步维护
- 系统管理员账号、角色、核心权限都有保护逻辑，不能随便禁用或删除

---

## 10. `food` 模块说明

目录：

- [modules/food](C:\Users\22390\Desktop\EFoodPass\src\main\java\com\epass\food\modules\food)

这是真正的业务域，目前包含：

- `category`
- `item`
- `order`
- `stock`

### 10.1 `food/category`

作用：

- 管理菜品分类

关键文件：

- [FoodCategoryController.java](C:\Users\22390\Desktop\EFoodPass\src\main\java\com\epass\food\modules\food\category\controller\FoodCategoryController.java)
- [FoodCategoryServiceImpl.java](C:\Users\22390\Desktop\EFoodPass\src\main\java\com\epass\food\modules\food\category\service\impl\FoodCategoryServiceImpl.java)

提供的核心能力：

- 分类列表
- 分类详情
- 新增分类
- 修改分类
- 修改分类状态
- 删除分类

接手时要注意：

- 分类删除前会检查是否已被菜品引用
- 分类状态只允许 0/1

### 10.2 `food/item`

作用：

- 管理菜品
- 管理上下架
- 手工调整库存

关键文件：

- [FoodItemController.java](C:\Users\22390\Desktop\EFoodPass\src\main\java\com\epass\food\modules\food\item\controller\FoodItemController.java)
- [FoodItemServiceImpl.java](C:\Users\22390\Desktop\EFoodPass\src\main\java\com\epass\food\modules\food\item\service\impl\FoodItemServiceImpl.java)

提供的核心能力：

- 菜品列表
- 菜品详情
- 新增/修改菜品
- 修改上下架状态
- 调整库存
- 删除菜品

接手时要注意：

- 分类停用后，不允许继续操作该分类下菜品
- 删除菜品前会检查是否已被订单明细引用
- 库存调整会记录库存日志

### 10.3 `food/order`

作用：

- 订单主流程
- 用户端订单接口
- 订单统计

关键文件：

- [FoodOrderController.java](C:\Users\22390\Desktop\EFoodPass\src\main\java\com\epass\food\modules\food\order\controller\FoodOrderController.java)
- [AppOrderController.java](C:\Users\22390\Desktop\EFoodPass\src\main\java\com\epass\food\modules\food\order\controller\AppOrderController.java)
- [FoodOrderStatController.java](C:\Users\22390\Desktop\EFoodPass\src\main\java\com\epass\food\modules\food\order\controller\FoodOrderStatController.java)
- [FoodOrderServiceImpl.java](C:\Users\22390\Desktop\EFoodPass\src\main\java\com\epass\food\modules\food\order\service\impl\FoodOrderServiceImpl.java)

它其实包含三块职责：

1. 后台订单管理
2. 当前登录用户的订单能力
3. 订单统计

后台订单管理接口：

- `/food/order/**`

用户端订单接口：

- `/app/order/**`

订单统计接口：

- `/food/order/stat/**`

当前订单状态流：

- `10` 待确认
- `20` 制作中
- `30` 已完成
- `40` 已取消

接手时要注意：

- 下单会校验用户、菜品、分类、库存
- 下单会扣减库存
- 取消订单会回补库存
- 用户端订单接口不接收 `userId`，而是从登录态获取
- 统计目前是第一版，逻辑在 Service 层做聚合，后续数据量上来可以再下沉到 SQL

### 10.4 `food/stock`

作用：

- 记录库存变动日志
- 提供库存日志查询

关键文件：

- [FoodStockLogController.java](C:\Users\22390\Desktop\EFoodPass\src\main\java\com\epass\food\modules\food\stock\controller\FoodStockLogController.java)
- [FoodStockLogServiceImpl.java](C:\Users\22390\Desktop\EFoodPass\src\main\java\com\epass\food\modules\food\stock\service\impl\FoodStockLogServiceImpl.java)

接手时要注意：

- 当前库存日志主要覆盖：
    - 下单扣减
    - 取消回补
    - 后台手工调整

---

## 11. `admin` 与 `test` 模块说明

### `admin`

这个目录目前不是主业务核心，更多像测试或占位用途。

### `test`

同样偏测试用途，不属于主业务模块。

接手时建议：

- 先把它们视为辅助或示例代码
- 真正理解系统时优先看 `auth`、`system`、`food`

---

## 12. 单个模块的常见分层结构

这个项目里的模块基本遵循类似结构：

- `controller`
- `dto`
- `entity`
- `mapper`
- `service`
- `service/impl`

各层职责：

### `controller`

作用：

- 接收 HTTP 请求
- 做参数绑定和参数校验入口
- 调用 Service
- 包装 `Result`

特点：

- 一般比较薄
- 不应承载重业务逻辑

### `dto`

作用：

- 定义接口入参与返回对象

通常会拆成：

- `*Query`
- `*CreateRequest`
- `*UpdateRequest`
- `*Response`

### `entity`

作用：

- 与数据库表一一映射

### `mapper`

作用：

- MyBatis-Plus 数据访问层

### `service`

作用：

- 定义模块提供的业务能力

### `service/impl`

作用：

- 实现核心业务逻辑
- 做业务校验
- 组织数据库访问

---

## 13. 配置文件说明

资源目录：

- [src/main/resources](C:\Users\22390\Desktop\EFoodPass\src\main\resources)

当前主要配置文件：

- [application.yaml](C:\Users\22390\Desktop\EFoodPass\src\main\resources\application.yaml)
- [application-dev.yml](C:\Users\22390\Desktop\EFoodPass\src\main\resources\application-dev.yml)
- [application-prod.yml](C:\Users\22390\Desktop\EFoodPass\src\main\resources\application-prod.yml)

一般理解方式：

- `application.yaml`：公共配置入口
- `application-dev.yml`：开发环境配置
- `application-prod.yml`：生产环境配置

接手时要注意：

- 数据库、JWT、端口、日志等核心配置通常都从这里看
- 如果要部署，优先确认当前激活的是哪个 profile

---

## 14. 数据库脚本说明

目录：

- [sql](C:\Users\22390\Desktop\EFoodPass\sql)

当前最重要的文件：

- [init-rbac.sql](C:\Users\22390\Desktop\EFoodPass\sql\init-rbac.sql)

这份脚本的作用不只是“建表”，还包括：

- 创建业务表
- 创建系统表
- 初始化 admin 账号
- 初始化 ADMIN 角色
- 初始化权限数据
- 绑定角色权限

接手时要注意：

- 这份脚本现在更适合作为“新库初始化脚本”
- 如果做增量升级，不建议简单反复当迁移脚本使用
- 新增权限或业务表时，要同步维护这里

---

## 15. 阅读源码的推荐顺序

如果你是第一次接手这个项目，建议按下面顺序看：

### 第一步：看系统怎么启动

- [EFoodPassApplication.java](C:\Users\22390\Desktop\EFoodPass\src\main\java\com\epass\food\EFoodPassApplication.java)
- 配置文件

### 第二步：看统一返回和异常

- `common/result`
- `common/exception`
- `common/page`

### 第三步：看认证与权限体系

- `config/security`
- `modules/auth`
- `modules/system`
- `sql/init-rbac.sql`

### 第四步：看业务主链路

按顺序看：

1. `food/category`
2. `food/item`
3. `food/order`
4. `food/stock`

这个顺序符合真实业务依赖关系：

- 分类依赖最少
- 菜品依赖分类
- 订单依赖菜品和用户
- 库存日志依赖订单和菜品

---

## 16. 项目中的重要约定

### 统一返回约定

所有接口基本都返回：

- `Result<T>`
- `Result<PageResult<T>>`

### 权限控制约定

接口鉴权主要通过：

- `@PreAuthorize("hasAuthority('xxx')")`

权限码来源于：

- `sql/init-rbac.sql`

### 业务异常约定

业务校验失败时，统一抛：

- `BusinessException`

错误码集中在：

- [BizErrorCode.java](C:\Users\22390\Desktop\EFoodPass\src\main\java\com\epass\food\common\result\BizErrorCode.java)

### 分页约定

列表查询基本都继承：

- `PageQuery`

分页返回统一用：

- `PageResult`

---

## 17. 维护时的注意事项

### 1. 改接口时，不要只改 Controller

一个接口的完整改动通常会涉及：

- Controller
- DTO
- Service
- ServiceImpl
- SQL 权限初始化
- 前端文档或调用方

### 2. 新增权限时，要同步 SQL

如果 Controller 上加了新的 `hasAuthority(...)`，但 `init-rbac.sql` 没补，管理员也会 403。

### 3. 新增业务模块时，优先复用现有风格

这个项目当前风格已经稳定：

- Controller 薄
- Service 承担业务逻辑
- DTO 明确拆分
- 权限码细分到接口动作

### 4. 订单与库存是强关联的

维护订单逻辑时要特别注意：

- 下单扣库存
- 取消回补库存
- 库存日志记录

这三件事应该保持一致。

### 5. 中文乱码历史问题

项目里存在历史注释乱码和编码污染痕迹。  
当前核心模块已经做过一轮收口，但接手时仍建议保持 UTF-8 编码并谨慎批量修改。

---

## 18. 当前阶段项目状态

从工程成熟度看，这个项目现在已经不是纯脚手架，而是一套“第一版可用后端”。

当前比较完整的部分：

- 认证
- 权限
- 系统管理
- 餐饮业务主流程

仍然适合继续演进的方向：

- 编译与联调验收
- 并发库存控制增强
- 更细的前端配套
- 日志与审计增强
- 数据统计增强

---

## 19. 对新接手者的最后建议

理解这个项目时，不要从单个 ServiceImpl 死抠实现细节开始。  
最有效的方式是：

1. 先理解目录结构和模块边界
2. 再理解认证与权限体系
3. 最后顺着“分类 -> 菜品 -> 订单 -> 库存日志”的链路看业务

这样你会更快明白这套系统，而不是陷在局部代码里。

### 20. 启动redis

cd "C:\Program Files\Redis-3.0.504"; .\redis-server.exe
