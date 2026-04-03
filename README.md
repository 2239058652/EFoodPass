# EFoodPass

一个适合练手和二次开发的 Spring Boot 点餐后台项目。

这套项目现在已经包含这些主要能力：

- 登录、登出、刷新 token、会话管理
- 用户、角色、权限管理
- 菜品分类和菜品管理
- 用户端菜单、购物车、下单、支付、取消订单
- 后台订单处理、退款、导出、统计
- 登录日志、操作日志
- Flyway 自动数据库迁移

如果你是第一次接触这个项目，不要先去看代码。先把项目跑起来，再照着 Swagger 点一遍，你会轻松很多。

## 最快上手

1. 安装好 `Java 17`、`MySQL 8`、`Redis`
2. 在 MySQL 里新建数据库：`e_food`
3. 打开 [src/main/resources/application-dev.yml](/C:/Users/22390/Desktop/EFoodPass/src/main/resources/application-dev.yml)，把数据库、Redis、AI 配置改成你自己的
4. 只需要保证数据库 `e_food` 已经创建好
5. 在项目根目录运行：`mvnw.cmd spring-boot:run`
6. 首次启动会自动执行 Flyway 迁移
7. 浏览器打开：`http://localhost:5603/swagger-ui.html`

如果你的数据库是以前旧版本留下来的，不想重建库，可以执行：

- [sql/upgrade-existing-db.sql](/C:/Users/22390/Desktop/EFoodPass/sql/upgrade-existing-db.sql)

默认开发环境：

- Profile：`dev`
- 端口：`5603`
- Swagger：`http://localhost:5603/swagger-ui.html`
- 管理员账号：`admin`
- 管理员密码：`Admin@123`

## 宝宝级教程

- 零基础启动教程：[docs/baby-start.md](/C:/Users/22390/Desktop/EFoodPass/docs/baby-start.md)
- 零基础操作教程：[docs/baby-operations.md](/C:/Users/22390/Desktop/EFoodPass/docs/baby-operations.md)
- 认证与会话接口样例教程：[docs/baby-api-auth-and-user.md](/C:/Users/22390/Desktop/EFoodPass/docs/baby-api-auth-and-user.md)
- 点餐与订单接口样例教程：[docs/baby-api-food-order.md](/C:/Users/22390/Desktop/EFoodPass/docs/baby-api-food-order.md)
- Flyway 宝宝级教科书：[docs/flyway-baby.md](/C:/Users/22390/Desktop/EFoodPass/docs/flyway-baby.md)
- Flyway 实战宝宝教程：[docs/flyway-practice-baby.md](/C:/Users/22390/Desktop/EFoodPass/docs/flyway-practice-baby.md)
- 数据库迁移说明：[docs/db-migration.md](/C:/Users/22390/Desktop/EFoodPass/docs/db-migration.md)

## 项目目录怎么认

- [src/main/java](/C:/Users/22390/Desktop/EFoodPass/src/main/java)：Java 代码
- [src/main/resources](/C:/Users/22390/Desktop/EFoodPass/src/main/resources)：配置文件
- [sql](/C:/Users/22390/Desktop/EFoodPass/sql)：建表和初始化脚本
- [scripts/auth.http](/C:/Users/22390/Desktop/EFoodPass/scripts/auth.http)：接口调试样例
- [docs](/C:/Users/22390/Desktop/EFoodPass/docs)：文档

## 启动前一定看

- 项目默认连接本机 MySQL：`localhost:3306/e_food`
- 项目默认连接本机 Redis：`localhost:6379`
- 项目默认使用 `dev` 环境，配置文件是 [application-dev.yml](/C:/Users/22390/Desktop/EFoodPass/src/main/resources/application-dev.yml)
- 如果你改了数据库名、账号、密码，记得同步改配置文件
- 如果 AI 配置无效，和 AI 相关的功能可能报错，所以最好先换成你自己的可用 key

## 推荐启动顺序

1. 先创建空数据库
2. 再改配置文件
3. 再启动后端，让 Flyway 自动迁移
4. 最后打开 Swagger 测接口

## 现在最适合怎么学

建议按这个顺序：

1. 先看 [docs/baby-start.md](/C:/Users/22390/Desktop/EFoodPass/docs/baby-start.md)，把项目启动成功
2. 再看 [docs/baby-operations.md](/C:/Users/22390/Desktop/EFoodPass/docs/baby-operations.md)，把完整流程点通
3. 再看 [docs/baby-api-auth-and-user.md](/C:/Users/22390/Desktop/EFoodPass/docs/baby-api-auth-and-user.md) 和 [docs/baby-api-food-order.md](/C:/Users/22390/Desktop/EFoodPass/docs/baby-api-food-order.md)，直接照抄接口样例
4. 最后再回来看控制器和服务层代码
