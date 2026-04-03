# EFoodPass 宝宝级启动教程

这份教程是写给第一次碰 Java 后端项目的人看的。

你不用先懂 Spring Boot、Spring Security、JWT、MyBatis-Plus。你只要照着做，先把项目跑起来就行。

## 1. 你要先准备什么

先确认电脑里有这几个东西：

- `Java 17`
- `MySQL 8`
- `Redis`
- 一个能打开这个项目的 IDE，比如 IntelliJ IDEA

如果你不确定自己有没有装好，可以在终端里自己检查：

```powershell
java -version
mysql --version
redis-server --version
```

只要能看到版本号，通常就说明装上了。

## 2. 这个项目默认用什么配置

项目默认启动的是 `dev` 环境，对应文件是：

- [application.yaml](/C:/Users/22390/Desktop/EFoodPass/src/main/resources/application.yaml)
- [application-dev.yml](/C:/Users/22390/Desktop/EFoodPass/src/main/resources/application-dev.yml)

默认信息如下：

- 服务端口：`5603`
- 数据库：`e_food`
- MySQL 地址：`localhost:3306`
- Redis 地址：`localhost:6379`
- Swagger 地址：`http://localhost:5603/swagger-ui.html`

## 3. 第一步：创建数据库

打开你的 MySQL，新建一个数据库，名字叫：

```sql
CREATE DATABASE e_food DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
```

如果你已经有这个库了，这一步可以跳过。

## 4. 第二步：导入初始化 SQL

这个项目最重要的初始化脚本是：

- [sql/init-rbac.sql](/C:/Users/22390/Desktop/EFoodPass/sql/init-rbac.sql)

它做了两件大事：

- 帮你建表
- 帮你插入初始管理员、角色、权限等基础数据

### 导入方法 A：用数据库工具导入

如果你用的是 Navicat、DataGrip、DBeaver，最简单：

1. 连接到你的 MySQL
2. 选中数据库 `e_food`
3. 打开 `sql/init-rbac.sql`
4. 点击执行

### 导入方法 B：用命令行导入

```powershell
mysql -u root -p e_food < sql/init-rbac.sql
```

执行后输入你的 MySQL 密码即可。

## 5. 第三步：改配置文件

打开这个文件：

- [src/main/resources/application-dev.yml](/C:/Users/22390/Desktop/EFoodPass/src/main/resources/application-dev.yml)

你至少要检查这三块：

### 数据库配置

```yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/e_food?...
    username: root
    password: 你的数据库密码
```

如果你的数据库名不是 `e_food`，或者账号不是 `root`，就把这里改成你自己的。

### Redis 配置

```yml
spring:
  data:
    redis:
      host: localhost
      port: 6379
```

如果你的 Redis 不在本机，或者端口不是 `6379`，这里也要改。

### AI 配置

```yml
spring:
  ai:
    openai:
      api-key: 你自己的 key
```

如果你后面要体验 AI 相关接口，这里最好换成你自己的可用 key。

如果你暂时只是想先跑基础点餐和后台管理，也建议先把这块检查一遍，避免因为配置问题影响启动。

## 6. 第四步：启动 Redis 和 MySQL

在启动项目之前，先保证：

- MySQL 已经启动
- Redis 已经启动

如果这两个没启动，项目大概率会直接报错。

## 7. 第五步：启动项目

在项目根目录打开终端，执行：

```powershell
mvnw.cmd spring-boot:run
```

第一次启动会比较慢，这是正常的。

只要最后看到类似下面这种信息，通常就说明启动成功了：

```text
Started EFoodPassApplication
```

## 8. 第六步：打开 Swagger

启动成功后，打开浏览器访问：

```text
http://localhost:5603/swagger-ui.html
```

你会看到接口文档页面。

这个页面就是新手最友好的入口，因为你不用先写前端，就能直接测试所有接口。

## 9. 第七步：先登录

默认管理员账号：

- 用户名：`admin`
- 密码：`Admin@123`

登录接口是：

```text
POST /auth/login
```

请求体示例：

```json
{
  "username": "admin",
  "password": "Admin@123"
}
```

登录成功后，你会拿到一个 `token`。

## 10. 第八步：把 token 放进 Swagger

很多接口都需要登录后才能调用。

操作方法：

1. 点击 Swagger 页面右上角的 `Authorize`
2. 把登录返回的 token 粘进去
3. 确认授权

之后你再点需要鉴权的接口，就会自动带上这个 token。

## 11. 第九步：确认项目真的跑通了

建议你按这个最简单的顺序自检：

1. 调 `POST /auth/login`
2. 调 `GET /auth/me`
3. 调 `GET /food/category/list`
4. 调 `GET /food/item/list`
5. 调 `GET /food/order/list`

只要这些都能正常返回，说明项目主体已经跑通。

## 12. 常见报错怎么查

### 报错 1：数据库连不上

常见原因：

- MySQL 没启动
- 用户名或密码写错
- 数据库 `e_food` 没创建
- `application-dev.yml` 里的地址写错

先检查：

- [application-dev.yml](/C:/Users/22390/Desktop/EFoodPass/src/main/resources/application-dev.yml)
- MySQL 服务状态

### 报错 2：Redis 连不上

常见原因：

- Redis 没启动
- 端口不是 `6379`
- 配置文件里的 host/port 不对

### 报错 3：登录提示账号密码不对

先确认你已经执行过：

- [sql/init-rbac.sql](/C:/Users/22390/Desktop/EFoodPass/sql/init-rbac.sql)

默认管理员是这个：

- 用户名：`admin`
- 密码：`Admin@123`

### 报错 4：打开 Swagger 是 404

先检查：

- 项目是否真的启动成功
- 端口是不是 `5603`
- 地址是不是 `http://localhost:5603/swagger-ui.html`

### 报错 5：接口返回 401

意思通常是：

- 你还没登录
- 你的 token 没带上
- 你的 token 过期了
- 你已经被登出

解决方法：

1. 重新调用 `POST /auth/login`
2. 重新点 Swagger 的 `Authorize`
3. 再试一次接口

### 报错 6：接口返回 403

意思通常是你登录了，但没有权限。

比如普通用户和管理员看到的接口就不一样。

## 13. 如果你已经启动成功，下一步看哪里

下一份教程在这里：

- [docs/baby-operations.md](/C:/Users/22390/Desktop/EFoodPass/docs/baby-operations.md)

它会带你从后台管理、用户点餐、订单处理三个角度，把主要功能都走一遍。

如果你不想自己猜请求体，还可以继续看这两份“照抄型”教程：

- [docs/baby-api-auth-and-user.md](/C:/Users/22390/Desktop/EFoodPass/docs/baby-api-auth-and-user.md)
- [docs/baby-api-food-order.md](/C:/Users/22390/Desktop/EFoodPass/docs/baby-api-food-order.md)
