# EFoodPass 宝宝级 Flyway 教科书

这份文档只讲一件事：

- 什么是 Flyway
- 它在这个项目里到底干什么
- 为什么你会看到 `flyway_schema_history`
- 为什么 `http://localhost:5603/actuator/flyway` 会返回 `401`

如果你是第一次听到 Flyway，这份就是给你看的。

## 1. 先用最简单的话说：Flyway 是干什么的

Flyway 是一个“数据库升级管家”。

你可以这样记：

- `Java 代码` 是项目的大脑
- `MySQL 数据库` 是项目的仓库
- `Flyway` 是专门负责“仓库装修升级”的管理员

如果没有 Flyway，经常会发生这种事：

1. 代码已经更新了
2. 代码里开始使用新字段了
3. 但数据库还停在老样子
4. 一启动就报错

你前面遇到的这个报错，就是最标准的例子：

```text
Unknown column 'payment_status' in 'field list'
```

意思就是：

- 代码已经要用 `payment_status`
- 但数据库里根本还没有这列

Flyway 的作用，就是避免这种“代码和数据库不同步”的问题。

## 2. 不用专业术语的话，Flyway 像什么

你可以把项目想成一家外卖店。

### 没有 Flyway 的世界

店里原来只有：

- 菜单
- 订单
- 用户

后来你给系统加了新功能：

- 支付状态
- 登录日志
- 操作日志
- 用户会话

如果没有 Flyway，就只能靠人脑记：

- 这个表加一列
- 那个表再加一列
- 再建两个新表
- 再补几条权限数据

问题是，人很容易漏。

### 有了 Flyway 的世界

每次店铺升级，你都写一张“装修清单”：

- `V1__...sql`
- `V2__...sql`
- `V3__...sql`

Flyway 每次启动时就会说：

- 我看看你这个店现在装修到第几版了
- 没做过的升级我补上
- 做过的我就不重复做

这就是它最核心的价值。

## 3. Flyway 在这个项目里现在怎么用

当前项目的迁移文件放在这里：

- [db/migration](/C:/Users/22390/Desktop/EFoodPass/src/main/resources/db/migration)

现在你会看到两份：

- [V1__bootstrap_current_schema.sql](/C:/Users/22390/Desktop/EFoodPass/src/main/resources/db/migration/V1__bootstrap_current_schema.sql)
- [V2__upgrade_legacy_schema.sql](/C:/Users/22390/Desktop/EFoodPass/src/main/resources/db/migration/V2__upgrade_legacy_schema.sql)

你先这样理解：

- `V1`：给空数据库用，相当于“一次性把当前完整店铺搭好”
- `V2`：给老数据库升级用，相当于“补上后来新增的装修内容”

## 4. 文件名为什么一定是 `V1`、`V2`

因为 Flyway 靠这个判断执行顺序。

比如：

- `V1__bootstrap_current_schema.sql`
- `V2__upgrade_legacy_schema.sql`
- `V3__add_coupon_tables.sql`

它会按数字顺序执行。

也就是说：

- 先做 `V1`
- 再做 `V2`
- 再做 `V3`

所以这里的 `V`，你可以直接把它理解成：

- `Version`
- 版本号

## 5. Flyway 怎么知道自己执行到哪一步了

它会在数据库里维护一张表：

- `flyway_schema_history`

这张表不是业务表。

它只是 Flyway 自己的“施工记录本”。

里面会记这些东西：

- 哪个版本执行过
- 哪个脚本执行过
- 执行成功还是失败
- 什么时候执行的

所以如果你看到它，不要害怕。

它不是异常表，也不是垃圾表。

它就是 Flyway 的记账本。

## 6. 为什么你前面会报 Flyway 错

你前面遇到过这种报错：

```text
Detected failed migration to version 1
```

翻成人话就是：

- Flyway 之前想帮你升级数据库
- 但那次升级没有成功
- 它把“失败记录”写进了 `flyway_schema_history`
- 下次启动时，它先查到自己上次失败了
- 所以它不敢继续乱跑，先拦下来

这不是 Flyway 在找你麻烦。

恰恰相反，它是在保护你的库，避免“半升级状态”越来越乱。

## 7. 为什么这个项目要把老库和新库分成 `V1`、`V2`

因为新库和老库不是一个场景。

### 场景 A：新库

如果数据库是空的：

- 直接执行 `V1`
- 把完整结构建出来
- 再执行 `V2`
- 补齐当前版本额外需要的增量内容

