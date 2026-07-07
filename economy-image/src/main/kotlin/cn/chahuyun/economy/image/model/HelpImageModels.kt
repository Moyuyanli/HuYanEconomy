package cn.chahuyun.economy.image.model

/**
 * 帮助图里的一条指令说明。
 *
 * 这里没有直接持有权限、回调等业务对象，只保存最终要画到图片上的文本。
 * 这样渲染器只关心“文字和排版”，不会和命令系统耦合在一起。
 */
data class HelpCommandItem(
    /** 指令文本，例如“签到 / sign”。 */
    val command: String,
    /** 指令右侧的简短说明。 */
    val description: String,
)

/**
 * 帮助图里的一个分组，例如“个人模块”“经济与银行”。
 *
 * renderHelp 会把多个 HelpSection 自动分配到左右两列。
 */
data class HelpSection(
    /** 分组标题。 */
    val title: String,
    /** 当前分组下要展示的指令列表。 */
    val items: List<HelpCommandItem>,
)

/**
 * 一张完整帮助图的输入模型。
 */
data class HelpImagePage(
    /** 帮助图主标题。 */
    val title: String,
    /** 标题下方的简短说明。 */
    val subtitle: String,
    /** 需要展示的指令分组。 */
    val sections: List<HelpSection>,
)
