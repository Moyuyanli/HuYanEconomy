package cn.chahuyun.economy.actionchain.runtime

import kotlin.reflect.KClass
import kotlin.jvm.javaObjectType

/** Executes immutable module registrations without reflection or a Mirai dependency. */
class ActionEngine(
    modules: Collection<ActionModule>,
    private val resultHandler: ActionResultHandler,
) {
    private val actionIds: Set<String>
    private val beforeRules: Map<String, List<ActionRule>>
    private val afterRules: Map<String, List<ActionRule>>
    private val providers: Map<String, Map<ProviderKey, ActionProvider<out Any>>>

    init {
        val actionsByModule = modules.flatMap { module -> module.actionIds.map { it to module.moduleId } }
        val duplicateAction = actionsByModule.groupBy({ it.first }, { it.second }).entries
            .firstOrNull { it.value.size > 1 }
        if (duplicateAction != null) {
            throw ActionConfigurationException(
                "Duplicate actionId '${duplicateAction.key}' in modules ${duplicateAction.value.joinToString()}"
            )
        }
        actionIds = actionsByModule.mapTo(linkedSetOf()) { it.first }

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
        fun sorted(stage: ActionRuleStage) = rules.asSequence()
            .filter { it.stage == stage }
            .groupBy(ActionRule::actionId)
            .mapValues { (_, scopedRules) -> scopedRules.sortedWith(compareBy(ActionRule::order, ActionRule::ruleId)) }
        beforeRules = sorted(ActionRuleStage.BEFORE)
        afterRules = sorted(ActionRuleStage.AFTER)

        val allProviders = modules.flatMap(ActionModule::providers)
        allProviders.forEach { provider ->
            if (provider.actionId != null && provider.actionId !in actionIds) {
                throw ActionConfigurationException(
                    "Provider ${provider.type.qualifiedName} references unregistered action '${provider.actionId}'"
                )
            }
        }
        validateProviderKeys(allProviders)
        providers = allProviders.groupBy { it.actionId.orEmpty() }.mapValues { (_, scopedProviders) ->
            scopedProviders.associateBy { ProviderKey(it.type, it.qualifier) }
        }
    }

    suspend fun <T> invokeFromEntry(
        actionId: String,
        event: Any,
        action: suspend (ActionContext) -> T,
    ): ActionExecution<T> {
        if (actionId !in actionIds) {
            throw ActionConfigurationException("Unregistered action '$actionId'")
        }
        val context = ActionContext(actionId, event, ::resolveProvider)
        var result: ActionResult = ActionResult.Continue
        var actionExecuted = false
        var actionValue: T? = null

        for (rule in beforeRules[actionId].orEmpty()) {
            val ruleResult = rule.invoke(context)
            if (ruleResult != ActionResult.Continue) {
                result = ruleResult
                break
            }
        }

        if (result == ActionResult.Continue) {
            actionValue = action(context)
            actionExecuted = true
            context.setActionResult(actionValue)

            for (rule in afterRules[actionId].orEmpty()) {
                val ruleResult = rule.invoke(context)
                if (ruleResult != ActionResult.Continue) {
                    result = ruleResult
                    break
                }
            }
        }

        if (result is ActionResult.EndWithMessage) {
            resultHandler.handle(context, result)
        }
        return ActionExecution(actionId, result, actionExecuted, actionValue)
    }

    private suspend fun resolveProvider(
        context: ActionContext,
        type: KClass<*>,
        qualifier: String?,
    ): Any {
        val scopedProviders = providers[context.actionId].orEmpty()
        val globalProviders = providers[""] .orEmpty()
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
        context.cachedProvider(type, provider.qualifier)?.let { return it }
        val value = provider.load(context)
        if (!type.javaObjectType.isInstance(value)) {
            throw ActionConfigurationException(
                "Provider ${provider.type.qualifiedName} returned ${value::class.qualifiedName}"
            )
        }
        context.cacheProvider(type, provider.qualifier, value)
        return value
    }

    private fun validateProviderKeys(allProviders: List<ActionProvider<out Any>>) {
        val duplicateGlobal = allProviders.filter { it.actionId == null }
            .groupBy { ProviderKey(it.type, it.qualifier) }
            .entries.firstOrNull { it.value.size > 1 }
        if (duplicateGlobal != null) {
            throw ActionConfigurationException("Duplicate global Provider key ${duplicateGlobal.key}")
        }

        val duplicateLocal = allProviders.filter { it.actionId != null }
            .groupBy { it.actionId!! to ProviderKey(it.type, it.qualifier) }
            .entries.firstOrNull { it.value.size > 1 }
        if (duplicateLocal != null) {
            throw ActionConfigurationException(
                "Duplicate Provider key ${duplicateLocal.key.second} for action '${duplicateLocal.key.first}'"
            )
        }

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
