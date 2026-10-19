package cn.chahuyun.economy.actionchain.ksp

import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.*

/**
 * Collects `@Action` / `@Before` / `@After` / `@Provider` declarations of the current Gradle module,
 * validates them, and generates one facade per declaring object plus a module descriptor.
 */
class ActionSymbolProcessor(
    private val environment: SymbolProcessorEnvironment,
) : SymbolProcessor {

    private val logger = environment.logger
    private val codeGenerator = environment.codeGenerator

    private val moduleId = environment.options[ActionChainNames.OPTION_MODULE_ID]
        ?.takeIf { it.isNotBlank() }
        ?: ActionChainNames.DEFAULT_MODULE_ID
    private val generatedPackage = environment.options[ActionChainNames.OPTION_PACKAGE]
        ?.takeIf { it.isNotBlank() }
        ?: ActionChainNames.DEFAULT_PACKAGE
    private val allowExternalActions =
        environment.options[ActionChainNames.OPTION_ALLOW_EXTERNAL]?.toBooleanStrictOrNull() ?: false

    private var generated = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (generated) return emptyList()

        val actionSymbols = resolver.getSymbolsWithAnnotation(ActionChainNames.ACTION)
            .filterIsInstance<KSFunctionDeclaration>().toList()
        val ruleSymbols = (
            resolver.getSymbolsWithAnnotation(ActionChainNames.BEFORE).toList() +
                resolver.getSymbolsWithAnnotation(ActionChainNames.AFTER).toList()
            ).filterIsInstance<KSFunctionDeclaration>().distinct()
        val providerSymbols = resolver.getSymbolsWithAnnotation(ActionChainNames.PROVIDER)
            .filterIsInstance<KSFunctionDeclaration>().toList()

        if (actionSymbols.isEmpty() && ruleSymbols.isEmpty() && providerSymbols.isEmpty()) {
            generated = true
            return emptyList()
        }

        val actions = actionSymbols.flatMap(::collectActions)
        val rules = ruleSymbols.flatMap(::collectRules)
        val providers = providerSymbols.mapNotNull(::collectProvider)

        validateActions(actions)
        validateRules(rules, actions)
        validateProviders(providers, actions)
        validateRuleParameters(rules, providers)

        val sources = (actionSymbols + ruleSymbols + providerSymbols)
            .mapNotNull { it.containingFile }
            .distinct()
        val dependencies = Dependencies(aggregating = true, *sources.toTypedArray())

        emitFacades(actions, dependencies)
        emitModule(actions, rules, providers, dependencies)

        generated = true
        return emptyList()
    }

    // ---------------------------------------------------------------- collection

    private fun collectActions(function: KSFunctionDeclaration): List<ActionDecl> {
        val declarations = mutableListOf<ActionDecl>()
        function.annotations
            .filter { it.annotationName() == ActionChainNames.ACTION }
            .forEach { annotation ->
                val actionId = annotation.stringArgument("actionId").orEmpty()
                if (actionId.isBlank()) {
                    logger.error("@Action 的 actionId 不能为空", function)
                    return@forEach
                }
                if (!validateCallable(function, "@Action")) return@forEach
                if (!function.modifiers.contains(Modifier.SUSPEND)) {
                    logger.error("@Action 方法必须是 suspend 函数：$actionId", function)
                    return@forEach
                }
                val parameters = function.parameters
                if (parameters.isEmpty() || parameters.size > 2) {
                    logger.error("@Action 只支持 (event) 或 (event, ActionContext) 两种签名：$actionId", function)
                    return@forEach
                }
                val eventType = parameters.first().type.resolve()
                if (eventType.qualifiedName() == ActionChainNames.CONTEXT) {
                    logger.error("@Action 的第一个参数必须是事件，不能是 ActionContext：$actionId", function)
                    return@forEach
                }
                var injectContext = false
                if (parameters.size == 2) {
                    val second = parameters[1].type.resolve()
                    if (second.qualifiedName() != ActionChainNames.CONTEXT) {
                        logger.error("@Action 的第二个参数只能是 ActionContext：$actionId", function)
                        return@forEach
                    }
                    injectContext = true
                }
                val returnType = function.returnType?.resolve()
                val returnsUnit = returnType == null || returnType.isUnit()
                declarations += ActionDecl(
                    actionId = actionId,
                    owner = function.parentDeclaration as KSClassDeclaration,
                    functionName = function.simpleName.asString(),
                    eventType = eventType,
                    returnsUnit = returnsUnit,
                    returnType = returnType,
                    injectContext = injectContext,
                )
            }
        return declarations
    }

    private fun collectRules(function: KSFunctionDeclaration): List<RuleDecl> {
        val declarations = mutableListOf<RuleDecl>()
        annotationLoop@ for (annotation in function.annotations) {
            val stage = when (annotation.annotationName()) {
                ActionChainNames.BEFORE -> RuleStage.BEFORE
                ActionChainNames.AFTER -> RuleStage.AFTER
                else -> continue@annotationLoop
            }
            val actionId = annotation.stringArgument("actionId").orEmpty()
            if (actionId.isBlank()) {
                logger.error("@Before/@After 的 actionId 不能为空", function)
                continue@annotationLoop
            }
            if (!validateCallable(function, "@Before/@After")) continue@annotationLoop
            if (!function.modifiers.contains(Modifier.SUSPEND)) {
                logger.error("规则函数必须是 suspend 函数：$actionId", function)
                continue@annotationLoop
            }
            val returnType = function.returnType?.resolve()
            val returnsActionResult = returnType != null && returnType.isActionResult()
            if (!returnsActionResult && (returnType == null || !returnType.isUnit())) {
                logger.error(
                    "规则函数只能返回 Unit 或 ActionResult：$actionId -> ${returnType?.renderType()}",
                    function,
                )
                continue@annotationLoop
            }
            val bindings = mutableListOf<ParamBinding>()
            var parametersValid = true
            parameterLoop@ for (parameter in function.parameters) {
                val resolved = parameter.type.resolve()
                if (resolved.qualifiedName() == ActionChainNames.CONTEXT) {
                    if (parameter.fromProviderQualifier() != null) {
                        logger.error("@FromProvider 不能标注 ActionContext 参数", parameter)
                        parametersValid = false
                    }
                    bindings += ParamBinding.Context
                    continue@parameterLoop
                }
                if (resolved.nullability == Nullability.NULLABLE || resolved.renderClassLiteral() == null) {
                    logger.error(
                        "规则参数必须是不可空的类类型，或 ActionContext：${resolved.renderType()}",
                        parameter,
                    )
                    parametersValid = false
                    continue@parameterLoop
                }
                bindings += ParamBinding.FromProvider(
                    resolved,
                    parameter.fromProviderQualifier()?.takeIf { it.isNotBlank() },
                )
            }
            if (!parametersValid) continue@annotationLoop
            declarations += RuleDecl(
                actionId = actionId,
                stage = stage,
                order = annotation.intArgument("order") ?: 100,
                ruleId = "${function.qualifiedOwnerName()}#${function.simpleName.asString()}",
                owner = function.qualifiedOwnerName(),
                functionName = function.simpleName.asString(),
                parameters = bindings,
                returnsActionResult = returnsActionResult,
            )
        }
        return declarations
    }

    private fun collectProvider(function: KSFunctionDeclaration): ProviderDecl? {
        val annotation = function.annotations.firstOrNull { it.annotationName() == ActionChainNames.PROVIDER }
            ?: return null
        val qualifier = annotation.stringArgument("qualifier").orEmpty()
        val scopedAction = annotation.stringArgument("actionId")?.takeIf { it.isNotBlank() }
        if (!validateCallable(function, "@Provider")) return null
        if (!function.modifiers.contains(Modifier.SUSPEND)) {
            logger.error("Provider 函数必须是 suspend 函数：${function.qualifiedOwnerName()}", function)
            return null
        }
        if (function.parameters.size > 1) {
            logger.error("Provider 只支持 () 或 (ActionContext) 两种签名：${function.qualifiedOwnerName()}", function)
            return null
        }
        function.parameters.firstOrNull()?.let { parameter ->
            val resolved = parameter.type.resolve()
            if (resolved.qualifiedName() != ActionChainNames.CONTEXT) {
                logger.error("Provider 的唯一参数只能是 ActionContext：${function.qualifiedOwnerName()}", function)
                return null
            }
        }
        val returnType = function.returnType?.resolve()
        if (returnType == null || returnType.isUnit() || returnType.nullability == Nullability.NULLABLE ||
            returnType.renderClassLiteral() == null
        ) {
            logger.error(
                "Provider 必须返回不可空的非 Unit 类类型：${function.qualifiedOwnerName()}",
                function,
            )
            return null
        }
        function.parameters.forEach { parameter ->
            if (parameter.fromProviderQualifier() != null) {
                logger.error("@FromProvider 只能用于规则参数", parameter)
            }
        }
        return ProviderDecl(
            actionId = scopedAction,
            qualifier = qualifier,
            valueType = returnType,
            owner = function.qualifiedOwnerName(),
            functionName = function.simpleName.asString(),
            takesContext = function.parameters.isNotEmpty(),
        )
    }

    // ---------------------------------------------------------------- validation

    private fun validateActions(actions: List<ActionDecl>) {
        actions.groupBy { it.actionId }
            .filterValues { it.size > 1 }
            .forEach { (actionId, duplicates) ->
                duplicates.drop(1).forEach { duplicate ->
                    logger.error(
                        "actionId '$actionId' 在本模块重复声明（另一个在 ${duplicates.first().owner.qualifiedName?.asString()}）",
                        duplicate.owner,
                    )
                }
            }
    }

    private fun validateRules(rules: List<RuleDecl>, actions: List<ActionDecl>) {
        val declared = actions.mapTo(hashSetOf()) { it.actionId }
        rules.forEach { rule ->
            if (rule.actionId !in declared) {
                if (allowExternalActions) {
                    logger.warn(
                        "规则 ${rule.ruleId} 引用了本模块未声明的 actionId '${rule.actionId}'，将在聚合期校验",
                    )
                } else {
                    logger.error(
                        "规则 ${rule.ruleId} 引用了不存在的 actionId '${rule.actionId}'" +
                            "（跨模块引用请开启 actionchain.allowExternalActions）",
                    )
                }
            }
        }
        rules.groupBy { it.actionId to it.ruleId }
            .filterValues { it.size > 1 }
            .forEach { (key, duplicates) ->
                duplicates.drop(1).forEach {
                    logger.error("action '${key.first}' 的 ruleId '${key.second}' 重复（同一函数不能重复绑定同一个 action）")
                }
            }
    }

    private fun validateProviders(providers: List<ProviderDecl>, actions: List<ActionDecl>) {
        val declared = actions.mapTo(hashSetOf()) { it.actionId }
        providers.forEach { provider ->
            val scoped = provider.actionId ?: return@forEach
            if (scoped !in declared && !allowExternalActions) {
                logger.error(
                    "Provider ${provider.owner} 绑定了本模块未声明的 actionId '$scoped'" +
                        "（跨模块引用请开启 actionchain.allowExternalActions）",
                )
            }
        }
        fun key(provider: ProviderDecl) =
            Triple(provider.actionId.orEmpty(), provider.valueType.renderType(), provider.qualifier)

        providers.groupBy(::key)
            .filterValues { it.size > 1 }
            .forEach { (duplicateKey, duplicates) ->
                duplicates.drop(1).forEach { duplicate ->
                    logger.error("Provider 重复声明：${duplicate.owner} (key=${duplicateKey})")
                }
            }

        val globalKeys = providers.filter { it.actionId == null }
            .map { it.valueType.renderType() to it.qualifier }
            .toSet()
        providers.filter { it.actionId != null }
            .filter { (it.valueType.renderType() to it.qualifier) in globalKeys }
            .forEach { local ->
                logger.error("Provider ${local.owner} 与全局 Provider 冲突（类型/限定名相同）")
            }
    }

    private fun validateRuleParameters(rules: List<RuleDecl>, providers: List<ProviderDecl>) {
        rules.forEach { rule ->
            rule.parameters.filterIsInstance<ParamBinding.FromProvider>().forEach { binding ->
                val typeName = binding.type.renderType()
                val candidates = providers.filter { provider ->
                    (provider.actionId == null || provider.actionId == rule.actionId) &&
                        provider.valueType.renderType() == typeName &&
                        (binding.qualifier == null || provider.qualifier == binding.qualifier)
                }
                when {
                    candidates.size > 1 -> logger.error(
                        "规则 ${rule.ruleId} 的参数类型 $typeName 存在多个 Provider（" +
                            candidates.joinToString { it.owner } +
                            "），请用 @FromProvider 指定限定名",
                    )
                    candidates.isEmpty() && !allowExternalActions -> logger.error(
                        "规则 ${rule.ruleId} 找不到类型 $typeName" +
                            (binding.qualifier?.let { "、限定名 '$it'" } ?: "") +
                            " 的 Provider（跨模块引用请开启 actionchain.allowExternalActions）",
                    )
                    candidates.isEmpty() -> logger.warn(
                        "规则 ${rule.ruleId} 的参数 Provider 不在本模块，将在聚合期校验",
                    )
                }
            }
        }
    }

    private fun validateCallable(function: KSFunctionDeclaration, annotation: String): Boolean {
        val owner = function.parentDeclaration as? KSClassDeclaration
        if (owner == null || owner.classKind != ClassKind.OBJECT) {
            logger.error("$annotation 只能标注在 object 的成员函数上：${function.simpleName.asString()}", function)
            return false
        }
        if (function.functionKind != FunctionKind.MEMBER) {
            logger.error("$annotation 只支持 object 成员函数（不支持顶层/扩展函数）", function)
            return false
        }
        if (function.modifiers.contains(Modifier.PRIVATE) || function.modifiers.contains(Modifier.PROTECTED)) {
            logger.error("$annotation 方法必须至少是 internal 可见性", function)
            return false
        }
        return true
    }

    // ---------------------------------------------------------------- emission

    private fun emitFacades(actions: List<ActionDecl>, dependencies: Dependencies) {
        actions.groupBy { it.owner }.forEach { (owner, ownerActions) ->
            val ownerName = owner.qualifiedName?.asString() ?: return@forEach
            val packageName = owner.packageName.asString()
            val simpleName = owner.simpleName.asString()
            val source = ActionCodeGenerator.facadeFile(packageName, simpleName, ownerName, ownerActions)
            codeGenerator.createNewFile(dependencies, packageName, "${simpleName}Actions").use { stream ->
                stream.write(source.toByteArray())
            }
        }
    }

    private fun emitModule(
        actions: List<ActionDecl>,
        rules: List<RuleDecl>,
        providers: List<ProviderDecl>,
        dependencies: Dependencies,
    ) {
        val objectName = moduleId.toPascalCase() + "ActionModule"
        val packageName = generatedPackage
        val source = ActionCodeGenerator.moduleFile(
            packageName = packageName,
            objectName = objectName,
            moduleId = moduleId,
            actionIds = actions.map { it.actionId }.distinct(),
            rules = rules.sortedWith(compareBy({ it.actionId }, { it.stage }, { it.order }, { it.ruleId })),
            providers = providers.sortedWith(compareBy({ it.actionId ?: "" }, { it.qualifier }, { it.owner })),
        )
        codeGenerator.createNewFile(dependencies, packageName, objectName).use { stream ->
            stream.write(source.toByteArray())
        }
    }

    // ---------------------------------------------------------------- helpers

    private fun KSAnnotation.annotationName(): String? =
        annotationType.resolve().declaration.qualifiedName?.asString()

    private fun KSAnnotation.stringArgument(name: String): String? =
        arguments.firstOrNull { it.name?.asString() == name }?.value as? String

    private fun KSAnnotation.intArgument(name: String): Int? =
        arguments.firstOrNull { it.name?.asString() == name }?.value as? Int

    /** Empty string is a valid qualifier value, `null` means the annotation is absent. */
    private fun KSValueParameter.fromProviderQualifier(): String? {
        val annotation = annotations.firstOrNull { it.annotationName() == ActionChainNames.FROM_PROVIDER }
            ?: return null
        return annotation.stringArgument("qualifier").orEmpty()
    }

    private fun KSFunctionDeclaration.qualifiedOwnerName(): String =
        parentDeclaration?.qualifiedName?.asString() ?: "<unknown>"
}