### 场景 B：老库

如果数据库本来就有旧表：

- 不适合再把 `V1` 当成“从零开始建库脚本”硬跑一遍
- 所以项目现在会把老库 baseline 到 `1`
- 然后只继续跑 `V2`

你可以把它理解成：

- 新店从毛坯房开始装修
- 老店只做增量改造

## 8. 什么叫 baseline

这是 Flyway 里最容易把新手绕晕的词。

你就把它理解成一句话：

- “从这一版开始记账”

比如老数据库本来就已经有一堆旧表了，但从来没接过 Flyway。

这时候 Flyway 会说：

- 好，那我先默认你已经站在 `V1`
- 从现在开始，我接手继续往后管

这就叫 baseline。

所以：

- `baseline` 不是执行 SQL
- `baseline` 更像是“立一个起跑线”

## 9. 现在项目里的 baseline 是怎么配的

配置在这里：

- [application.yaml](/C:/Users/22390/Desktop/EFoodPass/src/main/resources/application.yaml)

核心配置：

```yml
spring:
  flyway:
    enabled: true
    baseline-on-migrate: true
    baseline-version: 1
    locations: classpath:db/migration
```

这四行你先这样记：

- `enabled: true`
  说明启用了 Flyway

- `baseline-on-migrate: true`
  说明老库也允许接入 Flyway

- `baseline-version: 1`
  说明老库会从 `V1` 这条线开始记账

- `locations: classpath:db/migration`
  说明去哪个目录找迁移文件

## 10. 为什么访问 `/actuator/flyway` 会返回 401

这不是 Flyway 坏了。

这是因为这个项目开了 Spring Security。

当前安全配置在：

- [SecurityConfig.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/config/security/SecurityConfig.java)

里面只放行了这些：

- `/auth/login`
- `/v3/api-docs/**`
- `/swagger-ui/**`
- `/swagger-ui.html`
- `/test/**`

其他接口默认都要登录。

而：

- `/actuator/flyway`

不在放行名单里，所以直接浏览器打开时，没有带 token，就会返回：

```json
{
  "code": 401,
  "message": "未登录或token无效",
  "data": null
}
```

这个现象是正常的，不是异常。

## 11. 那我怎么正确查看 `/actuator/flyway`

最简单的正确方式有两种。

### 方式 A：用 Swagger 先登录，再用带 token 的工具访问

1. 先调用 `POST /auth/login`
2. 拿到 token
3. 用 Postman、Apifox、curl 带上 `Authorization: Bearer <token>`
4. 再访问 `/actuator/flyway`

### 方式 B：先不看接口，直接看数据库表

去数据库里查：

- `flyway_schema_history`

这也是最直接的方法。

## 12. 为什么我不建议把 `/actuator/flyway` 直接改成公开

因为这个接口会暴露数据库迁移信息。

里面通常会包含：

- 版本号
- 脚本名
- 执行状态

对开发者很有用，但不适合随便裸奔在公网。

所以当前返回 `401`，从安全角度看其实是合理的。

## 13. Flyway 最常见的 4 种报错

### 13.1 数据库字段不存在

比如：

```text
Unknown column 'payment_status'
```

意思通常是：

- 代码升级了
- 但数据库升级没跟上

### 13.2 检测到失败迁移

比如：

```text
Detected failed migration to version 1
```

意思通常是：

- 之前迁移跑到一半失败了
- Flyway 历史表里留下了失败记录

### 13.3 checksum 校验失败

意思通常是：

- 你改了一个已经执行过的旧版本脚本

这也是为什么我一直强调：

- 不要随便改已经执行过的 `V1`

### 13.4 访问 `/actuator/flyway` 返回 401

意思通常是：

- 你没带 token
- 不是 Flyway 自己出错

## 14. 以后如果再加数据库新功能，正确姿势是什么

假设你以后又加了一个新功能，比如优惠券。

正确姿势不是：

- 直接改 `V1`

而是：

- 新建一个新的版本文件

比如：

```text
V3__add_coupon_tables.sql
```

这样做的好处是：

- 旧环境能升级
- 新环境能重放
- 每一步变更都能追踪

## 15. 最后用一句最适合新手的话总结 Flyway

Flyway 就是：

- 帮你记住数据库已经升级到哪一步
- 帮你自动把数据库升级到代码需要的版本
- 防止“代码变了，数据库没变”

如果你以后只记住一句话，就记这句。
