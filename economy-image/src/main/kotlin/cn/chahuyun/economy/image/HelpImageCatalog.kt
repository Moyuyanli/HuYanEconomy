package cn.chahuyun.economy.image

import cn.chahuyun.economy.image.model.HelpCommandItem
import cn.chahuyun.economy.image.model.HelpImagePage
import cn.chahuyun.economy.image.model.HelpSection

object HelpImageCatalog {

    /**
     * 主帮助图的数据源。
     *
     * 这里维护适合快速扫读的短句；复杂规则放在具体指令反馈里。
     */
    fun mainHelpPage(): HelpImagePage = HelpImagePage(
        title = "HuYanEconomy 帮助",
        subtitle = "基础经济、个人资产、银行、道具与管理入口",
        sections = listOf(
            HelpSection(
                "个人与帮助",
                listOf(
                    HelpCommandItem("帮助 / help", "查看主帮助图"),
                    HelpCommandItem("游戏帮助 / gameHelp", "查看游戏指令图"),
                    HelpCommandItem("签到 / 打卡 / sign", "每日签到获取金币"),
                    HelpCommandItem("个人信息 / info", "查看个人资产卡片"),
                    HelpCommandItem("个人信息# / info#", "查看文字版个人信息"),
                    HelpCommandItem("我的资金 / money", "查看钱包和银行余额"),
                    HelpCommandItem("我的状态 / 我的位置", "查看当前位置状态"),
                    HelpCommandItem("回家 / 出院", "恢复位置或离开医院")
                )
            ),
            HelpSection(
                "经济与主银行",
                listOf(
                    HelpCommandItem("转账 @用户 金额", "向其他用户转账"),
                    HelpCommandItem("存款 金额 [银行]", "存入主银行或指定银行"),
                    HelpCommandItem("存款!", "钱包余额全部存默认银行"),
                    HelpCommandItem("存款!!", "钱包余额全部存主银行"),
                    HelpCommandItem("取款 金额 [银行]", "从主银行或指定银行取款"),
                    HelpCommandItem("取款!", "默认银行余额全部取出"),
                    HelpCommandItem("取款!!", "主银行余额全部取出"),
                    HelpCommandItem("本周利率 / 银行利率", "查询主银行利率"),
                    HelpCommandItem("富豪榜 / 经济排行", "查看财富排行")
                )
            ),
            HelpSection(
                "私人银行",
                listOf(
                    HelpCommandItem("银行列表", "查看可用私人银行"),
                    HelpCommandItem("银行创建 code 名称", "创建自己的银行"),
                    HelpCommandItem("默认银行", "查看当前默认银行"),
                    HelpCommandItem("默认银行设置 银行", "切换默认银行"),
                    HelpCommandItem("银行信息 [银行]", "查看银行详情"),
                    HelpCommandItem("我的银行 / 我的银行#", "查看自己的银行"),
                    HelpCommandItem("银行描述修改", "修改本行描述"),
                    HelpCommandItem("银行资料修改 code 名称", "修改本行 code 和名称"),
                    HelpCommandItem("银行利率变更 rate", "调整储户利率"),
                    HelpCommandItem("银行补资 金额 [P/F]", "补入准备金或流动金"),
                    HelpCommandItem("银行撤资 金额 [P/F]", "撤回准备金或流动金"),
                    HelpCommandItem("放贷 金额 [利率]", "发布贷款额度"),
                    HelpCommandItem("放贷列表", "查看已发布额度"),
                    HelpCommandItem("撤贷 ID", "撤回未借出的额度"),
                    HelpCommandItem("贷款利息修改 ID rate", "调整贷款日利率"),
                    HelpCommandItem("贷款/借款 金额 [银行]", "申请银行贷款"),
                    HelpCommandItem("借贷列表", "查看自己的借款明细"),
                    HelpCommandItem("还款 金额 [银行]", "偿还贷款"),
                    HelpCommandItem("银行评分 1-5 [内容]", "给银行评分"),
                    HelpCommandItem("国卷 / 国卷列表", "查看国卷发行和持仓"),
                    HelpCommandItem("国卷购买 金额", "用流动金购买国卷"),
                    HelpCommandItem("国卷赎回 [ID]", "赎回到期或指定持仓"),
                    HelpCommandItem("狐卷 / 狐卷查看", "查看可竞标狐卷"),
                    HelpCommandItem("狐卷竞标 code 溢价 利率", "参与狐卷竞标")
                )
            ),
            HelpSection(
                "道具与称号",
                listOf(
                    HelpCommandItem("我的背包 / backpack", "查看背包道具"),
                    HelpCommandItem("使用/use ID...", "使用背包道具"),
                    HelpCommandItem("丢弃/dis ID...", "丢弃背包道具"),
                    HelpCommandItem("道具商店 [页码]", "查看道具商店"),
                    HelpCommandItem("购买道具/buy 道具[*数量]", "购买一个或多个道具"),
                    HelpCommandItem("我的称号 / 称号列表", "查看已有称号"),
                    HelpCommandItem("称号商店", "查看可购买称号"),
                    HelpCommandItem("购买称号 名称", "购买指定称号"),
                    HelpCommandItem("切换称号 编号", "切换当前称号")
                )
            ),
            HelpSection(
                "管理与私聊",
                listOf(
                    HelpCommandItem("greedisgood 金额", "管理员获取金币"),
                    HelpCommandItem("开启/关闭 签到", "管理员开关签到"),
                    HelpCommandItem("刷新签到", "管理员刷新签到状态"),
                    HelpCommandItem("回家 @QQ", "管理员送用户回家"),
                    HelpCommandItem("hye v", "Console 查看版本"),
                    HelpCommandItem("hye repair", "Console 修复数据结构")
                )
            )
        )
    )

