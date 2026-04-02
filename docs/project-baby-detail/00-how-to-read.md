# 00 如何使用这套教程

这一章不讲业务，只讲学习方法。

如果你直接跳过去，后面很容易又回到以前那种感觉：

- 看了一堆代码
- 好像都认识
- 但是自己写不出来

所以这一章很重要。

## 一 你现在真正缺的不是“更多代码”

你现在缺的是下面这 4 个能力：

1. 把一个请求拆成层次。
2. 看到字段时知道它是“存数据的”还是“传数据的”。
3. 看到注解时知道它影响的是哪一层。
4. 知道一个类为什么存在，而不是只知道它“能运行”。

这套教程就是围绕这 4 件事写的。

## 二 这套项目可以粗暴分成哪几层

先不要一上来就看所有文件。

你先只记住这几个层次：

### 1. 启动与配置层

代表文件：

- [EFoodPassApplication.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/EFoodPassApplication.java)
- [MybatisPlusConfig.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/config/MybatisPlusConfig.java)
- [PasswordConfig.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/config/PasswordConfig.java)
- [OpenApiConfig.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/config/OpenApiConfig.java)

### 2. 公共基础层

代表文件：

- [Result.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/common/result/Result.java)
- [PageQuery.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/common/page/PageQuery.java)
- [PageResult.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/common/page/PageResult.java)
- [BusinessException.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/common/exception/BusinessException.java)
- [GlobalExceptionHandler.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/common/exception/GlobalExceptionHandler.java)

### 3. 安全认证层

代表文件：

- [SecurityConfig.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/config/security/SecurityConfig.java)
- [JwtAuthenticationFilter.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/config/security/JwtAuthenticationFilter.java)
- [JwtTokenProvider.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/config/security/JwtTokenProvider.java)
- [LoginUser.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/config/security/LoginUser.java)
- [AuthController.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/modules/auth/controller/AuthController.java)
- [AuthServiceImpl.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/modules/auth/service/impl/AuthServiceImpl.java)

### 4. 业务模块层

这里又分两块：

- system：用户、角色、权限
- food：分类、菜品、订单、库存

## 三 你读代码的顺序不要乱

正确顺序是：

1. 先看 controller
2. 再看 request/response DTO
3. 再看 service 接口
4. 最后看 service impl

原因很简单：

- controller 告诉你入口
- DTO 告诉你请求长什么样
- service 接口告诉你能力边界
- impl 才是具体实现

## 四 你遇到字段时，要先判断它属于哪一类

### 1. 数据库实体字段

比如 [SysUser.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/modules/system/user/entity/SysUser.java) 里的：

- `id`
- `username`
- `passwordHash`
- `status`
- `tokenVersion`

### 2. 请求 DTO 字段

比如 [LoginRequest.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/modules/auth/dto/LoginRequest.java) 里的：

- `username`
- `password`

### 3. 响应 DTO 字段

比如 `LoginResponse`、`CurrentUserResponse` 里的字段。

### 4. 上下文字段

比如 [LoginUser.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/config/security/LoginUser.java) 里的：

- `userId`
- `username`
- `nickname`

## 五 你遇到注解时，要先判断它影响哪一层

### 1. Spring Bean 相关

- `@Configuration`
- `@Service`
- `@Component`
- `@RestController`

### 2. Web 接口相关

- `@RequestMapping`
- `@GetMapping`
- `@PostMapping`
- `@RequestBody`
- `@PathVariable`

### 3. 校验相关

- `@Valid`
- `@NotBlank`
- `@NotNull`
- `@Min`
- `@DecimalMin`

### 4. 权限相关

- `@PreAuthorize`

### 5. MyBatis Plus 相关

- `@TableName`
- `@TableId`

## 六 你要怎么复刻

不要试图一次重写全项目。

正确做法是按这个顺序：

1. 先只复刻统一返回和异常
2. 再复刻登录和 JWT
3. 再复刻 system 用户模块
4. 再复刻 food 分类和菜品
5. 最后复刻订单和库存

## 七 这一章看完之后你该做什么

在 IDE 里自己跟一遍下面这条链：

- [AuthController.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/modules/auth/controller/AuthController.java)
- [AuthServiceImpl.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/modules/auth/service/impl/AuthServiceImpl.java)
- [JwtTokenProvider.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/config/security/JwtTokenProvider.java)

只跟这一条就够。
