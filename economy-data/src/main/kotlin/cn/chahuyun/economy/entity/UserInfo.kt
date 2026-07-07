package cn.chahuyun.economy.entity

import jakarta.persistence.*
import java.io.Serializable
import java.util.*

/**
 * User V1 persistence entity.
 */
@Entity(name = "UserInfo")
@Table(name = "UserInfo")
class UserInfo(
    @Id
    var id: String? = null,

    var qq: Long = 0,

    var name: String? = null,

    var registerGroup: Long = 0,

    var registerTime: Date? = null,

    var sign: Boolean = false,

    var signTime: Date? = null,

    var signNumber: Int = 0,

    var oldSignNumber: Int = 0,

    var signEarnings: Double = 0.0,

    var bankEarnings: Double = 0.0,

    var defaultPrivateBankCode: String? = null,

    var funding: String? = null,

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "UserInfo_id")
    var backpacks: MutableList<UserBackpack> = mutableListOf()
) : Serializable {

    constructor(qq: Long, registerGroup: Long, name: String?, registerTime: Date?) : this(
        id = "u$qq",
        qq = qq,
        registerGroup = registerGroup,
        name = name,
        registerTime = registerTime
    )
}
