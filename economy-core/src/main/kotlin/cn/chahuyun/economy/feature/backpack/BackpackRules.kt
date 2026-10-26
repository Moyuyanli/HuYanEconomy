package cn.chahuyun.economy.feature.backpack

import cn.chahuyun.economy.actionchain.annotation.After
import cn.chahuyun.economy.actionchain.annotation.FromProvider
import cn.chahuyun.economy.utils.Log

/**
 * 背包功能的规则链：`@Before` / `@After` 规则集合。
 *
 * 背包三个动作都是「按 ID 逐个处理」的批操作，单个道具成功与否由道具效果自己返回，
 * 无法用一条前置规则统一拦截（前置拦截会让整批中止），因此这里只挂后置审计。
 *
 * 需要「背包满了不许再捡」「禁用某类道具」这类**整批级**约束时，
 * 在本文件加 `@Before(order=5)` 即可，动作本体不用改。
 */
object BackpackRules {

    /**
     * 后置规则（order=10）：丢弃道具审计。
     *
     * 丢弃是不可逆操作，必须能回答「谁在哪个群丢了什么」。
     *
     * @param actor 本次背包指令的参与者快照。
     */
    @After(actionId = "core.backpack.discard", order = 10)
    suspend fun logDiscarded(
        @FromProvider("backpack-actor") actor: BackpackActor,
    ) {
        Log.info("背包丢弃:群(${actor.group.id}) 用户(${actor.member.id}) 指令(${actor.message.contentToString()})")
    }

    /**
     * 后置规则（order=20）：使用道具审计。
     *
     * 使用可能带来经济影响（改名卡、禁言卡、抽奖券），留痕便于事后核对。
     *
     * @param actor 本次背包指令的参与者快照。
     */
    @After(actionId = "core.backpack.use", order = 20)
    suspend fun logUsed(
        @FromProvider("backpack-actor") actor: BackpackActor,
    ) {
        Log.info("背包使用:群(${actor.group.id}) 用户(${actor.member.id}) 指令(${actor.message.contentToString()})")
    }
}
