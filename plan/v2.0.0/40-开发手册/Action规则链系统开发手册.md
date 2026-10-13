# Action 规则链系统开发手册

> 状态：阶段一设计与 API 骨架已完成；Action 执行系统与 KSP Processor 尚未实现。  
> 设计依据：[KSP 驱动的 Action 规则链框架设计](../../../docs/KSP%20%E9%A9%B1%E5%8A%A8%E7%9A%84%20Action%20%E8%A7%84%E5%88%99%E9%93%BE%E6%A1%86%E6%9E%B6%20%C2%B7%20%E8%AE%BE%E8%AE%A1%E6%96%87%E6%A1%A3.md)

本文将 Action 规则链工作拆分为五个可验收阶段：设计 Action 骨架、实现 Action 系统、设计并实现 KSP、多模块测试、编写 API 文档。各阶段完成后再进入下一阶段；发现设计缺口时回到相应阶段更新契约，不以跳过验证的方式推进。

## 1. 项目基线与范围

当前项目使用 Kotlin `1.9.20`、KSP `1.9.20-1.0.14`、JVM target 11 和 Gradle Kotlin DSL。`HuYanAuthorize` 的 KSP 当前在 `economy-main` 处理 Mirai Action 入口。本开发工作新增自己的运行时模块和 KSP Processor，不替换 HuYanAuthorize，也不改变其命令匹配、权限校验和事件注册职责。

目标结构：

```text
economy-main/action (HuYanAuthorize, 命令/权限入口)
    -> 复杂流程的生成 facade
    -> economy-core / economy-game (object usecase、rules、providers)
    -> economy-data / economy-base

简单动作仍由入口直接调用，不要求添加 @Action。
```

建议模块：

- `economy-action`：运行时注解/API、ActionContext、动作结果、规则执行和模块描述契约。
- `economy-action-ksp`：编译期 KSP Processor；只作为处理器使用，不进入插件运行时。

每个业务模块只处理自己的源码并生成模块描述，最终由 `economy-main` 显式聚合。KSP 不跨 Gradle 模块扫描所有源码。

## 2. 阶段关系

```text
阶段 1 设计 Action 骨架
        |
        v
阶段 2 实现 Action 运行时系统
        |
        v
阶段 3 设计并实现 KSP
        |
        v
阶段 4 多模块集成与测试
        |
        v
阶段 5 编写并核对 API 文档
```

阶段 1 的契约是阶段 2、3 的输入；阶段 3 依赖运行时 API 与模块描述契约稳定；阶段 4 验证真实 Gradle/KSP 模块边界；阶段 5 以经过测试的实际 API 为准。阶段 4 若暴露跨模块限制，应回到阶段 1 或 3 修订，不在文档中掩盖限制。

## 3. 阶段一：设计 Action 骨架

### 目标

在写引擎和 Processor 前，确定最小公共 API、模块边界、调用签名和结果语义，避免运行时实现与生成代码各自发展成不同契约。

### 执行状态

阶段一方案与代码产物见[Action 骨架契约](Action%E9%AA%A8%E6%9E%B6%E5%A5%91%E7%BA%A6.md)。运行时注解模块和 KSP 工程壳已建立；结果 API 名称、Action 返回值和回复发送责任等事项仍是阶段二的实现前置决议，详见契约第 6 节。

### 关键任务

1. 在 `settings.gradle.kts` 注册两个模块并确认依赖方向：业务模块依赖运行时模块；业务模块未来应用 KSP Processor；运行时模块不依赖 core/game/HuYanAuthorize。
2. 定义 `@Action`、`@Before`、`@After`、Provider 声明和参数限定注解的目标、参数及保留策略。具体注解命名尚未最终锁定，先形成一组一致 API 草案。
3. 固定 Action 形态：业务实现是 Kotlin `object`；常见签名为一个 Mirai event；少数情况允许 `event + ActionContext`；生成 facade 对入口公开 event，不要求入口构造 context。
4. 固定规则执行顺序、相同 order 的稳定排序规则、`Unit` 是否映射为 Continue，以及 before/after 提前结束时的行为。
5. 固定动作结果：Continue、静默结束、回复后结束。Rollback 只登记为待定项，阶段一不定义可执行回滚语义。
6. 固定 Provider 标识及作用域：解析由 action 范围、类型和 qualifier 确定；明确全局 Provider 与 action 局部 Provider 如何合并及冲突如何处理。
7. 画出一次 Action 的上下文生命周期，以及 event、Provider 结果、规则间临时数据和 Action 返回值如何传递。
8. 明确简单 Action 不要求标注 `@Action`，只将复杂流程迁入规则链。

