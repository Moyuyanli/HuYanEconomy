@file:Suppress("DuplicatedCode")

package cn.chahuyun.economy.feature.redpack

import cn.chahuyun.authorize.utils.PermUtil
import cn.chahuyun.authorize.utils.UserUtil
import cn.chahuyun.economy.actionchain.annotation.Action
import cn.chahuyun.economy.actionchain.runtime.ActionContext
import cn.chahuyun.economy.constant.EconPerm
import cn.chahuyun.economy.feature.redpack.RedPackUsecase.receive
import cn.chahuyun.economy.model.redpack.RedPackDto
import cn.chahuyun.economy.model.redpack.RedPackKind
import cn.chahuyun.economy.runtime.EconomyRuntime
import cn.chahuyun.economy.utils.*
import cn.hutool.core.date.DateUnit
import cn.hutool.core.date.DateUtil
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.ForwardMessageBuilder
import net.mamoe.mirai.message.data.buildMessageChain
import java.util.*

/**
 * 红包功能的主用例：发红包、领红包、列表、开关。
 *
 * ## 动作清单
 * | actionId | 指令 | 规则链 |
 * |---|---|---|
 * | `core.redpack.create` | 发红包 金额 个数 [sj\|随机\|kl\|口令 ...] | `@Before(10)` 参数与余额校验；`@After` 留痕 |
 * | `core.redpack.receive` | 领红包 X / 收红包 X | 无前置（ID 与口令两条分支都在动作内） |
 * | `core.redpack.grabByPassword` | 直接发口令 | 与 `receive` 共用实现 |
 * | `core.redpack.grabNewest` | 抢红包 | `@After` 留痕 |
 * | `core.redpack.list` | 红包列表 | — |
 * | `core.redpack.globalList` | 全局红包列表（owner） | — |
 * | `core.redpack.open` / `.close` | 开启/关闭 红包 | `@After` 留痕 |
 *
 * 领取类动作没法前置校验：是拿 ID 还是拿口令要等消息解析完才知道，
 * 因此统一在动作内分支，用 `RedPackManager.getRedPack` 的返回值决定是否回复。
 */
object RedPackUsecase {

    /**
     * 创建红包（`core.redpack.create`）。
     *
     * 金额、个数、类型、口令、余额都已由 [RedPackRules.validateCreate] 确认；
     * 这里负责扣款、生成随机分配、落库与提示文案。
     *
     * @param event 群消息事件，仅用于来源日志。
     * @param context 本次调用上下文，读取 [RedPackCreatePlan] 与参与者快照。
     */
    @Action("core.redpack.create")
    suspend fun create(event: GroupMessageEvent, context: ActionContext) {
        Log.info("发红包指令:用户(${event.sender.id})")

        val actor = context.provide(RedPackActor::class, RedPackProvider.ACTOR)
        val plan = context.get(RedPackCreatePlan::class)
        if (plan == null) {
            Log.warning("红包:创建规则未产出计划，本次终止（用户 ${event.sender.id}）")
            return
        }

        val subject = actor.subject
        val sender = actor.sender
        val group = requireNotNull(actor.group) { "红包只能在群聊中创建" }

        if (!EconomyUtil.plusMoneyToUser(sender, -plan.money)) {
            subject.sendMessage(MessageUtil.formatMessageChain(actor.message, "红包发送失败！"))
            return
        }

        var pack = RedPackDto(
            name = "${sender.nick}的红包",
            groupId = group.id,
            sender = sender.id,
            money = plan.money,
            number = plan.number,
            type = plan.type,
            password = plan.password,
            createTime = Date().time,
        )

        if (pack.isRandomAllocation) {
            val doubles = RedPackAllocationService.generateRandomPack(plan.money, plan.number)
            pack = pack.copy(randomPackList = doubles)
        }

        val resultPack = RedPackManager.save(pack)
        val id = resultPack.id

        if (id == 0) {
            subject.sendMessage(MessageUtil.formatMessageChain(actor.message, "红包创建失败！"))
            return
        }

        val prefix = EconomyRuntime.config.prefix
        val typeDesc = plan.type.description

        val claimMsg = if (plan.type == RedPackKind.PASSWORD) {
            "领取方式: 直接发送口令 [ ${plan.password} ] 即可领取"
        } else {
            "领取命令: ${prefix.ifBlank { "" }}领红包 $id"
        }

        subject.sendMessage(
            MessageUtil.formatMessageChain(
                "${sender.nick} 发送了一个 ${typeDesc}，快来抢红包吧！\n" +
                    "红包 ID: ${id}\n" +
                    "红包金额: ${MoneyFormatUtil.format(plan.money)} 枚金币\n" +
                    "红包个数: ${plan.number}\n" +
                    "发送时间: ${TimeConvertUtil.timeConvert(Date(pack.createTime))}\n" +
                    "${claimMsg}"
            )
        )
    }

