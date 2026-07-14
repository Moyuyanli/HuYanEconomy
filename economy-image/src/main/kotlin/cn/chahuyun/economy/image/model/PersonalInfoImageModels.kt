package cn.chahuyun.economy.image.model

import java.awt.Color

/**
 * 个人信息图中“银行存款”面板的一行数据。
 */
data class BankDepositLine(
    /** 银行展示名。 */
    val bankName: String,
    /** 已格式化好的金额文本，例如 1.52w。 */
    val amount: String,
    /** 补充信息，例如该银行的存款利率。 */
    val detail: String = "",
    /**
     * 原始金额数值，用来排序。
     *
     * 图片上画的是 amount，但排序用字符串会出错，例如 "9.8w" 和 "88.6w"。
     */
    val amountValue: Double = 0.0,
)

/**
 * 个人信息图的完整输入模型。
 *
 * Manager / Usecase 负责把业务数据整理成这个结构，渲染器只负责把它画出来。
 * 这种做法可以让图片样式调整时不影响业务逻辑。
 */
data class PersonalInfoCard(
    /** 用户 QQ。 */
    val qq: String,
    /** 用户昵称。 */
    val nickname: String,
    /** 当前称号文本。 */
    val title: String,
    /** 称号起始色，非渐变模式下也会使用这个颜色。 */
    val titleStartColor: Color,
    /** 称号结束色，仅在 titleGradient 为 true 时使用。 */
    val titleEndColor: Color,
    /** 称号是否使用渐变文字。 */
    val titleGradient: Boolean,
    /** 昵称起始色，非渐变模式下暂时不使用，保留给后续样式扩展。 */
    val nicknameStartColor: Color,
    /** 昵称结束色，仅在 nicknameGradient 为 true 时使用。 */
    val nicknameEndColor: Color,
    /** 昵称是否使用渐变文字。 */
    val nicknameGradient: Boolean,
    /** 上次签到时间，作为长文本单独绘制。 */
    val signTime: String,
    /** 连续签到天数。 */
    val signDays: String,
    /** 钱包金币文本。 */
    val wallet: String,
    /** 签到累计收益文本。 */
    val signEarnings: String,
    /** 银行收益文本。 */
    val bankEarnings: String,
    /** 当前所在位置。 */
    val location: String,
    /** 钓鱼冷却状态。 */
    val fishingCooldown: String,
    /** 农场状态概览。 */
    val farmStatus: String,
    /** 各银行存款列表。 */
    val bankDeposits: List<BankDepositLine>,
    /** 右下角信息卡标题，例如“一言”或“签到信息”。 */
    val infoTitle: String,
    /** 右下角信息卡正文，允许包含换行。 */
    val infoText: String,
    /** 信息署名；为空时不绘制署名区域。 */
    val infoSignature: String = "",
)
