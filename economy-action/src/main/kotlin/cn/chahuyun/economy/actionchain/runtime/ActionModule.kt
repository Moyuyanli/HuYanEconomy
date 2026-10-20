package cn.chahuyun.economy.actionchain.runtime

import cn.chahuyun.economy.actionchain.runtime.ActionRule.Companion.after
import cn.chahuyun.economy.actionchain.runtime.ActionRule.Companion.before
import kotlin.reflect.KClass

/**
 * 一个模块（通常对应一个 Gradle 子工程）的规则链注册集合。
 *
 * 正常情况下由 KSP 生成：`actionchain.moduleId` 决定 [moduleId]，
 * 生成对象名为 `<ModuleId>ActionModule`（例如 `economy-core` → `EconomyCoreActionModule`），
 * 放在 `cn.chahuyun.economy.actionchain.generated` 包下，由 `economy-main` 显式聚合。
 *
 * 本类是**不可变描述**：模块只负责“声明自己有什么”，不负责执行。
 * 真正的校验与调度发生在 [ActionEngine] 构造和调用时，因此同一个模块对象可以被多个引擎复用。
 *
 * 构造期只做最基本的非空校验；更严格的校验（actionId 全局唯一、规则引用存在、
 * Provider 冲突等）交给 [ActionEngine]，因为那些规则需要“看到所有模块”才能判断。
 *
 * @property moduleId 模块标识，必须非空；同时用于生成对象名，建议用 Gradle 模块名（如 `economy-game`）。
 * @property actionIds 本模块声明的全部动作 id（不重复、不含空白）。跨模块重复会在引擎构造时报错。
 * @property rules 本模块声明的全部前置/后置规则，默认空。
 * @property providers 本模块声明的全部 Provider，默认空；`actionId == null` 表示全局可见。
 */
data class ActionModule(
    val moduleId: String,
    val actionIds: Set<String>,
    val rules: List<ActionRule> = emptyList(),
    val providers: List<ActionProvider<out Any>> = emptyList(),
) {
    init {
        // 模块标识要为生成对象名和日志负责，空字符串会让聚合与排查都失去线索。
        require(moduleId.isNotBlank()) { "moduleId must not be blank" }
        // 空白 actionId 永远无法被调用，属于配置笔误，构造期直接拒绝。
        require(actionIds.none(String::isBlank)) { "Action IDs must not be blank" }
    }
}

/**
 * 规则所处的阶段。
 *
 * - [BEFORE]：动作执行前，可拦截动作；
 * - [AFTER]：动作执行后，只能在动作真正执行过之后运行。
 *
 * 两个阶段的规则分别排序、分别执行，互不混排。
 */
enum class ActionRuleStage { BEFORE, AFTER }

/**
 * 一条具体的规则：绑定到某个动作、位于某个阶段、带执行顺序与调用体。
 *
 * [ruleId] 由 KSP 生成为 `<声明对象全限定名>#<函数名>`，例如
 * `cn.chahuyun.economy.game.fishing.FishingChain#beforeFish`。
 * 它既用于日志排查，也是“同 order 时的次级排序键”，保证执行顺序在不同构建之间稳定。
 *
 * 一般不直接构造本类，而是用 [before] / [after] 两个工厂方法，避免把阶段写错。
 *
 * @property actionId 规则所属动作 id，必须已在某个模块的 `actionIds` 中声明。
 * @property ruleId 规则唯一标识（同一动作内唯一），生成侧保证跨构建稳定。
 * @property order 执行顺序，数值越小越先执行，默认 100。
 * @property stage 规则阶段（前置/后置）。
 * @property invoke 规则调用体；参数为本次调用上下文，返回控制流结果。
 */