    /**
     * 领取红包（`core.redpack.receive`）：`领红包 <ID|口令>` / `收红包 <ID|口令>`。
     *
     * @param event 群消息事件，仅用于来源日志。
     * @param context 本次调用上下文，读取参与者快照。
     */
    @Action("core.redpack.receive")
    suspend fun receive(event: GroupMessageEvent, context: ActionContext) {
        val actor = context.provide(RedPackActor::class, RedPackProvider.ACTOR)
        handleReceive(actor, hasPrefix = true)
    }

    /**
     * 直接发送口令领取红包（`core.redpack.grabByPassword`）。
     *
     * 与 [receive] 共用实现，区别只是「消息里没有指令前缀」。
     *
     * @param event 群消息事件，仅用于来源日志。
     * @param context 本次调用上下文，读取参与者快照。
     */
    @Action("core.redpack.grabByPassword")
    suspend fun grabByPassword(event: GroupMessageEvent, context: ActionContext) {
        val actor = context.provide(RedPackActor::class, RedPackProvider.ACTOR)
        handleReceive(actor, hasPrefix = false)
    }

    /**
     * 一键抢本群所有非口令红包（`core.redpack.grabNewest`）。
     *
     * @param event 群消息事件，仅用于来源日志。
     * @param context 本次调用上下文，读取参与者快照。
     */
    @Action("core.redpack.grabNewest")
    suspend fun grabNewestRedPack(event: GroupMessageEvent, context: ActionContext) {
        Log.info("一键抢红包:用户(${event.sender.id})")

        val actor = context.provide(RedPackActor::class, RedPackProvider.ACTOR)
        val subject = actor.subject
        val sender = actor.sender
        val group = requireNotNull(actor.group) { "红包只能在群聊中领取" }
        try {
            val redPacks = RedPackManager.listByGroupId(group.id)
                .filter { it.type != RedPackKind.PASSWORD } // 默认不抢口令红包

            if (redPacks.isEmpty()) {
                subject.sendMessage("当前群没有可领取的红包哦！")
                return
            }

            val results = mutableListOf<Pair<String, Double>>()

            for (redPack in redPacks) {
                // 检查是否领过或者领完
                if (redPack.receiverList.contains(sender.id)) continue
                if (redPack.number <= redPack.receiverList.size) continue

                // 检查是否过期
                if (DateUtil.between(Date(redPack.createTime), Date(), DateUnit.DAY) >= 1) {
                    RedPackManager.expireRedPack(group, redPack)
                    RedPackManager.delete(redPack)
                    continue
                }

                val result = RedPackManager.getRedPack(sender, subject, redPack, skipMessage = true)
                if (result.success) {
                    results.add((redPack.name.ifBlank { "红包" }) to result.amount)
                }
            }

            if (results.isEmpty()) {
                subject.sendMessage("你已经抢过群里所有的红包啦，或者红包已经领完/过期了！")
                return
            }

            val messageChain = buildMessageChain {
                +At(sender.id)
                +"你本次抢到的红包:\n"
                results.forEach { (name, amount) ->
                    +"$name，你抢到了 ${MoneyFormatUtil.format(amount)} 枚金币\n"
                }
            }
            subject.sendMessage(messageChain)
        } catch (e: Exception) {
            subject.sendMessage("领取失败！原因: ${e.message}")
            Log.error("一键抢红包异常", e)
        }
    }

    /**
     * 查询本群红包列表（`core.redpack.list`）。
     *
     * @param event 群消息事件，仅用于来源日志。
     * @param context 本次调用上下文，读取参与者快照。
     */
    @Action("core.redpack.list")
    suspend fun queryRedPackList(event: GroupMessageEvent, context: ActionContext) {
        Log.info("红包查询指令:用户(${event.sender.id})")

        val actor = context.provide(RedPackActor::class, RedPackProvider.ACTOR)
        val subject = actor.subject
        val group = requireNotNull(actor.group) { "红包只能在群聊中查询" }
        try {
            val redPacks = RedPackManager.listByGroupId(group.id)

            val forwardMessage = ForwardMessageBuilder(subject)
            if (redPacks.isEmpty()) {
                subject.sendMessage("本群暂无红包！")
                return
            }
            RedPackManager.viewRedPack(subject, subject.bot, redPacks, forwardMessage)
        } catch (e: Exception) {
            subject.sendMessage("查询失败！原因: ${e.message}")
            throw RuntimeException(e)
        }
    }

