---
applyTo: "**/*.kt"
---

# Kotlin 编码规范

## 风格

- 使用 Kotlin 官方代码风格（`kotlin.code.style=official`）
- JVM Target 11，不要使用 11+ 独有 API
- 使用 `object` 单例模式实现 Manager 和工具类
- 包名前缀: `cn.chahuyun.economy`

## mirai-console 开发模式

- 插件主类为 `object HuYanEconomy : KotlinPlugin(...)` 单例
- 配置类使用 `object XxxConfig : AutoSavePluginConfig("文件名")` 单例
- 在配置类中使用 `@ValueName("key")` 和 `@ValueDescription("描述")` 注解定义配置项
- 指令入口放在 `action/` 包，使用 `@MessageAuthorize` 注解标注
- Manager 类提供模块级 API，在 `onEnable()` 中通过 `XxxManager.init()` 初始化

## 数据层

- 实体类使用 JPA/Hibernate 注解（`@Entity`, `@Id`, `@Table` 等）
- Repository 层通过 `HibernateUtil` 获取 Session
- 不要将 Hibernate 核心依赖打入插件 jar（运行时由 mirai-hibernate-plugin 提供）

## 日志

- 使用项目自定义 `Log` 工具类，不要直接使用 `java.util.logging` 或 log4j
- 插件绝对不能携带 log4j 依赖（会导致 ClassLoader 冲突）

## 事件系统

- 自定义事件类继承 mirai 的 `Event` 接口
- 事件监听在 `onEnable()` 中通过 `GlobalEventChannel.parentScope(this)` 注册
- 使用 `subscribeAlways<EventType> { ... }` 订阅事件
