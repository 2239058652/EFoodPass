# EFoodPass 宝宝级排错教程

这份文档专门处理一句话：

我明明是照着教程做的，为什么还是报错？

不要慌。大多数新手第一次跑项目，问题都集中在这几类：

- 数据库没建好
- 配置文件没改对
- 旧数据库结构和新代码对不上
- Flyway 迁移失败
- Swagger 页面打不开
- 没带 token 就去访问受保护接口

这份文档只讲最常见、最容易复现、最容易处理的问题。

## 1. 启动直接失败

先看启动日志里最前面那条真正的报错，不要只看最后一句 `Application run failed`。

常见原因：

- MySQL 没启动
- Redis 没启动
- [application-dev.yml](/C:/Users/22390/Desktop/EFoodPass/src/main/resources/application-dev.yml) 里的地址、账号、密码不对
- 数据库 `e_food` 还没创建

你先检查这 4 件事：

1. MySQL 能不能连上
2. Redis 能不能连上
3. 数据库 `e_food` 是否存在
4. [application-dev.yml](/C:/Users/22390/Desktop/EFoodPass/src/main/resources/application-dev.yml) 的配置是不是你自己机器上的真实配置

## 2. 报错 `Unknown column 'payment_status'`

这是最典型的“代码升级了，数据库没升级”。

意思是：

- 代码已经开始查询 `food_order.payment_status`
- 但你现在数据库里的 `food_order` 表还没有这列

处理方法有两种。

### 方法 A：让 Flyway 自动补

前提是：

- 你的项目已经接入了 Flyway
- 你的数据库账号有执行建表、改表的权限

做法：

1. 确认数据库 `e_food` 已存在
2. 重启项目
3. 让 Flyway 自动执行迁移

### 方法 B：手工执行升级脚本

如果你的数据库权限不够，或者你想手工控制，就执行：

- [upgrade-existing-db.sql](/C:/Users/22390/Desktop/EFoodPass/sql/upgrade-existing-db.sql)

这个脚本会补齐订单支付字段、日志表、会话表和相关权限。

## 3. 报错 `ADD COLUMN IF NOT EXISTS` 语法不支持

这说明你当前 MySQL 版本或兼容模式不支持这种写法。

现在项目里的脚本已经改成兼容写法了，不应该再手敲这种旧 SQL：

```sql
ALTER TABLE sys_user
ADD COLUMN IF NOT EXISTS token_version INT NOT NULL DEFAULT 0;
```

正确做法：

1. 不要自己凭印象补旧 SQL
2. 直接执行项目里的最新版 [upgrade-existing-db.sql](/C:/Users/22390/Desktop/EFoodPass/sql/upgrade-existing-db.sql)
3. 或者直接重启项目，让 Flyway 跑迁移

## 4. Flyway 报错 `Detected failed migration to version 1`

这个问题的意思是：

- 你之前跑过一次迁移
- 但那次迁移失败了
- Flyway 在 `flyway_schema_history` 里记下了一条失败记录
- 所以后面每次启动都会先拦下来

最常见的开发环境处理方式：

```sql
USE e_food;
DROP TABLE IF EXISTS flyway_schema_history;
```

然后重新启动项目。

为什么这一步通常是安全的？

- `flyway_schema_history` 只记录迁移历史
- 它不是业务表
- 删除它不会直接删除订单、用户、菜品这些业务数据

注意：

- 这是开发环境常见处理办法
- 正式环境不要不看情况就删

如果你想先搞懂这个原理，再去处理，先看：

- [flyway-baby.md](/C:/Users/22390/Desktop/EFoodPass/docs/flyway-baby.md)
- [flyway-practice-baby.md](/C:/Users/22390/Desktop/EFoodPass/docs/flyway-practice-baby.md)

## 5. 打开 Swagger 出现 `Unable to render this definition`

这不一定是 Swagger 页面坏了。

很多时候真正的问题是：

- `/v3/api-docs` 返回的不是正常 OpenAPI JSON
- 而是一段后端报错 JSON

