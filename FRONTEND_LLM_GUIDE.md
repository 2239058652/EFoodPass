# EFoodPass Frontend Generation Guide

## Purpose

This document is for another large model that needs to generate a frontend for this backend repository.

The goal is not to "design a generic admin system", but to generate a frontend that matches the actual backend that already exists in this repo.

Read this document first, then inspect the referenced backend files, then generate the frontend.

Do not invent routes, field names, permission codes, or response structures that are not present in the backend.

---

## Core Rule

When generating the frontend, always treat the backend source code in this repository as the single source of truth.

Priority order:

1. Controller request mappings and method signatures
2. DTO request/response classes
3. `Result<T>` and `PageResult<T>` response wrappers
4. Security and auth flow
5. SQL permission codes in `sql/init-rbac.sql`

If any previous prompt text conflicts with the backend code, follow the backend code.

---

## Repo Files You Must Inspect First

### Response shape

- `src/main/java/com/epass/food/common/result/Result.java`
- `src/main/java/com/epass/food/common/page/PageResult.java`

Backend responses are wrapped in:

- `Result<T>`
- `Result<PageResult<T>>`

Expected frontend handling:

- `code === 200` means success
- `message` is the backend message
- real payload is in `data`
- paginated records are in `data.records`

Do not assume backend returns raw arrays directly.

---

### Auth and current user

- `src/main/java/com/epass/food/modules/auth/controller/AuthController.java`
- `src/main/java/com/epass/food/modules/auth/dto/LoginRequest.java`
- `src/main/java/com/epass/food/modules/auth/dto/LoginResponse.java`
- `src/main/java/com/epass/food/modules/auth/dto/CurrentUserResponse.java`
- `src/main/java/com/epass/food/config/security/SecurityConfig.java`

You must implement:

- login page
- token storage
- current-user bootstrap
- permission-based route/menu filtering

Important backend facts:

- login endpoint: `POST /auth/login`
- current user endpoint: `GET /auth/me`
- all routes except login/swagger/test require authentication
- token is JWT Bearer token

Frontend auth behavior:

1. Login with username and password
2. Save token
3. Send `Authorization: Bearer <token>` on authenticated requests
4. Call `/auth/me` after login or page refresh
5. Use returned `roleCodes` and `permissionCodes` to build route guards and menus

---

### System management modules

- `src/main/java/com/epass/food/modules/system/user/controller/SysUserController.java`
- `src/main/java/com/epass/food/modules/system/role/controller/SysRoleController.java`
- `src/main/java/com/epass/food/modules/system/permission/controller/SysPermissionController.java`

Inspect matching DTO folders:

- `src/main/java/com/epass/food/modules/system/user/dto`
- `src/main/java/com/epass/food/modules/system/role/dto`
- `src/main/java/com/epass/food/modules/system/permission/dto`

These pages should form the admin system-management area.

---

### Food business modules

- `src/main/java/com/epass/food/modules/food/category/controller/FoodCategoryController.java`
- `src/main/java/com/epass/food/modules/food/item/controller/FoodItemController.java`
- `src/main/java/com/epass/food/modules/food/order/controller/FoodOrderController.java`
- `src/main/java/com/epass/food/modules/food/order/controller/FoodOrderStatController.java`
- `src/main/java/com/epass/food/modules/food/order/controller/AppOrderController.java`
- `src/main/java/com/epass/food/modules/food/stock/controller/FoodStockLogController.java`

Inspect matching DTO folders:

- `src/main/java/com/epass/food/modules/food/category/dto`
- `src/main/java/com/epass/food/modules/food/item/dto`
- `src/main/java/com/epass/food/modules/food/order/dto`
- `src/main/java/com/epass/food/modules/food/stock/dto`

These pages should form the business area of the frontend.

---

### Permission source of truth

- `sql/init-rbac.sql`

This file defines the actual permission codes that the frontend should respect.

Do not invent permission strings.

---

## Required Frontend Areas

The frontend should be generated as two areas:

1. Admin backend
2. User-side app order area

Do not merge them into one flat page set.

---

## Admin Backend Pages

### 1. Login

Page purpose:

- username/password login

Uses:

- `POST /auth/login`

After success:

- store token
- request `/auth/me`
- route to admin dashboard

---

### 2. Dashboard

This can be lightweight.

Recommended content:

- current user info
- quick links to category, item, order, statistics

Do not fabricate dashboard data APIs unless you explicitly reuse existing order statistics endpoints.

---

### 3. User Management

Use:

- `GET /system/user/list`
- `GET /system/user/{id}`
- `POST /system/user`
- `PUT /system/user`
- `PUT /system/user/status`
- `PUT /system/user/reset-password`
- `POST /system/user/assign-role`
- `DELETE /system/user/{id}`

Recommended page structure:

- list page with filters
- create/edit drawer or modal
- detail panel
- assign-role modal
- reset-password modal

Expected key fields:

- username
- nickname
- phone
- status
- roleIds or roleCodes

---

### 4. Role Management

Use:

- `GET /system/role/list`
- `GET /system/role/{id}`
- `POST /system/role`
- `PUT /system/role`
- `PUT /system/role/status`
- `POST /system/role/assign-permission`
- `DELETE /system/role/{id}`

Recommended page structure:

- role list
- create/edit modal
- permission assignment modal
- detail panel

---

### 5. Permission Management

Use:

- `GET /system/permission/list`
- `GET /system/permission/{id}`
- `POST /system/permission`
- `PUT /system/permission`
- `PUT /system/permission/status`
- `DELETE /system/permission/{id}`

Recommended page structure:

- permission list
- create/edit modal
- detail panel

Do not assume there is a separate tree endpoint.
If a tree view is needed, build it client-side from flat permission list using `parentId` if present in DTOs.

---

### 6. Category Management

Use:

- `GET /food/category/list`
- `GET /food/category/{id}`
- `POST /food/category`
- `PUT /food/category`
- `PUT /food/category/status`
- `DELETE /food/category/{id}`

Page requirements:

- category list page
- create/edit modal
- detail drawer or modal
- enable/disable action

Expected key columns:

- name
- sortNo
- status

---

### 7. Item Management

Use:

- `GET /food/item/list`
- `GET /food/item/{id}`
- `POST /food/item`
- `PUT /food/item`
- `PUT /food/item/on-sale`
- `PUT /food/item/stock`
- `DELETE /food/item/{id}`

Page requirements:

- item list page
- create/edit modal
- detail drawer
- on-sale toggle
- stock-adjust modal

Expected key columns:

- categoryName
- name
- price
- stock
- isOnSale

Important:

- this backend already distinguishes:
  - full update
  - on-sale update
  - stock adjustment

The frontend should expose these as different actions, not merge everything into one generic update flow.

---

### 8. Order Management

Use:

- `GET /food/order/list`
- `GET /food/order/{id}`
- `POST /food/order`
- `PUT /food/order/process`
- `PUT /food/order/cancel`
- `PUT /food/order/complete`

Page requirements:

- order list page
- order detail page or drawer
- create-order page or modal
- process/complete/cancel actions depending on status

Status semantics:

- `10` pending
- `20` processing
- `30` completed
- `40` canceled

Frontend must render status-aware actions:

- pending: process, cancel
- processing: complete, cancel
- completed: read-only
- canceled: read-only

Do not show invalid action buttons for every row.

---

### 9. Order Statistics

Use:

- `GET /food/order/stat/overview`
- `GET /food/order/stat/status-count`
- `GET /food/order/stat/top-item`
- `GET /food/order/stat/daily-amount`

Page requirements:

- KPI cards for overview
- status count chart
- top selling items table or bar chart
- daily amount line/bar chart

Do not invent date filter APIs unless they exist in backend.
Use the current endpoints as-is.

---

### 10. Stock Log

Use:

- `GET /food/stock-log/list`

Page requirements:

- stock log list page
- filters:
  - foodItemId
  - changeType

Expected display fields:

- foodItemName
- changeType
- changeAmount
- beforeStock
- afterStock
- bizId
- remark
- createdAt

`changeType` meaning:

- `1`: order deduct
- `2`: order restore
- `3`: manual adjust

---

## User-Side App Pages

These pages use `/app/order` endpoints and are for the currently logged-in user.

Do not ask the user to choose `userId`.
The backend derives current user from the token.

### 1. My Orders List

Use:

- `GET /app/order/list`

Recommended:

- status filter
- pagination
- order summary cards or list items

### 2. Order Detail

Use:

- `GET /app/order/{id}`

Must show:

- order number
- status
- total amount
- remark
- item snapshots

### 3. Create Order

Use:

- `POST /app/order`

Request body does not contain `userId`.

Recommended page behavior:

- choose items from available food items
- maintain cart-like state client-side
- submit `items: [{ foodItemId, quantity }]`

Important:

There is no dedicated public item-list endpoint yet in the current backend.
If generating a true user-side order page, either:

1. reuse an existing item list endpoint if the deployment permits it, or
2. clearly mark that a user-facing item browsing API is still needed