### 交付物

- 经评审的运行时 API/注解草案及至少一个复杂 Action 示例。
- 一份模块依赖图和生成物/聚合流程图。
- “已确定 / 待确认”决策清单，尤其列明 Provider 跨模块校验和回复适配边界。
- 若存在无法一次决定的项，给出首版限制，不将未定行为写成既定能力。

### 阶段依赖

无前序阶段。输入为现有 Action 规则链设计文档和当前 Gradle 模块配置。阶段一只创建 Gradle 模块骨架与注解定义，不包含 Action 执行、Provider 装配或 KSP 处理实现。

### 验收标准

- Action 实现、规则、Provider、生成 facade 的职责边界清楚。
- facade 如何创建 context 并调用 object Action 已有明确契约。
- 同类型多 Provider 的解析不依赖 Kotlin 参数名，限定名及 action 局部可见规则已定义。
- 四类动作结果中回滚明确标记为暂缓，未承诺自动补偿。
- 两个以上示例能够按契约解释，包括普通单 event Action 和转账同类型双账户 Provider。
- 设计评审没有未标记的“实现时再猜”行为；尚未确定项必须列明负责人/最迟阶段和接口影响。

### 阶段一验收结果

阶段一验收已完成，决议记录见[Action 骨架契约](Action%E9%AA%A8%E6%9E%B6%E5%A5%91%E7%BA%A6.md)。进入阶段二前须先处理该契约中标明的阶段二门槛：结果类型名称、Action 本体返回值、EndWithMessage 的发送与去重责任、Provider 每次调用的缓存策略。

## 4. 阶段二：实现 Action 系统

### 目标

先不依赖 KSP，完成可被手工构造/注册的运行时执行核心，用测试验证执行语义和 Provider 查找规则。

### 关键任务

1. 在阶段一创建的 `economy-action` 骨架上实现运行时 API，并按既有 Kotlin/JVM 11 配置构建。
2. 实现阶段一确认的注解/API、ActionContext、动作结果、规则描述和运行时模块描述接口。
3. 实现 Action 执行顺序：构建 context、按序执行 before、执行 Action 本体、按序执行 after，并将终止结果返回入口适配层。
4. 实现 Provider 按已确认的作用域、类型和限定名查找；限定名缺失、重复或类型不符应给出清楚错误。
5. 保证运行时调用不使用反射；Provider/规则/action 的调用入口由普通 Kotlin 函数引用或接口承载，具体形式按阶段一 API 契约实现。
6. 为运行时核心增加单元测试。第一版不实现 Compensation、自动回滚或数据库事务包装。
7. 使模块可以在没有 Mirai Console 运行环境的单元测试中验证；若 API 需要 Mirai 类型，控制其影响范围并记录原因。

### 交付物

- `economy-action` 运行时模块及测试。
- 可手工组装的最小 ActionModule/规则/provider 测试样例。
- 已确认 API 与运行时行为一致的测试结果。

### 阶段依赖

依赖阶段一通过验收。若运行时 API 为 KSP 生成代码保留了未定义的隐式行为，应退回阶段一补充契约。

### 验收标准

- 单测覆盖 before/after 顺序、正常继续、静默结束、文本回复结束、Action 异常传播。
- before 终止时动作本体和 after 是否执行符合阶段一的明确契约。
- Provider 测试覆盖全局、局部、类型限定、同类型多个 qualifier、重复/缺失/冲突。
- 同一 Action 的局部 Provider 对其他 Action 不可见。
- 不存在自动回滚或“补偿器会被调用”的虚假能力声明。
- `:economy-action:test` 与 `:economy-action:build` 通过（任务在模块创建后生效）。

## 5. 阶段三：设计并实现 KSP

### 目标

让编译器根据业务模块中的 Action、规则和 Provider 声明生成 facade 与模块描述，并尽可能在编译期报告配置错误。

### 关键任务