你应该先直接访问：

```text
http://localhost:5603/v3/api-docs
```

正常情况下，你应该看到一大段 OpenAPI JSON。

如果你看到的是类似这种：

```json
{
  "code": 500,
  "message": "system error: ..."
}
```

说明是后端生成文档时出错了，不是前端页面问题。

这个项目之前最常见的原因是 Swagger 依赖版本冲突。现在 [pom.xml](/C:/Users/22390/Desktop/EFoodPass/pom.xml) 已经做了统一版本处理。

如果你刚更新过代码但问题还在，最常见原因是：

- 你没有重启项目
- 旧进程还在跑旧依赖

处理方法：

1. 彻底停掉旧进程
2. 重新启动项目
3. 再打开 `http://localhost:5603/swagger-ui.html`

## 6. 访问 `/actuator/flyway` 返回 `401`

这不是 Flyway 坏了，这是你没登录。

当前项目的安全配置要求：

- 大多数接口都要先登录
- `/actuator/flyway` 也一样

所以直接浏览器打开：

```text
http://localhost:5603/actuator/flyway
```

如果返回：

```json
{
  "code": 401,
  "message": "未登录或token无效",
  "data": null
}
```

这是正常现象。

正确做法：

1. 先调用 `POST /auth/login`
2. 拿到 token
3. 再带 `Authorization: Bearer 你的token` 去访问 `/actuator/flyway`

可以参考：

- [baby-api-auth-and-user.md](/C:/Users/22390/Desktop/EFoodPass/docs/baby-api-auth-and-user.md)
- [flyway-practice-baby.md](/C:/Users/22390/Desktop/EFoodPass/docs/flyway-practice-baby.md)
- [baby-acceptance.http](/C:/Users/22390/Desktop/EFoodPass/scripts/baby-acceptance.http)

## 7. 登录成功了，但后续接口还是 `401`

最常见原因有 4 个：

1. 你没有带 `Authorization` 请求头
2. 你带的是旧 token
3. 你写成了 `Bearer` 之外的格式
4. 你刚改过密码或退出登录，旧 token 已失效

正确格式是：

```text
Authorization: Bearer 这里放你的token
```

注意中间有一个空格。

## 8. 页面打不开，但其实后端已经启动了

你要先确认是不是地址打错了。

当前项目常用地址：

- Swagger 页面：`http://localhost:5603/swagger-ui.html`
- OpenAPI JSON：`http://localhost:5603/v3/api-docs`

如果你访问的是旧地址或错误地址，自然会觉得“页面打不开”。

## 9. 项目启动了，但接口调不通

先做最小排查：

1. 先调 `POST /auth/login`
2. 再调 `GET /auth/me`
3. 再打开 Swagger
4. 再调 `GET /app/menu/tree`

如果这 4 步都正常，说明项目主体已经通了。

后面的购物车、下单、支付、后台处理、统计、日志，再一段一段排。

## 10. 不知道到底该查哪里

新手最稳的办法不是乱猜，而是按顺序查：

1. 看 [baby-start.md](/C:/Users/22390/Desktop/EFoodPass/docs/baby-start.md)
2. 看 [acceptance-baby.md](/C:/Users/22390/Desktop/EFoodPass/docs/acceptance-baby.md)
3. 用 [baby-acceptance.http](/C:/Users/22390/Desktop/EFoodPass/scripts/baby-acceptance.http) 一步一步调
4. 用 [flyway-check.sql](/C:/Users/22390/Desktop/EFoodPass/scripts/flyway-check.sql) 查数据库
5. 还不行，再回头看启动日志的第一条真实报错

## 11. 一句话记忆版

你可以只记这几句：

- `Unknown column`：数据库没升级
- `401`：大概率没登录或 token 无效
- Swagger 渲染失败：先看 `/v3/api-docs`
- Flyway 校验失败：先看 `flyway_schema_history`
- 不要凭记忆手敲 SQL，优先用项目里的脚本