    /**
     * 查询全局红包列表（`core.redpack.globalList`，入口已限 owner 权限）。
     *
     * @param event 消息事件，仅用于来源日志。
     * @param context 本次调用上下文，读取参与者快照。
     */
    @Action("core.redpack.globalList")
    suspend fun queryGlobalRedPackList(event: MessageEvent, context: ActionContext) {
        Log.info("红包查看指令:用户(${event.sender.id})")

        val actor = context.provide(RedPackActor::class, RedPackProvider.ACTOR)
        val subject = actor.subject
        try {
            val redPacks = RedPackManager.listAll()

            val forwardMessage = ForwardMessageBuilder(subject)

            if (redPacks.isEmpty()) {
                subject.sendMessage("全局暂无红包！")
                return
            }
            RedPackManager.viewRedPack(subject, subject.bot, redPacks, forwardMessage)
        } catch (e: Exception) {
            subject.sendMessage("查询失败！原因: ${e.message}")
            throw RuntimeException(e)
        }
    }

    /**
     * 开启本群红包功能（`core.redpack.open`，入口已限 owner/admin）。
     *
     * @param event 群消息事件，仅用于来源日志。
     * @param context 本次调用上下文，读取参与者快照。
     */
    @Action("core.redpack.open")
    suspend fun startRob(event: GroupMessageEvent, context: ActionContext) {
        Log.info("开启红包:用户(${event.sender.id})")

        val actor = context.provide(RedPackActor::class, RedPackProvider.ACTOR)
        val group = requireNotNull(actor.group) { "红包只能在群聊中开关" }
        val user = UserUtil.group(group.id)

        if (PermUtil.checkUserHasPerm(user, EconPerm.RED_PACKET_PERM)) {
            group.sendMessage(MessageUtil.formatMessageChain(actor.message, "本群的红包已经开启了！"))
            return
        }

        if (PermUtil.addUserToPermGroupByName(user, EconPerm.GROUP.RED_PACKET_PERM_GROUP)) {
            group.sendMessage(MessageUtil.formatMessageChain(actor.message, "本群的红包开启成功！"))
        } else {
            group.sendMessage(MessageUtil.formatMessageChain(actor.message, "本群的红包开启失败！"))
        }
    }

    /**
     * 关闭本群红包功能（`core.redpack.close`，入口已限 owner/admin）。
     *
     * @param event 群消息事件，仅用于来源日志。
     * @param context 本次调用上下文，读取参与者快照。
     */
    @Action("core.redpack.close")
    suspend fun endRob(event: GroupMessageEvent, context: ActionContext) {
        Log.info("关闭红包:用户(${event.sender.id})")

        val actor = context.provide(RedPackActor::class, RedPackProvider.ACTOR)
        val group = requireNotNull(actor.group) { "红包只能在群聊中开关" }
        val user = UserUtil.group(group.id)

        if (!PermUtil.checkUserHasPerm(user, EconPerm.RED_PACKET_PERM)) {
            group.sendMessage(MessageUtil.formatMessageChain(actor.message, "本群的红包已经关闭！"))
            return
        }

        val permGroup = PermUtil.takePermGroupByName(EconPerm.GROUP.RED_PACKET_PERM_GROUP)
        permGroup.users.remove(user)
        permGroup.save()

        group.sendMessage(MessageUtil.formatMessageChain(actor.message, "本群的红包关闭成功！"))
    }

    // -----------------------------------------------------------------------
    // 内部实现
    // -----------------------------------------------------------------------

