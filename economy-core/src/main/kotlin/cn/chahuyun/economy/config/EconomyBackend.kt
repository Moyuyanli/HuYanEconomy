package cn.chahuyun.economy.config

/**
 * 经济底层实现选择。
 *
 * 只有显式选择 [MIRAI_ECONOMY] 且运行环境探测到上游插件时才会使用 mirai-economy，
 * 其余情况一律回落到 [BASE]（本插件自建账户）。
 */
enum class EconomyBackend {
    /** 本插件自建账户（economy-base）：默认值，不依赖任何前置插件。 */
    BASE,

    /** 上游 mirai-economy 插件：必须先安装该插件，否则启动时降级或拒绝启动。 */
    MIRAI_ECONOMY,
}
