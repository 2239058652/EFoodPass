-- Upgrade script for databases that were initialized before
-- payment/session/logging features were added.
-- Run this against an existing e_food database if you do not want to recreate it.

SET @ddl = (
    SELECT IF(
                   EXISTS(
                           SELECT 1
                           FROM information_schema.columns
                           WHERE table_schema = DATABASE()
                             AND table_name = 'sys_user'
                             AND column_name = 'token_version'
                   ),
                   'SELECT 1',
                   'ALTER TABLE sys_user ADD COLUMN token_version INT NOT NULL DEFAULT 0 COMMENT ''令牌版本号'''
           )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS sys_operation_log
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

CREATE TABLE IF NOT EXISTS sys_login_log
(
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    user_id    BIGINT UNSIGNED          DEFAULT NULL COMMENT '用户ID',
    username   VARCHAR(50)              DEFAULT NULL COMMENT '登录账号',
    request_ip VARCHAR(64)              DEFAULT NULL COMMENT '请求IP',
    success    TINYINT         NOT NULL DEFAULT 1 COMMENT '1成功 0失败',
    message    VARCHAR(200)             DEFAULT NULL COMMENT '结果说明',
    login_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
    PRIMARY KEY (id),
    KEY idx_sys_login_log_user_id (user_id),
    KEY idx_sys_login_log_username (username),
    KEY idx_sys_login_log_login_time (login_time)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='登录日志表';

CREATE TABLE IF NOT EXISTS sys_user_session
(
    id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '会话ID',
    user_id          BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    session_id       VARCHAR(64)     NOT NULL COMMENT '会话标识',
    token_version    INT             NOT NULL DEFAULT 0 COMMENT '令牌版本号',
    request_ip       VARCHAR(64)              DEFAULT NULL COMMENT '登录IP',
    user_agent       VARCHAR(255)             DEFAULT NULL COMMENT '设备标识',
    login_time       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
    last_access_time DATETIME                 DEFAULT NULL COMMENT '最后访问时间',
    expire_time      DATETIME        NOT NULL COMMENT '过期时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_user_session_session_id (session_id),
    KEY idx_sys_user_session_user_id (user_id),
    KEY idx_sys_user_session_expire_time (expire_time),
    CONSTRAINT fk_sys_user_session_user_id FOREIGN KEY (user_id) REFERENCES sys_user (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户会话表';

SET @ddl = (
    SELECT IF(
                   EXISTS(
                           SELECT 1
                           FROM information_schema.columns
                           WHERE table_schema = DATABASE()
                             AND table_name = 'food_order'
                             AND column_name = 'payment_status'
                   ),
                   'SELECT 1',
                   'ALTER TABLE food_order ADD COLUMN payment_status TINYINT NOT NULL DEFAULT 10 COMMENT ''支付状态：10待支付 20已支付 30已退款'''
           )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(
                   EXISTS(
                           SELECT 1
                           FROM information_schema.columns
                           WHERE table_schema = DATABASE()
                             AND table_name = 'food_order'
                             AND column_name = 'payment_method'
                   ),
                   'SELECT 1',
                   'ALTER TABLE food_order ADD COLUMN payment_method VARCHAR(20) DEFAULT NULL COMMENT ''支付方式'''
           )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(
                   EXISTS(
                           SELECT 1
                           FROM information_schema.columns
                           WHERE table_schema = DATABASE()
                             AND table_name = 'food_order'
                             AND column_name = 'paid_at'
                   ),
                   'SELECT 1',
                   'ALTER TABLE food_order ADD COLUMN paid_at DATETIME DEFAULT NULL COMMENT ''支付时间'''
           )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(
                   EXISTS(
                           SELECT 1
                           FROM information_schema.columns
                           WHERE table_schema = DATABASE()
                             AND table_name = 'food_order'
                             AND column_name = 'close_reason'
                   ),
                   'SELECT 1',
                   'ALTER TABLE food_order ADD COLUMN close_reason VARCHAR(100) DEFAULT NULL COMMENT ''关闭原因'''
           )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (
    SELECT IF(
                   EXISTS(
                           SELECT 1
                           FROM information_schema.columns
                           WHERE table_schema = DATABASE()
                             AND table_name = 'food_order'
                             AND column_name = 'closed_at'
                   ),
                   'SELECT 1',
                   'ALTER TABLE food_order ADD COLUMN closed_at DATETIME DEFAULT NULL COMMENT ''关闭时间'''
           )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

INSERT INTO sys_permission (parent_id, perm_code, perm_name, perm_type, path, method, sort_no, status)
VALUES (0, 'system:operation-log', '操作日志管理', 1, '/system/operation-log', NULL, 40, 1)
ON DUPLICATE KEY UPDATE perm_name = VALUES(perm_name),
                        perm_type = VALUES(perm_type),
                        path      = VALUES(path),
                        method    = VALUES(method),
                        sort_no   = VALUES(sort_no),
                        status    = VALUES(status);

INSERT INTO sys_permission (parent_id, perm_code, perm_name, perm_type, path, method, sort_no, status)
VALUES (0, 'system:login-log', '登录日志管理', 1, '/system/login-log', NULL, 50, 1)
ON DUPLICATE KEY UPDATE perm_name = VALUES(perm_name),
                        perm_type = VALUES(perm_type),
                        path      = VALUES(path),
                        method    = VALUES(method),
                        sort_no   = VALUES(sort_no),
                        status    = VALUES(status);

INSERT INTO sys_permission (parent_id, perm_code, perm_name, perm_type, path, method, sort_no, status)
SELECT p.id, 'system:operation-log:list', '操作日志列表查询', 3, '/system/operation-log/list', 'GET', 41, 1
FROM sys_permission p
WHERE p.perm_code = 'system:operation-log'
ON DUPLICATE KEY UPDATE perm_name = VALUES(perm_name),
                        parent_id = VALUES(parent_id),
                        perm_type = VALUES(perm_type),
                        path      = VALUES(path),
                        method    = VALUES(method),
                        sort_no   = VALUES(sort_no),
                        status    = VALUES(status);

INSERT INTO sys_permission (parent_id, perm_code, perm_name, perm_type, path, method, sort_no, status)
SELECT p.id, 'system:login-log:list', '登录日志列表查询', 3, '/system/login-log/list', 'GET', 51, 1
FROM sys_permission p
WHERE p.perm_code = 'system:login-log'
ON DUPLICATE KEY UPDATE perm_name = VALUES(perm_name),
                        parent_id = VALUES(parent_id),
                        perm_type = VALUES(perm_type),
                        path      = VALUES(path),
                        method    = VALUES(method),
                        sort_no   = VALUES(sort_no),
                        status    = VALUES(status);

INSERT INTO sys_permission (parent_id, perm_code, perm_name, perm_type, path, method, sort_no, status)
SELECT p.id, 'food:order:refund', '订单退款', 3, '/food/order/refund', 'PUT', 2208, 1
FROM sys_permission p
WHERE p.perm_code = 'food:order'
ON DUPLICATE KEY UPDATE perm_name = VALUES(perm_name),
                        parent_id = VALUES(parent_id),
                        perm_type = VALUES(perm_type),
                        path      = VALUES(path),
                        method    = VALUES(method),
                        sort_no   = VALUES(sort_no),
                        status    = VALUES(status);

INSERT INTO sys_permission (parent_id, perm_code, perm_name, perm_type, path, method, sort_no, status)
SELECT p.id, 'food:order:export', '订单导出', 3, '/food/order/export', 'GET', 2209, 1
FROM sys_permission p
WHERE p.perm_code = 'food:order'
ON DUPLICATE KEY UPDATE perm_name = VALUES(perm_name),
                        parent_id = VALUES(parent_id),
                        perm_type = VALUES(perm_type),
                        path      = VALUES(path),
                        method    = VALUES(method),
                        sort_no   = VALUES(sort_no),
                        status    = VALUES(status);

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
         JOIN sys_permission p ON p.perm_code IN (
                                                  'system:operation-log',
                                                  'system:operation-log:list',
                                                  'system:login-log',
                                                  'system:login-log:list',
                                                  'food:order:refund',
                                                  'food:order:export'
    )
WHERE r.role_code = 'ADMIN'
  AND NOT EXISTS (SELECT 1
                  FROM sys_role_permission rp
                  WHERE rp.role_id = r.id
                    AND rp.permission_id = p.id);
