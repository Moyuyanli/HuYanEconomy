package cn.chahuyun.economy.actionchain.runtime

import kotlin.reflect.KClass
import kotlin.jvm.javaObjectType

/** Mutable state scoped to a single Action invocation. */
class ActionContext internal constructor(
    val actionId: String,
    val event: Any,
    private val providerResolver: suspend (ActionContext, KClass<*>, String?) -> Any,
) {
    private val values = mutableMapOf<ValueKey, Any>()
    private val providerCache = mutableMapOf<ValueKey, Any>()
    private var storedActionResult: Any? = null
    private var hasActionResult = false

    val attrs: MutableMap<String, Any?> = mutableMapOf()

    fun <T : Any> put(type: KClass<T>, value: T, qualifier: String = "") {
        values[ValueKey(type, qualifier)] = value
    }

    fun <T : Any> get(type: KClass<T>, qualifier: String = ""): T? =
        values[ValueKey(type, qualifier)]?.takeIf { type.javaObjectType.isInstance(it) }
            ?.let(type.javaObjectType::cast)

    suspend fun <T : Any> provide(type: KClass<T>, qualifier: String? = null): T {
        val value = providerResolver(this, type, qualifier)
        require(type.javaObjectType.isInstance(value)) {
            "Provider for ${type.qualifiedName} returned ${value::class.qualifiedName}"
        }
        return type.javaObjectType.cast(value)
    }

    fun <T : Any> actionResult(type: KClass<T>): T? {
        if (!hasActionResult) return null
        return storedActionResult?.takeIf(type.javaObjectType::isInstance)?.let(type.javaObjectType::cast)
    }

    internal fun setActionResult(value: Any?) {
        storedActionResult = value
        hasActionResult = true
    }

    internal fun cachedProvider(type: KClass<*>, qualifier: String): Any? =
        providerCache[ValueKey(type, qualifier)]

    internal fun cacheProvider(type: KClass<*>, qualifier: String, value: Any) {
        providerCache[ValueKey(type, qualifier)] = value
    }

    private data class ValueKey(val type: KClass<*>, val qualifier: String)
}
