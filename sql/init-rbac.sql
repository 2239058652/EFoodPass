CREATE DATABASE IF NOT EXISTS e_food
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE e_food;

CREATE TABLE sys_user
(
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    username      VARCHAR(50)     NOT NULL COMMENT '登录账号',
    password_hash VARCHAR(255)    NOT NULL COMMENT '密码哈希',
    nickname      VARCHAR(50)     NOT NULL COMMENT '昵称',
    phone         VARCHAR(20)              DEFAULT NULL COMMENT '手机号',
    status        TINYINT         NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
    last_login_at DATETIME                 DEFAULT NULL COMMENT '最后登录时间',
    created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_user_username (username),
    UNIQUE KEY uk_sys_user_phone (phone)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户表';

CREATE TABLE sys_role
(
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '角色ID',
    role_code  VARCHAR(50)     NOT NULL COMMENT '角色编码',
    role_name  VARCHAR(50)     NOT NULL COMMENT '角色名称',
    status     TINYINT         NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
    created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_role_code (role_code)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='角色表';

CREATE TABLE sys_permission
(
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '权限ID',
    parent_id  BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '父级ID，0为根节点',
    perm_code  VARCHAR(100)    NOT NULL COMMENT '权限编码',
    perm_name  VARCHAR(100)    NOT NULL COMMENT '权限名称',
    perm_type  TINYINT         NOT NULL COMMENT '1目录 2菜单 3接口',
    path       VARCHAR(255)             DEFAULT NULL COMMENT '路由或接口路径',
    method     VARCHAR(10)              DEFAULT NULL COMMENT '请求方法',
    sort_no    INT             NOT NULL DEFAULT 0 COMMENT '排序号',
    status     TINYINT         NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
    created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_permission_code (perm_code),
    KEY idx_sys_permission_parent_id (parent_id),
    KEY idx_sys_permission_type (perm_type)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='权限表';

CREATE TABLE sys_user_role
(
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id    BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    role_id    BIGINT UNSIGNED NOT NULL COMMENT '角色ID',
    created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_user_role_user_role (user_id, role_id),
    KEY idx_sys_user_role_role_id (role_id),
    CONSTRAINT fk_sys_user_role_user_id FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_sys_user_role_role_id FOREIGN KEY (role_id) REFERENCES sys_role (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户角色关联表';

CREATE TABLE sys_role_permission
(
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    role_id       BIGINT UNSIGNED NOT NULL COMMENT '角色ID',
    permission_id BIGINT UNSIGNED NOT NULL COMMENT '权限ID',
    created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_role_permission_role_perm (role_id, permission_id),
    KEY idx_sys_role_permission_perm_id (permission_id),
    CONSTRAINT fk_sys_role_permission_role_id FOREIGN KEY (role_id) REFERENCES sys_role (id),
    CONSTRAINT fk_sys_role_permission_permission_id FOREIGN KEY (permission_id) REFERENCES sys_permission (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='角色权限关联表';

CREATE TABLE food_category
(
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    name       VARCHAR(50)     NOT NULL COMMENT '分类名称',
    sort_no    INT             NOT NULL DEFAULT 0 COMMENT '排序号',
    status     TINYINT         NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
    created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_food_category_name (name)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='菜品分类表';

CREATE TABLE food_item
(
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '菜品ID',
    category_id BIGINT UNSIGNED NOT NULL COMMENT '分类ID',
    name        VARCHAR(100)    NOT NULL COMMENT '菜品名称',
    price       DECIMAL(10, 2)  NOT NULL COMMENT '当前价格',
    stock       INT             NOT NULL DEFAULT 0 COMMENT '库存',
    is_on_sale  TINYINT         NOT NULL DEFAULT 1 COMMENT '1上架 0下架',
    description VARCHAR(500)             DEFAULT NULL COMMENT '描述',
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_food_item_category_id (category_id),
    KEY idx_food_item_on_sale (is_on_sale),
    CONSTRAINT fk_food_item_category_id FOREIGN KEY (category_id) REFERENCES food_category (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='菜品表';

CREATE TABLE food_order
(
    id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '订单ID',
    order_no     VARCHAR(32)     NOT NULL COMMENT '订单编号',
    user_id      BIGINT UNSIGNED NOT NULL COMMENT '下单用户ID',
    total_amount DECIMAL(10, 2)  NOT NULL COMMENT '订单总金额',
    order_status TINYINT         NOT NULL DEFAULT 10 COMMENT '10待下单确认 20制作中 30已完成 40已取消',
    remark       VARCHAR(300)             DEFAULT NULL COMMENT '备注',
    created_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_food_order_order_no (order_no),
    KEY idx_food_order_user_id (user_id),
    KEY idx_food_order_status (order_status),
    KEY idx_food_order_created_at (created_at),
    CONSTRAINT fk_food_order_user_id FOREIGN KEY (user_id) REFERENCES sys_user (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='订单表';

CREATE TABLE food_order_item
(
    id                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '明细ID',
    order_id           BIGINT UNSIGNED NOT NULL COMMENT '订单ID',
    food_item_id       BIGINT UNSIGNED NOT NULL COMMENT '菜品ID',
    food_name_snapshot VARCHAR(100)    NOT NULL COMMENT '下单时菜名快照',
    price_snapshot     DECIMAL(10, 2)  NOT NULL COMMENT '下单时价格快照',
    quantity           INT             NOT NULL COMMENT '数量',
    amount             DECIMAL(10, 2)  NOT NULL COMMENT '小计金额',
    created_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_food_order_item_order_id (order_id),
    KEY idx_food_order_item_food_item_id (food_item_id),
    CONSTRAINT fk_food_order_item_order_id FOREIGN KEY (order_id) REFERENCES food_order (id),
    CONSTRAINT fk_food_order_item_food_item_id FOREIGN KEY (food_item_id) REFERENCES food_item (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='订单明细表';

CREATE TABLE sys_operation_log
(
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    request_id    VARCHAR(64)              DEFAULT NULL COMMENT '请求链路ID',
    user_id       BIGINT UNSIGNED          DEFAULT NULL COMMENT '操作用户ID',
    username      VARCHAR(50)              DEFAULT NULL COMMENT '操作账号',
    module        VARCHAR(50)     NOT NULL COMMENT '模块',
    action        VARCHAR(50)     NOT NULL COMMENT '动作',
    method        VARCHAR(10)              DEFAULT NULL COMMENT '请求方法',
    path          VARCHAR(255)             DEFAULT NULL COMMENT '请求路径',
    request_ip    VARCHAR(64)              DEFAULT NULL COMMENT '请求IP',
    success       TINYINT         NOT NULL DEFAULT 1 COMMENT '1成功 0失败',
    error_message VARCHAR(500)             DEFAULT NULL COMMENT '错误信息',
    cost_ms       INT                      DEFAULT NULL COMMENT '耗时毫秒',
    operate_time  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (id),
    KEY idx_sys_operation_log_user_id (user_id),
    KEY idx_sys_operation_log_module (module),
    KEY idx_sys_operation_log_operate_time (operate_time)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='操作日志表';

ALTER TABLE sys_user
    ADD COLUMN token_version INT NOT NULL DEFAULT 0 COMMENT '令牌版本号';


# 初始化admin账号
INSERT INTO sys_user (username,
                      password_hash,
                      nickname,
                      phone,
                      status,
                      token_version)
VALUES ('admin',
        '$2a$10$ul9WaxC8WF.7P0vzj0og5uswfqT1foa7ZjPi1lh/F7LCaMTKszx92',
        '系统管理员',
        '13800000000',
        1,
        0)
ON DUPLICATE KEY UPDATE password_hash = VALUES(password_hash),
                        nickname      = VALUES(nickname),
                        phone         = VALUES(phone),
                        status        = VALUES(status);

# 在 sys_role 表里初始化一个管理员角色
INSERT INTO sys_role (role_code,
                      role_name,
                      status)
VALUES ('ADMIN',
        '系统管理员',
        1)
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name),
                        status    = VALUES(status);

# 管理首页权限
INSERT INTO sys_permission (parent_id,
                            perm_code,
                            perm_name,
                            perm_type,
                            path,
                            method,
                            sort_no,
                            status)
VALUES (0,
        'admin:dashboard',
        '管理首页查看',
        3,
        '/admin/dashboard',
        'GET',
        1,
        1)
ON DUPLICATE KEY UPDATE perm_name = VALUES(perm_name),
                        path      = VALUES(path),
                        method    = VALUES(method),
                        sort_no   = VALUES(sort_no),
                        status    = VALUES(status);

# 用户管理权限
INSERT INTO sys_permission (parent_id,
                            perm_code,
                            perm_name,
                            perm_type,
                            path,
                            method,
                            sort_no,
                            status)
VALUES (0, 'system:user:list', '用户列表查询', 3, '/system/user/list', 'GET', 10, 1),
       (0, 'system:user:add', '新增用户', 3, '/system/user', 'POST', 11, 1),
       (0, 'system:user:update', '修改用户', 3, '/system/user', 'PUT', 12, 1),
       (0, 'system:user:delete', '删除用户', 3, '/system/user/{id}', 'DELETE', 13, 1),
       (0, 'system:user:assign-role', '用户分配角色', 3, '/system/user/assign-role', 'POST', 14, 1)
ON DUPLICATE KEY UPDATE perm_name = VALUES(perm_name),
                        path      = VALUES(path),
                        method    = VALUES(method),
                        sort_no   = VALUES(sort_no),
                        status    = VALUES(status);

# 角色管理权限
INSERT INTO sys_permission (parent_id,
                            perm_code,
                            perm_name,
                            perm_type,
                            path,
                            method,
                            sort_no,
                            status)
VALUES (0, 'system:role:list', '角色列表查询', 3, '/system/role/list', 'GET', 20, 1),
       (0, 'system:role:add', '新增角色', 3, '/system/role', 'POST', 21, 1),
       (0, 'system:role:update', '修改角色', 3, '/system/role', 'PUT', 22, 1),
       (0, 'system:role:delete', '删除角色', 3, '/system/role/{id}', 'DELETE', 23, 1),
       (0, 'system:role:assign-permission', '角色分配权限', 3, '/system/role/assign-permission', 'POST', 24, 1)
ON DUPLICATE KEY UPDATE perm_name = VALUES(perm_name),
                        path      = VALUES(path),
                        method    = VALUES(method),
                        sort_no   = VALUES(sort_no),
                        status    = VALUES(status);

# 权限管理权限
INSERT INTO sys_permission (parent_id,
                            perm_code,
                            perm_name,
                            perm_type,
                            path,
                            method,
                            sort_no,
                            status)
VALUES (0, 'system:permission:list', '权限列表查询', 3, '/system/permission/list', 'GET', 30, 1),
       (0, 'system:permission:add', '新增权限', 3, '/system/permission', 'POST', 31, 1),
       (0, 'system:permission:update', '修改权限', 3, '/system/permission', 'PUT', 32, 1),
       (0, 'system:permission:delete', '删除权限', 3, '/system/permission/{id}', 'DELETE', 33, 1)
ON DUPLICATE KEY UPDATE perm_name = VALUES(perm_name),
                        path      = VALUES(path),
                        method    = VALUES(method),
                        sort_no   = VALUES(sort_no),
                        status    = VALUES(status);

# 初始化用户角色关联 SQL
INSERT INTO sys_user_role (user_id,
                           role_id)
SELECT u.id,
       r.id
FROM sys_user u
         JOIN sys_role r ON r.role_code = 'ADMIN'
WHERE u.username = 'admin'
  AND NOT EXISTS (SELECT 1
                  FROM sys_user_role ur
                  WHERE ur.user_id = u.id
                    AND ur.role_id = r.id);

# 把 ADMIN 角色与权限全部关联起来
INSERT INTO sys_role_permission (role_id,
                                 permission_id)
SELECT r.id,
       p.id
FROM sys_role r
         JOIN sys_permission p ON p.perm_code IN (
                                                  'admin:dashboard',
                                                  'system:user:list',
                                                  'system:user:add',
                                                  'system:user:update',
                                                  'system:user:delete',
                                                  'system:user:assign-role',
                                                  'system:role:list',
                                                  'system:role:add',
                                                  'system:role:update',
                                                  'system:role:delete',
                                                  'system:role:assign-permission',
                                                  'system:permission:list',
                                                  'system:permission:add',
                                                  'system:permission:update',
                                                  'system:permission:delete'
    )
WHERE r.role_code = 'ADMIN'
  AND NOT EXISTS (SELECT 1
                  FROM sys_role_permission rp
                  WHERE rp.role_id = r.id
                    AND rp.permission_id = p.id);

-- food_category 模块父权限
INSERT INTO sys_permission (parent_id,
                            perm_code,
                            perm_name,
                            perm_type,
                            path,
                            method,
                            sort_no,
                            status)
VALUES (0,
        'food:category',
        '分类管理',
        1,
        '/food/category',
        NULL,
        2000,
        1)
ON DUPLICATE KEY UPDATE perm_name = VALUES(perm_name),
                        perm_type = VALUES(perm_type),
                        path      = VALUES(path),
                        method    = VALUES(method),
                        sort_no   = VALUES(sort_no),
                        status    = VALUES(status);

-- food_category 模块子权限
INSERT INTO sys_permission (parent_id,
                            perm_code,
                            perm_name,
                            perm_type,
                            path,
                            method,
                            sort_no,
                            status)
SELECT p.id,
       'food:category:list',
       '分类列表',
       3,
       '/food/category/list',
       'GET',
       2001,
       1
FROM sys_permission p
WHERE p.perm_code = 'food:category'
ON DUPLICATE KEY UPDATE perm_name = VALUES(perm_name),
                        parent_id = VALUES(parent_id),
                        perm_type = VALUES(perm_type),
                        path      = VALUES(path),
                        method    = VALUES(method),
                        sort_no   = VALUES(sort_no),
                        status    = VALUES(status);

INSERT INTO sys_permission (parent_id,
                            perm_code,
                            perm_name,
                            perm_type,
                            path,
                            method,
                            sort_no,
                            status)
SELECT p.id,
       'food:category:detail',
       '分类详情',
       3,
       '/food/category/{id}',
       'GET',
       2002,
       1
FROM sys_permission p
WHERE p.perm_code = 'food:category'
ON DUPLICATE KEY UPDATE perm_name = VALUES(perm_name),
                        parent_id = VALUES(parent_id),
                        perm_type = VALUES(perm_type),
                        path      = VALUES(path),
                        method    = VALUES(method),
                        sort_no   = VALUES(sort_no),
                        status    = VALUES(status);

INSERT INTO sys_permission (parent_id,
                            perm_code,
                            perm_name,
                            perm_type,
                            path,
                            method,
                            sort_no,
                            status)
SELECT p.id,
       'food:category:add',
       '新增分类',
       3,
       '/food/category',
       'POST',
       2003,
       1
FROM sys_permission p
WHERE p.perm_code = 'food:category'
ON DUPLICATE KEY UPDATE perm_name = VALUES(perm_name),
                        parent_id = VALUES(parent_id),
                        perm_type = VALUES(perm_type),
                        path      = VALUES(path),
                        method    = VALUES(method),
                        sort_no   = VALUES(sort_no),
                        status    = VALUES(status);

INSERT INTO sys_permission (parent_id,
                            perm_code,
                            perm_name,
                            perm_type,
                            path,
                            method,
                            sort_no,
                            status)
SELECT p.id,
       'food:category:update',
       '修改分类',
       3,
       '/food/category',
       'PUT',
       2004,
       1
FROM sys_permission p
WHERE p.perm_code = 'food:category'
ON DUPLICATE KEY UPDATE perm_name = VALUES(perm_name),
                        parent_id = VALUES(parent_id),
                        perm_type = VALUES(perm_type),
                        path      = VALUES(path),
                        method    = VALUES(method),
                        sort_no   = VALUES(sort_no),
                        status    = VALUES(status);

INSERT INTO sys_permission (parent_id,
                            perm_code,
                            perm_name,
                            perm_type,
                            path,
                            method,
                            sort_no,
                            status)
SELECT p.id,
       'food:category:update-status',
       '修改分类状态',
       3,
       '/food/category/status',
       'PUT',
       2005,
       1
FROM sys_permission p
WHERE p.perm_code = 'food:category'
ON DUPLICATE KEY UPDATE perm_name = VALUES(perm_name),
                        parent_id = VALUES(parent_id),
                        perm_type = VALUES(perm_type),
                        path      = VALUES(path),
                        method    = VALUES(method),
                        sort_no   = VALUES(sort_no),
                        status    = VALUES(status);

INSERT INTO sys_permission (parent_id,
                            perm_code,
                            perm_name,
                            perm_type,
                            path,
                            method,
                            sort_no,
                            status)
SELECT p.id,
       'food:category:delete',
       '删除分类',
       3,
       '/food/category/{id}',
       'DELETE',
       2006,
       1
FROM sys_permission p
WHERE p.perm_code = 'food:category'
ON DUPLICATE KEY UPDATE perm_name = VALUES(perm_name),
                        parent_id = VALUES(parent_id),
                        perm_type = VALUES(perm_type),
                        path      = VALUES(path),
                        method    = VALUES(method),
                        sort_no   = VALUES(sort_no),
                        status    = VALUES(status);

INSERT INTO sys_role_permission (role_id,
                                 permission_id)
SELECT r.id,
       p.id
FROM sys_role r
         JOIN sys_permission p ON p.perm_code IN (
                                                  'food:category',
                                                  'food:category:list',
                                                  'food:category:detail',
                                                  'food:category:add',
                                                  'food:category:update',
                                                  'food:category:update-status',
                                                  'food:category:delete'
    )
WHERE r.role_code = 'ADMIN'
  AND NOT EXISTS (SELECT 1
                  FROM sys_role_permission rp
                  WHERE rp.role_id = r.id
                    AND rp.permission_id = p.id);

-- food_item 模块父权限
INSERT INTO sys_permission (parent_id,
                            perm_code,
                            perm_name,
                            perm_type,
                            path,
                            method,
                            sort_no,
                            status)
VALUES (0,
        'food:item',
        '菜品管理',
        1,
        '/food/item',
        NULL,
        2100,
        1)
ON DUPLICATE KEY UPDATE perm_name = VALUES(perm_name),
                        perm_type = VALUES(perm_type),
                        path      = VALUES(path),
                        method    = VALUES(method),
                        sort_no   = VALUES(sort_no),
                        status    = VALUES(status);

-- food_item 模块子权限
INSERT INTO sys_permission (parent_id,
                            perm_code,
                            perm_name,
                            perm_type,
                            path,
                            method,
                            sort_no,
                            status)
SELECT p.id,
       'food:item:list',
       '菜品列表',
       3,
       '/food/item/list',
       'GET',
       2101,
       1
FROM sys_permission p
WHERE p.perm_code = 'food:item'
ON DUPLICATE KEY UPDATE perm_name = VALUES(perm_name),
                        parent_id = VALUES(parent_id),
                        perm_type = VALUES(perm_type),
                        path      = VALUES(path),
                        method    = VALUES(method),
                        sort_no   = VALUES(sort_no),
                        status    = VALUES(status);

INSERT INTO sys_permission (parent_id,
                            perm_code,
                            perm_name,
                            perm_type,
                            path,
                            method,
                            sort_no,
                            status)
SELECT p.id,
       'food:item:detail',
       '菜品详情',
       3,
       '/food/item/{id}',
       'GET',
       2102,
       1
FROM sys_permission p
WHERE p.perm_code = 'food:item'
ON DUPLICATE KEY UPDATE perm_name = VALUES(perm_name),
                        parent_id = VALUES(parent_id),
                        perm_type = VALUES(perm_type),
                        path      = VALUES(path),
                        method    = VALUES(method),
                        sort_no   = VALUES(sort_no),
                        status    = VALUES(status);

INSERT INTO sys_permission (parent_id,
                            perm_code,
                            perm_name,
                            perm_type,
                            path,
                            method,
                            sort_no,
                            status)
SELECT p.id,
       'food:item:add',
       '新增菜品',
       3,
       '/food/item',
       'POST',
       2103,
       1
FROM sys_permission p
WHERE p.perm_code = 'food:item'
ON DUPLICATE KEY UPDATE perm_name = VALUES(perm_name),
                        parent_id = VALUES(parent_id),
                        perm_type = VALUES(perm_type),
                        path      = VALUES(path),
                        method    = VALUES(method),
                        sort_no   = VALUES(sort_no),
                        status    = VALUES(status);

INSERT INTO sys_permission (parent_id,
                            perm_code,
                            perm_name,
                            perm_type,
                            path,
                            method,
                            sort_no,
                            status)
SELECT p.id,
       'food:item:update',
       '修改菜品',
       3,
       '/food/item',
       'PUT',
       2104,
       1
FROM sys_permission p
WHERE p.perm_code = 'food:item'
ON DUPLICATE KEY UPDATE perm_name = VALUES(perm_name),
                        parent_id = VALUES(parent_id),
                        perm_type = VALUES(perm_type),
                        path      = VALUES(path),
                        method    = VALUES(method),
                        sort_no   = VALUES(sort_no),
                        status    = VALUES(status);

INSERT INTO sys_permission (parent_id,
                            perm_code,
                            perm_name,
                            perm_type,
                            path,
                            method,
                            sort_no,
                            status)
SELECT p.id,
       'food:item:update-on-sale',
       '修改上下架状态',
       3,
       '/food/item/on-sale',
       'PUT',
       2105,
       1
FROM sys_permission p
WHERE p.perm_code = 'food:item'
ON DUPLICATE KEY UPDATE perm_name = VALUES(perm_name),
                        parent_id = VALUES(parent_id),
                        perm_type = VALUES(perm_type),
                        path      = VALUES(path),
                        method    = VALUES(method),
                        sort_no   = VALUES(sort_no),
                        status    = VALUES(status);

INSERT INTO sys_permission (parent_id,
                            perm_code,
                            perm_name,
                            perm_type,
                            path,
                            method,
                            sort_no,
                            status)
SELECT p.id,
       'food:item:delete',
       '删除菜品',
       3,
       '/food/item/{id}',
       'DELETE',
       2106,
       1
FROM sys_permission p
WHERE p.perm_code = 'food:item'
ON DUPLICATE KEY UPDATE perm_name = VALUES(perm_name),
                        parent_id = VALUES(parent_id),
                        perm_type = VALUES(perm_type),
                        path      = VALUES(path),
                        method    = VALUES(method),
                        sort_no   = VALUES(sort_no),
                        status    = VALUES(status);

INSERT INTO sys_role_permission (role_id,
                                 permission_id)
SELECT r.id,
       p.id
FROM sys_role r
         JOIN sys_permission p ON p.perm_code IN (
                                                  'food:item',
                                                  'food:item:list',
                                                  'food:item:detail',
                                                  'food:item:add',
                                                  'food:item:update',
                                                  'food:item:update-on-sale',
                                                  'food:item:update-stock',
                                                  'food:item:delete'
    )
WHERE r.role_code = 'ADMIN'
  AND NOT EXISTS (SELECT 1
                  FROM sys_role_permission rp
                  WHERE rp.role_id = r.id
                    AND rp.permission_id = p.id);

-- food_order 模块父权限
INSERT INTO sys_permission (parent_id,
                            perm_code,
                            perm_name,
                            perm_type,
                            path,
                            method,
                            sort_no,
                            status)
VALUES (0,
        'food:order',
        '订单管理',
        1,
        '/food/order',
        NULL,
        2200,
        1)
ON DUPLICATE KEY UPDATE perm_name = VALUES(perm_name),
                        perm_type = VALUES(perm_type),
                        path      = VALUES(path),
                        method    = VALUES(method),
                        sort_no   = VALUES(sort_no),
                        status    = VALUES(status);

INSERT INTO sys_permission (parent_id,
                            perm_code,
                            perm_name,
                            perm_type,
                            path,
                            method,
                            sort_no,
                            status)
SELECT p.id,
       'food:order:list',
       '订单列表',
       3,
       '/food/order/list',
       'GET',
       2201,
       1
FROM sys_permission p
WHERE p.perm_code = 'food:order'
ON DUPLICATE KEY UPDATE perm_name = VALUES(perm_name),
                        parent_id = VALUES(parent_id),
                        perm_type = VALUES(perm_type),
                        path      = VALUES(path),
                        method    = VALUES(method),
                        sort_no   = VALUES(sort_no),
                        status    = VALUES(status);

INSERT INTO sys_permission (parent_id,
                            perm_code,
                            perm_name,
                            perm_type,
                            path,
                            method,
                            sort_no,
                            status)
SELECT p.id,
       'food:order:detail',
       '订单详情',
       3,
       '/food/order/{id}',
       'GET',
       2202,
       1
FROM sys_permission p
WHERE p.perm_code = 'food:order'
ON DUPLICATE KEY UPDATE perm_name = VALUES(perm_name),
                        parent_id = VALUES(parent_id),
                        perm_type = VALUES(perm_type),
                        path      = VALUES(path),
                        method    = VALUES(method),
                        sort_no   = VALUES(sort_no),
                        status    = VALUES(status);

INSERT INTO sys_permission (parent_id,
                            perm_code,
                            perm_name,
                            perm_type,
                            path,
                            method,
                            sort_no,
                            status)
SELECT p.id,
       'food:order:add',
       '创建订单',
       3,
       '/food/order',
       'POST',
       2203,
       1
FROM sys_permission p
WHERE p.perm_code = 'food:order'
ON DUPLICATE KEY UPDATE perm_name = VALUES(perm_name),
                        parent_id = VALUES(parent_id),
                        perm_type = VALUES(perm_type),
                        path      = VALUES(path),
                        method    = VALUES(method),
                        sort_no   = VALUES(sort_no),
                        status    = VALUES(status);

INSERT INTO sys_permission (parent_id,
                            perm_code,
                            perm_name,
                            perm_type,
                            path,
                            method,
                            sort_no,
                            status)
SELECT p.id,
       'food:order:process',
       '开始制作',
       3,
       '/food/order/process',
       'PUT',
       2204,
       1
FROM sys_permission p
WHERE p.perm_code = 'food:order'
ON DUPLICATE KEY UPDATE perm_name = VALUES(perm_name),
                        parent_id = VALUES(parent_id),
                        perm_type = VALUES(perm_type),
                        path      = VALUES(path),
                        method    = VALUES(method),
                        sort_no   = VALUES(sort_no),
                        status    = VALUES(status);

INSERT INTO sys_permission (parent_id,
                            perm_code,
                            perm_name,
                            perm_type,
                            path,
                            method,
                            sort_no,
                            status)
SELECT p.id,
       'food:order:cancel',
       '取消订单',
       3,
       '/food/order/cancel',
       'PUT',
       2205,
       1
FROM sys_permission p
WHERE p.perm_code = 'food:order'
ON DUPLICATE KEY UPDATE perm_name = VALUES(perm_name),
                        parent_id = VALUES(parent_id),
                        perm_type = VALUES(perm_type),
                        path      = VALUES(path),
                        method    = VALUES(method),
                        sort_no   = VALUES(sort_no),
                        status    = VALUES(status);

INSERT INTO sys_permission (parent_id,
                            perm_code,
                            perm_name,
                            perm_type,
                            path,
                            method,
                            sort_no,
                            status)
SELECT p.id,
       'food:order:complete',
       '完成订单',
       3,
       '/food/order/complete',
       'PUT',
       2206,
       1
FROM sys_permission p
WHERE p.perm_code = 'food:order'
ON DUPLICATE KEY UPDATE perm_name = VALUES(perm_name),
                        parent_id = VALUES(parent_id),
                        perm_type = VALUES(perm_type),
                        path      = VALUES(path),
                        method    = VALUES(method),
                        sort_no   = VALUES(sort_no),
                        status    = VALUES(status);

INSERT INTO sys_role_permission (role_id,
                                 permission_id)
SELECT r.id,
       p.id
FROM sys_role r
         JOIN sys_permission p ON p.perm_code IN (
                                                  'food:order',
                                                  'food:order:list',
                                                  'food:order:detail',
                                                  'food:order:add',
                                                  'food:order:process',
                                                  'food:order:cancel',
                                                  'food:order:complete',
                                                  'food:order:stat'
    )
WHERE r.role_code = 'ADMIN'
  AND NOT EXISTS (SELECT 1
                  FROM sys_role_permission rp
                  WHERE rp.role_id = r.id
                    AND rp.permission_id = p.id);

INSERT INTO sys_permission (parent_id,
                            perm_code,
                            perm_name,
                            perm_type,
                            path,
                            method,
                            sort_no,
                            status)
SELECT p.id,
       'food:order:stat',
       '订单统计',
       3,
       '/food/order/stat/**',
       'GET',
       2207,
       1
FROM sys_permission p
WHERE p.perm_code = 'food:order'
ON DUPLICATE KEY UPDATE perm_name = VALUES(perm_name),
                        parent_id = VALUES(parent_id),
                        perm_type = VALUES(perm_type),
                        path      = VALUES(path),
                        method    = VALUES(method),
                        sort_no   = VALUES(sort_no),
                        status    = VALUES(status);

CREATE TABLE food_stock_log
(
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    food_item_id  BIGINT UNSIGNED NOT NULL COMMENT '菜品ID',
    change_type   TINYINT         NOT NULL COMMENT '变动类型：1下单扣减 2取消回补 3后台调整',
    change_amount INT             NOT NULL COMMENT '变动数量，扣减为负数，回补为正数',
    before_stock  INT             NOT NULL COMMENT '变动前库存',
    after_stock   INT             NOT NULL COMMENT '变动后库存',
    biz_id        BIGINT UNSIGNED          DEFAULT NULL COMMENT '业务ID，例如订单ID',
    remark        VARCHAR(255)             DEFAULT NULL COMMENT '备注',
    created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_food_stock_log_food_item_id (food_item_id),
    KEY idx_food_stock_log_change_type (change_type),
    KEY idx_food_stock_log_biz_id (biz_id),
    CONSTRAINT fk_food_stock_log_food_item_id FOREIGN KEY (food_item_id) REFERENCES food_item (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='库存变动日志表';