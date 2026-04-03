# EFoodPass Flyway 实战宝宝教程

这份文档不是讲概念的。

它只做一件事：

- 手把手带你在这个项目里真正用一次 Flyway

也就是教你完成这三件事：

1. 看懂当前项目的迁移文件
2. 模拟以后自己新增一个 `V3` 迁移
3. 学会验证迁移到底有没有成功

如果你还没理解 Flyway 是什么，先看：

- [flyway-baby.md](./flyway-baby.md)

如果你想看维护视角的说明，再看：

- [db-migration.md](./db-migration.md)

## 1. 先认清你现在这个项目的迁移目录

位置在这里：

- [db/migration](/C:/Users/22390/Desktop/EFoodPass/src/main/resources/db/migration)

现在你应该能看到至少这两个文件：

- [V1__bootstrap_current_schema.sql](/C:/Users/22390/Desktop/EFoodPass/src/main/resources/db/migration/V1__bootstrap_current_schema.sql)
- [V2__upgrade_legacy_schema.sql](/C:/Users/22390/Desktop/EFoodPass/src/main/resources/db/migration/V2__upgrade_legacy_schema.sql)

你先把它理解成：

- `V1`：从零把完整结构搭起来
- `V2`：给旧库补齐后面新增的结构

## 2. 先确认项目现在已经接上 Flyway

看这里：

- [application.yaml](/C:/Users/22390/Desktop/EFoodPass/src/main/resources/application.yaml)

你应该能看到：

```yml
spring:
  flyway:
    enabled: true
    baseline-on-migrate: true
    baseline-version: 1
    locations: classpath:db/migration
```

你只要记住这句话：

- 项目启动时，Flyway 会自动看 `db/migration` 目录，然后按版本执行

## 3. 实战一：怎么看数据库现在迁移到哪了

最直接有两种方法。

### 方法 A：查数据库表

执行：

```sql
USE e_food;

SELECT installed_rank,
       version,
       description,
       type,
       script,
       success,
       installed_on
FROM flyway_schema_history
ORDER BY installed_rank;
```

你会看到类似这样的结果：

```text
1 | 1 | bootstrap current schema | SQL | V1__bootstrap_current_schema.sql | 1
2 | 2 | upgrade legacy schema    | SQL | V2__upgrade_legacy_schema.sql    | 1
```

只要 `success = 1`，通常就说明这一条成功了。

### 方法 B：访问 `/actuator/flyway`

注意：

- 直接浏览器打开通常会 `401`
- 因为这个项目要求先登录

所以正确方式是：

1. 先调 `POST /auth/login`
2. 拿到 token
3. 再用带 token 的方式访问

## 4. 实战二：怎么带 token 访问 `/actuator/flyway`

### 第 1 步：先登录

请求：

```http
POST /auth/login
```

请求体：

```json
{
  "username": "admin",
  "password": "Admin@123"
}
```

返回里会有：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzUxMiJ9.xxx"
  }
}
```

把 `token` 复制出来。

### 第 2 步：Windows 命令行查看 Flyway

如果你在 Windows 上，可以这样：

```powershell
curl.exe -H "Authorization: Bearer 你的token" http://localhost:5603/actuator/flyway
```

或者 PowerShell 这样：

```powershell
$token = "你的token"
Invoke-WebRequest -Uri "http://localhost:5603/actuator/flyway" `
  -Headers @{ Authorization = "Bearer $token" } `
  -UseBasicParsing
