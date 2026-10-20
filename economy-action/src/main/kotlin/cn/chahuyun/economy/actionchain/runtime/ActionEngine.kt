package cn.chahuyun.economy.actionchain.runtime

import kotlin.reflect.KClass

/**
 * 规则链执行引擎：把多个 [ActionModule] 合成一张不可变的调度表，并按
 * `before 规则 → 动作 → after 规则` 的顺序执行。
 *
 * ## 设计要点
 * - **无反射**：规则、Provider 都是 KSP 生成的 lambda/对象，引擎只做查表与调用；
 * - **无 Mirai 依赖**：事件一律以 [Any] 传递，回复出口由宿主实现的 [ActionResultHandler] 提供，
 *   因此本模块可以被普通 JVM 单元测试直接覆盖；
 * - **构造期校验**：所有配置错误（重复 id、悬空引用、Provider 冲突）都在实例化时抛出
 *   [ActionConfigurationException]，而不是等玩家触发指令才炸；
 * - **不可变**：构造完成后内部集合不再变化，可安全地被多线程/多协程共享。
 *
 * ## 执行流程（[invokeFromEntry]）
 * 1. 校验 actionId 已注册；
 * 2. 创建本次调用的 [ActionContext]；
 * 3. 按 order、ruleId 顺序执行全部 before 规则，遇到非 Continue 立即停止；
 * 4. 只有 before 全部放行时才执行动作本体，并把返回值写入上下文；
 * 5. 动作执行完后按同样顺序执行 after 规则，遇到非 Continue 立即停止；
 * 6. 如果最终结果是 [ActionResult.EndWithMessage]，交给 [ActionResultHandler] 发送一次；
 * 7. 返回 [ActionExecution] 描述本次结果。
 *
 * 注意：before 被拦截时 after **不会**执行；after 阶段不能“撤销”动作已产生的影响，
 * 框架也不提供自动补偿（需要时由业务规则自行写反向逻辑）。
 *
 * @param modules 参与本次装配的全部模块描述。模块顺序不影响结果（排序键是 order + ruleId），
 *                只影响重复 id 报错时提示的模块列表。
 * @param resultHandler `EndWithMessage` 的发送适配器，由宿主（如 economy-main）实现。
 * @throws ActionConfigurationException 任一配置校验失败时抛出。
 */