1. 在阶段一创建的 `economy-action-ksp` 工程壳中实现 `SymbolProcessorProvider` 注册和 Processor；使用项目当前 Kotlin/KSP 版本，不顺带升级工具链。
2. 为每种注解编写符号发现、暂不可处理符号处理、错误报告和增量生成来源关联策略。
3. 实现 object Action facade：公开签名接收入口参数，内部创建/进入运行时调用并将引擎 context 注入业务 Action。
4. 为每个模块生成规则排序结果、Provider 元数据/调用函数及 ActionModule 描述。
5. 实现设计文档承诺的编译期校验：actionId 存在和唯一、规则引用有效、签名受支持、规则返回值合法、Provider key/类型/限定匹配。
6. 对 Kotlin object、suspend 函数、事件参数、可选 context 参数和生成代码可见性写 compile-testing 或 Gradle fixture。
7. 不生成反射调用、`Class.forName`、方法名字符串调用或无参构造业务类的代码。
8. 明确跨模块校验边界：本模块 Processor 只扫描本模块源码；外部模块 Provider/Action 信息如何提供给消费者，需要经阶段四验证。无法编译期验证的部分必须以聚合校验或明确限制呈现。

### 交付物

- `economy-action-ksp` Processor、ServiceLoader 注册及测试。
- 生成代码样例和成功/失败编译用例。
- Processor 实际支持的注解签名与设计契约对照表。

### 阶段依赖

依赖阶段二的运行时 API、ActionModule 契约与阶段一的注解/验证规则稳定。

### 验收标准

- 正向 fixture 生成的 facade/模块描述可编译、可调用。
- 负向 fixture 对重复 actionId、无对应 action、Provider 缺失/歧义/类型不符和不支持签名报告定位明确的 KSP 编译错误。
- 生成实现与手工运行时测试使用相同执行语义。
- Processor 仅为编译期依赖，不成为业务模块或最终插件的运行时依赖。
- 不声称已验证的跨模块编译期能力；跨模块支持留待阶段四集成验收。

## 6. 阶段四：多模块集成与测试

### 目标

在真实 Gradle 工程中验证各业务模块独立生成、Provider 局部作用域、main 聚合和最终 Mirai 插件打包。

### 关键任务

1. 为 `economy-core`、`economy-game` 应用 `economy-action-ksp`，按实际需要决定 `economy-main` 是否也应用。
2. 在 core/game 中各放置最小测试 ActionModule；至少一个 Provider/规则留在声明 Action 的同模块，验证基础路径。
3. 在 `economy-main` 组装各模块生成描述并创建唯一运行时引擎；初始化时机安排在相关 Manager/service 可用之后、入口开始处理事件之前。
4. 验证 Action ID 全局唯一，Provider 的全局/局部作用域、限定名和跨模块 key 冲突处理符合契约。
5. 针对“规则在一个模块、Provider 在另一个模块”的场景验证工具链实际能力；按结果选择生成元数据消费、main 聚合校验或首版禁止跨模块引用，并更新设计文档。
6. 运行模块单测和项目既有六模块测试，构建 `buildPlugin`，检查最终插件中含运行时模块而不含 KSP Processor。
7. 保留现有 `HuYanAuthorize-ksp` 注册路径，验证命令匹配、权限检查和 Action facade 调用同时正常。
8. 选一个低副作用复杂流程做受控试点；查询/帮助/简单开关继续直接调用，不为测试强行迁移。

### 交付物

- Gradle/settings 与各模块 KSP 配置。
- main 聚合入口和模块级生成结果。
- 多模块测试/编译 fixture、集成验证记录及插件打包检查结果。
- 跨模块 Provider 规则的最终支持范围和限制。

### 阶段依赖

依赖阶段二运行时和阶段三 Processor 单模块生成通过。阶段四发现契约不支持真实模块边界时，必须回到阶段一/三修订。

### 验收标准

- 以下构建均通过：

  ```powershell
  .\gradlew.bat :economy-action:test :economy-action-ksp:test --console=plain
  .\gradlew.bat :economy-common:test :economy-data:test :economy-image:test :economy-core:test :economy-game:test :economy-main:test --console=plain
  .\gradlew.bat buildPlugin --console=plain
  ```

- 若 Processor 项目不提供独立 `test` 任务，应以其实际测试任务替换，不能为满足文档而添加空任务。
- core/game 生成的模块描述能被 main 显式聚合，Action facade 在入口可见。
- Provider 作用域和重复检测在跨模块组合后仍然正确，不因模块注册顺序改变结果。
- HuYanAuthorize 继续成功注册现有入口；插件运行时没有装入 KSP Processor/KSP API。
- 至少一个复杂 Action 通过 facade 实际执行；未迁移的直接 Action 行为不变。
- 最终插件可构建，调试环境可启动并完成该试点的冒烟流程。

## 7. 阶段五：编写 API 文档

### 目标

根据实现和测试结果编写使用者可依赖的 API 文档，不把设计草案或未实现能力写成现状。