    /**
     * 领取红包的公共实现，`receive` 与 `grabByPassword` 共用。
     *
     * @param actor 本次指令的参与者快照。
     * @param hasPrefix 消息是否带 `领红包` / `收红包` 前缀；不带时按纯口令处理。
     */
    private suspend fun handleReceive(actor: RedPackActor, hasPrefix: Boolean) {
        val subject = actor.subject
        val group = actor.group ?: return
        val sender = actor.sender
        val content = actor.content
        val info = content.split(" ").filter { it.isNotBlank() }

        if (hasPrefix) {
            Log.info("收红包指令:用户(${sender.id})")

            if (info.size < 2) {
                // 只有前缀，提示输入ID或口令（因为只有抢红包才支持一键全抢）
                subject.sendMessage(MessageUtil.formatMessageChain(actor.message, "请在指令后输入红包ID或口令！"))
                return
            }

            val id = info[1].toIntOrNull()

            if (id != null) {
                // 是数字ID，领取指定红包
                val redPack = RedPackManager.findById(id)
                if (redPack == null) {
                    subject.sendMessage(MessageUtil.formatMessageChain(actor.message, "红包不存在！"))
                    return
                }

                if (redPack.groupId != group.id) {
                    subject.sendMessage(MessageUtil.formatMessageChain(actor.message, "这不是这个群的红包！"))
                    return
                }

                if (DateUtil.between(Date(redPack.createTime), Date(), DateUnit.DAY) >= 1) {
                    subject.sendMessage(MessageUtil.formatMessageChain(actor.message, "这个红包已经过期了。"))
                    RedPackManager.expireRedPack(group, redPack)
                    RedPackManager.delete(redPack)
                    return
                }

                // 如果是口令红包，不能通过ID领取
                if (redPack.type == RedPackKind.PASSWORD) {
                    subject.sendMessage(MessageUtil.formatMessageChain(actor.message, "这是口令红包，请直接发送口令来领取！"))
                    return
                }

                RedPackManager.getRedPack(sender, subject, redPack, actor.message)
            } else {
                // 不是数字，尝试作为口令
                val password = info.subList(1, info.size).joinToString(" ")
                grabByPasswordInternal(actor, password)
            }
        } else {
            // 没有前缀，说明是直接发送的口令（由 core.redpack.grabByPassword 触发）
            val password = content.trim()
            if (!RedPackManager.hasActivePassword(group.id, password)) return
            grabByPasswordInternal(actor, password, logOnMatch = true)
        }
    }

    /**
     * 按口令批量领取。
     *
     * 无匹配口令时**静默返回**——群里任何一句话都会进入这条分支，回复就是刷屏。
     *
     * @param actor 本次指令的参与者快照。
     * @param password 待匹配口令。
     * @param logOnMatch 命中时才打日志（避免口令探测把日志刷满）。
     */
    private suspend fun grabByPasswordInternal(
        actor: RedPackActor,
        password: String,
        logOnMatch: Boolean = false,
    ) {
        val group = actor.group ?: return
        val sender = actor.sender
        val subject = actor.subject

        val redPacks = RedPackManager.listPasswordByGroupAndPassword(group.id, password)
        if (redPacks.isEmpty()) {
            return // 没有匹配的口令红包，不做处理
        }

        if (logOnMatch) {
            Log.info("收红包指令:用户(${sender.id}) 口令")
        }

        val results = mutableListOf<Pair<String, Double>>()

        for (redPack in redPacks) {
            // 检查是否领过
            if (redPack.receiverList.contains(sender.id)) continue
            // 检查是否领完
            if (redPack.number <= redPack.receiverList.size) continue
            // 检查是否过期
            if (DateUtil.between(Date(redPack.createTime), Date(), DateUnit.DAY) >= 1) {
                RedPackManager.expireRedPack(group, redPack)
                RedPackManager.delete(redPack)
                continue
            }

            val result = RedPackManager.getRedPack(
                sender, subject, redPack, skipMessage = true, passwordOverride = password
            )
            if (result.success) {
                results.add((redPack.name.ifBlank { "红包" }) to result.amount)
            }
        }

        if (results.size == 1) {
            val (name, amount) = results[0]
            val messageChain = buildMessageChain {
                +At(sender.id)
                +" $password"
                +"\n你抢 $name 抢到 ${MoneyFormatUtil.format(amount)} 枚金币"
            }
            subject.sendMessage(messageChain)
        } else if (results.size > 1) {
            val messageChain = buildMessageChain {
                +At(sender.id)
                +"你本次抢到的口令红包:\n"
                results.forEach { (name, amount) ->
                    +"$name，你抢到了 ${MoneyFormatUtil.format(amount)} 枚金币\n"
                }
            }
            subject.sendMessage(messageChain)
        }
    }
}
