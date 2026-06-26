---
description: "创建一个新的功能模块（包含 action、usecase、manager、entity 层），遵循壶言经济插件的分层架构。"
---

# 创建新功能模块

请为以下功能模块创建完整的分层代码结构：

## 功能描述

${feature:请描述要创建的功能模块，例如："宠物系统"}

## 需要创建的文件

请根据功能描述，在以下层级创建文件：

1. **Entity 层** (`src/main/kotlin/cn/chahuyun/economy/entity/${module}/`)
   - 数据库实体类，使用 JPA `@Entity`、`@Id`、`@Table` 注解
   - 主键使用 `@GeneratedValue(strategy = GenerationType.IDENTITY)`

2. **Repository 层** (`src/main/kotlin/cn/chahuyun/economy/repository/`)
   - 数据访问类，通过 `HibernateUtil` 获取 Session 进行 CRUD 操作

3. **Manager 层** (`src/main/kotlin/cn/chahuyun/economy/manager/`)
   - 模块管理器（`object` 单例），提供 `init()` 方法
   - 在 `HuYanEconomy.onEnable()` 中添加 `XxxManager.init()` 调用

4. **Usecase 层** (`src/main/kotlin/cn/chahuyun/economy/usecase/`)
   - 业务逻辑层，被 Action 调用

5. **Action 层** (`src/main/kotlin/cn/chahuyun/economy/action/`)
   - 指令入口，使用 `@MessageAuthorize` 注解
   - 通过 `AuthorizeServer.registerEvents()` 自动扫描注册

## 约定

- 所有类使用 Kotlin `object` 或普通 `class`
- 日志使用 `Log` 工具类
- 权限检查使用 HuYanAuthorize 的权限系统
- 不要手动在 `resources/META-INF/services/` 中添加条目