data class ActionRule(
    val actionId: String,
    val ruleId: String,
    val order: Int = 100,
    val stage: ActionRuleStage,
    val invoke: suspend (ActionContext) -> ActionResult,
) {
    init {
        // actionId 为空说明生成/手写配置有误，规则将永远匹配不到动作。
        require(actionId.isNotBlank()) { "actionId must not be blank" }
        // ruleId 为空会让排序和重复检测失效。
        require(ruleId.isNotBlank()) { "ruleId must not be blank" }
    }

    companion object {

        /**
         * 创建一条前置规则。
         *
         * @param actionId 规则所属动作 id。
         * @param ruleId 规则唯一标识，建议 `<owner FQN>#<method>`。
         * @param order 执行顺序，越小越先执行。
         * @param invoke 规则调用体，返回 [ActionResult]；只想放行时返回 [ActionResult.Continue]。
         */
        fun before(
            actionId: String,
            ruleId: String,
            order: Int = 100,
            invoke: suspend (ActionContext) -> ActionResult,
        ) = ActionRule(actionId, ruleId, order, ActionRuleStage.BEFORE, invoke)

        /**
         * 创建一条后置规则。
         *
         * @param actionId 规则所属动作 id。
         * @param ruleId 规则唯一标识，建议 `<owner FQN>#<method>`。
         * @param order 执行顺序，越小越先执行。
         * @param invoke 规则调用体，返回 [ActionResult]；只想放行时返回 [ActionResult.Continue]。
         */
        fun after(
            actionId: String,
            ruleId: String,
            order: Int = 100,
            invoke: suspend (ActionContext) -> ActionResult,
        ) = ActionRule(actionId, ruleId, order, ActionRuleStage.AFTER, invoke)
    }
}

/**
 * 一个值提供者（Provider）：按“类型 + 限定名”向规则/动作提供运行时对象。
 *
 * ## 作用域
 * - [actionId] 为 `null`：全局 Provider，任何动作都能解析到；
 * - [actionId] 非空：局部 Provider，只有该动作能解析到。
 *
 * ## 缓存
 * 同一个 [ActionContext]（同一次调用）内，同一个“类型 + 限定名”只会调用一次 [load]，
 * 结果由 [ActionContext] 缓存；即使规则多次声明同一个参数，也不会重复执行加载逻辑。
 *
 * ## 解析规则
 * 引擎会把全局 Provider 与当前动作的局部 Provider 合并后匹配：
 * 0 个候选 → 抛异常（缺少 Provider）；多个候选 → 抛异常（歧义，需要用限定名区分）；
 * 因此同一模块内“类型 + 限定名”必须唯一，且局部不能与全局同 key（引擎构造期校验）。
 *
 * @param T Provider 产出的值类型，必须是具体类（生成代码要用 `T::class` 做键）。
 * @property type 值类型，参与 Provider 匹配。
 * @property qualifier 限定名，同一类型有多个 Provider 时用于区分；默认空串。
 * @property actionId 局部作用域的动作 id；`null` 表示全局。
 * @property load 加载逻辑，入参为本次调用上下文；必须是 `suspend`，可访问数据库/网络。
 */
class ActionProvider<T : Any>(
    val type: KClass<T>,
    val qualifier: String = "",
    val actionId: String? = null,
    val load: suspend (ActionContext) -> T,
) {
    init {
        // 只允许 null（全局）或非空白字符串（局部）；空串会造成“看起来是局部、实际匹配不上”的困惑。
        require(actionId == null || actionId.isNotBlank()) { "actionId must be null or non-blank" }
    }
}

/**
 * 用具体类型实参创建 [ActionProvider] 的内联工厂，省去手写 `T::class`。
 *
 * KSP 生成的模块描述就是通过它注册 Provider 的：
 * ```kotlin
 * actionProvider<Account>(qualifier = "from", actionId = null) { context -> ... }
 * ```
 *
 * @param T 值类型；由于需要 `T::class`，这里使用 `reified`。
 * @param qualifier 限定名，默认空串。
 * @param actionId 局部作用域动作 id；`null` 表示全局。
 * @param load 加载逻辑，入参为本次调用上下文。
 */
inline fun <reified T : Any> actionProvider(
    qualifier: String = "",
    actionId: String? = null,
    noinline load: suspend (ActionContext) -> T,
) = ActionProvider(T::class, qualifier, actionId, load)

/**
 * 规则链配置错误。
 *
 * 触发场景全部是“声明阶段就能发现的问题”，而不是业务运行时异常：
 * - 跨模块 actionId 重复；
 * - 规则/Provider 引用了未注册的 actionId；
 * - 同一动作内 ruleId 重复；
 * - Provider 重复声明、局部遮蔽全局、按类型解析不到或出现歧义；
 * - 调用了未注册的 actionId。
 *
 * 这类异常应该在启动装配期就暴露，而不是等玩家触发指令才出现。
 */
class ActionConfigurationException(message: String) : IllegalArgumentException(message)

/**
 * Provider 的匹配键：类型 + 限定名。
 *
 * 内部使用，用于在构造期做重复/冲突检测，以及在调用期做缓存定位。
 */
internal data class ProviderKey(val type: KClass<*>, val qualifier: String)
