package cn.chahuyun.economy.entity.fish

import jakarta.persistence.*
import java.io.Serializable

/**
 * Fish pond V1 persistence entity.
 */
@Entity(name = "FishPond")
@Table
class FishPond(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    var id: Int = 0,

    var code: String = "",

    var admin: Long = 0,

    var pondType: Int = 0,

    var name: String? = null,

    var description: String? = null,

    var pondLevel: Int = 0,

    var minLevel: Int = 0,

    var rebate: Double = 0.05,

    var number: Int = 0,

    @OneToMany(targetEntity = Fish::class, fetch = FetchType.EAGER)
    @JoinColumn(name = "pond_id")
    var fishList: MutableList<Fish>? = null
) : Serializable {

    constructor(pondType: Int, group: Long, admin: Long, name: String?, description: String?) : this(
        code = when (pondType) {
            1 -> "g-$group"
            2 -> "g-$group-$admin"
            else -> admin.toString()
        },
        admin = admin,
        name = name,
        description = description,
        pondLevel = if (pondType == 1) 6 else 1,
        pondType = pondType,
        minLevel = 0,
        rebate = 0.05,
        number = 0
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FishPond) return false
        return code == other.code
    }

    override fun hashCode(): Int {
        return code.hashCode()
    }
}