    /**
     * 游戏帮助图的数据源。
     */
    fun gameHelpPage(): HelpImagePage = HelpImagePage(
        title = "HuYanEconomy 游戏帮助",
        subtitle = "钓鱼、抢劫、红包、猜签、抽奖与农场玩法",
        sections = listOf(
            HelpSection(
                "钓鱼游戏",
                listOf(
                    HelpCommandItem("钓鱼 / 抛竿", "开始钓鱼"),
                    HelpCommandItem("拉起!", "浮漂提示后收竿"),
                    HelpCommandItem("购买鱼竿", "购买初始鱼竿"),
                    HelpCommandItem("升级鱼竿[*]", "升级一次或连续升级"),
                    HelpCommandItem("鱼竿等级", "查看自己的鱼竿等级"),
                    HelpCommandItem("鱼塘等级", "查看本群鱼塘"),
                    HelpCommandItem("钓鱼信息 / 钓鱼信息#", "查看钓鱼统计"),
                    HelpCommandItem("刷新钓鱼", "管理员重置钓鱼状态"),
                    HelpCommandItem("开启/关闭 钓鱼", "管理员开关钓鱼")
                )
            ),
            HelpSection(
                "抢劫游戏",
                listOf(
                    HelpCommandItem("抢劫 @用户", "按武力值判定抢劫"),
                    HelpCommandItem("打人 @用户", "打人入口"),
                    HelpCommandItem("出院", "从医院恢复"),
                    HelpCommandItem("开启/关闭 抢劫", "管理员开关抢劫")
                )
            ),
            HelpSection(
                "红包游戏",
                listOf(
                    HelpCommandItem("发红包 金额 数量", "创建均分红包"),
                    HelpCommandItem("发红包 金额 数量 sj", "创建随机红包"),
                    HelpCommandItem("发红包 金额 数量 kl 口令", "创建口令红包"),
                    HelpCommandItem("抢红包", "领取本群最新红包"),
                    HelpCommandItem("领红包/收红包 内容", "领取指定红包"),
                    HelpCommandItem("直接发送口令", "领取口令红包"),
                    HelpCommandItem("红包列表", "查看本群可领红包"),
                    HelpCommandItem("全局红包列表", "管理员查看全局红包"),
                    HelpCommandItem("开启/关闭 红包", "管理员开关红包")
                )
            ),
            HelpSection(
                "猜签与抽奖",
                listOf(
                    HelpCommandItem("猜签/lottery 数字 金额", "参与猜签彩票"),
                    HelpCommandItem("开启/关闭 猜签", "管理员开关猜签"),
                    HelpCommandItem("抽奖 / raffle", "默认奖池单抽"),
                    HelpCommandItem("十连 / ten", "默认奖池十连抽")
                )
            ),
            HelpSection(
                "农场玩法",
                listOf(
                    HelpCommandItem("我的农场 / 农场信息", "查看农场详情图"),
                    HelpCommandItem("我的农场# / 农场信息#", "查看农场文字信息"),
                    HelpCommandItem("农场详情 / 农场等级", "查看详情或等级"),
                    HelpCommandItem("农场商店 / 农场仓库", "查看种子和库存"),
                    HelpCommandItem("购买种子 名称 [数量]", "购买作物种子"),
                    HelpCommandItem("播种/种植 地块 种子", "指定地块播种"),
                    HelpCommandItem("收获 地块", "收获指定地块"),
                    HelpCommandItem("卖出果实 内容", "卖出指定果实"),
                    HelpCommandItem("一键播种 种子", "批量播种"),
                    HelpCommandItem("一键收获 / 一键卖出", "批量收获或卖出"),
                    HelpCommandItem("升级农场[*]", "升级农场等级"),
                    HelpCommandItem("帮浇水 @用户", "帮助浇水加速，可掉落农场抽奖券"),
                    HelpCommandItem("偷菜 @用户", "尝试偷取成熟作物，与浇水共享次数"),
                    HelpCommandItem("激活守护 / 黑市", "付费守护农场或进入特殊功能")
                )
            )
        )
    )
}
