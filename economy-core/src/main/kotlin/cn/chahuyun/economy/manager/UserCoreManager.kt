package cn.chahuyun.economy.manager

import cn.chahuyun.economy.constant.TitleCode
import cn.chahuyun.economy.constant.UserLocation
import cn.chahuyun.economy.data.proxy.DataVersion
import cn.chahuyun.economy.data.proxy.EntityProxyRegistry
import cn.chahuyun.economy.data.repository.PrivateBankRepository
import cn.chahuyun.economy.data.repository.UserInfoRepository
import cn.chahuyun.economy.game.GameOverviewBridge
import cn.chahuyun.economy.image.PersonalInfoImageRenderer
import cn.chahuyun.economy.image.model.BankDepositLine
import cn.chahuyun.economy.image.model.PersonalInfoCard
import cn.chahuyun.economy.model.user.*
import cn.chahuyun.economy.utils.DateUtil
import cn.chahuyun.economy.utils.EconomyUtil
import cn.chahuyun.economy.utils.FormatUtil
import cn.chahuyun.economy.utils.Log
import cn.chahuyun.economy.utils.MoneyFormatUtil.toMoneyFormat
import net.mamoe.mirai.contact.*
import xyz.cssxsh.mirai.economy.service.EconomyAccount
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.IOException
import java.util.*

/**
 * 用户核心能力：用户资料获取、创建、保存，以及个人信息图片渲染。
 */
object UserCoreManager {

    fun getUserInfoDto(qq: Long): UserInfoDto? {
        return userProxy.findByKey(qq.toString())
    }

    fun saveUserInfoDto(dto: UserInfoDto): UserInfoDto {
        return userProxy.save(dto)
    }

