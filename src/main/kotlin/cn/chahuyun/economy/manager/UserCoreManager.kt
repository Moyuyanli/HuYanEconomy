package cn.chahuyun.economy.manager

import cn.chahuyun.economy.constant.ImageDrawXY
import cn.chahuyun.economy.model.user.TitleInfoDto
import cn.chahuyun.economy.model.user.UserInfoDto
import cn.chahuyun.economy.plugin.ImageManager
import cn.chahuyun.economy.proxy.EntityProxyRegistry
import cn.chahuyun.economy.utils.EconomyUtil
import cn.chahuyun.economy.utils.ImageUtil
import cn.chahuyun.economy.utils.Log
import cn.chahuyun.economy.utils.MoneyFormatUtil.toMoneyFormat
import cn.hutool.core.date.DateUtil
import net.mamoe.mirai.contact.*
import xyz.cssxsh.mirai.economy.service.EconomyAccount
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.io.IOException
import java.net.URL
import java.util.*
import javax.imageio.ImageIO

/**
 * 从原 Java `UserManager` 中抽离出来的“非消息事件”逻辑（纯工具/数据层）。
 *
 * - 事件/指令处理应放在 action 包（如 `UserAction.java` 的 @MessageAuthorize 方法）。
 * - 这里仅保留：用户信息获取/创建、个人信息底图绘制等工具方法。
 */
object UserCoreManager {

    /**
     * 通过代理器获取用户信息DTO
     *
     * @param qq QQ号
     * @return 用户信息DTO，不存在则返回null
     */
    fun getUserInfoDto(qq: Long): UserInfoDto? {
        return userProxy.findByKey(qq.toString())
    }

    /**
     * 通过代理器保存用户信息
     *
     * @param dto 用户信息DTO
     * @return 保存后的DTO
     */
    fun saveUserInfoDto(dto: UserInfoDto): UserInfoDto {
        return userProxy.save(dto)
    }

    @JvmStatic
    fun getUserInfo(user: User): UserInfoDto {
        val userId = user.id
        val group: Group? = (user as? Member)?.group
        val dto = getUserInfoDto(userId)

        val existingOrCreated = if (dto == null) {
            saveUserInfoDto(
                UserInfoDto(
                    id = net.mamoe.mirai.console.permission.AbstractPermitteeId.ExactUser(userId).asString(),
                    qq = userId,
                    name = user.nick,
                    registerGroup = group?.id ?: 0,
                    registerTime = Date().time
                )
            )
        } else {
            dto
        }

        existingOrCreated.user = user
        existingOrCreated.group = group
        return existingOrCreated
    }

    @JvmStatic
    fun getUserInfo(account: EconomyAccount): UserInfoDto {
        val userId = account.uuid
        return userProxy.findWhere { it.id == userId }.firstOrNull()
            ?: throw RuntimeException("该经济账号不存在用户信息")
    }

    @JvmStatic
    fun getUserInfo(userId: Long?): UserInfoDto? {
        if (userId == null) return null
        return getUserInfoDto(userId)
    }

    @JvmStatic
    fun getUserInfo(uuid: String?): UserInfoDto? {
        if (uuid.isNullOrBlank()) return null
        return userProxy.findWhere { it.funding == uuid }.firstOrNull()
    }

    private val userProxy
        get() = EntityProxyRegistry.get<UserInfoDto>("user") ?: error("用户代理器未初始化")

    fun saveUserInfo(userInfo: UserInfoDto): UserInfoDto {
        return saveUserInfoDto(userInfo)
    }

    @JvmStatic
    fun getUserInfoImageBase(userInfo: UserInfoDto): BufferedImage? {
        return try {
            customBottom(userInfo)
        } catch (e: Exception) {
            Log.error("用户管理:个人信息基础信息绘图错误!", e)
            null
        }
    }

