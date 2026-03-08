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
- The conversation designed `sql/init-rbac.sql`
- Current repository scan only clearly shows `sql/e-food.sql`
- In the next chat, first verify whether `sql/init-rbac.sql` already exists on disk
- If not, recreate it from the designed content before moving on

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

### 4.1 User list pagination has started and appears to be implemented
Observed in code:
- `common/page/PageQuery.java`
- `common/page/PageResult.java`
- `UserListQuery extends PageQuery`
- `SysUserService.listUsers(...)` now returns `PageResult<UserListResponse>`
- `SysUserServiceImpl.listUsers(...)` uses MyBatis-Plus `Page<SysUser>`
- `MybatisPlusConfig` already contains `PaginationInnerInterceptor`

So user pagination is no longer "planned"; it is already present in code.

### 4.2 Role and permission list pagination are still not generalized
Current role and permission list services still return plain `List<...>`.
Natural next refactor is:
- copy the same pagination pattern from user list
- apply it to role list
- apply it to permission list

### 4.3 Redis is intentionally deferred
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
For simplicity, some detail endpoints currently reuse list permissions, for example:
- user detail may reuse `system:user:list`
- role detail may reuse `system:role:list`
- permission detail may reuse `system:permission:list`

This is acceptable for now, but later you may want finer permissions such as:
- `system:user:detail`
- `system:role:detail`
- `system:permission:detail`

## 8. What Should Be Done Next

Recommended next priority order:

### Phase 1: Finish pagination consistency
1. paginate role list
2. paginate permission list
3. optionally standardize pagination DTO usage across list modules

### Phase 2: Finish remaining system-management polish
1. audit all existing management endpoints
2. add any missing validation consistency
3. optionally add detail permissions instead of reusing list permissions
4. optionally centralize business error codes

### Phase 3: Consider missing management features
Possible next endpoints/refactors:
- user/role/permission list paging cleanup
- optional "role tree" / permission tree output if frontend needs it
- optional "list all roles" lightweight API for user edit pages
- optional "list all permissions" lightweight API for role edit pages

### Phase 4: Start first real business module
Recommended first business module:
- `food_category`

Reason:
- simple structure
- good CRUD training target
- foundation for `food_item`

## 9. Suggested Immediate Next Task In New Chat

If opening a new conversation, ask the new assistant to do this first:

1. Read this handoff document
2. Verify whether `sql/init-rbac.sql` exists on disk
3. Verify current user-list pagination endpoint actually runs correctly
4. Continue with role-list pagination and permission-list pagination

That is the most natural continuation point based on current code state.

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
- continue using detailed teaching style if the user asks step-by-step guidance

## 12. Short Summary

Current state:
- RBAC foundation is already usable
- system management modules are mostly built
- user list pagination has already started/landed in code
- role/permission list pagination is the clearest next technical step
- business modules have not started yet and should wait until system-management groundwork is stable

先阅读 PROJECT_HANDOFF.md，然后继续这个项目。保持教学式、一步一步带我做，不要直接全包代做。先检查当前代码状态，再从文档里记录的“下一步推荐顺序”继续。