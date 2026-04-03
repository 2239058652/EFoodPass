USE e_food;

-- 1. 看 Flyway 历史表是否存在
SHOW TABLES LIKE 'flyway_schema_history';

-- 2. 看迁移执行历史
SELECT installed_rank,
       version,
       description,
       type,
       script,
       success,
       installed_on
FROM flyway_schema_history
ORDER BY installed_rank;

-- 3. 检查订单支付字段是否已经补齐
SELECT column_name
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'food_order'
  AND column_name IN ('payment_status', 'payment_method', 'paid_at', 'close_reason', 'closed_at')
ORDER BY column_name;

-- 4. 检查新日志表和会话表是否存在
SHOW TABLES LIKE 'sys_login_log';
SHOW TABLES LIKE 'sys_operation_log';
SHOW TABLES LIKE 'sys_user_session';

-- 5. 检查管理员是否拿到了新增权限
SELECT p.perm_code
FROM sys_role r
         JOIN sys_role_permission rp ON rp.role_id = r.id
         JOIN sys_permission p ON p.id = rp.permission_id
WHERE r.role_code = 'ADMIN'
  AND p.perm_code IN (
                      'system:login-log:list',
                      'system:operation-log:list',
                      'food:order:refund',
                      'food:order:export'
    )
ORDER BY p.perm_code;
