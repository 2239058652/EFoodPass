# EFoodPass Project Handoff

## 1. Current Project Position

This project is no longer just an RBAC skeleton.

At the current stage, it already contains a usable first-version backend with:

- JWT authentication
- Spring Security based permission control
- system user / role / permission management
- food category management
- food item management
- backend order management
- current-user order APIs
- order statistics
- stock adjustment
- stock change logs

The project has moved from "system foundation only" into "business backend first version is structurally complete".

---

## 2. Core Stack

Confirmed stack in use:

- Spring Boot 3.5.x
- Spring Security
- Spring Validation
- MyBatis-Plus
- MySQL
- JWT via `jjwt`
- Lombok
- Swagger / SpringDoc OpenAPI
- Redis dependency exists but is not a core runtime dependency for the current business flow

Java version:

- Java 17

Main dependency definition:

- `pom.xml`

---

## 3. What Is Already Completed

### 3.1 Infrastructure and Shared Foundation

Already completed:

- unified `Result<T>` response wrapper
- unified `PageQuery` / `PageResult<T>`
- `BusinessException`
- `GlobalExceptionHandler`
- `BizErrorCode` constant class
- MyBatis-Plus pagination configuration
- Swagger / OpenAPI configuration

Important files:

- `src/main/java/com/epass/food/common/result/Result.java`
- `src/main/java/com/epass/food/common/page/PageQuery.java`
- `src/main/java/com/epass/food/common/page/PageResult.java`
- `src/main/java/com/epass/food/common/exception/BusinessException.java`
- `src/main/java/com/epass/food/common/exception/GlobalExceptionHandler.java`
- `src/main/java/com/epass/food/common/result/BizErrorCode.java`
- `src/main/java/com/epass/food/config/MybatisPlusConfig.java`
- `src/main/java/com/epass/food/config/OpenApiConfig.java`

### 3.2 Authentication and Security

Already completed:

- `/auth/login`
- `/auth/me`
- JWT generation and parsing
- token version validation
- current user restored on each request
- unauthenticated handling
- forbidden handling

Important behavior:

- multiple valid tokens per user are still allowed by design
- resetting password increments `tokenVersion`
- old tokens become invalid naturally after password reset
- disabled users lose access because current user status is checked during auth

Important files:

- `src/main/java/com/epass/food/config/security/SecurityConfig.java`
- `src/main/java/com/epass/food/config/security/JwtAuthenticationFilter.java`
- `src/main/java/com/epass/food/config/security/JwtTokenProvider.java`
- `src/main/java/com/epass/food/config/security/LoginUser.java`
- `src/main/java/com/epass/food/modules/auth/controller/AuthController.java`
- `src/main/java/com/epass/food/modules/auth/service/impl/AuthServiceImpl.java`

### 3.3 RBAC and System Management

Already completed:

- user management
- role management
- permission management
- user-role binding
- role-permission binding
- interface-level permission control with `@PreAuthorize`

Implemented system APIs:

#### Auth

- `POST /auth/login`
- `GET /auth/me`

#### User management

- `GET /system/user/list`
- `GET /system/user/{id}`
- `POST /system/user`
- `PUT /system/user`
- `PUT /system/user/status`
- `PUT /system/user/reset-password`
- `POST /system/user/assign-role`
- `DELETE /system/user/{id}`

#### Role management

- `GET /system/role/list`
- `GET /system/role/{id}`
- `POST /system/role`
- `PUT /system/role`
- `PUT /system/role/status`
- `POST /system/role/assign-permission`
- `DELETE /system/role/{id}`

#### Permission management

- `GET /system/permission/list`
- `GET /system/permission/{id}`
- `POST /system/permission`
- `PUT /system/permission`
- `PUT /system/permission/status`
- `DELETE /system/permission/{id}`

Important business protections already in code:

- admin user cannot be disabled
- admin user cannot be deleted
- ADMIN role cannot be disabled
- ADMIN role cannot be deleted
- core permission `admin:dashboard` cannot be disabled
- core permission `admin:dashboard` cannot be deleted

### 3.4 Food Business Modules

These modules are now already implemented, not just planned.

#### Food Category

Implemented:

- category list
- category detail
- create category
- update category
- update category status
- delete category

APIs:

- `GET /food/category/list`
- `GET /food/category/{id}`
- `POST /food/category`
- `PUT /food/category`
- `PUT /food/category/status`
- `DELETE /food/category/{id}`

Business rules already enforced:

- category name required
- category name unique
- status only `0/1`
- deleting a category requires no `food_item` reference

Important files:

- `src/main/java/com/epass/food/modules/food/category/controller/FoodCategoryController.java`
- `src/main/java/com/epass/food/modules/food/category/service/impl/FoodCategoryServiceImpl.java`

#### Food Item

Implemented:

- item list
- item detail
- create item
- update item
- update on-sale status
- manual stock adjustment
- delete item

APIs:

