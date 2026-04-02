# 08 常用注解和代码套路速查

这一章不是新业务，而是把前面反复出现的注解和套路集中讲一遍。

如果你读前面某章时突然卡住了，可以回这章查。

## 一 最常见的类级注解

### `@SpringBootApplication`

常见位置：

- [EFoodPassApplication.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/EFoodPassApplication.java)

作用：

- 告诉 Spring Boot 这是启动类
- 开启自动配置
- 开始扫描组件

### `@Configuration`

常见位置：

- [SecurityConfig.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/config/security/SecurityConfig.java)
- [MybatisPlusConfig.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/config/MybatisPlusConfig.java)
- [PasswordConfig.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/config/PasswordConfig.java)

作用：

- 这个类是配置类
- 里面通常会定义 `@Bean`

### `@RestController`

常见位置：

- 各种 controller

作用：

- 表示这是 Web 控制器
- 方法返回值默认按 JSON 输出

### `@Service`

常见位置：

- 各种 `ServiceImpl`

作用：

- 这个类是业务服务类
- 交给 Spring 容器管理

### `@Component`

常见位置：

- 过滤器、工具类、某些通用组件

作用：

- 这也是 Spring Bean
- 只是语义比 `@Service` 更通用

## 二 最常见的接口映射注解

### `@RequestMapping`

作用：

- 给整个类加统一路径前缀

比如 [AuthController.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/modules/auth/controller/AuthController.java) 上的：

```java
@RequestMapping("/auth")
```

### `@GetMapping`

作用：

- 处理 GET 请求

### `@PostMapping`

作用：

- 处理 POST 请求

### `@PutMapping`

作用：

- 处理 PUT 请求

### `@DeleteMapping`

作用：

- 处理 DELETE 请求

## 三 参数注解最容易混的几个

### `@RequestBody`

作用：

- 从 JSON 请求体里取参数

最常见于：

- 新增
- 修改
- 登录

### `@PathVariable`

作用：

- 从 URL 路径里取参数

比如：

```java
@GetMapping("/{id}")
public Result<?> detail(@PathVariable Long id)
```

### `@Valid`

作用：

- 让 DTO 上的校验注解真正生效

如果没有 `@Valid`，很多 `@NotBlank`、`@NotNull` 不会自动帮你拦。

## 四 校验注解最常见的几个

### `@NotBlank`

适合字符串。

表示：

- 不能是 `null`
- 不能是空字符串
- 不能全是空格

### `@NotNull`

表示：

- 不能是 `null`

但它不管是不是空字符串。

### `@NotEmpty`

常用于集合或字符串。

表示：

- 不能是空集合
- 不能是空字符串

### `@Min`

表示数值不能小于指定值。

### `@DecimalMin`

和 `@Min` 类似，但更适合小数。

## 五 权限相关注解

### `@PreAuthorize`

常见位置：

- 各种 controller 方法

例子：

```java
@PreAuthorize("hasAuthority('system:user:list')")
```

意思是：

- 当前用户必须有这个权限编码
- 否则这个接口不能执行

## 六 数据库映射相关注解

### `@TableName`

作用：

- 指定实体类映射哪张表

### `@TableId`

作用：

- 指定主键字段
- 指定主键生成方式

例子：

```java
@TableId(type = IdType.AUTO)
private Long id;
```

意思是数据库自增主键。

## 七 Lombok 注解为什么到处都是

### `@Data`

作用：

- 自动生成 getter
- 自动生成 setter
- 自动生成 `toString`
- 自动生成 `equals/hashCode`

### `@AllArgsConstructor`

作用：

- 自动生成全参构造方法

### `@NoArgsConstructor`

作用：

- 自动生成无参构造方法

## 八 这个项目里最常见的 8 个代码套路

### 套路 1 controller 只做转发

几乎都是：

1. 接参数
2. 调 service
3. 返回 `Result.success(...)`

### 套路 2 DTO 做基础校验

比如：

- 非空
- 最小值
- 字符串不能为空白

### 套路 3 service 做业务校验

比如：

- 用户是否存在
- 状态值是否合法
- 库存是否足够
- 角色是否启用

### 套路 4 私有校验方法收规则

比如：

- `validateUserStatus`
- `validatePrice`
- `validateStock`
- `validateOrderStatus`

### 套路 5 `getRequiredXxx(...)`

比如：

- `getRequiredOrder`
- `getRequiredItem`
- `getRequiredCategory`

意思通常是：

- 查对象
- 查不到就直接抛业务异常

### 套路 6 分页统一模板

典型步骤是：

1. query 为 null 就 new 默认对象
2. 用 `LambdaQueryWrapper` 拼条件
3. `Page<T>` 分页
4. 实体转 response DTO
5. 组装 `PageResult`

### 套路 7 覆盖式分配关系

比如：

- 用户分配角色
- 角色分配权限

常见做法是：

1. 先删旧关系
2. 再插新关系

### 套路 8 事务包住多表联动

最典型的是订单创建和取消。

因为会同时操作多张表，所以要用 `@Transactional`。

## 九 这章怎么用最合适

建议不是从头顺读，而是：

- 读前面章节时卡住某个注解
- 再回来翻这一章

它更像一本小字典。
