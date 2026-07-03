package cn.chahuyun.economy.manager

import cn.chahuyun.economy.constant.FarmConstants
import cn.chahuyun.economy.constant.TitleCode
import cn.chahuyun.economy.constant.UserLocation
import cn.chahuyun.economy.data.proxy.EntityProxyRegistry
import cn.chahuyun.economy.model.user.TitleInfoDto
import cn.chahuyun.economy.model.user.UserInfoDto
import cn.chahuyun.economy.plugin.ImageManager
import cn.chahuyun.economy.privatebank.PrivateBankRepository
import cn.chahuyun.economy.utils.*
import cn.chahuyun.economy.utils.MoneyFormatUtil.toMoneyFormat
import net.mamoe.mirai.contact.*
import xyz.cssxsh.mirai.economy.service.EconomyAccount
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.IOException
import java.net.URL
import java.util.*
import javax.imageio.ImageIO

/**
 * 鐢ㄦ埛鏍稿績鑳藉姏锛氱敤鎴疯祫鏂欒幏鍙栥€佸垱寤恒€佷繚瀛橈紝浠ュ強涓汉淇℃伅鍥剧墖娓叉煋銆? */
object UserCoreManager {

    fun getUserInfoDto(qq: Long): UserInfoDto? {
        return userProxy.findByKey(qq.toString())
    }

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
            ?: throw RuntimeException("璇ョ粡娴庤处鍙蜂笉瀛樺湪鐢ㄦ埛淇℃伅")
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
        return try {
            renderUserInfoImage(userInfo, infoTitle, infoText, infoSignature)
        } catch (e: Exception) {
            Log.error("鐢ㄦ埛绠＄悊:涓汉淇℃伅鍥剧墖鐢熸垚澶辫触!", e)
            null
        }
    }

    @Throws(IOException::class)
    private fun renderUserInfoImage(
        userInfo: UserInfoDto,
        infoTitle: String,
        infoText: String,
        infoSignature: String,
    ): BufferedImage {
        val user = requireNotNull(userInfo.user) { "用户信息未附带 user 对象" }

        val avatarUrl = user.avatarUrl(AvatarSpec.LARGE)
        val avatar = ImageIO.read(URL(avatarUrl)) ?: throw IOException("头像读取失败: $avatarUrl")

        val background = ImageManager.getNextBottom()

        val title: TitleInfoDto = TitleManager.getDefaultTitle(userInfo)
        val titleText = title.title.ifBlank { "[无]" }
        val titleStartColor = runCatching { title.startColor }.getOrElse { Color.BLACK }
        val titleEndColor = runCatching { title.endColor }.getOrElse { titleStartColor }

        val nick = user.nick
        val nicknameColors = resolveNicknameColors(user, title, titleStartColor, titleEndColor)
        val signTime = userInfo.signTime
            .takeIf { it > 0 }
            ?.let { DateUtil.format(Date(it), "yyyy-MM-dd HH:mm:ss") }
            ?: "鏆傛湭绛惧埌"

        val fishInfo = userInfo.getFishInfo()
        val fishTitle = TitleManager.checkTitleIsOnEnable(userInfo, TitleCode.FISHING)

        // PersonalInfoCard 鏄€滅粯鍥句笓鐢?DTO鈥濓細閲岄潰鍙斁宸茬粡鏍煎紡鍖栧ソ鐨勫瓧绗︿覆鍜岄鑹诧紝
        return EconomyImageRenderer.renderPersonalInfo(
            PersonalInfoCard(
                qq = user.id.toString(),
                nickname = nick,
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
                fishingCooldown = GamesManager.getFishingCooldownText(userInfo, fishTitle, fishInfo),
                farmStatus = getFarmStatusText(userInfo.qq),
                bankDeposits = getBankDepositLines(user),
                infoTitle = infoTitle,
                infoText = infoText,
                infoSignature = infoSignature,
            ),
            avatar,
            background,
            ImageManager.getCustomFont()
        )
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
            else -> Triple(ImageUtil.hexColor("fce38a"), ImageUtil.hexColor("f38181"), true)
        }
    }

    private fun getLocationText(userInfo: UserInfoDto): String {
        return try {
            val status = UserStatusManager.getUserStatus(userInfo)
            UserLocation.values().firstOrNull { it.name == status.place }?.displayName ?: status.place
        } catch (e: Exception) {
            Log.error("鑾峰彇鐢ㄦ埛浣嶇疆澶辫触", e)
            "鏈煡"
        }
    }

    private fun getFarmStatusText(qq: Long): String {
        return try {
            val now = System.currentTimeMillis()
            val plots = FarmManager.getOrCreateFarm(qq).plots.filter { it.status != FarmConstants.PLOT_LOCKED }
            val planted = plots.filter { it.status == FarmConstants.PLOT_PLANTED }
            if (planted.isEmpty()) {
                "未种植"
            } else {
                val mature = planted.count { it.nextMatureAt <= now }
                when {
                    mature == 0 -> "成长中"
                    mature == planted.size -> "全成熟"
                    else -> "有成熟"
                }
            }
        } catch (e: Exception) {
            Log.error("获取农场状态失败", e)
            "未种植"
        }
    }

    private fun getBankDepositLines(user: User): List<BankDepositLine> {
        val lines = mutableListOf<BankDepositLine>()

        val mainBankMoney = EconomyUtil.getMoneyByBank(user)
        lines += BankDepositLine("主银行", mainBankMoney.toMoneyFormat(), mainBankMoney)

        try {
            PrivateBankRepository.listBanks()
                .mapNotNull { bank ->
                    val deposit = PrivateBankRepository.findDeposit(bank.code, user.id) ?: return@mapNotNull null
                    if (deposit.principal <= 0.0) return@mapNotNull null
                    BankDepositLine(bank.name.ifBlank { bank.code }, deposit.principal.toMoneyFormat(), deposit.principal)
                }
                .forEach(lines::add)
        } catch (e: Exception) {
            Log.error("鑾峰彇绉佷汉閾惰瀛樻澶辫触", e)
        }

        return lines.sortedByDescending { it.amountValue }
    }
}
