package cn.chahuyun.economy.model.redpack

import kotlinx.serialization.Serializable

@Serializable
enum class RedPackKind(val description: String) {
    NORMAL("普通红包"),
    RANDOM("随机红包"),
    PASSWORD("口令红包")
}

/**
 * 红包 DTO。
 */
@Serializable
data class RedPackDto(
    /** 红包 ID */
    val id: Int = 0,
    /** 红包名称/祝福语 */
    val name: String = "",
    /** 群号 */
    val groupId: Long = 0,
    /** 发送者 QQ */
    val sender: Long = 0,
    /** 红包总金额 */
    val money: Double = 0.0,
    /** 红包个数 */
    val number: Int = 0,
    /** 创建时间 */
    val createTime: Long = 0,
    /** 红包类型 */
    val type: RedPackKind = RedPackKind.NORMAL,
    /** 口令，仅口令红包使用 */
    val password: String = "",
    /** 已领取金额 */
    val takenMoneys: Double = 0.0,
    /** 已领取用户 */
    val receiverList: List<Long> = emptyList(),
    /** 随机红包剩余金额列表 */
    val randomPackList: List<Double> = emptyList()
)
