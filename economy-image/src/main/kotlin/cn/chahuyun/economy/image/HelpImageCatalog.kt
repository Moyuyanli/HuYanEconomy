package cn.chahuyun.economy.image

import cn.chahuyun.economy.image.model.HelpCommandItem
import cn.chahuyun.economy.image.model.HelpImagePage
import cn.chahuyun.economy.image.model.HelpSection

object HelpImageCatalog {

    /**
     * 主帮助图的数据源。
     *
     * 这里故意只维护“适合放在图片里的短句”，不要把完整业务说明塞进来。
     * 图片帮助适合快速扫读，详细规则仍应放在具体指令反馈里。
     */
    fun mainHelpPage(): HelpImagePage = HelpImagePage(
        title = "HuYanEconomy 帮助",
        subtitle = "基础经济、个人资产、银行与农场模块",
        sections = listOf(
            // 个人模块：围绕用户自己的资产、状态、签到入口。
            HelpSection(
                "个人模块",
                listOf(
                    HelpCommandItem("签到 / sign", "每日签到获取金币"),
                    HelpCommandItem("个人信息 / info", "查看个人资产卡片"),
                    HelpCommandItem("我的资金", "查看钱包与银行余额"),
                    HelpCommandItem("我的位置", "查看当前位置状态"),
                    HelpCommandItem("回家 / 出院", "位置恢复指令")
                )
            ),
            // 经济与银行：基础金币流转和主银行操作。
            HelpSection(
                "经济与银行",
                listOf(
                    HelpCommandItem("转账 @用户 金额", "向其他用户转账"),
                    HelpCommandItem("存款!", "钱包余额全部存默认银行"),
                    HelpCommandItem("存款!!", "钱包余额全部存主银行"),
                    HelpCommandItem("存款 金额 [!/银行]", "存入主银行或指定银行"),
                    HelpCommandItem("取款!", "默认银行余额全部取出"),
                    HelpCommandItem("取款!!", "主银行余额全部取出"),
                    HelpCommandItem("取款 金额 [!/银行]", "从主银行或指定银行取款"),
                    HelpCommandItem("银行利率 / 富豪榜", "查询利率与排行")
                )
            ),
            // 私人银行：用户创建的银行、贷款和评分相关指令。
            HelpSection(
                "私人银行",
                listOf(
                    HelpCommandItem("银行列表", "查看可用银行"),
                    HelpCommandItem("银行创建 code 名称", "创建自己的银行"),
                    HelpCommandItem("默认银行 / 设置", "查询或切换默认银行"),
                    HelpCommandItem("银行信息 [银行]", "查看银行详情"),
                    HelpCommandItem("银行描述修改", "修改本行描述"),
                    HelpCommandItem("银行利率变更 rate", "调整储户利率"),
                    HelpCommandItem("放贷 金额 [利率]", "发布贷款标的"),
                    HelpCommandItem("贷款 金额 [银行]", "申请银行贷款"),
                    HelpCommandItem("还款 金额 [银行]", "偿还贷款"),
                    HelpCommandItem("银行评分 1-5 [内容]", "给银行评分"),
                    HelpCommandItem("国卷 / 国卷购买", "查看和购买国卷"),
                    HelpCommandItem("国卷赎回 [ID]", "赎回国卷持仓"),
                    HelpCommandItem("狐卷 / 狐卷竞标", "查看和竞标狐卷")
                )
            ),
            // 道具与称号：背包消耗品和称号系统。
            HelpSection(
                "道具与称号",
                listOf(
                    HelpCommandItem("我的背包", "查看背包道具"),
                    HelpCommandItem("道具商店 [页码]", "购买道具"),
                    HelpCommandItem("使用 道具 数量", "使用背包道具"),
                    HelpCommandItem("我的称号", "查看称号列表"),
                    HelpCommandItem("称号商店", "购买称号"),
                    HelpCommandItem("切换称号 编号", "切换当前称号")
                )
            ),
            // 其他：只放跨模块入口和私聊绑定类指令。
            HelpSection(
                "其他",
                listOf(
                    HelpCommandItem("#fund bind QQ", "私聊绑定资助"),
                    HelpCommandItem("#fund get code 金额", "领取资助金币"),
                    HelpCommandItem("游戏帮助", "查看游戏指令图")
                )
            )
        )
    )

    /**
     * 游戏帮助图的数据源。
     *
     * 游戏类玩法的指令数量较多，单独成图可以避免主帮助图过长。
     */
    fun gameHelpPage(): HelpImagePage = HelpImagePage(
        title = "HuYanEconomy 游戏帮助",
        subtitle = "钓鱼、抢劫、红包、猜签、抽奖与农场玩法",
        sections = listOf(
            // 钓鱼游戏：开始、收竿、鱼塘和管理员开关。
            HelpSection(
                "钓鱼游戏",
                listOf(
                    HelpCommandItem("钓鱼 / 抛竿", "开始钓鱼"),
                    HelpCommandItem("拉起!", "浮漂提示后收竿"),
                    HelpCommandItem("鱼塘等级", "查看本群鱼塘"),
                    HelpCommandItem("刷新钓鱼", "管理员重置状态"),
                    HelpCommandItem("开启 / 关闭 钓鱼", "管理员开关")
                )
            ),
            // 抢劫游戏：按武力值和状态判定的对抗玩法。
            HelpSection(
                "抢劫游戏",
                listOf(
                    HelpCommandItem("抢劫 @用户", "按武力值判定成功率"),
                    HelpCommandItem("抢劫榜", "查看抢劫排行"),
                    HelpCommandItem("出院", "从医院恢复"),
                    HelpCommandItem("开启 / 关闭 抢劫", "管理员开关")
                )
            ),
            // 红包游戏：群内红包的创建、领取和列表查询。
            HelpSection(
                "红包游戏",
                listOf(
                    HelpCommandItem("发红包 金额 数量", "默认均分红包"),
                    HelpCommandItem("发红包 金额 数量 sj", "随机红包"),
                    HelpCommandItem("抢红包", "领取本群红包"),
                    HelpCommandItem("领红包 id", "领取指定红包"),
                    HelpCommandItem("红包列表", "查看可领红包")
                )
            ),
            // 猜签与抽奖：概率类金币玩法。
            HelpSection(
                "猜签与抽奖",
                listOf(
                    HelpCommandItem("猜签 数字 金额", "模拟彩票玩法"),
                    HelpCommandItem("抽奖", "默认奖池抽奖"),
                    HelpCommandItem("抽奖 奖池编号", "指定奖池抽奖"),
                    HelpCommandItem("切换奖池 编号", "切换默认奖池"),
                    HelpCommandItem("奖池列表", "查看奖池"),
                    HelpCommandItem("抽奖信息 / 好运榜", "查看统计和排行")
                )
            ),
            // 农场玩法也会出现在游戏帮助里，方便纯娱乐玩家从这里进入。
            HelpSection(
                "农场玩法",
                listOf(
                    HelpCommandItem("我的农场", "查看农场状态"),
                    HelpCommandItem("农场商店 / 仓库", "种子与库存"),
                    HelpCommandItem("购买种子 名称 数量", "购买种子"),
                    HelpCommandItem("播种 1,2,3 名称", "指定地块播种"),
                    HelpCommandItem("收获 1,2,3", "指定地块收获"),
                    HelpCommandItem("一键播种 / 收获", "批量农场操作"),
                    HelpCommandItem("一键收获 / 卖出", "批量处理"),
                    HelpCommandItem("帮浇水 @用户", "加速好友作物"),
                    HelpCommandItem("黑市 / 激活守护", "特殊农场功能")
                )
            )
        )
    )
}
