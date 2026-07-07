package cn.chahuyun.economy.entity

import jakarta.persistence.*
import java.io.Serializable

@Entity(name = "UserBackpack")
@Table
class UserBackpack(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: Long? = null,

    val userId: String? = null,

    val propCode: String? = null,

    val propKind: String? = null,

    val propId: Long? = null
) : Serializable
