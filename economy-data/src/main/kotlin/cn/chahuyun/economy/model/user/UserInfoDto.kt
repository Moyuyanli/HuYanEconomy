package cn.chahuyun.economy.model.user

import kotlinx.serialization.Serializable

/**
 * User data transfer object shared by V1/V2 data access.
 */
@Serializable
data class UserInfoDto(
    var id: String = "",
    var qq: Long = 0,
    var name: String = "",
    var registerGroup: Long = 0,
    var registerTime: Long = 0,
    var sign: Boolean = false,
    var signTime: Long = 0,
    var signNumber: Int = 0,
    var oldSignNumber: Int = 0,
    var signEarnings: Double = 0.0,
    var bankEarnings: Double = 0.0,
    var defaultPrivateBankCode: String = "",
    var funding: String = "",
    var backpackCount: Int = 0,
    var backpacks: List<UserBackpackDto> = emptyList()
)
