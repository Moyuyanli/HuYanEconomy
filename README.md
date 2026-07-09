# HuYanEconomy 壶言经济

[![version](https://img.shields.io/github/v/release/moyuyanli/huyaneconomy)](https://github.com/Moyuyanli/HuYanEconomy/releases)
[![download](https://img.shields.io/github/downloads/moyuyanli/huyaneconomy/total)](https://github.com/Moyuyanli/HuYanEconomy/releases)
[![license](https://img.shields.io/github/license/moyuyanli/huyaneconomy)](LICENSE)
[![mirai](https://img.shields.io/badge/Mirai%20Console-2.16.0-green)](https://github.com/mamoe/mirai)
[![kotlin](https://img.shields.io/badge/Kotlin-1.9.20-blue)](https://kotlinlang.org)

> 基于 [Mirai Console](https://github.com/mamoe/mirai) 的 QQ 机器人经济插件，提供签到、银行、钓鱼、抢劫、抽奖、道具、私人银行等娱乐经济功能。

---

## 目录

- [✨ 功能特性](#-功能特性)
- [🚀 快速开始](#-快速开始)
- [📖 指令概览](#-指令概览)
- [🛠️ 开发与构建](#️-开发与构建)
- [📁 项目结构](#-项目结构)
- [🏛️ 项目架构](#️-项目架构)
- [📚 文档](#-文档)
- [🤝 参与贡献](#-参与贡献)
- [📄 许可证](#-许可证)

---

## ✨ 功能特性

- [x] **签到系统** — 每日签到获取金币（50~500），支持图片签到 & 自定义背景/字体，连续签到奖励
- [x] **银行系统** — 主银行存取款、每周利息结算、富豪榜（含银行总额占比）
- [x] **私人银行系统** — 创建银行、存取、放贷、评分、狐卷/国卷竞标，完整金融生态
- [x] **钓鱼系统** — 鱼塘管理、鱼竿升级、操作博弈、钓鱼排行榜、彩蛋鱼、惊喜尺寸
- [x] **抢劫系统** — 抢劫其他玩家、抢银行、监狱 & 保释机制、禁言卡、反抗因子
- [x] **红包系统** — 普通/随机/口令红包，24 小时自动退回
- [x] **抽奖系统** — 彩票猜签（小/中/大签）、单抽 & 十连、多种奖池
- [x] **道具系统** — 道具商店、背包管理、购买/使用/丢弃、功能道具（禁言卡等）
- [x] **称号系统** — 称号商店、购买/切换、自定义称号、渐变色、特殊 Buff
- [x] **转账系统** — 用户间转账
- [x] **用户系统** — 个人信息、经济信息、状态管理（位置：家/医院/监狱/鱼塘）

---

## 🚀 快速开始

### 前置条件

| 条件 | 说明 |
|------|------|
| JDK 11+ | JVM Target 为 11 |
| [Mirai Console](https://github.com/mamoe/mirai) 2.16.0 | QQ 机器人框架 |

### 前置插件

| 插件 | 版本 | 说明 |
|------|------|------|
| [mirai-economy-core](https://github.com/cssxsh/mirai-economy-core) | ≥1.0.6 | 经济前置，依赖 [mirai-hibernate-plugin](https://github.com/cssxsh/mirai-hibernate-plugin) |
| [HuYanAuthorize](https://github.com/Moyuyanli/HuYanAuthorize) | ≥1.3.6 | 权限系统（KSP 注解处理） |

### 安装步骤

1. 从 [Releases](https://github.com/Moyuyanli/HuYanEconomy/releases) 下载最新版本
2. 将插件 jar 和前置插件一起放入 Mirai 的 `plugins/` 目录
3. **首次启动后立即停止**，生成配置文件
4. 编辑配置文件（见下方）
5. 重新启动 Mirai 即可使用

### 配置

编辑 `config/cn.chahuyun.HuYanEconomy/Config.yml`：

```yaml
# 主人 QQ 号（拥有最高权限）
owner: 123456

# 指令触发前缀（空字符串表示无前缀）
prefix: ''

# 每日签到刷新时间（0-23 时）
reSignTime: 4

# 数据库类型：H2 / SQLITE / MYSQL
dataType: H2

# MySQL 配置（仅 dataType=MYSQL 时生效）
mysqlUrl: 'localhost:3306/test'
mysqlUser: 'root'
mysqlPassword: '123456'

# 钓鱼模式：new / old
fishType: 'new'

# 无法使用禁言卡的群号列表
unableToUseMuteGroup: []
```

> 📌 更多配置说明详见 [配置说明.md](docs/配置说明.md)

### 版本升级

部分版本升级需执行修复指令（建议在 Console 中执行）：

```
hye repair
```

---

## 📖 指令概览

> 以下为常用指令，实际可用指令以权限配置为准。完整指令说明详见 [指令参考手册.md](docs/指令参考手册.md)

### 💰 经济 & 银行

| 指令 | 说明 |
|------|------|
| `存款 <金额>` / `取款 <金额>` | 主银行存取款 |
| `存款!` | 一键存入钱包全部余额 |
| `银行利率` | 查看当前利率 |
| `富豪榜` | 查看排行榜 |
| `转账 @用户 <金额>` | 向其他用户转账 |

### 🏦 私人银行

| 指令 | 说明 |
|------|------|
| `银行创建 <code> <name>` | 创建银行（需主银行存款 ≥ 1 亿） |
| `银行列表` / `银行信息 [code]` | 查看银行列表或详情 |
| `存款 <金额> [code]` / `取款 <金额> [code]` | 向指定私人银行存取 |
| `贷款 <金额> [code]` / `还款 <金额> [code]` | 贷款 / 还款 |
| `银行评分 <1-5> [描述] [code]` | 给银行评分 |
| `银行放贷 <金额> <利率>` | 行长发布放贷标的 |
| `狐卷` / `狐卷竞标 <code> <溢价> <利率>` | 狐卷相关操作 |

### ✅ 签到

| 指令 | 说明 |
|------|------|
| `签到` / `打卡` / `sign` | 每日签到，随机 50~500 金币 |

### 🎣 钓鱼

| 指令 | 说明 |
|------|------|
| `购买鱼竿` | 购买鱼竿（500 金币） |
| `钓鱼` / `抛竿` | 开始钓鱼 |
| `升级鱼竿` | 升级鱼竿等级 |
| `鱼竿等级` / `鱼塘等级` | 查看等级 |
| `钓鱼排行榜` | 查看排行 |

### 🔫 抢劫

| 指令 | 说明 |
|------|------|
| `抢劫 @用户` | 抢劫其他玩家 |
| `抢银行` | 抢劫银行 |
| `释放出狱` | 出狱 |
| `保释 @用户` | 保释其他玩家 |

### 🧧 红包

| 指令 | 说明 |
|------|------|
| `发红包 <金额> <个数> [随机]` | 发红包 |
| `抢红包` / `领红包 <id>` | 领取红包 |
| `红包列表` | 查看可领红包 |

### 🎰 抽奖

| 指令 | 说明 |
|------|------|
| `猜签 <号码> <金额>` | 彩票猜签（小/中/大签） |
| `抽奖` / `十连` | 抽奖 |

### 🎒 道具 & 称号

| 指令 | 说明 |
|------|------|
| `我的背包` / `道具商店` | 查看背包 / 商店 |
| `购买 <道具> [数量]` | 购买道具 |
| `使用 <道具> [数量]` | 使用道具 |
| `我的称号` / `称号商店` | 查看称号 / 商店 |
| `购买称号 <称号>` | 购买称号 |
| `切换称号 <序号>` | 切换称号（0 = 卸下） |

### 👤 用户 & 管理

| 指令 | 说明 |
|------|------|
| `个人信息` / `info` | 查看个人信息 |
| `经济信息` / `money` | 查看资金 |
| `greedisgood <金额>` | 管理员：获取指定金额 |
| `hye v` | Console：查看版本 |
| `hye repair` | Console：修复数据库 |

---

## 🛠️ 开发与构建

### 环境要求

- JDK 11+
- Kotlin 1.9.20（由 Gradle 自动管理）
- Gradle 8.x（使用项目自带 Wrapper）

### 构建

```bash
# Windows
./gradlew.bat buildPlugin --console=plain

# Linux / macOS
./gradlew buildPlugin --console=plain
```

构建产物输出到 `build/mirai/`，文件名格式：`HuYanEconomy-<version>.mirai2.jar`

### 调试

1. 将构建好的 jar 复制到 `debug-sandbox/plugins/` 目录
2. 配置 `debug-sandbox/config/` 和 `debug-sandbox/bots/`
3. 启动 mirai-console 测试运行

> 📌 详细的开发指南请参阅 [项目开发说明.md](docs/项目开发说明.md)

---

## 📁 项目结构

```
HuYanEconomy/
├── economy-main/        # Mirai 插件入口、Action、命令、事件、权限注册、最终打包
├── economy-core/        # 签到、银行、背包、道具、称号、红包、私人银行等核心经济流程
├── economy-game/        # 钓鱼、抢劫、抽奖、农场等玩法流程
├── economy-data/        # 实体、DTO、Converter、Repository、EntityProxy、数据版本与缓存
├── economy-image/       # 图片渲染、画布工具、Renderer
├── economy-common/      # 通用基础能力、常量、时间/金额/文本工具
├── docs/                # 长期开发文档
├── plan/                # 版本规划与迁移记录
└── debug-sandbox/       # 本地 mirai-console 调试环境
```

> 📌 详细的目录说明请参阅 [项目结构说明.md](docs/项目结构说明.md)

---

## 🏛️ 项目架构

### 分层架构

```
用户消息
  ↓
economy-main/action       # @MessageAuthorize 指令入口
  ↓
economy-core|game/usecase # 业务流程编排
  ↓
economy-core|game/manager # 模块级 API 与生命周期
  ↓
economy-data/repository   # 数据访问与版本代理
  ↓
economy-data/entity       # Hibernate @Entity
```

### 技术栈

| 层面 | 技术 |
|------|------|
| 语言 | Kotlin 1.9.20, JVM Target 11 |
| 框架 | mirai-console 2.16.0 |
| ORM | Hibernate（通过 mirai-hibernate-plugin 运行时提供） |
| 权限 | HuYanAuthorize（KSP 注解处理） |
| 序列化 | kotlinx-serialization |
| 图片生成 | Apache POI + Java AWT |
| 工具库 | Hutool 5.8.40 |
| 构建 | Gradle KTS |

---

## 📚 文档

| 文档 | 说明 |
|------|------|
| [项目结构说明](docs/项目结构说明.md) | 详细的目录结构和模块职责说明 |
| [项目开发说明](docs/项目开发说明.md) | 开发环境搭建、构建测试、如何添加新功能 |
| [项目功能设计说明](docs/项目功能设计说明.md) | 各功能模块的设计文档和业务流程 |
| [配置说明](docs/配置说明.md) | 所有配置项的详细说明 |
| [指令参考手册](docs/指令参考手册.md) | 完整的指令列表和使用说明 |
| [数据库设计](docs/数据库设计.md) | 数据库表结构和实体关系 |
| [FAQ](docs/FAQ.md) | 常见问题解答 |

---

## 🤝 参与贡献

1. Fork 本仓库
2. 创建功能分支：`git checkout -b feature/your-feature`
3. 提交更改：`git commit -m 'Add some feature'`
4. 推送分支：`git push origin feature/your-feature`
5. 提交 Pull Request

> 📌 开发规范请参阅 [项目开发说明.md](docs/项目开发说明.md)

---

## 📄 许可证

本项目基于 [AGPL-3.0](LICENSE) 许可证开源。

---

## 🔗 相关链接

- [Mirai Console](https://github.com/mamoe/mirai) — QQ 机器人框架
- [mirai-economy-core](https://github.com/cssxsh/mirai-economy-core) — 经济前置插件
- [mirai-hibernate-plugin](https://github.com/cssxsh/mirai-hibernate-plugin) — Hibernate 运行时插件
- [HuYanAuthorize](https://github.com/Moyuyanli/HuYanAuthorize) — 权限系统
- [Hibernate Plus](https://github.com/Moyuyanli/hibernate-plus) — Hibernate 工具封装