Do not silently invent a `/app/item/list` endpoint.

### 4. Cancel My Order

Use:

- `PUT /app/order/cancel/{id}`

Only show cancel action for orders that are still cancelable.

---

## Routing Guidance

Recommended frontend route groups:

- `/login`
- `/admin`
- `/admin/system/users`
- `/admin/system/roles`
- `/admin/system/permissions`
- `/admin/food/categories`
- `/admin/food/items`
- `/admin/food/orders`
- `/admin/food/order-stat`
- `/admin/food/stock-logs`
- `/app/orders`
- `/app/orders/:id`
- `/app/order/create`

These are frontend routes only.
They do not need to mirror backend paths 1:1, but they must map cleanly to backend modules.

---

## Permission Handling Rules

Use `/auth/me` response as the frontend permission source.

Key permission codes:

### Category

- `food:category:list`
- `food:category:detail`
- `food:category:add`
- `food:category:update`
- `food:category:update-status`
- `food:category:delete`

### Item

- `food:item:list`
- `food:item:detail`
- `food:item:add`
- `food:item:update`
- `food:item:update-on-sale`
- `food:item:update-stock`
- `food:item:delete`

### Order

- `food:order:list`
- `food:order:detail`
- `food:order:add`
- `food:order:process`
- `food:order:cancel`
- `food:order:complete`
- `food:order:stat`

### Stock Log

- `food:stock-log:list`

### System

- `system:user:list`
- `system:user:add`
- `system:user:update`
- `system:user:delete`
- `system:user:assign-role`
- `system:role:list`
- `system:role:add`
- `system:role:update`
- `system:role:delete`
- `system:role:assign-permission`
- `system:permission:list`
- `system:permission:add`
- `system:permission:update`
- `system:permission:delete`

Frontend behavior:

- hide menus if user lacks page-level permission
- hide action buttons if user lacks action permission
- still handle backend 403 gracefully

Do not rely on frontend-only security.

---

## Request/Response Conventions

### Success

Typical success response:

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```
```

### Paginated success

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 100,
    "pageNum": 1,
    "pageSize": 10,
    "records": []
  }
}
```
```

### Failure

Business exceptions return non-200 business codes with a message.

Frontend requirements:

- show backend `message` directly
- do not replace it with a generic "operation failed" unless needed as fallback

---

## Form and Table Design Rules

When generating frontend forms and tables:

1. Use backend DTO names and fields as source of truth
2. Do not add frontend-only required fields unless they exist in backend request DTO
3. For boolean/status-like fields, use backend numeric semantics exactly
4. For list pages, default to backend query DTO fields only

Examples:

- Category list filters should use `name`, `status`, pagination
- Item list filters should use `name`, `categoryId`, `isOnSale`, pagination
- Order list filters should use `orderNo`, `userId`, `orderStatus`, pagination
- Stock log filters should use `foodItemId`, `changeType`, pagination

---

## UI Expectations

Generate a practical management UI, not a demo landing page.

For admin pages:

- left sidebar navigation
- top header with current user info and logout
- tables for list pages
- modal or drawer forms for CRUD
- clear status badges
- action buttons per row

For app order pages:

- simpler layout
- mobile-friendly list/detail experience
- order status emphasized visually

Do not generate a flashy marketing site.
Generate a usable internal system frontend.

---

## What Not To Invent

Do not invent:

- extra backend endpoints
- extra permission codes
- websocket features
- payment flow
- file upload
- image management
- public item browsing APIs that do not exist
- separate dashboard metrics APIs that do not exist

If a user-facing feature depends on a missing backend API, state that clearly in the frontend output.

---

## Recommended Frontend Generation Output

When another model generates the frontend, the output should include:

1. route structure
2. auth bootstrap flow
3. api client layer
4. permission utility
5. admin layout
6. pages for all modules listed above
7. reusable table/form/status components where appropriate

If the model must choose a frontend stack, prefer a common admin stack such as:

- React + Vite + TypeScript + Ant Design

or

- Vue 3 + Vite + TypeScript + Element Plus

But the chosen stack must preserve the backend contract exactly.

---

## Final Instruction To The Frontend Model

Before writing any frontend code:

1. inspect the controller files listed in this document
2. inspect the DTO classes for each module
3. inspect `Result.java` and `PageResult.java`
4. inspect `SecurityConfig.java`
5. inspect `sql/init-rbac.sql` for permission codes

Then generate the frontend strictly around the backend that already exists here.

Do not "improve" the backend contract by guessing.
Do not generate APIs that are not present.
Do not merge admin and app flows into one ambiguous UI.
