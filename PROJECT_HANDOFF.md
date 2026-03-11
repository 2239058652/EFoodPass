# EFoodPass Project Handoff

## 1. Project Overview

This project is a Spring Boot 3 + Spring Security + MyBatis-Plus + MySQL backend.

Current focus has been the backend management foundation first, not the food business modules yet.

The project already has a usable RBAC skeleton and a substantial part of the system-management APIs.

Core stack already in use:
- Spring Boot 3.5.x
- Spring Security
- MyBatis-Plus
- MySQL
- Redis dependency exists, but Redis is intentionally not deeply integrated yet
- JWT via `jjwt`
- Lombok

## 2. What Has Been Completed

### 2.1 Authentication and Security Foundation
Implemented and working:
- `Result<T>` unified response structure
- `BusinessException`
- `GlobalExceptionHandler`
- Spring Security basic configuration
- JWT login flow
- `/auth/login`
- `/auth/me`
- JWT filter parses token and restores login state
- `tokenVersion` validation is already wired into JWT validation
- `AuthenticationEntryPointImpl` for unauthenticated `401`
- `AccessDeniedHandlerImpl` plus `GlobalExceptionHandler` handling for forbidden `403`
- `PasswordEncoder` bean extracted into standalone `PasswordConfig`

Important behavior already confirmed:
- Multiple valid tokens per user are currently allowed by design
- `tokenVersion` is checked on each request
- Resetting password increments `tokenVersion` so old tokens become invalid
- Disabled users lose access because JWT filter checks `user.status == 1`

### 2.2 RBAC Core
Implemented and working:
- User -> Role relation
- Role -> Permission relation
- Role-based authorization via `hasRole(...)`
- Permission-based authorization via `hasAuthority(...)`
- `/auth/me` returns:
  - current user basic info
  - `roleCodes`
  - `permissionCodes`

Current authorization model:
- Roles are converted to `ROLE_xxx`
- Permissions are directly added as `SimpleGrantedAuthority(permissionCode)`
- JWT token itself only carries minimal identity info
- Roles and permissions are restored from DB on each request through the JWT filter

### 2.3 Initialization SQL / Deployment Bootstrapping
A full RBAC initialization design was completed in the conversation.

Initialization content includes:
- admin user
- ADMIN role
- base permissions
- user-role relation
- role-permission relation

Admin account agreed in design:
- username: `admin`
- password: `Admin@123`
- bcrypt hash used in SQL:
  - `$2a$10$ul9WaxC8WF.7P0vzj0og5uswfqT1foa7ZjPi1lh/F7LCaMTKszx92`

Important:
- `sql/init-rbac.sql` now exists on disk
- later chats should read and reuse this file instead of redesigning it from scratch
- only recreate it if it is missing or clearly corrupted

### 2.4 System Management APIs Already Built
#### User module
Implemented:
- user list
- user detail
- create user
- update user basic info
- update user status
- delete user
- assign roles to user
- reset user password

#### Role module
Implemented:
- role list
- role detail
- create role
- update role basic info
- update role status
- delete role
- assign permissions to role

#### Permission module
Implemented:
- permission list
- permission detail
- create permission
- update permission basic info
- update permission status
- delete permission

## 3. Current Code Structure

Main packages:
- `com.epass.food.common`
  - `exception`
  - `page`
  - `result`
- `com.epass.food.config`
  - `MybatisPlusConfig`
  - `PasswordConfig`
- `com.epass.food.config.security`
  - `SecurityConfig`
  - `JwtAuthenticationFilter`
  - `JwtTokenProvider`
  - `JwtProperties`
  - `AuthenticationEntryPointImpl`
  - `AccessDeniedHandlerImpl`
  - `LoginUser`
- `com.epass.food.modules.auth`
- `com.epass.food.modules.system.user`
- `com.epass.food.modules.system.role`
- `com.epass.food.modules.system.permission`

Notable files:
- `src/main/java/com/epass/food/config/security/SecurityConfig.java`
- `src/main/java/com/epass/food/config/security/JwtAuthenticationFilter.java`
- `src/main/java/com/epass/food/modules/auth/service/impl/AuthServiceImpl.java`
- `src/main/java/com/epass/food/modules/system/user/service/impl/SysUserServiceImpl.java`
- `src/main/java/com/epass/food/modules/system/role/service/impl/SysRoleServiceImpl.java`
- `src/main/java/com/epass/food/modules/system/permission/service/impl/SysPermissionServiceImpl.java`

## 4. Important Current Technical State

