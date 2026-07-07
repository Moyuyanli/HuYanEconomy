package cn.chahuyun.economy.entity.props

import jakarta.persistence.*
import java.util.*

/**
 * 道具数据实体 (优化重构版)
 * 采用“混合存储”模式：核心字段提取为列，扩展数据保留 JSON
 *
 * @author Moyuyanli
 */
@Entity(name = "PropsData")
@Table
class PropsData(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    /**
     * 道具大类
     */
    var kind: String? = null,

    /**
     * 道具编码
     */
    var code: String? = null,

    /**
     * 数量 (提取为列以优化查询)
     */
    @Column(columnDefinition = "integer default 1")
    var num: Int = 1,

    /**
     * 过期时间 (提取为列，支持索引查询)
     */
    var expiredTime: Date? = null,

    /**
     * 状态 (提取为列，例如：已激活/已穿戴)
     */
    @Column(columnDefinition = "boolean default false")
    var status: Boolean = false,

    /**
     * 动态扩展数据 (JSON 存储)
     */
    @Lob
    @Column(columnDefinition = "TEXT")
    var data: String? = null
)
