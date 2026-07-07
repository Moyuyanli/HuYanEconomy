package cn.chahuyun.economy.data

import cn.chahuyun.economy.constant.PrizeType
import cn.chahuyun.economy.prizes.Prize
import cn.chahuyun.economy.prizes.PrizeGroup
import cn.chahuyun.economy.prizes.PrizeLevel
import cn.chahuyun.economy.prizes.PrizePool
import net.mamoe.mirai.console.data.AutoSavePluginData
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.value

object PrizesData : AutoSavePluginData("prizes") {

    /**
     * 奖品信息
     */
    @ValueDescription("奖品信息")
    val prizes: List<Prize> by value(
        listOf(
            Prize(
                "prize-title-01", "[华丽的转身]", "一个稀有的抽奖称号",
                type = PrizeType.TITLE, metadata = mapOf("title-raffle-01" to 1)
            ),
            Prize(
                "prize-title-02", "[今天的爆率那叫一个高啊!]", "今天的爆率那叫一个高啊!",
                type = PrizeType.TITLE, metadata = mapOf("title-raffle-02" to 1)
            ),
            Prize(
                "prize-money-01", "金币奖励", "800大洋",
                type = PrizeType.MONEY, metadata = mapOf("money" to 800)
            ),
            Prize(
                "prize-money-02", "金币奖励", "1500大洋",
                type = PrizeType.MONEY, metadata = mapOf("money" to 1500)
            ),
            Prize(
                "prize-money-03", "金币奖励", "3000大洋",
                type = PrizeType.MONEY, metadata = mapOf("money" to 3000)
            ),
            Prize("prize-other-001", "谢谢参与", "谢谢喵~"),
            Prize("prize-other-002", "再接再厉", "手滑了，下次好运喵！"),
            Prize("prize-other-003", "感谢支持", "月兔挥了挥爪，没中奖哦~"),
            Prize("prize-other-004", "没中奖", "风铃响了，但奖品没来~"),
            Prize("prize-other-005", "空手而归", "啊呜…扑了个空，但有我陪你~"),
            Prize("prize-other-006", "摸摸头", "虽然没中，但送你一朵小云☁"),
            Prize("prize-other-007", "继续加油", "灵符失效了，再试一次吧！"),
            Prize("prize-other-008", "谢谢捧场", "锦鲤游走了，但好运在排队~"),
        )
    )

    /**
     * 奖池信息
     */
    @ValueDescription("奖池信息")
    val pool: List<PrizePool> by value(
        listOf(
            PrizePool(
                "raffle-pool-default", "抽奖默认奖池", "最最基本的奖池", 1000,
                PrizeLevel(
                    1, 1,
                    PrizeGroup("prize-title-01", weight = 100),
                    PrizeGroup("prize-title-02", weight = 1),
                ),
                PrizeLevel(
                    2, 35,
                    PrizeGroup("prize-money-01", "prize-money-02", weight = 120),
                    PrizeGroup("prize-money-03", weight = 30),
                ),
                PrizeLevel(
                    3, 120,
                    PrizeGroup(
                        "prize-other-001", "prize-other-002", "prize-other-003",
                        "prize-other-004", "prize-other-005", "prize-other-006",
                        "prize-other-007", "prize-other-008",
                    )
                )
            )
        )
    )

}