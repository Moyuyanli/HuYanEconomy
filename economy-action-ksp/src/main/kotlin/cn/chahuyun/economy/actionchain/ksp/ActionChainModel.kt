package cn.chahuyun.economy.actionchain.ksp

import com.google.devtools.ksp.symbol.*

internal object ActionChainNames {
    const val ACTION = "cn.chahuyun.economy.actionchain.annotation.Action"
    const val BEFORE = "cn.chahuyun.economy.actionchain.annotation.Before"
    const val AFTER = "cn.chahuyun.economy.actionchain.annotation.After"
    const val PROVIDER = "cn.chahuyun.economy.actionchain.annotation.Provider"
    const val FROM_PROVIDER = "cn.chahuyun.economy.actionchain.annotation.FromProvider"

    const val CONTEXT = "cn.chahuyun.economy.actionchain.runtime.ActionContext"
    const val ACTION_RESULT = "cn.chahuyun.economy.actionchain.runtime.ActionResult"
    const val UNIT = "kotlin.Unit"

    const val OPTION_MODULE_ID = "actionchain.moduleId"
    const val OPTION_PACKAGE = "actionchain.generatedPackage"
    const val OPTION_ALLOW_EXTERNAL = "actionchain.allowExternalActions"

    const val DEFAULT_PACKAGE = "cn.chahuyun.economy.actionchain.generated"
    const val DEFAULT_MODULE_ID = "unnamed-module"
}

internal enum class RuleStage { BEFORE, AFTER }

/** One parameter of a rule or provider function, resolved at compile time. */
internal sealed interface ParamBinding {
    /** Pass the invocation context itself. */
    data object Context : ParamBinding

    /** Resolve a value through [cn.chahuyun.economy.actionchain.runtime.ActionContext.provide]. */
    data class FromProvider(val type: KSType, val qualifier: String?) : ParamBinding
}

internal data class ActionDecl(
    val actionId: String,
    val owner: KSClassDeclaration,
    val functionName: String,
    val eventType: KSType,
    val returnsUnit: Boolean,
    val returnType: KSType?,
    val injectContext: Boolean,
)

internal data class RuleDecl(
    val actionId: String,
    val stage: RuleStage,
    val order: Int,
    val ruleId: String,
    val owner: String,
    val functionName: String,
    val parameters: List<ParamBinding>,
    val returnsActionResult: Boolean,
)

internal data class ProviderDecl(
    val actionId: String?,
    val qualifier: String,
    val valueType: KSType,
    val owner: String,
    val functionName: String,
    val takesContext: Boolean,
)

/** Renders a resolved type as Kotlin source, keeping type arguments and nullability. */
internal fun KSType.renderType(): String {
    val declaration = declaration
    val name = when (declaration) {
        is KSTypeParameter -> declaration.name.asString()
        else -> declaration.qualifiedName?.asString() ?: declaration.simpleName.asString()
    }
    val arguments = if (arguments.isEmpty()) {
        ""
    } else {
        arguments.joinToString(prefix = "<", postfix = ">") { argument ->
            if (argument.variance == Variance.STAR) {
                "*"
            } else {
                val prefix = when (argument.variance) {
                    Variance.COVARIANT -> "out "
                    Variance.CONTRAVARIANT -> "in "
                    else -> ""
                }
                prefix + (argument.type?.resolve()?.renderType() ?: "*")
            }
        }
    }
    val nullable = if (nullability == Nullability.NULLABLE) "?" else ""
    return name + arguments + nullable
}

/** Class literal usable for `Type::class`; type arguments are dropped on purpose. */
internal fun KSType.renderClassLiteral(): String? =
    (declaration as? KSClassDeclaration)?.qualifiedName?.asString()

internal fun KSType.qualifiedName(): String? = declaration.qualifiedName?.asString()

internal fun KSType.isUnit(): Boolean = qualifiedName() == ActionChainNames.UNIT

/** True when the type is (a subtype of) the runtime `ActionResult` interface. */
internal fun KSType.isActionResult(): Boolean {
    if (qualifiedName() == ActionChainNames.ACTION_RESULT) return true
    val declaration = declaration as? KSClassDeclaration ?: return false
    return declaration.superTypes.any { superType -> superType.resolve().isActionResult() }
}

internal fun String.escapeKotlin(): String = replace("\\", "\\\\").replace("\"", "\\\"")

/** `economy-core` / `economy_core` -> `EconomyCore`. */
internal fun String.toPascalCase(): String =
    split('-', '_', '.', ' ')
        .filter { it.isNotEmpty() }
        .joinToString("") { part -> part.replaceFirstChar { it.uppercaseChar() } }

/** `economy-core` -> `economycore`, usable as a package segment. */
internal fun String.toPackageSegment(): String =
    filter { it.isLetterOrDigit() }.lowercase().ifEmpty { "generated" }
