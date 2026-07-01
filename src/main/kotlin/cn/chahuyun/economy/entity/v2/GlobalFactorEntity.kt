package cn.chahuyun.economy.entity.v2

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.io.Serializable

@Entity(name = "GlobalFactorEntityV2")
@Table(name = "hye_global_factor")
class GlobalFactorEntity(
    @Id
    @Column(name = "id")
    var id: Long = 0,

    @Column(name = "rob_factor", nullable = false)
    var robFactor: Double = 0.4,

    @Column(name = "rob_blank_factor", nullable = false)
    var robBlankFactor: Double = 0.01,

    @Column(name = "created_at", nullable = false)
    var createdAt: Long = 0,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Long = 0
) : Serializable
