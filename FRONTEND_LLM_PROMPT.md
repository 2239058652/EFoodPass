# Frontend LLM Prompt

你现在要为这个仓库生成前端，但必须严格以仓库内现有后端代码为准，不能凭经验自由发挥。

先阅读以下文件，再开始生成前端：

1. `src/main/java/com/epass/food/common/result/Result.java`
2. `src/main/java/com/epass/food/common/page/PageResult.java`
3. `src/main/java/com/epass/food/modules/auth/controller/AuthController.java`
4. `src/main/java/com/epass/food/config/security/SecurityConfig.java`
5. 所有业务 Controller：
   - `src/main/java/com/epass/food/modules/system/user/controller/SysUserController.java`
   - `src/main/java/com/epass/food/modules/system/role/controller/SysRoleController.java`
   - `src/main/java/com/epass/food/modules/system/permission/controller/SysPermissionController.java`
   - `src/main/java/com/epass/food/modules/food/category/controller/FoodCategoryController.java`
   - `src/main/java/com/epass/food/modules/food/item/controller/FoodItemController.java`
   - `src/main/java/com/epass/food/modules/food/order/controller/FoodOrderController.java`
   - `src/main/java/com/epass/food/modules/food/order/controller/FoodOrderStatController.java`
   - `src/main/java/com/epass/food/modules/food/order/controller/AppOrderController.java`
   - `src/main/java/com/epass/food/modules/food/stock/controller/FoodStockLogController.java`
6. 各模块对应 DTO 目录
7. `sql/init-rbac.sql`
8. `FRONTEND_LLM_GUIDE.md`

必须遵守以下规则：

1. 后端源码是唯一事实来源。
2. 不要编造不存在的接口、权限码、字段名、状态值、返回结构。
3. 所有接口响应都按 `Result<T>` 或 `Result<PageResult<T>>` 处理。
4. 登录流程必须基于：
   - `POST /auth/login`
   - `GET /auth/me`
   - `Authorization: Bearer <token>`
5. 前端必须区分两套区域：
   - 管理端
   - 用户端订单页
6. 管理端至少生成这些页面：
   - 登录页
   - 仪表盘
   - 用户管理
   - 角色管理
   - 权限管理
   - 分类管理
   - 菜品管理
   - 订单管理
   - 订单统计
   - 库存日志
7. 用户端至少生成这些页面：
   - 我的订单列表
   - 订单详情
   - 创建订单
8. 菜品管理页面必须把以下动作分开：
   - 新增/编辑
   - 上下架
   - 调整库存
9. 订单管理页面必须按真实状态流渲染操作：
   - `10` 待确认
   - `20` 制作中
   - `30` 已完成
   - `40` 已取消
10. 权限控制必须基于 `/auth/me` 返回的 `permissionCodes`，并严格对应 `sql/init-rbac.sql`。
11. 不要生成不存在的用户端菜品浏览接口；如果页面需要而后端没有，请明确说明依赖缺失。
12. 不要输出营销官网风格页面，要输出可用的后台管理系统和用户订单页面。

输出要求：

1. 先给出前端技术栈选择
2. 再给出路由结构
3. 再给出 API 封装层设计
4. 再给出权限控制设计
5. 最后按页面逐个生成

如果发现某个前端功能缺少后端支持，不要自行脑补接口，直接标注“后端暂未提供该接口”。
