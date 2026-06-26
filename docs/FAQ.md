# 壶言经济（HuYanEconomy）常见问题解答（FAQ）

## 目录

- [一、安装与配置](#一安装与配置)
- [二、运行与启动](#二运行与启动)
- [三、功能使用](#三功能使用)
- [四、开发相关](#四开发相关)
- [五、故障排除](#五故障排除)
- [六、性能优化](#六性能优化)
- [七、扩展开发](#七扩展开发)

---

## 一、安装与配置

### Q1：插件需要哪些前置依赖？

**A**：壶言经济需要以下前置插件：

| 插件 | 最低版本 | 说明 |
|------|----------|------|
| [mirai-economy-core](https://github.com/cssxsh/mirai-economy-core) | 1.0.6 | 经济前置，自动引入 mirai-hibernate-plugin |
| [HuYanAuthorize](https://github.com/Moyuyanli/HuYanAuthorize) | 1.3.6 | 权限系统 |

同时需要 JDK 11+ 和 Mirai Console 2.16.0。

### Q2：如何配置数据库？

**A**：在 `config/cn.chahuyun.HuYanEconomy/Config.yml` 中设置：

```yaml
# 开发/测试环境使用 H2（默认）
dataType: H2

# 小型部署使用 SQLite
dataType: SQLITE

# 生产环境使用 MySQL
dataType: MYSQL
mysqlUrl: 'your-server:3306/huyan_economy'
mysqlUser: 'your_user'
mysqlPassword: 'your_password'
```

详细说明请参阅 [配置说明](配置说明.md#八数据库配置)。

### Q3：如何设置指令前缀？

**A**：在 `Config.yml` 中设置 `prefix` 字段：

```yaml
# 无前缀（默认）
prefix: ''

# 设置 / 前缀
prefix: '/'
```

设置前缀后，所有指令需此前缀才能触发。

### Q4：一个 Mirai 实例可以运行多个 Bot 吗？

**A**：不可以。壶言经济仅支持绑定一个 Bot，由配置中的 `owner` 和 Bot QQ 号决定。如需多 Bot 支持，需要运行多个 Mirai 实例。

### Q5：首次启动后需要做什么？

**A**：

1. 首次启动 Mirai Console，等待插件加载完成
2. **立即停止** Mirai Console（此时会自动生成配置文件）
3. 编辑 `config/cn.chahuyun.HuYanEconomy/Config.yml`，配置 `owner` 等参数
4. 编辑 `config/cn.chahuyun.HuYanAuthorize/AuthorizeConfig.yml`，配置权限系统
5. 重新启动 Mirai Console

---

## 二、运行与启动

### Q6：启动时出现 LinkageError: loader constraint violation

**A**：这是 ClassLoader 冲突问题。mirai-console 的 app classloader 已包含 log4j-api，插件不应再打包 log4j。

**解决方法**：
- 确保 `build.gradle.kts` 中有全局排除：
  ```kotlin
  configurations.configureEach {
      exclude(group = "org.apache.logging.log4j")
  }
  ```
- 新增依赖时，如传递依赖包含 log4j，需手动排除：
  ```kotlin
  implementation("some:library:1.0") {
      exclude(group = "org.apache.logging.log4j")
  }
  ```

### Q7：启动时出现 ServiceConfigurationError: "xxx not a subtype"

**A**：Hibernate/HikariCP 被打入插件 jar，与 mirai-hibernate-plugin 冲突。

**解决方法**：
- 不要将 Hibernate 核心依赖设为 `implementation`
- 运行期 Hibernate 由 mirai-hibernate-plugin 提供
- 使用 `compileOnly` 引用 Hibernate API

### Q8：插件加载后找不到配置文件

**A**：配置文件在首次启动后自动生成。如果未生成：

1. 确认插件 jar 放在了正确的 `plugins/` 目录
2. 检查 Mirai Console 启动日志是否有加载错误
3. 确认前置插件（mirai-economy-core、HuYanAuthorize）已正确安装
4. 手动创建配置目录和文件

### Q9：升级版本后数据库报错

**A**：部分版本升级需要修复数据库结构：

```
# 在 Mirai Console 中执行
hye repair
```

建议在版本升级后优先执行此命令。详细说明请参阅 [数据库设计](数据库设计.md#十三数据迁移与修复)。

---

## 三、功能使用

### Q10：签到获得的金币范围是多少？

**A**：每日签到随机获得 50~500 金币。签到刷新时间由配置 `reSignTime` 控制（默认凌晨 4 点）。

### Q11：如何创建私人银行？

**A**：需要满足以下条件：

1. 主银行存款 ≥ 1 亿金币
2. 缴纳 1000 万保证金
3. 发送指令：`银行创建 <code> <name>`

详细说明请参阅 [指令参考手册](指令参考手册.md#五私人银行系统)。

### Q12：钓鱼系统如何操作？

**A**：

1. 购买鱼竿：发送 `购买鱼竿`（500 金币）
2. 开始钓鱼：发送 `钓鱼` 或 `抛竿`
3. 根据提示进行操作博弈（新版模式）
4. 钓到的鱼自动出售获得金币

鱼竿等级越高，能进入的鱼塘等级越高，能钓到的鱼越好。

### Q13：抢劫失败会怎样？

**A**：抢劫失败可能导致以下后果：

- **被发现**：目标发现你的企图，抢劫失败
- **被抓住**：需要赔偿目标金币
- **被抓入狱**：被禁言（默认 3600 秒），并罚款

概率由全局因子（`GlobalFactor`）和用户因子（`UserFactor`）共同决定。

### Q14：红包 24 小时未领完怎么办？

**A**：红包 24 小时未领完会自动退回发送者。退回金额 = 总金额 - 已领走金额。

### Q15：彩票有几种类型？

**A**：三种类型：

| 类型 | 说明 | 开奖频率 |
|------|------|----------|
| 小签 | 分钟彩票 | 每分钟 |
| 中签 | 小时彩票 | 每小时 |
| 大签 | 天彩票 | 每天 |

### Q16：称号有什么用？

**A**：称号系统提供以下功能：

- **显示效果**：在签到图片中显示称号名称
- **自定义颜色**：支持渐变色显示
- **影响昵称**：部分称号会替换签到图片中的昵称
- **Buff 效果**：部分称号提供特殊增益

---

## 四、开发相关

### Q17：如何添加新的功能模块？

**A**：按照以下步骤：

1. **entity/** — 创建实体类（`@Entity`）
2. **repository/** — 创建数据访问层
3. **manager/** — 创建模块管理器（`init()`/`shutdown()`）
4. **usecase/** — 创建业务逻辑层
5. **action/** — 创建指令入口（`@EventComponent` + `@MessageAuthorize`）
6. **主类** — 在 `onEnable()` 中初始化 Manager

详细步骤请参阅 [项目开发说明](项目开发说明.md#4-如何添加新功能)。

### Q18：如何添加新的权限码？

**A**：

1. 在 `constant/EconPerm.kt` 中添加权限常量
2. 在 `plugin/PermCodeManager.init()` 中注册权限
3. 在 Action 中使用 `@MessageAuthorize` 的 `userPermissions` 参数

详细说明请参阅 [项目开发说明](项目开发说明.md#8-权限系统使用)。

### Q19：KSP 生成代码在哪里？可以修改吗？

**A**：KSP 生成代码位于 `build/generated/ksp/` 目录下，由 HuYanAuthorize 的 KSP 处理器自动生成。

**严禁手动修改！** 每次构建会自动重新生成。如需调整，修改源码中的注解即可。

### Q20：如何调试数据库？

**A**：

1. **H2 模式**：使用 [DBeaver](https://dbeaver.io/) 等工具连接 `debug-sandbox/data/cn.chahuyun.HuYanEconomy/HuYanEconomy.h2.mv.db`
2. **SQLite 模式**：连接 `debug-sandbox/data/cn.chahuyun.HuYanEconomy/HuYanEconomy`
3. **MySQL 模式**：使用常规 MySQL 客户端连接

### Q21：如何自定义消息文案？

**A**：编辑对应的配置文件：

- 钓鱼消息：`config/cn.chahuyun.HuYanEconomy/FishingMsgConfig.yml`
- 抢劫消息：`config/cn.chahuyun.HuYanEconomy/RobMsgConfig.yml`

详细说明请参阅 [配置说明](配置说明.md)。

---

## 五、故障排除

### Q22：指令发送后没有反应

**可能原因及解决方法**：

1. **指令前缀问题**：检查 `Config.yml` 中的 `prefix` 设置
2. **权限不足**：检查用户是否拥有对应权限码
3. **黑名单**：检查用户是否在黑名单权限组中
4. **插件未加载**：检查 Mirai Console 启动日志
5. **Bot 未登录**：确认 Bot QQ 号已正确登录

### Q23：签到图片生成失败

**可能原因**：

1. **资源文件缺失**：检查 `src/main/resources/` 中的图片资源
2. **字体问题**：系统缺少对应字体文件
3. **内存不足**：图片生成需要一定内存，检查 JVM 内存配置

### Q24：钓鱼时卡住不动

**可能原因**：

1. **线程数不足**：增加配置 `nextMessageExecutorsNumber` 的值
2. **消息监听冲突**：检查是否有其他插件拦截了消息事件
3. **数据库连接超时**：检查数据库连接配置

### Q25：数据库连接失败（MySQL）

**检查项**：

1. MySQL 服务是否正常运行
2. 连接地址和端口是否正确
3. 用户名和密码是否正确
4. 数据库是否已创建
5. 网络是否可达
6. MySQL 用户是否有对应权限

---

## 六、性能优化

### Q26：如何优化数据库性能？

**建议**：

1. **使用 MySQL**：生产环境建议使用 MySQL，避免 H2/SQLite 的并发限制
2. **定期清理**：清理过期的红包、彩票等数据
3. **索引优化**：实体类中对常用查询字段添加索引
4. **连接池**：MySQL 模式使用连接池（由 mirai-hibernate-plugin 管理）

### Q27：如何减少内存占用？

**建议**：

1. **控制鱼塘数量**：每个鱼塘包含大量鱼实体数据
2. **清理历史数据**：定期清理过期的抽奖记录、红包记录
3. **JVM 参数调优**：设置合适的堆内存大小

### Q28：如何提高并发处理能力？

**建议**：

1. **增加线程池大小**：调整 `nextMessageExecutorsNumber`
2. **使用 MySQL**：MySQL 对并发写入的支持更好
3. **避免长事务**：数据库操作尽量快速完成

---

## 七、扩展开发

### Q29：如何添加自定义道具？

**A**：

1. 在 `prop/` 包下创建道具类，继承 `BaseProp`
2. 在 `PropsManager` 中注册新道具
3. 在 `PropsShop` 中添加商店配置
4. 实现道具使用逻辑

道具类型（`PropsKind`）：
- `CARD` — 卡片类道具
- `F_PROP` — 功能道具
- `FISH_BAIT` — 鱼饵

### Q30：如何添加自定义称号模板？

**A**：

1. 创建称号模板类，实现 `TitleTemplate` 接口
2. 在 `TitleTemplateManager.loadingCustomTitle()` 中注册
3. 配置称号的颜色、渐变、有效期等属性

### Q31：如何添加新的定时任务？

**A**：

1. 使用 `HuYanScheduler` 调度器注册任务
2. 任务实现 `ScheduledTask` 接口
3. 在 Manager 的 `init()` 中注册，在 `shutdown()` 中取消

```kotlin
// 示例：注册一个每小时执行的任务
HuYanScheduler.scheduleAtFixedRate(
    task = MyTask(),
    initialDelay = 0,
    period = 60 * 60 * 1000 // 1 小时
)
```

### Q32：如何添加新的自定义事件？

**A**：

1. 创建事件类，继承 `AbstractEvent`
2. 在需要的地方触发事件：`GlobalEventChannel.broadcast(MyEvent())`
3. 在 `HuYanEconomy.onEnable()` 中订阅事件

```kotlin
// 定义事件
class MyEvent(val userId: Long) : AbstractEvent()

// 触发事件
GlobalEventChannel.broadcast(MyEvent(userId))

// 订阅事件
eventChannel.subscribeAlways<MyEvent> { event ->
    // 处理逻辑
}
```

### Q33：如何贡献代码？

**A**：

1. Fork 项目仓库
2. 创建功能分支：`git checkout -b feature/your-feature`
3. 按照项目架构规范编写代码
4. 确保编译通过：`./gradlew.bat buildPlugin --console=plain`
5. 提交 Pull Request
6. 等待代码审查

**注意事项**：
- 遵循分层架构：action → usecase → manager → repository → entity
- 使用 `object` 单例模式
- 日志使用 `Log` 工具类
- 不要手动修改 KSP 生成代码
- 不要将 log4j/Hibernate 打入插件 jar

---

## 相关文档

- [项目结构说明](项目结构说明.md)
- [项目开发说明](项目开发说明.md)
- [配置说明](配置说明.md)
- [指令参考手册](指令参考手册.md)
- [数据库设计](数据库设计.md)
- [项目功能设计说明](项目功能设计说明.md)
