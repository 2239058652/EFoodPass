# EFoodPass 非 AI 部分宝宝级教程

这套文档只讲这个项目里 **AI 之外** 的部分。

目标不是让你看完觉得“我懂了”，而是让你能：

1. 知道每个模块是干什么的。
2. 知道一个请求到底经过了哪些类。
3. 知道常见字段、方法、注解为什么要这样写。
4. 能自己慢慢照着复刻一遍。

这套文档和你前面的 AI 教程是分开的。

- AI 教程：`docs/spring-ai-baby/`、`docs/spring-ai-baby-detail/`
- 非 AI 教程：当前目录 `docs/project-baby-detail/`

## 推荐阅读顺序

1. [00-how-to-read.md](/C:/Users/22390/Desktop/EFoodPass/docs/project-baby-detail/00-how-to-read.md)
2. [01-boot-common-config.md](/C:/Users/22390/Desktop/EFoodPass/docs/project-baby-detail/01-boot-common-config.md)
3. [02-auth-security.md](/C:/Users/22390/Desktop/EFoodPass/docs/project-baby-detail/02-auth-security.md)
4. [03-system-user-role-permission.md](/C:/Users/22390/Desktop/EFoodPass/docs/project-baby-detail/03-system-user-role-permission.md)
5. [04-food-category-item.md](/C:/Users/22390/Desktop/EFoodPass/docs/project-baby-detail/04-food-category-item.md)
6. [05-food-order-stock.md](/C:/Users/22390/Desktop/EFoodPass/docs/project-baby-detail/05-food-order-stock.md)
7. [06-project-main-flow.md](/C:/Users/22390/Desktop/EFoodPass/docs/project-baby-detail/06-project-main-flow.md)
8. [07-rebuild-guide.md](/C:/Users/22390/Desktop/EFoodPass/docs/project-baby-detail/07-rebuild-guide.md)
9. [08-annotation-and-code-patterns.md](/C:/Users/22390/Desktop/EFoodPass/docs/project-baby-detail/08-annotation-and-code-patterns.md)

## 这套文档怎么写的

这套文档不是“概念型教程”，而是“拆代码型教程”。

你会反复看到下面这种讲法：

- 这个字段是做什么的
- 这个方法为什么要存在
- 这个注解起什么作用
- 这个类在请求链路里处于哪一层
- 如果你自己重写，最小版本应该先写什么

## 你要先接受一件事

你现在不是“学不会”，而是你还没有把这套代码拆成足够小的块去理解。

所以这套教程的核心思路不是炫耀项目做了多少功能，而是不断把一个大问题拆成小问题：

- 启动类做什么
- 统一返回做什么
- 全局异常做什么
- JWT 怎么进来的
- 用户角色权限怎么串起来
- 菜品、订单、库存为什么要分模块

## 你读文档时的建议动作

每读一章，最好同时做这 3 件事：

1. 打开文档里提到的源代码文件。
2. 自己在 IDE 里从 controller 一步点到 service。
3. 用 Swagger 或 Postman 调一遍接口。

这样你不会停留在“看懂文字”，而是开始形成对代码的真实感觉。
