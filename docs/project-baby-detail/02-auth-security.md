# 02 登录、JWT、Spring Security 在这个项目里怎么串起来

这一章是整个项目最重要的一章之一。

重点文件：

- [AuthController.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/modules/auth/controller/AuthController.java)
- [LoginRequest.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/modules/auth/dto/LoginRequest.java)
- [AuthServiceImpl.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/modules/auth/service/impl/AuthServiceImpl.java)
- [SecurityConfig.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/config/security/SecurityConfig.java)
- [JwtAuthenticationFilter.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/config/security/JwtAuthenticationFilter.java)
- [JwtTokenProvider.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/config/security/JwtTokenProvider.java)
- [LoginUser.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/config/security/LoginUser.java)

## 一 登录接口入口在哪里

看 [AuthController.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/modules/auth/controller/AuthController.java)。

### 类上的注解

#### `@RestController`

表示这是一个返回 JSON 的控制器。

#### `@RequestMapping("/auth")`

表示这个类下面的接口都带 `/auth` 前缀。

### 登录方法

```java
@PostMapping("/login")
public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request)
```

逐个看：

- `@PostMapping("/login")`：POST `/auth/login`
- `@RequestBody`：从 JSON 请求体取参数
- `@Valid`：让 `LoginRequest` 上的校验生效

## 二 `LoginRequest` 为什么要单独写一个 DTO

看 [LoginRequest.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/modules/auth/dto/LoginRequest.java)。

它只有两个字段：

- `username`
- `password`

### `@NotBlank`

作用是：

- 不能是 `null`
- 不能是空字符串
- 不能全是空格

### `@Schema`

作用是：

- 给 Swagger 文档看
- 说明字段含义和示例

## 三 `AuthServiceImpl.login()` 到底干了什么

看 [AuthServiceImpl.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/modules/auth/service/impl/AuthServiceImpl.java)。

可以拆成 5 步：

1. 根据用户名查用户
2. 用户不存在就报错
3. 检查用户状态是不是启用
4. 用 `PasswordEncoder` 校验密码
5. 用 `JwtTokenProvider` 生成 token

### 为什么数据库里不是 `password`

数据库里存的是 `passwordHash`。

也就是说：

- 前端传原始密码
- 后端只存加密结果

## 四 为什么 token 里还放 `tokenVersion`

这是这个项目认证设计里一个很好的点。

当用户重置密码时，可以把 `tokenVersion + 1`。

这样旧 token 立即失效，不必等它自己过期。

## 五 `JwtTokenProvider` 是做什么的

看 [JwtTokenProvider.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/config/security/JwtTokenProvider.java)。

它主要负责两件事：

1. `createToken(...)`
2. `parseToken(...)`

### `createToken(...)`

它会把这些内容写进 JWT：

- `subject`：用户 id
- `username`
- `tokenVersion`
- 签发时间
- 过期时间

### `parseToken(...)`

它会做的事很简单：

- 用密钥验证 token
- 解析出 claims

## 六 `SecurityConfig` 在整条链里是什么位置

看 [SecurityConfig.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/config/security/SecurityConfig.java)。

这个类定义的是整个系统的安全规则。

### 类上的注解

- `@Configuration`：配置类
- `@EnableWebSecurity`：启用 Web 安全
- `@EnableMethodSecurity`：启用 `@PreAuthorize`
- `@EnableConfigurationProperties(JwtProperties.class)`：让 JWT 配置类可读取配置文件

### `securityFilterChain(...)` 干了什么

你可以拆成 5 块：

1. 关闭 CSRF
2. 设置无状态 session
3. 配未登录和无权限处理器
4. 指定哪些请求可以放行
5. 把 JWT 过滤器加进过滤器链

## 七 `JwtAuthenticationFilter` 为什么这么关键

看 [JwtAuthenticationFilter.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/config/security/JwtAuthenticationFilter.java)。

它继承了 `OncePerRequestFilter`，意思是每个请求执行一次。

### `doFilterInternal(...)` 的主要流程

1. 从请求头拿 token
2. 解析 token
3. 取出 userId 和 tokenVersion
4. 查数据库拿用户
5. 查角色
6. 查权限
7. 组装 `LoginUser` 和 `authorities`
8. 放入 `SecurityContextHolder`

### 为什么解析出 token 后还要查数据库

因为系统不能只信 token 里的内容。

还要确认：

- 用户还存在
- 用户没被禁用
- tokenVersion 还匹配

## 八 `LoginUser` 为什么单独存在

看 [LoginUser.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/config/security/LoginUser.java)。

它只有：

- `userId`
- `username`
- `nickname`

它不是数据库实体。

它是“当前登录用户上下文对象”。

为什么不直接塞整个 `SysUser`？

因为没必要。

当前请求最常用的只是一小部分信息。

## 九 `@PreAuthorize` 为什么能挡住接口

比如 controller 里经常能看到：

```java
@PreAuthorize("hasAuthority('system:user:list')")
```

意思是：

- 当前用户必须有这个权限编码
- 才能执行这个方法

而这个权限编码列表，正是 JWT 过滤器提前查好并塞到 `SecurityContextHolder` 里的。

## 十 这一章最建议你自己做的练习

你自己回答下面 3 个问题：

1. 为什么 `AuthController.login()` 不直接操作 JWT，而是交给 `AuthServiceImpl`
2. 为什么 `JwtAuthenticationFilter` 解析出 token 后还要查数据库
3. 为什么 `LoginUser` 单独建类，而不是直接用 `SysUser`