class ActionEngine(
    modules: Collection<ActionModule>,
    private val resultHandler: ActionResultHandler,
) {
    /** 全部已注册动作 id，用于调用期快速校验。 */
    private val actionIds: Set<String>

    /** actionId → 已排序的 before 规则列表（order 升序，同 order 按 ruleId 字典序）。 */
    private val beforeRules: Map<String, List<ActionRule>>

    /** actionId → 已排序的 after 规则列表（排序规则同上）。 */
    private val afterRules: Map<String, List<ActionRule>>

    /**
     * Provider 作用域表：key 为 `actionId`，全局 Provider 存在空串键下。
     *
     * value 为“类型 + 限定名 → Provider”，用于 O(1) 定位候选。
     */
    private val providers: Map<String, Map<ProviderKey, ActionProvider<out Any>>>

    init {
        // ---------- 1. 动作 id 全局唯一 ----------
        // 先展开成 (actionId, moduleId) 对，重复时能直接告诉调用方是哪两个模块撞了。
        val actionsByModule = modules.flatMap { module -> module.actionIds.map { it to module.moduleId } }
        val duplicateAction = actionsByModule.groupBy({ it.first }, { it.second }).entries
            .firstOrNull { it.value.size > 1 }
        if (duplicateAction != null) {
            throw ActionConfigurationException(
                "Duplicate actionId '${duplicateAction.key}' in modules ${duplicateAction.value.joinToString()}"
            )
        }
        // 用 linkedSet 保留注册顺序，日志/调试时输出稳定。
        actionIds = actionsByModule.mapTo(linkedSetOf()) { it.first }

        // ---------- 2. 规则引用必须存在，且同一动作内 ruleId 唯一 ----------
        val rules = modules.flatMap(ActionModule::rules)
        rules.forEach { rule ->
            if (rule.actionId !in actionIds) {
                throw ActionConfigurationException(
                    "Rule '${rule.ruleId}' references unregistered action '${rule.actionId}'"
                )
            }
        }
        val duplicateRule = rules.groupBy { it.actionId to it.ruleId }.entries.firstOrNull { it.value.size > 1 }
        if (duplicateRule != null) {
            throw ActionConfigurationException(
                "Duplicate ruleId '${duplicateRule.key.second}' for action '${duplicateRule.key.first}'"
            )
        }

        // 按阶段分组后排序：order 是主键，ruleId 是次级键。
        // 显式指定次级键是为了让“同 order”的规则在不同构建、不同模块注册顺序下都保持一致。
        fun sorted(stage: ActionRuleStage) = rules.asSequence()
            .filter { it.stage == stage }
            .groupBy(ActionRule::actionId)
            .mapValues { (_, scopedRules) -> scopedRules.sortedWith(compareBy(ActionRule::order, ActionRule::ruleId)) }
        beforeRules = sorted(ActionRuleStage.BEFORE)
        afterRules = sorted(ActionRuleStage.AFTER)

        // ---------- 3. Provider 引用与键冲突 ----------
        // 局部 Provider 绑定的 actionId 必须真实存在，否则它永远不会被解析到。
        val allProviders = modules.flatMap(ActionModule::providers)
        allProviders.forEach { provider ->
            if (provider.actionId != null && provider.actionId !in actionIds) {
                throw ActionConfigurationException(
                    "Provider ${provider.type.qualifiedName} references unregistered action '${provider.actionId}'"
                )
            }
        }
        // 全局内不重复、同一动作内不重复、局部不能遮蔽同 key 的全局。
        validateProviderKeys(allProviders)
        // 作用域分桶：null → 空串（全局），其余按 actionId 分组，再按“类型 + 限定名”建索引。
        providers = allProviders.groupBy { it.actionId.orEmpty() }.mapValues { (_, scopedProviders) ->
            scopedProviders.associateBy { ProviderKey(it.type, it.qualifier) }
        }
    }

    /**
     * 从入口发起一次规则链调用。
     *
     * 由 KSP 生成的 `<Owner>Actions` 门面间接调用；业务代码通常不直接调用本方法。
     *
     * @param T 动作本体的返回值类型；动作返回 `Unit` 时使用 `ActionExecution<Unit>`。
     * @param actionId 要执行的动作 id，必须已在某个模块中注册。
     * @param event 触发事件，原样放进 [ActionContext.event]，引擎不对其类型做假设。
     * @param action 动作本体（门面传入的 lambda），只有 before 全部放行时才会被调用。
     * @return 本次调用的结果，包含最终控制流、动作是否执行、动作返回值。
     * @throws ActionConfigurationException actionId 未注册时抛出。
     */
    suspend fun <T> invokeFromEntry(
        actionId: String,
        event: Any,
        action: suspend (ActionContext) -> T,
    ): ActionExecution<T> {
        if (actionId !in actionIds) {
            throw ActionConfigurationException("Unregistered action '$actionId'")
        }

        // 每次调用都新建上下文：属性、Provider 缓存、动作返回值都只属于这一次调用。
        val context = ActionContext(actionId, event, ::resolveProvider)
        var result: ActionResult = ActionResult.Continue
        var actionExecuted = false
        var actionValue: T? = null

        // ---------- 阶段一：before 规则（可拦截动作） ----------
        for (rule in beforeRules[actionId].orEmpty()) {
            val ruleResult = rule.invoke(context)
            // 任意非 Continue 结果（EndSilently / EndWithMessage）都会短路整个调用。
            if (ruleResult != ActionResult.Continue) {
                result = ruleResult
                break
            }
        }

        // ---------- 阶段二：动作本体 + after 规则（仅当前置全部放行） ----------
        if (result == ActionResult.Continue) {
            actionValue = action(context)
            actionExecuted = true
            // 写入上下文，供 after 规则通过 actionResult 读取。
            context.setActionResult(actionValue)

            for (rule in afterRules[actionId].orEmpty()) {
                val ruleResult = rule.invoke(context)
                if (ruleResult != ActionResult.Continue) {
                    result = ruleResult
                    break
                }
            }
        }

        // ---------- 阶段三：统一回复出口 ----------
        // 只在最终结果是 EndWithMessage 时发送，且一次调用最多发送一次；
        // before 与 after 都返回 EndWithMessage 时，以先触发（即最终 result）的那个为准。
        if (result is ActionResult.EndWithMessage) {
            resultHandler.handle(context, result)
        }
        return ActionExecution(actionId, result, actionExecuted, actionValue)
    }

    /**
     * 解析 Provider：按“类型 + 限定名”在当前动作可见范围内查找并加载。
     *
     * 可见范围 = 全局 Provider + 当前动作的局部 Provider。
     *
     * 匹配规则：
     * - 限定名为 `null` 或空串 → 不按限定名过滤（空串也视为“未指定”）；
     * - 指定了限定名 → 只匹配相同限定名的候选；
     * - 0 个候选 → 抛 [ActionConfigurationException]（缺少 Provider）；
     * - 多个候选 → 抛 [ActionConfigurationException]（歧义，需要限定名区分）。
     *
     * 缓存：同一 [ActionContext] 内同一“类型 + 限定名”只加载一次，
     * 多条规则共用同一 Provider 时不会重复执行数据库查询等昂贵操作。
     *
     * @param context 本次调用上下文，同时作为缓存载体与加载参数。
     * @param type 目标类型。
     * @param qualifier 限定名；`null` 表示未指定。
     * @return Provider 产出的值。
     */
    private suspend fun resolveProvider(
        context: ActionContext,
        type: KClass<*>,
        qualifier: String?,
    ): Any {
        // 全局 Provider 挂在空串键下，局部 Provider 挂在 actionId 键下。
        val scopedProviders = providers[context.actionId].orEmpty()
        val globalProviders = providers[""].orEmpty()
        val matching = (globalProviders.values + scopedProviders.values)
            .filter { it.type == type && (qualifier == null || qualifier.isEmpty() || it.qualifier == qualifier) }
        val provider = when {
            matching.isEmpty() -> throw ActionConfigurationException(
                "No Provider for ${type.qualifiedName} qualifier '${qualifier.orEmpty()}' in action '${context.actionId}'"
            )
            matching.size > 1 -> throw ActionConfigurationException(
                "Ambiguous Provider for ${type.qualifiedName} in action '${context.actionId}'; specify a qualifier"
            )
            else -> matching.single()
        }
        // 命中缓存直接返回；缓存键用 Provider 自身的限定名，避免“未指定限定名”和“空限定名”被当成两个键。
        context.cachedProvider(type, provider.qualifier)?.let { return it }
        val value = provider.load(context)
        // 防御性校验：Provider 声明的类型与实际返回值不一致时立即报错，而不是让下游出现 ClassCastException。
        if (!type.javaObjectType.isInstance(value)) {
            throw ActionConfigurationException(
                "Provider ${provider.type.qualifiedName} returned ${value::class.qualifiedName}"
            )
        }
        context.cacheProvider(type, provider.qualifier, value)
        return value
    }

    /**
     * Provider 键冲突校验，全部在构造期完成。
     *
     * 三类问题：
     * 1. 两个全局 Provider 的“类型 + 限定名”相同 → 解析时必然歧义；
     * 2. 同一动作内两个局部 Provider 的“类型 + 限定名”相同 → 同上；
     * 3. 局部 Provider 与全局 Provider 键相同 → 局部会遮蔽全局，语义不明确，直接禁止。
     *
     * @param allProviders 所有模块声明的全部 Provider。
     * @throws ActionConfigurationException 命中任一冲突时抛出。
     */
    private fun validateProviderKeys(allProviders: List<ActionProvider<out Any>>) {
        // 1. 全局重复。
        val duplicateGlobal = allProviders.filter { it.actionId == null }
            .groupBy { ProviderKey(it.type, it.qualifier) }
            .entries.firstOrNull { it.value.size > 1 }
        if (duplicateGlobal != null) {
            throw ActionConfigurationException("Duplicate global Provider key ${duplicateGlobal.key}")
        }

        // 2. 同一动作内的局部重复（不同动作之间可以同名，因为作用域隔离）。
        val duplicateLocal = allProviders.filter { it.actionId != null }
            .groupBy { it.actionId!! to ProviderKey(it.type, it.qualifier) }
            .entries.firstOrNull { it.value.size > 1 }
        if (duplicateLocal != null) {
            throw ActionConfigurationException(
                "Duplicate Provider key ${duplicateLocal.key.second} for action '${duplicateLocal.key.first}'"
            )
        }

        // 3. 局部遮蔽全局。
        val globalKeys = allProviders.filter { it.actionId == null }
            .mapTo(hashSetOf()) { ProviderKey(it.type, it.qualifier) }
        val shadowedLocal = allProviders.firstOrNull { provider ->
            provider.actionId != null && ProviderKey(provider.type, provider.qualifier) in globalKeys
        }
        if (shadowedLocal != null) {
            throw ActionConfigurationException(
                "Local Provider ${ProviderKey(shadowedLocal.type, shadowedLocal.qualifier)} for " +
                    "action '${shadowedLocal.actionId}' conflicts with a global Provider"
            )
        }
    }
}
