package cn.chahuyun.economy.entity

import cn.chahuyun.economy.utils.ImageUtil
import jakarta.persistence.*
import java.awt.Color
import java.io.Serializable
import java.util.*

/**
 * 称号信息
 *
 * @author Moyuyanli
 * @date 2022/12/5 17:01
 */
@Entity(name = "TitleInfo")
@Table
class TitleInfo(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    var id: Int = 0,

    /**
     * 所属者用户id
     */
    var userId: Long = 0,

    /**
     * 称号类型
     */
    var code: String? = null,

    /**
     * 称号名称
     */
    var name: String? = null,

    /**
     * 使用状态
     */
    var status: Boolean = false,

    /**
     * 称号
     */
    var title: String? = null,

    /**
     * 是否影响名称
     */
    var impactName: Boolean = false,

    /**
     * 是否渐变
     */
    var gradient: Boolean = false,

    /**
     * 称号颜色
     */
    var sColor: String? = null,

    /**
     * 称号颜色
     */
    var eColor: String? = null,

    /**
     * 称号到期时间
     */
    var dueTime: Date? = null
) : Serializable {

    val startColor: Color
        get() = ImageUtil.hexColor(sColor)

    val endColor: Color
        get() = ImageUtil.hexColor(eColor)

    constructor(userId: Long, code: String?, name: String?, title: String?) : this(
        userId = userId,
        code = code,
        name = name,
        title = title,
        status = false
    )
}
