# 03 system 模块怎么读：用户、角色、权限三件事

这一章讲 system 模块。

重点文件：

- [SysUserController.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/modules/system/user/controller/SysUserController.java)
- [SysUserServiceImpl.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/modules/system/user/service/impl/SysUserServiceImpl.java)
- [SysUser.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/modules/system/user/entity/SysUser.java)
- [SysRoleServiceImpl.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/modules/system/role/service/impl/SysRoleServiceImpl.java)
- [SysPermissionServiceImpl.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/modules/system/permission/service/impl/SysPermissionServiceImpl.java)

## 一 先不要把 RBAC 想复杂

这个模块本质上只是在做 3 件事：

1. 管用户
2. 管角色
3. 管权限

以及两种关系：

4. 用户和角色的关系
5. 角色和权限的关系

## 二 `SysUser` 这个实体每个字段是什么意思

看 [SysUser.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/modules/system/user/entity/SysUser.java)。

### 类上的注解

- `@TableName("sys_user")`：映射数据库表 `sys_user`
- `@Data`：Lombok 自动生成 getter/setter 等方法

### 字段逐个看

#### `id`

主键。

上面有：

```java
@TableId(type = IdType.AUTO)
```

表示主键自增。

#### `username`

登录用户名。

#### `passwordHash`

数据库存的是加密后的密码，不是原始密码。

#### `nickname`

显示昵称。

#### `phone`

手机号。

#### `status`

用户状态。

在这个项目里常见语义是：

- `1` 启用
- `0` 禁用

#### `lastLoginAt`

最后登录时间。

#### `createdAt` / `updatedAt`

创建时间和更新时间。

#### `tokenVersion`

用于控制旧 token 失效。

## 三 `SysUserController` 是怎么当入口的

看 [SysUserController.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/modules/system/user/controller/SysUserController.java)。

你会发现每个方法基本都长这样：

1. `@PreAuthorize(...)`
2. `@GetMapping / @PostMapping / @PutMapping / @DeleteMapping`
3. 调 service
4. `return Result.success(...)`

### controller 的职责只有 4 个

1. 接请求
2. 接参数
3. 调 service
4. 返回统一结果

复杂业务逻辑一般不写在 controller。

## 四 `UserCreateRequest` 为什么不直接复用实体

看 [UserCreateRequest.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/modules/system/user/dto/UserCreateRequest.java)。

它有：

- `username`
- `password`
- `nickname`
- `phone`
- `status`

### 为什么不能直接让前端传 `SysUser`

因为：

- 请求 DTO 是前端输入结构
- 实体类是数据库结构

比如请求里需要的是 `password`，但数据库真正存的是 `passwordHash`。

## 五 `SysUserServiceImpl` 最值得你看哪些方法

看 [SysUserServiceImpl.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/modules/system/user/service/impl/SysUserServiceImpl.java)。

重点看：

- `getByUsername`
- `listUsers`
- `createUser`
- `assignRoles`
- `updateUserStatus`
- `resetPassword`

### 1. `getByUsername`

登录时要用。

它用 `LambdaQueryWrapper` 按用户名查一条记录。

### 2. `listUsers`

这是标准分页查询流程：

1. query 为空就给默认对象
2. 组查询条件
3. 创建 `Page`
4. 执行分页查询
5. 查询每个用户的角色
6. 组装 `PageResult`

### 3. `createUser`

顺序很标准：

1. 用户名不能重复
2. 用户状态要合法
3. 原始密码转成 hash
4. 初始化 `tokenVersion`
5. 保存用户

### 4. `assignRoles`

这里体现了典型的覆盖式分配：

1. 先确认用户存在
2. 再确认角色都存在且启用
3. 删除旧的用户-角色关系
4. 插入新的关系

### 5. `updateUserStatus`

这里体现业务保护规则：

- 用户不存在不行
- 状态值非法不行
- admin 账号不能被禁用

### 6. `resetPassword`

这里最重要的不是改 hash，而是：

```java
user.setTokenVersion(oldVersion + 1);
```

这个动作会让旧 token 失效。

## 六 `SysRoleServiceImpl` 在做什么

看 [SysRoleServiceImpl.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/modules/system/role/service/impl/SysRoleServiceImpl.java)。

它主要做两件事：

1. 管角色本身
2. 管角色和权限的关系

### `getRolesByUserId`

流程是：

1. 查用户角色关系表
2. 拿到 roleId 列表
3. 再查角色表
4. 只返回启用角色

### `assignPermissions`

流程和用户分配角色很像：

1. 先确认角色存在
2. 再确认权限都存在且启用
3. 删除旧关系
4. 插入新关系

## 七 `SysPermissionServiceImpl` 在做什么

看 [SysPermissionServiceImpl.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/modules/system/permission/service/impl/SysPermissionServiceImpl.java)。

最关键的方法是：

- `getPermissionCodesByUserId`

### 它的流程是什么

1. 根据用户查角色
2. 根据角色查角色-权限关系
3. 根据权限 id 查权限表
4. 只拿启用的权限
5. 提取权限编码

最终得到的就是：

- `system:user:list`
- `food:item:add`
- `food:order:detail`

这种权限字符串。

## 八 你现在应该形成的整体理解

system 模块不是三个孤立模块，而是一条链：

1. 用户登录
2. 查用户角色
3. 查角色权限
4. 权限编码进入 SecurityContext
5. `@PreAuthorize` 判断接口能不能访问

所以 system 模块其实是在给整个项目提供权限数据基础。

## 九 这一章最建议你自己画一张图

请你自己画这 5 张表的关系：

- `sys_user`
- `sys_role`
- `sys_permission`
- `sys_user_role`
- `sys_role_permission`

然后自己解释：

- 用户怎么拿角色
- 角色怎么拿权限
- 权限怎么影响接口访问