### 4.1 User list pagination is implemented
Observed in code:
- `common/page/PageQuery.java`
- `common/page/PageResult.java`
- `UserListQuery extends PageQuery`
- `SysUserService.listUsers(...)` now returns `PageResult<UserListResponse>`
- `SysUserServiceImpl.listUsers(...)` uses MyBatis-Plus `Page<SysUser>`
- `MybatisPlusConfig` already contains `PaginationInnerInterceptor`

So user pagination is no longer planned work; it is already implemented and being used as the reference pattern for other list modules.

### 4.2 Role and permission list pagination are now implemented
Current confirmed state in code:
- `RoleListQuery extends PageQuery`
- `PermissionListQuery extends PageQuery`
- `SysRoleService.listRoles(...)` returns `PageResult<RoleListResponse>`
- `SysPermissionService.listPermissions(...)` returns `PageResult<PermissionListResponse>`
- role / permission controllers now return paged list results instead of plain `List<...>`

So the original pagination follow-up has already been completed.

### 4.3 Management-module consistency polish has started and partly landed
Confirmed improvements already applied in code:
- `listUsers(...)`, `listRoles(...)`, `listPermissions(...)` now all guard `query == null` and fall back to default paging query objects
- user status validation was aligned across create / update / update-status flows
- role status validation was aligned across create / update / update-status flows
- permission status validation was aligned across create / update / update-status flows
- permission type validation was aligned across create / update flows
- duplicated validation logic in service layer has begun to be extracted into private helper methods
- helper methods now include patterns such as:
  - `validateUserStatus(...)`
  - `validateRoleStatus(...)`
  - `validatePermissionStatus(...)`
  - `validatePermissionType(...)`

This means the project has already entered Phase 2 "management polish", not just Phase 1 pagination.

### 4.4 Redis is intentionally deferred
Important project decision from the conversation:
- Redis should be integrated later
- Do not rush Redis into the current stage
- Current backend core should first be made structurally complete

Planned Redis use cases for later:
- captcha
- SMS/email verification code
- token blacklist
- refresh-token/session management
- permission cache
- rate limiting

## 5. Known Business Rules Already Agreed

### 5.1 Stable identifiers should not be casually editable
Do not casually allow editing of:
- `username`
- `roleCode`
- `permCode`

Current design intentionally treats them as stable business identifiers.

### 5.2 Core built-in entities are protected
Current agreed protections include:
- admin user cannot be deleted
- admin user cannot be disabled
- ADMIN role cannot be deleted
- ADMIN role cannot be disabled
- `admin:dashboard` permission cannot be deleted
- `admin:dashboard` permission cannot be disabled

### 5.3 Delete operations must clear relations first
Agreed deletion rule:
- delete user: clear `sys_user_role` first
- delete role: clear `sys_user_role` and `sys_role_permission` first
- delete permission: clear `sys_role_permission` first

### 5.4 Password changes must invalidate old tokens
Already reflected in service logic:
- reset password increments `tokenVersion`
- old JWT becomes invalid naturally

## 6. Important Design Decisions From the Conversation

### 6.1 Multi-token strategy is currently intentional
The project currently allows multiple valid JWTs per user.
This is not treated as a bug.

Decision made in conversation:
- keep multi-token valid for now
- do not switch to "new login invalidates old login" yet
- if needed later, Redis or login-session management can be introduced

### 6.2 Token should carry minimal identity, not full auth graph
Agreed design:
- token carries minimal identity info
- roles and permissions are read from DB during request authentication
- this keeps permission changes effective without needing token refresh for every auth change

### 6.3 Service layer should own business logic aggregation
Repeatedly reinforced during development:
- Controller should stay thin
- Service layer should aggregate entity, role, permission logic
- DTOs should not be replaced by direct entity returns

### 6.4 Detail permissions are intentionally still reused for now
Current explicit decision from the follow-up conversation:
- detail endpoints continue reusing list permissions for the current stage
- do not split `system:user:detail`, `system:role:detail`, `system:permission:detail` yet
- if frontend later needs finer-grained control, detail permissions can be introduced in a later refactor

## 7. Current Inconsistencies / Things To Clean Up Later

### 7.1 Some source comments are garbled
Some Java source comments show encoding-garbled Chinese text.
This seems to come from previous file encoding issues.
It does not necessarily break code, but should be cleaned later.

Recommended later task:
- normalize file encoding to UTF-8
- clean broken Chinese comments

### 7.2 Error codes are currently handwritten in many places
Current code uses many numeric business codes directly, for example:
- `4004`
- `4010`
- `4016`
- etc.

Recommended later refactor:
- centralize these in `ResultCode` or a dedicated business error enum set

