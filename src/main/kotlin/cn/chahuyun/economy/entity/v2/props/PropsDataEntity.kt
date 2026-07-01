package cn.chahuyun.economy.entity.v2.props

import jakarta.persistence.*
import java.io.Serializable

@Entity(name = "PropsDataEntityV2")
@Table(
    name = "hye_props_data",
    indexes = [
        Index(name = "idx_hye_props_kind_code", columnList = "kind,code"),
        Index(name = "idx_hye_props_expired_time", columnList = "expired_time")
    ]
)
class PropsDataEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long = 0,

    @Column(name = "kind", length = 128)
    var kind: String = "",

    @Column(name = "code", length = 128)
    var code: String = "",

    @Column(name = "num", nullable = false)
    var num: Int = 1,

    @Column(name = "expired_time", nullable = false)
    var expiredTime: Long = 0,

    @Column(name = "status", nullable = false)
    var status: Boolean = false,

    @Lob
    @Column(name = "data", columnDefinition = "TEXT")
    var data: String = "",

    @Column(name = "created_at", nullable = false)
    var createdAt: Long = 0,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Long = 0
) : Serializable
