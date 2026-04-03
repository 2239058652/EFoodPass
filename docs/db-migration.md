# EFoodPass 数据库迁移说明

这份文档是给项目维护者看的。

它解决的是这个问题：

- 以前数据库升级靠手工执行 SQL
- 容易漏字段、漏表、漏权限
- 一旦代码先升级，数据库没跟上，就会出现运行时报错

现在项目已经接入了 Flyway，数据库迁移会在应用启动时自动执行。

## 1. 现在的迁移规则

版本化迁移文件放在这里：

- [db/migration](/C:/Users/22390/Desktop/EFoodPass/src/main/resources/db/migration)

当前第一份迁移文件是：

- [V1__bootstrap_current_schema.sql](/C:/Users/22390/Desktop/EFoodPass/src/main/resources/db/migration/V1__bootstrap_current_schema.sql)

它是当前版本的完整幂等初始化脚本。

最简单理解：

- 新库：启动时直接建表并插入初始化数据
- 老库：启动时自动补齐缺失结构

## 2. Flyway 现在怎么配置的

公共配置在：

- [application.yaml](/C:/Users/22390/Desktop/EFoodPass/src/main/resources/application.yaml)

关键配置是：

```yml
spring:
  flyway:
    enabled: true
    baseline-on-migrate: true
    baseline-version: 1
    locations: classpath:db/migration
```

这套配置的目的：

- 空库可以直接从 `V1` 开始初始化
- 已经有旧表的数据库也能接入 Flyway
- 老库会先做 baseline，再继续跑版本迁移

## 3. 旧数据库为什么也能接上

因为现在配置了：

- `baseline-on-migrate: true`
- `baseline-version: 1`

这意味着：

1. 如果数据库里已经有旧表，但还没有 `flyway_schema_history`
2. Flyway 会先建立迁移历史
3. 然后继续执行高于 `0` 的版本迁移
4. 老库会把 `V1` 视为基线
5. 然后继续执行 `V2` 及后续增量迁移

而我们的 `V1` 是幂等脚本，里面大量使用了：

- `CREATE TABLE IF NOT EXISTS`
- `information_schema` + `ALTER TABLE`
- `ON DUPLICATE KEY UPDATE`
- `NOT EXISTS`

所以它可以同时兼容：

- 新库初始化
- 老库补齐结构

## 3.1 当前版本的迁移分工

- `V1`：完整初始化当前结构，给空库使用
- `V2`：给旧库补齐支付、日志、会话、权限相关结构

这样：

- 空库启动时会执行 `V1` 再执行 `V2`
- 老库启动时会 baseline 到 `1`，然后执行 `V2`

## 4. 以后新增数据库变更，要怎么做

规则非常简单：

1. 不要再改已经执行过的 `V1`
2. 新增一个更高版本的 SQL 文件
3. 文件名必须按 Flyway 规则命名

示例：

- `V2__add_coupon_tables.sql`
- `V3__add_order_delivery_fields.sql`
- `V4__seed_refund_permissions.sql`

## 5. 以后最推荐的变更拆分方式

如果你下一次要加一个新功能，建议拆成这几类 SQL：

- 表结构变化：`ALTER TABLE`、`CREATE TABLE`
- 索引变化：`CREATE INDEX`
- 初始数据：权限、菜单、默认角色绑定

这样后面排查问题时最好找。

## 6. 绝对不要做的事

### 6.1 不要修改已经在线上跑过的旧版本脚本

比如：

- 不要改 `V1__bootstrap_current_schema.sql`

因为 Flyway 会校验历史版本的 checksum。

一旦改了，已经执行过这个版本的环境，下次启动可能直接报校验错误。

## 7. 如果确实要补救旧脚本，怎么办

正确做法不是改旧文件，而是新建一个更高版本，比如：

- `V2__fix_order_close_reason_length.sql`

让数据库通过新的增量脚本修正。

## 8. 手工 SQL 还保留吗

保留。

项目里现在还有这些 SQL：

- [init-rbac.sql](/C:/Users/22390/Desktop/EFoodPass/sql/init-rbac.sql)
- [upgrade-existing-db.sql](/C:/Users/22390/Desktop/EFoodPass/sql/upgrade-existing-db.sql)

用途分别是：

- `init-rbac.sql`：手工全量初始化参考
- `upgrade-existing-db.sql`：手工升级兜底脚本

但以后优先推荐的方式已经变成：

- 代码合并
- 启动项目
- Flyway 自动迁移

## 9. 怎么确认迁移有没有成功

项目已经把 Flyway actuator 端点暴露出来了。

开发环境启动后可以看：

```text
http://localhost:5603/actuator/flyway
```

注意：

- 这个接口当前受 Spring Security 保护
- 直接浏览器打开通常会返回 `401`
- 需要你先登录并带上 token 再访问

如果你在生产环境改了端口，就换成对应端口。

你也可以直接去数据库里看这张表：

- `flyway_schema_history`

## 10. 这一套能解决你这次遇到的问题吗

能。

你这次的问题本质上就是：

- 代码已经升级
- 数据库没有升级
- 结果运行时查了新字段，数据库里却没有

接入 Flyway 后，这种问题会少很多，因为结构升级会跟着应用启动自动执行。

## 11. 如果你之前已经出现过失败的 V1 记录

比如启动日志里有这种错误：

```text
Validate failed: Detected failed migration to version 1
```

说明你之前已经留下了一条失败的 Flyway 历史记录。

这时候最简单的处理方式是：

1. 确认业务表数据还在
2. 删除 Flyway 的历史表
3. 重新启动项目

执行 SQL：

```sql
DROP TABLE IF EXISTS flyway_schema_history;
```

因为这张表只记录迁移历史，不存业务数据。
