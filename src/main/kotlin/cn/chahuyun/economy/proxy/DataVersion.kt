package cn.chahuyun.economy.proxy

/**
 * 数据源版本枚举
 */
enum class DataVersion {
    /** 现有实体（V1） */
    V1,
    /** 新设计实体（V2） */
    V2,
    /** 未来预留 */
    V3,
    /** Redis缓存 */
    REDIS
}
