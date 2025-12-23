package cn.chahuyun.economy.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * 用户属性
 *
 * @author Moyuyanli
 * @date 2024-10-17 10:45
 */
@Entity
@Table(name = "UserProperty")
class UserProperty(
    @Id
    var id: Long? = null
)
