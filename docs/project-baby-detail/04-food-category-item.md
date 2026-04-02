# 04 food 模块前半段：分类和菜品怎么工作

这一章讲：

- 菜品分类
- 菜品管理

重点文件：

- [FoodCategoryServiceImpl.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/modules/food/category/service/impl/FoodCategoryServiceImpl.java)
- [FoodItemController.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/modules/food/item/controller/FoodItemController.java)
- [FoodItemServiceImpl.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/modules/food/item/service/impl/FoodItemServiceImpl.java)
- [FoodItem.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/modules/food/item/entity/FoodItem.java)
- [FoodItemCreateRequest.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/modules/food/item/dto/FoodItemCreateRequest.java)

## 一 为什么先讲分类，再讲菜品

因为在这个项目里：

- 菜品依赖分类
- 分类不依赖菜品

## 二 分类模块最核心的事是什么

看 [FoodCategoryServiceImpl.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/modules/food/category/service/impl/FoodCategoryServiceImpl.java)。

主要做：

1. 分类列表
2. 分类详情
3. 分类新增
4. 分类修改
5. 分类改状态
6. 分类删除

### `validateCategoryStatus`

作用是把“状态只能是 0 或 1”这条规则收成单独方法。

### `normalizeCategoryName`

作用是去掉名字首尾空格。

### `validateCategoryNameUnique`

作用是校验分类名不重复。

这里有 `excludeId`，是为了更新时排除自己。

### `deleteCategory`

删除前先查这个分类下有没有菜品。

有菜品就不让删。

## 三 `FoodItem` 实体的字段怎么理解

看 [FoodItem.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/modules/food/item/entity/FoodItem.java)。

### 关键字段

- `categoryId`：所属分类
- `name`：菜品名称
- `price`：价格，用 `BigDecimal`
- `stock`：库存
- `isOnSale`：上下架状态
- `description`：描述

## 四 `FoodItemCreateRequest` 为什么值得认真看

看 [FoodItemCreateRequest.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/modules/food/item/dto/FoodItemCreateRequest.java)。

这个类体现了“请求 DTO + 基础校验”的标准做法。

### 你要重点看这些校验

- `@NotNull`：不能为空
- `@NotBlank`：字符串不能为空白
- `@DecimalMin("0.00")`：价格不能小于 0
- `@Min(0)`：库存不能小于 0

### 为什么 `isOnSale` 这里只校验非空

因为是否只能取 `0/1`，更偏业务规则，放在 service 里校验更合适。

## 五 `FoodItemController` 的风格你要记住

看 [FoodItemController.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/modules/food/item/controller/FoodItemController.java)。

它暴露的接口很清晰：

- `list`
- `detail`
- `create`
- `update`
- `updateOnSale`
- `adjustStock`
- `delete`

controller 基本只做：

1. 权限控制
2. 参数接收
3. 调 service
4. 返回结果

## 六 `FoodItemServiceImpl` 是怎么做业务校验的

看 [FoodItemServiceImpl.java](/C:/Users/22390/Desktop/EFoodPass/src/main/java/com/epass/food/modules/food/item/service/impl/FoodItemServiceImpl.java)。

### 几个关键私有方法

- `validateOnSaleStatus`
- `validatePrice`
- `validateStock`
- `getRequiredEnabledCategory`
- `validateItemNameUnique`

### `createItem` 的标准流程

1. 规范化菜品名称
2. 名称不能为空
3. 分类必须存在且启用
4. 校验价格
5. 校验库存
6. 校验上下架状态
7. 校验同分类下菜品名唯一
8. 组装实体并保存

### `updateOnSaleStatus`

这里体现一条很实用的规则：

- 如果要上架菜品
- 那它所属分类必须是启用状态

### `adjustStock`

这个方法不仅修改了库存，还写了库存日志。

这说明项目不是只关心“当前库存值”，还关心“库存变化过程”。

## 七 分类和菜品这两个模块的关系是什么

你可以先记住这两句话：

1. 菜品一定挂在分类下面
2. 分类删不掉时，常见原因是下面还有菜品

再进一步：

- 分类状态会影响菜品能不能上架
- 分类是否存在会影响菜品能不能创建

## 八 这一章最建议你的练习

自己回答下面几个问题：

1. 为什么 `FoodItemCreateRequest` 只校验 `isOnSale` 非空，而不是直接校验只能是 `0/1`
2. 为什么 `FoodItemServiceImpl` 里要有 `getRequiredEnabledCategory`
3. 为什么删除分类前要先查 `FoodItem`
4. 为什么 `adjustStock` 不只改库存，还要写库存日志
