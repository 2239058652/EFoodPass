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