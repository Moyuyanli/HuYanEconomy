package cn.chahuyun.economy.ledger

/**
 * 上游 mirai-economy 可用性探测。
 *
 * 只做类存在性判断：`Class.forName(..., initialize = false)` 不会触发上游的静态初始化，
 * 也不会因为上游插件未安装而抛异常到调用方。真正的功能性自检在
 * [MiraiEconomyLedger.start] 里做一次真实余额读取。
 */
internal object MiraiEconomyProbe {

    /** 上游服务入口的全限定名；不要改成 Kotlin 常量引用，否则本文件会直接依赖上游类型。 */
    private const val SERVICE_CLASS = "xyz.cssxsh.mirai.economy.EconomyService"

    /** 上游依赖是否存在于当前运行环境。 */
    fun available(): Boolean = runCatching {
        Class.forName(SERVICE_CLASS, false, MiraiEconomyProbe::class.java.classLoader)
        true
    }.getOrElse { false }
}