```

### 第 3 步：你应该看到什么

你看到的内容会是一段和 Flyway 迁移有关的 JSON。

不一定每台机器一模一样，但核心会包含：

- 迁移版本
- 脚本名
- 执行状态

如果你看到的还是：

```json
{
  "code": 401,
  "message": "未登录或token无效",
  "data": null
}
```

通常检查这几件事：

- token 有没有复制完整
- 有没有把 `Bearer ` 这个前缀带上
- token 有没有过期

## 5. 实战三：以后如果你要新增一个数据库变更，正确做法是什么

假设你以后要给 `food_order` 增加一个新字段：

- `pickup_code`

正确姿势不是：

- 去改 `V1`

正确姿势是：

- 新建一个更高版本的 SQL 文件

比如：

```text
V3__add_food_order_pickup_code.sql
```

位置放在：

- [db/migration](/C:/Users/22390/Desktop/EFoodPass/src/main/resources/db/migration)

## 6. 实战四：`V3` 文件应该怎么写

在这个项目里，建议优先写成“兼容旧版 MySQL”的风格。

原因很简单：

- 有些 MySQL 不支持 `ADD COLUMN IF NOT EXISTS`

所以你应该写成这种“先判断，再执行”的形式。

示例：

```sql
SET @ddl = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'food_order'
              AND column_name = 'pickup_code'
        ),
        'SELECT 1',
        'ALTER TABLE food_order ADD COLUMN pickup_code VARCHAR(32) DEFAULT NULL COMMENT ''取餐码'''
    )
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
```

这个写法的好处：

- 字段已经存在就跳过
- 字段不存在才执行 `ALTER TABLE`
- 对不同 MySQL 版本更稳

## 7. 实战五：新增 `V3` 之后，完整流程怎么走

以后你真正做变更时，按这个顺序最稳：

1. 在 `db/migration` 新建 `V3__xxx.sql`
2. 写你这次的数据库变更
3. 保存文件
4. 启动项目
5. 让 Flyway 自动执行
6. 查 `flyway_schema_history`
7. 再查目标表，确认字段或表真的存在

## 8. 实战六：怎么验证 `V3` 真的执行成功了

### 验证 1：查 Flyway 历史表

```sql
SELECT version, description, script, success
FROM flyway_schema_history
ORDER BY installed_rank;
```

你应该能看到新的一行：

```text
3 | add food order pickup code | V3__add_food_order_pickup_code.sql | 1
```

### 验证 2：查目标表结构

```sql
SHOW COLUMNS FROM food_order;
```

或者更准确一点：

```sql
SELECT column_name
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'food_order'
  AND column_name = 'pickup_code';
```

如果能查出来，说明字段真的落库了。

## 9. 实战七：如果我写坏了一个迁移文件怎么办

先分两种情况。

### 情况 A：你还没让任何环境执行过

这时候最简单：

- 直接改文件

### 情况 B：这个迁移已经在某个环境执行过了

这时候不要改旧文件。

正确姿势是：

- 新建更高版本修复

比如：

```text
V4__fix_pickup_code_length.sql
```

这是 Flyway 的核心纪律之一。

## 10. 实战八：如果启动时报 Flyway 失败，第一反应看什么

优先看这三样：

1. 启动日志里提到的是哪个版本
2. `flyway_schema_history` 里哪一条是失败的
3. 失败版本对应的 SQL 文件内容

比如你前面遇到的是：

```text
Detected failed migration to version 1
```

那第一反应就应该是：

- 看 `V1`
- 看 `flyway_schema_history`
- 看数据库里是不是已经留下半截结构

## 11. 实战九：开发环境里最常见的恢复办法

如果你是在本地开发环境，而且可以接受重跑迁移，最常见的恢复方式是：

```sql
DROP TABLE IF EXISTS flyway_schema_history;
```

然后重新启动项目。

但你要注意：

- 这只能清迁移记录
- 不能自动回滚已经执行过一半的表结构变化

所以如果失败脚本已经改了一半结构，你可能还要自己补清理。

## 12. 实战十：以后维护这个项目时，你最该记住什么

只记住这 5 条就够用：

1. 不要改已经执行过的 `V1`、`V2`
2. 新变更永远新建更高版本，比如 `V3`、`V4`
3. 优先写兼容旧版 MySQL 的 SQL
4. 验证时先查 `flyway_schema_history`
5. `/actuator/flyway` 返回 `401` 通常只是因为没登录

## 13. 给你一个最短的未来操作模板

以后你每次要改数据库，就按这个模板走：

1. 想清楚这次要加什么表/字段/索引/权限
2. 新建 `Vx__你的变更名.sql`
3. 启动项目
4. 查 `flyway_schema_history`
5. 查数据库结构
6. 再去联调接口

如果你能一直按这个顺序做，数据库版本这块就会稳定很多。