### 7.3 Some list/detail permissions are reused
For simplicity, detail endpoints currently still reuse list permissions, for example:
- `GET /system/user/{id}` uses `system:user:list`
- `GET /system/role/{id}` uses `system:role:list`
- `GET /system/permission/{id}` uses `system:permission:list`

This is intentional at the current stage.
Do not change this casually in the next chat.
Only split into separate detail permissions later if the frontend actually needs finer-grained control.

### 7.4 Swagger / OpenAPI was added after the original handoff draft
Current confirmed state from the follow-up conversation:
- Swagger dependency has been added via `springdoc-openapi-starter-webmvc-ui`
- Spring Security has been configured to permit:
  - `/v3/api-docs/**`
  - `/swagger-ui/**`
  - `/swagger-ui.html`
- OpenAPI basic config has been added in `src/main/java/com/epass/food/config/OpenApiConfig.java`
- Swagger entry path is `/swagger-ui.html`
- `/v3/api-docs` and Swagger UI were both manually verified to open successfully

This means API documentation is now usable and should be used to help verify later modules.
Do not spend the next chat re-adding Swagger unless the code on disk has clearly lost those changes.

## 8. What Should Be Done Next

Recommended next priority order:

### Phase 1: Current foundation status
Already completed in code:
1. user list pagination
2. role list pagination
3. permission list pagination
4. Swagger / OpenAPI basic access
5. a first round of management-endpoint consistency fixes

### Phase 2: Continue remaining system-management polish
Recommended remaining polish items:
1. audit whether there are still any missing validation consistency gaps
2. optionally centralize handwritten business error codes into enum / constants
3. keep detail endpoints reusing list permissions for now
4. only introduce separate detail permissions later if frontend needs them

### Phase 3: Start first real business module
Recommended first business module:
- `food_category`

Reason:
- simple structure
- good CRUD training target
- foundation for `food_item`
- now easier to build and verify because Swagger is available

Recommended first tasks inside `food_category`:
1. confirm final table fields to use from existing SQL design
2. design CRUD endpoint list and DTOs
3. define status / name uniqueness / delete rules
4. then implement backend step by step

## 9. Suggested Immediate Next Task In New Chat

If opening a new conversation, ask the new assistant to do this first:

1. Read this handoff document
2. Confirm current Swagger is already working instead of re-adding it
3. Keep current rule that detail endpoints still reuse list permissions
4. Do not restart old pagination work; it is already complete
5. Continue with `food_category` module design first:
   - fields
   - DTOs
   - endpoint list
   - business rules
6. Then implement `food_category` step by step
7. Use Swagger to verify each new endpoint as it is added
8. Continue using teaching style step by step, not all-at-once takeover

That is now the most natural continuation point based on the latest code state.

## 10. API Capability Summary

### Auth
- `POST /auth/login`
- `GET /auth/me`

### User management
- `GET /system/user/list`
- `GET /system/user/{id}`
- `POST /system/user`
- `PUT /system/user`
- `PUT /system/user/status`
- `PUT /system/user/reset-password`
- `POST /system/user/assign-role`
- `DELETE /system/user/{id}`

### Role management
- `GET /system/role/list`
- `GET /system/role/{id}`
- `POST /system/role`
- `PUT /system/role`
- `PUT /system/role/status`
- `POST /system/role/assign-permission`
- `DELETE /system/role/{id}`

### Permission management
- `GET /system/permission/list`
- `GET /system/permission/{id}`
- `POST /system/permission`
- `PUT /system/permission`
- `PUT /system/permission/status`
- `DELETE /system/permission/{id}`

## 11. Notes For The Next Assistant

When continuing from this project, please keep these constraints in mind:
- do not re-architect auth unless necessary
- keep current multi-token strategy for now
- Redis should be introduced later, not immediately forced in
- prefer continuing the current DTO + Service + Controller style
- keep stable identifiers non-editable for now:
  - username
  - roleCode
  - permCode
- preserve current built-in protection rules around `admin`, `ADMIN`, and `admin:dashboard`
- detail endpoints still intentionally reuse list permissions for now; this is a deliberate temporary decision, not an immediate bug to "fix"
- Swagger is already working; do not spend the next chat re-adding it unless the code on disk no longer contains it
- the user prefers step-by-step teaching collaboration
- do not immediately take over and mass-edit files unless explicitly requested
- prefer checking current code state first, then guiding one small change at a time

## 12. Short Summary

Current state:
- RBAC foundation is already usable
- system management modules are mostly built
- user / role / permission list pagination are all now implemented
- a round of management consistency fixes has already been applied
- Swagger / OpenAPI has been added and manually verified to work
- detail endpoints still intentionally reuse list permissions for now
- the clearest next module is now `food_category`
- business modules have not really started yet beyond SQL design


