# 01 启动类、公共层、配置层怎么读

这一章讲 3 类东西：

1. 项目怎么启动
2. 公共返回和异常怎么统一
3. 几个基础配置类是干什么的

对应重点文件：

- [EFoodPassApplication.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/EFoodPassApplication.java)
- [Result.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/common/result/Result.java)
- [PageQuery.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/common/page/PageQuery.java)
- [PageResult.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/common/page/PageResult.java)
- [BusinessException.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/common/exception/BusinessException.java)
- [GlobalExceptionHandler.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/common/exception/GlobalExceptionHandler.java)
- [MybatisPlusConfig.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/config/MybatisPlusConfig.java)
- [PasswordConfig.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/config/PasswordConfig.java)
- [OpenApiConfig.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/config/OpenApiConfig.java)

## 一 启动类到底做了什么

先看 [EFoodPassApplication.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/EFoodPassApplication.java)。

### 1. `@SpringBootApplication`

这是 Spring Boot 最核心的启动注解。

你可以先把它粗暴理解成 3 件事的组合：

- 这是配置类
- 开启自动配置
- 开始组件扫描

### 2. `@MapperScan("com.epass.food.modules.**.mapper")`

这个注解是给 MyBatis 用的。

它的作用是：

- 告诉 Spring：`mapper` 接口需要被扫描
- 扫到后，MyBatis 会帮你生成代理对象

### 3. `main` 方法

最关键的一行是：

```java
SpringApplication.run(EFoodPassApplication.class, args);
```

你可以先把它理解成：

- 创建 Spring 容器
- 初始化所有 Bean
- 启动 Web 服务

## 二 统一返回为什么要有 `Result<T>`

看 [Result.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/common/result/Result.java)。

这个类只有 3 个字段：

- `code`
- `message`
- `data`

意思分别是：

- `code`：状态码
- `message`：提示
- `data`：真正数据

### 1. 为什么不用 controller 直接返回对象

如果直接返回不同对象：

- 成功时是一种结构
- 出错时又是另一种结构

前端会很难处理。

### 2. `success(T data)` 的作用

这是静态工厂方法，帮助你快速返回成功结果。

controller 里经常写：

```java
return Result.success(response);
```

### 3. `success()` 的作用

这个版本没有 `data`，适合：

- 新增成功
- 修改成功
- 删除成功

## 三 分页参数为什么单独抽成 `PageQuery`

看 [PageQuery.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/common/page/PageQuery.java)。

它只有两个字段：

- `pageNum = 1L`
- `pageSize = 10L`

意思很简单：

- 默认第一页
- 默认每页 10 条

### 为什么默认值直接写在字段上

因为这样很多查询 DTO 继承它之后，就天然带分页默认值。

## 四 分页返回为什么单独抽成 `PageResult<T>`

看 [PageResult.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/common/page/PageResult.java)。

它有 4 个字段：

- `total`
- `pageNum`
- `pageSize`
- `records`

这四个字段基本就是前端表格分页最常用的内容。

## 五 为什么要自定义 `BusinessException`

看 [BusinessException.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/common/exception/BusinessException.java)。

这个类的重点不是继承了异常，而是多带了一个 `code`。

也就是说，系统不仅知道“出错了”，还知道“这是什么业务错误”。

## 六 `GlobalExceptionHandler` 到底拦什么

看 [GlobalExceptionHandler.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/common/exception/GlobalExceptionHandler.java)。

### 1. `@RestControllerAdvice`

表示：

- 这是全局异常处理类
- 返回结果按 JSON 输出

### 2. `handleBusinessException`

专门处理业务异常。

它做的事很清楚：

- 记日志
- 把异常 code 和 message 包成统一 `Result`

### 3. `handleAccessDeniedException`

处理“已登录但没权限”的情况。

### 4. `handleException`

处理其他未捕获异常。

而且还区分：

- dev 环境：返回更详细报错
- 非 dev 环境：返回通用系统错误

## 七 `MybatisPlusConfig` 为什么只写一个分页插件

看 [MybatisPlusConfig.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/config/MybatisPlusConfig.java)。

### `@Bean`

这个方法返回的对象要交给 Spring 管理。

### `MybatisPlusInterceptor`

可以先理解成 MyBatis Plus 的总拦截器容器。

### `PaginationInnerInterceptor(DbType.MYSQL)`

表示：

- 开启分页能力
- 当前数据库类型是 MySQL

## 八 `PasswordConfig` 为什么单独写

看 [PasswordConfig.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/config/PasswordConfig.java)。

它只提供一个 Bean：

```java
PasswordEncoder passwordEncoder()
```

返回的是 `BCryptPasswordEncoder`。

这样做的好处是：

- 其他类只依赖 `PasswordEncoder`
- 不依赖具体实现

## 九 `OpenApiConfig` 为什么重要

看 [OpenApiConfig.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/config/OpenApiConfig.java)。

它主要干两件事：

1. 配接口文档基本信息
2. 配 Bearer Token 安全方案

所以 Swagger 页面里你才能带 JWT 去调受保护接口。

## 十 这一章最值得你自己动手的练习

做下面 3 个小动作：

1. 自己找到一个 controller，确认最后都包了 `Result.success(...)`
2. 自己找到一个 service，确认业务不满足时都在抛 `BusinessException`
3. 自己从分页接口点到 `PageQuery` 和 `PageResult`