- `GET /food/item/list`
- `GET /food/item/{id}`
- `POST /food/item`
- `PUT /food/item`
- `PUT /food/item/on-sale`
- `PUT /food/item/stock`
- `DELETE /food/item/{id}`

Business rules already enforced:

- category must exist
- category must be enabled for item create/update
- item name unique inside same category
- `price >= 0`
- `stock >= 0`
- `isOnSale` only `0/1`
- enabling on-sale is blocked if category is disabled
- deleting item is blocked if referenced by order items

Important files:

- `src/main/java/com/epass/food/modules/food/item/controller/FoodItemController.java`
- `src/main/java/com/epass/food/modules/food/item/service/impl/FoodItemServiceImpl.java`

#### Food Order

Implemented:

- backend order list
- backend order detail
- backend create order
- backend process order
- backend cancel order
- backend complete order
- current-user order list
- current-user order detail
- current-user create order
- current-user cancel order
- order overview stats
- order status count stats
- top item stats
- daily amount stats

Backend order APIs:

- `GET /food/order/list`
- `GET /food/order/{id}`
- `POST /food/order`
- `PUT /food/order/process`
- `PUT /food/order/cancel`
- `PUT /food/order/complete`

Current-user order APIs:

- `GET /app/order/list`
- `GET /app/order/{id}`
- `POST /app/order`
- `PUT /app/order/cancel/{id}`

Order stat APIs:

- `GET /food/order/stat/overview`
- `GET /food/order/stat/status-count`
- `GET /food/order/stat/top-item`
- `GET /food/order/stat/daily-amount`

Current order status flow:

- `10` pending
- `20` processing
- `30` completed
- `40` canceled

Important business behavior already in code:

- order create checks user existence and user enabled status
- order create checks item existence
- order create checks item on-sale status
- order create checks category enabled status
- order create checks stock
- repeated same `foodItemId` in one order request is aggregated before stock validation
- order create deducts stock
- cancel order restores stock
- stock change logs are written during deduct/restore
- current-user order APIs check ownership

Important files:

- `src/main/java/com/epass/food/modules/food/order/controller/FoodOrderController.java`
- `src/main/java/com/epass/food/modules/food/order/controller/AppOrderController.java`
- `src/main/java/com/epass/food/modules/food/order/controller/FoodOrderStatController.java`
- `src/main/java/com/epass/food/modules/food/order/service/impl/FoodOrderServiceImpl.java`

#### Food Stock Log

Implemented:

- stock log table
- stock log entity / mapper / service
- stock log list API
- stock log writes for:
  - order deduct
  - order restore
  - manual stock adjust

APIs:

- `GET /food/stock-log/list`

Important files:

- `src/main/java/com/epass/food/modules/food/stock/controller/FoodStockLogController.java`
- `src/main/java/com/epass/food/modules/food/stock/service/impl/FoodStockLogServiceImpl.java`

---

## 4. Current SQL / Deployment State

### 4.1 Initialization SQL

The main initialization script is:

- `sql/init-rbac.sql`

This file has already been reworked to match the current backend state.

Current confirmed contents:

- database creation
- system tables
- food business tables
- stock log table
- admin user
- ADMIN role
- system permissions
- food category permissions
- food item permissions
- food order permissions
- food stock log permissions

Important fixes already applied:

- `food:item:update-stock` permission exists
- `food:order:stat` permission is inserted before admin binding
- `food:stock-log` and `food:stock-log:list` permissions exist
- `token_version` uses `ADD COLUMN IF NOT EXISTS`
- tables use `CREATE TABLE IF NOT EXISTS`

Current practical conclusion:

- this SQL is suitable as a new-database initialization script
- it is not meant to be treated as a sophisticated migration history system

### 4.2 Admin initialization

Admin credentials agreed in project context:

- username: `admin`
- password: `Admin@123`

The bcrypt hash is already placed in SQL.

---

## 5. Important File and Directory Meaning

### Root level

- `pom.xml`
  - Maven dependency and plugin definition
- `sql/`
  - database initialization
- `src/main/resources/`
  - Spring Boot config files
- `README.md`
  - project-level structural explanation
- `FRONTEND_LLM_GUIDE.md`
  - long-form guide for another model to generate frontend correctly
- `FRONTEND_LLM_PROMPT.md`
  - short prompt for another model to generate frontend correctly

### Java source root

- `src/main/java/com/epass/food/common`
  - global shared utilities and contracts
- `src/main/java/com/epass/food/config`
  - framework configuration
- `src/main/java/com/epass/food/modules`
  - business and system modules

### Modules

- `modules/auth`
  - login and current-user APIs
- `modules/system`
  - user / role / permission management
- `modules/food`
  - category / item / order / stock-log business
- `modules/admin`
  - not a core business area right now
- `modules/test`
  - test/demo style code, not core business

---

## 6. Current Code Style and Architectural Rules

These are the patterns already reinforced in the project and should continue unless there is a strong reason to change them.