    @Throws(IOException::class)
    private fun customBottom(userInfo: UserInfoDto): BufferedImage {
        val bottom = ImageManager.getNextBottom() ?: throw IOException("没有自定义底图，请检查data/bottom文件夹底图!")

        val user = requireNotNull(userInfo.user) { "用户信息中未附带 user 对象" }

        // 使用 Java getter 形式，避免 Kotlin 侧对 avatarUrl 的属性/扩展函数解析差异导致不兼容
        val avatarUrl = user.avatarUrl(AvatarSpec.LARGE)
        var avatar = ImageIO.read(URL(avatarUrl)) ?: throw IOException("头像读取失败: $avatarUrl")
        avatar = ImageUtil.makeRoundedCorner(avatar, 50)

        val g2d: Graphics2D = ImageUtil.getG2d(bottom)
        g2d.drawImage(
            avatar,
            ImageDrawXY.AVATAR.x,
            ImageDrawXY.AVATAR.y,
            avatar.width,
            avatar.height,
            null
        )

        val font: Font = ImageManager.getCustomFont()
        g2d.font = font
        g2d.color = Color.BLACK

        val id = user.id.toString()
        val title: TitleInfoDto = TitleManager.getDefaultTitle(userInfo)
        val titleText = title.title ?: "[无]"
        val titleStartColor = runCatching { title.startColor }.getOrElse { Color.BLACK }
        val titleEndColor = runCatching { title.endColor }.getOrElse { titleStartColor }

        g2d.font = font.deriveFont(32f)
        if (title.gradient) {
            ImageUtil.drawStringGradient(
                titleText,
                ImageDrawXY.TITLE.x,
                ImageDrawXY.TITLE.y,
                titleStartColor,
                titleEndColor,
                g2d
            )
            ImageUtil.drawStringGradient(
                id,
                ImageDrawXY.ID.x,
                ImageDrawXY.ID.y,
                titleStartColor,
                titleEndColor,
                g2d
            )
        } else {
            g2d.color = titleStartColor
            g2d.drawString(id, ImageDrawXY.ID.x, ImageDrawXY.ID.y)
            g2d.drawString(titleText, ImageDrawXY.TITLE.x, ImageDrawXY.TITLE.y)
        }

        val nick = user.nick
        var gradient = false
        var sColor: Color? = null
        var eColor: Color? = null

        if (title.impactName) {
            gradient = true
            sColor = titleStartColor
            eColor = titleEndColor
        } else {
            val member = user as? Member
            if (member != null) {
                gradient = true
                when (member.permission) {
                    MemberPermission.OWNER -> {
                        sColor = Color(68, 138, 255)
                        eColor = Color(100, 255, 218)
                    }

                    MemberPermission.ADMINISTRATOR -> {
                        sColor = Color(72, 241, 155)
                        eColor = Color(140, 241, 72)
                    }

                    else -> {
                        sColor = ImageUtil.hexColor("fce38a")
                        eColor = ImageUtil.hexColor("f38181")
                    }
                }
            }
        }

        g2d.font = if (nick.length > 16) font.deriveFont(Font.BOLD, 50f) else font.deriveFont(Font.BOLD, 60f)
        if (gradient) {
            ImageUtil.drawStringGradient(
                nick,
                ImageDrawXY.NICK_NAME.x,
                ImageDrawXY.NICK_NAME.y,
                sColor ?: Color.BLACK,
                eColor ?: Color.BLACK,
                g2d
            )
            g2d.color = Color.BLACK
        } else {
            g2d.color = Color.BLACK
            g2d.drawString(nick, ImageDrawXY.NICK_NAME.x, ImageDrawXY.NICK_NAME.y)
        }

        val signTime = userInfo.signTime.takeIf { it > 0 }?.let { DateUtil.format(Date(it), "yyyy-MM-dd HH:mm:ss") } ?: "暂未签到"

        g2d.font = font.deriveFont(Font.PLAIN, 24f)
        g2d.drawString(signTime, ImageDrawXY.SIGN_TIME.x, ImageDrawXY.SIGN_TIME.y)
        g2d.drawString(userInfo.signNumber.toString(), ImageDrawXY.SIGN_NUM.x, ImageDrawXY.SIGN_NUM.y)

        val money = EconomyUtil.getMoneyByUser(user).toMoneyFormat()
        val bank = EconomyUtil.getMoneyByBank(user).toMoneyFormat()
        val signEarnings = userInfo.signEarnings.toMoneyFormat()
        val bankEarnings = userInfo.bankEarnings.toMoneyFormat()

        g2d.font = font.deriveFont(32f)
        g2d.drawString(money, ImageDrawXY.MY_MONEY.x, ImageDrawXY.MY_MONEY.y)
        g2d.drawString(signEarnings, ImageDrawXY.SIGN_OBTAIN.x, ImageDrawXY.SIGN_OBTAIN.y)
        g2d.drawString(bank, ImageDrawXY.BANK_MONEY.x, ImageDrawXY.BANK_MONEY.y)
        g2d.drawString(bankEarnings, ImageDrawXY.BANK_INTEREST.x, ImageDrawXY.BANK_INTEREST.y)

        g2d.dispose()
        return bottom
    }
}


