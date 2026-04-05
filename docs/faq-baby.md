# EFoodPass 宝宝级 FAQ

这份文档不讲长篇大论，只回答新手最常问的问题。

## 1. 我到底先看哪份文档？

最推荐顺序：

1. [baby-start.md](/C:/Users/22390/Desktop/EFoodPass/docs/baby-start.md)
2. [baby-operations.md](/C:/Users/22390/Desktop/EFoodPass/docs/baby-operations.md)
3. [baby-api-auth-and-user.md](/C:/Users/22390/Desktop/EFoodPass/docs/baby-api-auth-and-user.md)
4. [baby-api-food-order.md](/C:/Users/22390/Desktop/EFoodPass/docs/baby-api-food-order.md)
5. [acceptance-baby.md](/C:/Users/22390/Desktop/EFoodPass/docs/acceptance-baby.md)
6. 出问题再看 [troubleshooting-baby.md](/C:/Users/22390/Desktop/EFoodPass/docs/troubleshooting-baby.md)

## 2. 为什么项目端口是 `5603`？

因为当前项目默认开发配置就是这个端口。

配置位置：

- [application-dev.yml](/C:/Users/22390/Desktop/EFoodPass/src/main/resources/application-dev.yml)

所以常用地址都是基于 `5603`：

- Swagger：`http://localhost:5603/swagger-ui.html`
- OpenAPI：`http://localhost:5603/v3/api-docs`

## 3. 为什么 Swagger 能打开，但接口一调就是 `401`？

因为 Swagger 页面能打开，不代表接口不需要登录。

大多数业务接口都要先拿 token，再点右上角 `Authorize`，填：

```text
Bearer 你的token
```

注意中间有空格。

## 4. 为什么 `/actuator/flyway` 直接浏览器打开是 `401`？

因为它也受登录保护。

这不是故障，是正常安全设计。

正确做法：

1. 先调用 `POST /auth/login`
2. 拿到 token
3. 再带 `Authorization: Bearer token` 访问 `/actuator/flyway`

如果你不想手工拼请求头，可以直接参考：

- [baby-acceptance.http](/C:/Users/22390/Desktop/EFoodPass/scripts/baby-acceptance.http)
- [baby-login-and-flyway.ps1](/C:/Users/22390/Desktop/EFoodPass/scripts/baby-login-and-flyway.ps1)

## 5. 什么是 Flyway？

一句话版本：

Flyway 是“帮你自动升级数据库结构”的工具。

代码升级了，表结构也要升级。Flyway 就是专门干这个的。

详细版看：

- [flyway-baby.md](/C:/Users/22390/Desktop/EFoodPass/docs/flyway-baby.md)
- [flyway-practice-baby.md](/C:/Users/22390/Desktop/EFoodPass/docs/flyway-practice-baby.md)

## 6. 我已经有旧数据库了，还要重新建库吗？

不一定。

你有两种选法：

- 想最省事：新建一个空的 `e_food`，让 Flyway 自动建
- 想保留旧数据：执行 [upgrade-existing-db.sql](/C:/Users/22390/Desktop/EFoodPass/sql/upgrade-existing-db.sql)

如果你是新手，我更建议先拿空库跑通，再考虑旧库升级。

## 7. 为什么会报 `Unknown column 'payment_status'`？

因为代码已经升级了，但数据库表结构还是旧的。

这类问题基本就一句话：

数据库没升级。

先看：

- [troubleshooting-baby.md](/C:/Users/22390/Desktop/EFoodPass/docs/troubleshooting-baby.md)

## 8. 为什么会报 `Detected failed migration to version 1`？

因为你之前跑 Flyway 时失败过，`flyway_schema_history` 里留下了失败记录。

这是 Flyway 迁移历史的问题，不是订单业务本身的问题。

先看：

- [flyway-practice-baby.md](/C:/Users/22390/Desktop/EFoodPass/docs/flyway-practice-baby.md)
- [troubleshooting-baby.md](/C:/Users/22390/Desktop/EFoodPass/docs/troubleshooting-baby.md)

## 9. 为什么我明明登录成功了，后面还是 `401`？

最常见原因：

1. 没带 `Authorization`
2. token 复制错了
3. 没写 `Bearer `
4. 你刚退出登录或改过密码，旧 token 已失效

## 10. 我到底该用 `.http` 文件还是 PowerShell 脚本？

都可以。

区别很简单：

- `.http` 文件适合在 IDEA 里一条一条点着测
- `PowerShell` 脚本适合在 Windows 里快速完成“登录 -> 看当前用户 -> 看 Flyway”

当前项目建议这样分工：

- 接口全量验收：用 [baby-acceptance.http](/C:/Users/22390/Desktop/EFoodPass/scripts/baby-acceptance.http)
- 快速验证登录和 Flyway：用 [baby-login-and-flyway.ps1](/C:/Users/22390/Desktop/EFoodPass/scripts/baby-login-and-flyway.ps1)

## 11. `scripts/auth.http` 和 `scripts/baby-acceptance.http` 有什么区别？

- [auth.http](/C:/Users/22390/Desktop/EFoodPass/scripts/auth.http) 更像历史调试集合，范围更杂
- [baby-acceptance.http](/C:/Users/22390/Desktop/EFoodPass/scripts/baby-acceptance.http) 是给新手按顺序走完整链路用的

如果你是第一次接这个项目，优先用 `baby-acceptance.http`。

## 12. 我需要先学代码再跑项目吗？

不需要。

更稳的顺序是：

1. 先启动
2. 先登录
3. 先点接口
4. 跑通菜单、购物车、下单、支付、后台处理
5. 最后再回来看代码

这是最快建立整体感觉的办法。

## 13. 我怎么确认数据库升级真的成功了？

你可以查这几样：

1. `flyway_schema_history` 表在不在
2. `food_order` 里有没有 `payment_status`
3. `sys_login_log`、`sys_operation_log`、`sys_user_session` 在不在

可以直接用：

- [flyway-check.sql](/C:/Users/22390/Desktop/EFoodPass/scripts/flyway-check.sql)

## 14. 我怎么确认项目真的“跑通了”？

最小标准是这几件事都通过：

1. 项目能启动
2. Swagger 能打开
3. `POST /auth/login` 正常
4. `GET /auth/me` 正常
5. `GET /app/menu/tree` 能看到菜单
6. 购物车、下单、支付能走通
7. 后台能处理订单
8. 日志和统计能查到数据

直接按这个做：

- [acceptance-baby.md](/C:/Users/22390/Desktop/EFoodPass/docs/acceptance-baby.md)

## 15. 我真的卡住了，最该先贴什么信息给别人看？

优先级最高的是：

1. 启动日志里第一段真实报错
2. 你访问的具体地址
3. 你调用的具体接口
4. 返回的状态码和返回体
5. 你当前执行到哪一步

不要只说一句“打不开”或者“报错了”，那样排查效率会很低。
