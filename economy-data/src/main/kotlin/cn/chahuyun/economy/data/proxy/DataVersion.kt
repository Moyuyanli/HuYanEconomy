package cn.chahuyun.economy.data.proxy

/**
 * 数据源版本枚举。
 */
enum class DataVersion {
    /** 当前稳定实体版本。 */
    V1,
    /** 新设计实体版本。 */
    V2,
    /** 未来预留。 */
    V3,
    /** Redis 缓存层。 */
    REDIS
}
