package cn.chahuyun.economy.actionchain.runtime

import kotlin.reflect.KClass

/**
 * 一次 Action 调用内的可变状态容器。
 *
 * 生命周期与调用同寿：引擎每次 [ActionEngine.invokeFromEntry] 都会新建一个实例，
 * 调用结束即丢弃，因此**不要在规则里把它保存到对象字段里长期持有**。
 *
 * 它承担三件事：
 * 1. 暴露本次调用的事件与动作 id（[actionId]、[event]）；
 * 2. 提供业务自定义的属性传递通道（[attrs] 与 [put] / [get]），让不同规则交换中间结果；
 * 3. 承载动作返回值（[actionResult]，由引擎在动作执行后写入），供 after 规则读取。
 *
 * Provider 解析也挂在这里（[provide]），并按“类型 + 限定名”缓存，保证同一次调用内只加载一次。
 *
 * 构造函数是 `internal` 的：只允许引擎创建，业务侧不要自行 new。
 *
 * @property actionId 本次调用对应的动作 id。
 * @property event 触发本次调用的事件对象（由入口原样传入，框架不做类型假设）。
 */
class ActionContext internal constructor(
    val actionId: String,
    val event: Any,
    /**
     * Provider 解析回调，由 [ActionEngine] 注入。
     *
     * 参数依次为：当前上下文、目标类型、限定名（`null` 表示未指定）。
     * 之所以用回调而不是直接持有引擎，是为了让上下文与引擎解耦，也方便测试替身。
     */
    private val providerResolver: suspend (ActionContext, KClass<*>, String?) -> Any,
) {
    /** 业务侧通过 [put] 放入的临时值，按“类型 + 限定名”索引。 */
    private val values = mutableMapOf<ValueKey, Any>()

    /** Provider 加载结果缓存，保证同一次调用内同一 Provider 只执行一次。 */
    private val providerCache = mutableMapOf<ValueKey, Any>()

    /** 动作本体的返回值；只有当动作真正执行过之后才有效。 */
    private var storedActionResult: Any? = null

    /** 标记动作是否已经执行过——用于区分“返回 null”和“从未执行”。 */
    private var hasActionResult = false

    /**
     * 业务自定义的属性袋，键为任意字符串，值为任意对象（可为 `null`）。
     *
     * 典型用法：before 规则算出冷却剩余时间，放进 `attrs["cooldown"]`，动作或 after 规则再取出来用。
     *
     * 注意：
     * - 只在本次调用内有效，不会跨请求共享；
     * - 框架不校验键名，建议用 `模块.含义` 形式（如 `fishing.cooldown`）避免撞键；
     * - 这里放的是“运行期数据”，不要放需要持久化的业务状态。
     */
    val attrs: MutableMap<String, Any?> = mutableMapOf()

    /**
     * 向上下文放入一个值，供后续规则或动作通过 [get] 读取。
     *
     * 同一个“类型 + 限定名”重复放入会覆盖旧值，后写生效。
     *
     * @param type 值类型，参与索引与读取时的类型校验。
     * @param value 要放入的值，必须与 [type] 兼容。
     * @param qualifier 限定名，同一类型需要放多个值（如“转出账户/转入账户”）时用于区分。
     */
    fun <T : Any> put(type: KClass<T>, value: T, qualifier: String = "") {
        values[ValueKey(type, qualifier)] = value
    }

    /**
     * 读取 [put] 放入的值。
     *
     * 如果键不存在，或存进去的对象与实际类型不匹配（例如用父类型键放子类型值后换了键读取），
     * 返回 `null`，不会抛异常。
     *
     * @param type 期望的值类型。
     * @param qualifier 限定名，需与 [put] 时一致。
     * @return 命中的值；未命中返回 `null`。
     */
    fun <T : Any> get(type: KClass<T>, qualifier: String = ""): T? =
        values[ValueKey(type, qualifier)]?.takeIf { type.javaObjectType.isInstance(it) }
            ?.let(type.javaObjectType::cast)

    /**
     * 从 Provider 解析一个值（不存在时抛异常）。
     *
     * 这是 KSP 生成代码注入规则参数的入口：生成代码把参数类型（可能带限定名）传进来，
     * 真正查找 Provider、缓存结果、处理歧义都在 [ActionEngine.resolveProvider] 内部完成。
     *
     * @param type 需要的值类型。
     * @param qualifier 限定名；`null` 表示“未指定”，引擎会按类型匹配并在多候选时报歧义。
     * @return 解析到的值。
     * @throws ActionConfigurationException 没有匹配的 Provider、存在多个候选，或返回类型不符。
     */
    suspend fun <T : Any> provide(type: KClass<T>, qualifier: String? = null): T {
        val value = providerResolver(this, type, qualifier)
        // 双保险：引擎侧已经校验过，这里再挡一次，避免自定义 resolver 返回错误类型。
        require(type.javaObjectType.isInstance(value)) {
            "Provider for ${type.qualifiedName} returned ${value::class.qualifiedName}"
        }
        return type.javaObjectType.cast(value)
    }

    /**
     * 读取动作本体的返回值。
     *
     * 只有动作真正执行过（[ActionExecution.actionExecuted] 为 `true`）才可能拿到值；
     * before 阶段提前结束时返回 `null`。类型不匹配同样返回 `null`。
     *
     * @param type 期望的返回值类型，需与动作声明一致。
     * @return 动作返回值；未执行或类型不符时返回 `null`。
     */
    fun <T : Any> actionResult(type: KClass<T>): T? {
        if (!hasActionResult) return null
        return storedActionResult?.takeIf(type.javaObjectType::isInstance)?.let(type.javaObjectType::cast)
    }

    /**
     * 由引擎在动作执行完成后写入返回值。
     *
     * 即使动作返回 `null` 也会把 [hasActionResult] 置为 `true`，
     * 这样 after 规则能区分“动作执行了但返回 null”和“动作根本没执行”。
     *
     * @param value 动作返回值，允许 `null`。
     */
    internal fun setActionResult(value: Any?) {
        storedActionResult = value
        hasActionResult = true
    }

    /**
     * 查询 Provider 缓存。
     *
     * 由引擎调用；命中表示本次调用已经解析过同一 Provider，可直接复用。
     *
     * @param type 值类型。
     * @param qualifier 限定名（未指定时由引擎归一化为空串）。
     * @return 缓存值；未命中返回 `null`。
     */
    internal fun cachedProvider(type: KClass<*>, qualifier: String): Any? =
        providerCache[ValueKey(type, qualifier)]

    /**
     * 写入 Provider 缓存。
     *
     * @param type 值类型。
     * @param qualifier 限定名（未指定时由引擎归一化为空串）。
     * @param value 加载结果。
     */
    internal fun cacheProvider(type: KClass<*>, qualifier: String, value: Any) {
        providerCache[ValueKey(type, qualifier)] = value
    }

    /** 上下文内部索引键：类型 + 限定名。临时值与 Provider 缓存都用它做键。 */
    private data class ValueKey(val type: KClass<*>, val qualifier: String)
}