### 关键任务

1. 记录运行时与 KSP 模块职责、Gradle 配置及业务模块依赖方式。
2. 逐项说明注解目标、参数、支持签名、object 调用规则和生成 facade 命名/可见性。
3. 说明规则顺序、Unit 默认结果、动作提前结束、回复由谁发送、异常传播和 after 执行条件。
4. 说明 Provider 注册方式、全局/Action 局部可见性、类型+限定名解析、冲突行为及跨模块限制。
5. 给出一个普通单 event Action 示例、一个 event+context 示例、一个同类型双 Provider 的转账示例。
6. 记录 KSP 编译错误示例、常见配置问题、模块聚合初始化要求和插件打包注意事项。
7. 在“当前限制”中明确标注自动回滚尚未实现；不得将 `Rollback` 预留项、事务包装或补偿行为描述成可用 API。
8. 将设计文档与 API 文档交叉链接；若项目启用 Dokka，再确认生成文档包含公开 API 并能本地构建。是否启用 Dokka 作为项目决定，不作为本阶段的预设依赖。

### 交付物

- `economy-action` 与 `economy-action-ksp` 的使用/API 文档。
- 可复制运行或可由测试验证的示例。
- 设计文档到实现 API 的差异与限制清单。

### 阶段依赖

依赖阶段二至四的 API、代码生成、聚合和测试基本稳定。若 API 在阶段五仍频繁变更，应先回到对应实现阶段。

### 验收标准

- 文档中的注解和方法签名与实际源码一致。
- 每个示例均由测试 fixture 验证，或明确标记为伪代码。
- 外部使用者可从空白业务模块按文档配置处理器并调用生成 facade。
- 所有未实现能力、限制、跨模块约束和回复职责均明确。
- 交叉链接有效，文档构建/格式检查（若项目配置）通过。

## 8. 待补充或确认事项

以下事项在现有设计中仍未完全确定。进入相关阶段前应形成明确决策；不应由实现者自行补造语义。

### 8.1 Action 与结果

- 结果类型最终命名为 `ActionResult` 还是 `RuleResult`。
- Action 本体是否允许返回非 Unit 业务结果，以及该结果如何供 after 规则注入。
- after 返回 EndWithMessage 时是否覆盖已有回复；after 结束是否只停止后续 after。
- EndWithMessage 首版仅支持 String 是否足够；图片/MessageChain 如何由上层适配。
- context 是仅由引擎创建，还是 Action 方法允许显式声明 `ctx`；当前文档建议后者由生成代码注入。
- Action / Provider 是否必须是 object 中的成员函数，顶层函数是否排除。

### 8.2 Provider

- Provider 注解最终命名与 qualifier 参数的 API 形式。
- qualifier 是否允许空值；无限定参数在唯一类型匹配时自动绑定，还是首版一律要求显式限定。
- Provider 之间是否需要依赖注入（首版 Provider 只接收 ActionContext）。
- 全局 Provider 与局部 Provider 同 key 时严格报错的检查是在各模块编译期、main 聚合期还是启动期完成。
- 跨模块规则引用 Provider 的首版支持范围。当前 KSP 不能假设跨模块扫描源码，需用项目版本做验证。
- Provider 值是否按一次 Action 执行缓存，以及相同 key 被多条规则引用时是否只加载一次。

### 8.3 KSP 与构建

- ActionModule 的具体公开 API、模块生成类名和 main 聚合方式。
- 同 order 规则的稳定顺序定义。
- Processor 的增量处理策略、跨模块错误如何关联到源符号。
- 当前 KSP/Kotlin 版本下，如何读取依赖模块的生成元数据并进行 compile-time 校验。
- 是否需要将 Action 模块发布到 Maven；首版是否仅作为本仓库内部项目依赖。
- 最终 Mirai 插件的类打包/去重行为如何验证。

### 8.4 回滚

- Rollback 是结果、异常还是事务边界信号。
- 回滚具体针对道具消耗、状态变更、资金流动中的哪些操作。
- “最后执行”指 after 完成后统一回滚，还是失败路径的最后一个阶段。
- 事务与显式补偿的边界、幂等和失败处理。

回滚问题不阻塞首版无回滚 Action 系统，但在引入 `Rollback` 可执行行为前必须单独完成设计和验收。

## 9. 开发记录

每个阶段完成后更新本节，记录日期、提交/PR、执行的构建命令、结果和未解决问题。当前不预填完成状态；此手册描述的是待执行计划。