    fun listRankingUsers(): List<UserInfoDto> {
        return when (userProxy.getCurrentVersion()) {
            DataVersion.V2 -> UserInfoRepository.listRankingUsersV2()
            else -> UserInfoRepository.listRankingUsers()
        }
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
    fun getUserInfoImageBase(
        userInfo: UserInfoDto,
        infoText: String = "愿你今日顺利，金币入账。",
        infoSignature: String = "-- HuYanEconomy",
        infoTitle: String = "今日一句",
    ): BufferedImage? {
        return runCatching {
            buildUserInfoCard(userInfo, infoText, infoSignature, infoTitle)
        }.mapCatching { card ->
            renderUserInfoCard(userInfo, card) ?: throw IOException("个人信息图片渲染失败")
        }.getOrElse { e ->
            Log.error("用户管理: 个人信息图片生成失败!", e)
            null
        }
    }

    @JvmStatic
    fun buildUserInfoCard(
        userInfo: UserInfoDto,
        infoText: String = "愿你今日顺利，金币入账。",
        infoSignature: String = "-- HuYanEconomy",
        infoTitle: String = "今日一句",
    ): PersonalInfoCard {
        val user = requireNotNull(userInfo.user) { "用户信息未附带 user 对象" }

        val title: TitleInfoDto = TitleManager.getDefaultTitle(userInfo)
        val titleText = title.title.ifBlank { "[无]" }
        val titleStartColor = runCatching { title.startColor }.getOrElse { Color.BLACK }
        val titleEndColor = runCatching { title.endColor }.getOrElse { titleStartColor }

        val nicknameColors = resolveNicknameColors(user, title, titleStartColor, titleEndColor)
        val signTime = userInfo.signTime
            .takeIf { it > 0 }
            ?.let { DateUtil.format(Date(it), "yyyy-MM-dd HH:mm:ss") }
            ?: "暂未签到"

        val fishInfo = userInfo.getFishInfo()
        val fishTitle = TitleManager.checkTitleIsOnEnable(userInfo, TitleCode.FISHING)

        // PersonalInfoCard is a rendering DTO; keep all text and colors preformatted here.
        return PersonalInfoCard(
            qq = user.id.toString(),
            nickname = user.nick,
            title = titleText,
            titleStartColor = titleStartColor,
            titleEndColor = titleEndColor,
            titleGradient = title.gradient,
            nicknameStartColor = nicknameColors.first,
            nicknameEndColor = nicknameColors.second,
            nicknameGradient = nicknameColors.third,
            signTime = signTime,
            signDays = "${userInfo.signNumber} 天",
            wallet = EconomyUtil.getMoneyByUser(user).toMoneyFormat(),
            signEarnings = userInfo.signEarnings.toMoneyFormat(),
            bankEarnings = userInfo.bankEarnings.toMoneyFormat(),
            location = getLocationText(userInfo),
            fishingCooldown = GameOverviewBridge.fishingCooldownText(userInfo, fishTitle, fishInfo),
            farmStatus = GameOverviewBridge.farmStatusText(userInfo.qq),
            bankDeposits = getBankDepositLines(user),
            infoTitle = infoTitle,
            infoText = infoText,
            infoSignature = infoSignature,
        )
    }

    @JvmStatic
    fun renderUserInfoCard(userInfo: UserInfoDto, card: PersonalInfoCard): BufferedImage? {
        return try {
            val user = requireNotNull(userInfo.user) { "用户信息未附带 user 对象" }
            PersonalInfoImageRenderer.render(card, user.avatarUrl(AvatarSpec.LARGE))
        } catch (e: Exception) {
            Log.error("用户管理: 个人信息图片生成失败!", e)
            null
        }
    }

    private fun resolveNicknameColors(
        user: User,
        title: TitleInfoDto,
        titleStartColor: Color,
        titleEndColor: Color,
    ): Triple<Color, Color, Boolean> {
        if (title.impactName) {
            return Triple(titleStartColor, titleEndColor, true)
        }

        val member = user as? Member ?: return Triple(Color.BLACK, Color.BLACK, false)
        return when (member.permission) {
            MemberPermission.OWNER -> Triple(Color(68, 138, 255), Color(100, 255, 218), true)
            MemberPermission.ADMINISTRATOR -> Triple(Color(72, 241, 155), Color(140, 241, 72), true)
            else -> Triple(Color(0xfc, 0xe3, 0x8a), Color(0xf3, 0x81, 0x81), true)
        }
    }

    private fun getLocationText(userInfo: UserInfoDto): String {
        return try {
            val status = UserStatusManager.getUserStatus(userInfo)
            UserLocation.values().firstOrNull { it.name == status.place }?.displayName ?: status.place
        } catch (e: Exception) {
            Log.error("获取用户位置失败", e)
            "未知"
        }
    }

    private fun getBankDepositLines(user: User): List<BankDepositLine> {
        val lines = mutableListOf<BankDepositLine>()

        val mainBankMoney = EconomyUtil.getMoneyByBank(user)
        val mainBankInterest = BankManager.getBankInfo(1)?.interest ?: 0
        lines += BankDepositLine(
            "主银行",
            mainBankMoney.toMoneyFormat(),
            "利率 ${FormatUtil.fixed(mainBankInterest / 10.0, 1)}%",
            mainBankMoney
        )

        try {
            PrivateBankRepository.listBanks()
                .mapNotNull { bank ->
                    val deposit = PrivateBankRepository.findDeposit(bank.code, user.id) ?: return@mapNotNull null
                    if (deposit.principal <= 0.0) return@mapNotNull null
                    BankDepositLine(
                        bank.name.ifBlank { bank.code },
                        deposit.principal.toMoneyFormat(),
                        "利率 ${FormatUtil.fixed(bank.depositorInterest / 10.0, 1)}%",
                        deposit.principal
                    )
                }
                .forEach(lines::add)
        } catch (e: Exception) {
            Log.error("获取私人银行存款失败", e)
        }

        return lines.sortedByDescending { it.amountValue }
    }
}