### 6.1 Thin Controller, Business in Service

Controllers should:

- receive request
- do parameter binding
- call service
- return `Result`

Business logic should live in service implementation classes.

### 6.2 DTO-based API boundary

Do not return entities directly as API output unless there is a very good reason.

Use:

- `*Query`
- `*CreateRequest`
- `*UpdateRequest`
- `*Response`

### 6.3 Stable identifiers should remain stable

Do not casually make these editable:

- username
- roleCode
- permCode

They are treated as stable business identifiers.

### 6.4 Permission source of truth

Permission strings should be aligned between:

- `@PreAuthorize(...)` in controller
- `sql/init-rbac.sql`

If one side changes and the other does not, admin login may still get `403`.

### 6.5 Unified error handling

Project now uses:

- `BusinessException`
- `BizErrorCode`

Formal business modules have already mostly moved away from raw numeric codes.

---

## 7. Important Current Constraints / Known Limits

### 7.1 Redis is still deferred

Redis dependency exists, but Redis is intentionally not deeply integrated into the current main flow.

Potential later use cases:

- captcha
- SMS/email code
- token blacklist
- refresh token/session control
- permission cache
- rate limit

Do not force Redis into the current phase unless a new requirement really needs it.

### 7.2 Multi-token strategy is still intentional

Current rule:

- a user can have multiple valid JWT tokens

This is not currently treated as a bug.

### 7.3 Inventory concurrency is not fully hardened yet

Current stock logic covers:

- create order stock validation
- stock deduct
- cancel restore
- duplicate item aggregation inside one request

But this is still a first-version implementation, not a high-concurrency inventory locking design.

Future strengthening area:

- optimistic locking
- conditional stock update
- stronger concurrent oversell prevention

### 7.4 Some legacy comments / display still have encoding history

Although several core files were rewritten and cleaned, there may still be legacy comment encoding issues in non-core files or in terminal display.

Do not assume every garbled terminal line means the source file is broken.

---

## 8. Frontend Guidance Already Added

This round also added explicit docs for another model to generate frontend correctly:

- `FRONTEND_LLM_GUIDE.md`
- `FRONTEND_LLM_PROMPT.md`

These files should be reused instead of re-explaining the backend every time frontend generation is requested.

Use them whenever:

- another model is asked to generate admin frontend
- another model is asked to generate app-side order pages

---

## 9. Recommended Next Work

At this point, the project should not immediately expand endlessly into new modules.

The most natural next work is:

### Priority 1: Real compilation and run verification

This is the highest-value next step.

Recommended:

1. create a fresh database
2. run `sql/init-rbac.sql`
3. start the backend
4. verify login
5. verify Swagger
6. verify core module APIs

Reason:

- many structural changes have already landed
- the next highest-value work is no longer design, but reality-check

### Priority 2: Integration / manual API verification

Recommended verification scope:

- auth
- system management
- food category
- food item
- food order
- app order
- order statistics
- stock log

### Priority 3: Inventory concurrency strengthening

If the project is going beyond first-version demo / management usage, stock deduction should be hardened.

### Priority 4: Optional cleanup

Possible cleanup tasks:

- clean remaining legacy comments
- clean or remove demo/test code
- expand `BizErrorCode` usage into any leftover test/demo places

---

## 10. Recommended Reading Order For A New Assistant

If a future assistant continues this project, the best reading order is:

1. this handoff file
2. `README.md`
3. `sql/init-rbac.sql`
4. `common/result`, `common/page`, `common/exception`
5. `config/security`
6. `modules/auth`
7. `modules/system`
8. `modules/food/category`
9. `modules/food/item`
10. `modules/food/order`
11. `modules/food/stock`

This reading order matches the actual dependency direction of the project.

---

## 11. Notes For The Next Assistant

When continuing from this project, keep the following in mind:

- do not go back and redesign pagination; it is already complete
- do not re-add Swagger; it already exists
- do not redesign RBAC from scratch
- do not remove current multi-token behavior unless explicitly requested
- do not casually change stable identifiers:
  - username
  - roleCode
  - permCode
- keep SQL permission codes aligned with controller permissions
- treat `sql/init-rbac.sql` as the current initialization source of truth
- use `FRONTEND_LLM_GUIDE.md` and `FRONTEND_LLM_PROMPT.md` for frontend-generation tasks
- the next valuable phase is verification and hardening, not blind module expansion

If a next conversation starts with "continue", the best default continuation is:

1. verify compile / run / SQL init
2. verify key APIs
3. then fix any real runtime issues found

---

## 12. Short Summary

Current state:

- backend foundation is complete
- system management is complete enough for first-version use
- food business modules are already implemented
- order and stock log chain is already connected
- initialization SQL has been updated to match the current backend
- frontend guidance docs have already been added

The project has now reached a good handoff point.

The next phase should be:

- real validation
- runtime correction
- then selective hardening
