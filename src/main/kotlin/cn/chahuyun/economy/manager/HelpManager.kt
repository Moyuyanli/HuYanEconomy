package cn.chahuyun.economy.manager

import cn.chahuyun.authorize.EventComponent
import cn.chahuyun.authorize.MessageAuthorize
import cn.chahuyun.economy.BuildConstants
import net.mamoe.mirai.event.events.GroupMessageEvent

@EventComponent
class HelpManager {

    @MessageAuthorize(["help", "帮助"])
    suspend fun help(event: GroupMessageEvent) {
        event.subject.sendMessage(
            """
            ══════════ HuYan经济插件帮助 ══════════
            版本：${BuildConstants.VERSION}
            
            ── 个人模块 ──
            签到         每日签到功能
            个人信息      查询个人信息
            我的资金      简易余额查询
            当前位置      查询当前位置
            回家         回家
            
            ── 经济模块 ──
            转账(@用户) 金币数量  转账功能
            存款 金币数量       存款功能
            取款 金币数量       取款功能
            银行利率           银行利率查询
            富豪榜            查询富豪榜
            
            ── 称号模块 ──
            我的称号           查询称号
            购买称号 (称号)     购买称号
            切换称号 (称号编号) 切换称号（0为卸下）
            称号商店           称号商店
            
            ── 道具功能 ──
            背包               查询当前背包
            道具商店           道具商店
            购买 (道具|code) 数量  购买对应道具
            使用 (道具|code) 数量  使用对应道具
            
            ── 其他指令 ──
            帮助       获取帮助
            游戏帮助    获取游戏帮助
            ═══════════════════════════════════
        """.trimIndent()
        )
    }


    @MessageAuthorize(["gameHelp", "游戏帮助"])
    suspend fun gameHelp(event: GroupMessageEvent) {
        event.subject.sendMessage(
            """
            ══════════ HuYan经济插件游戏帮助 ══════════
            版本：${BuildConstants.VERSION}
            
            ── 钓鱼游戏 ──
            购买鱼竿     购买一根0级鱼竿
            鱼竿等级     获取鱼竿等级
            升级鱼竿     升级鱼竿等级
            鱼塘等级     获取鱼塘等级
            钓鱼榜       获取钓鱼排行榜
            钓鱼         开始钓鱼（需要鱼竿和鱼饵）
                        等待后发送(起|!|！)收竿
            
            ── 抢劫游戏 ──
            抢劫(@用户)  根据武力值变动成功率
            出院        从医院出院
            
            ── 红包游戏 ──
            发红包 (金额) (数量) [sj|随机]  
                       默认均分，携带随机参数发随机红包
            抢红包      抢本群红包（每人一次）
            领红包 (红包id) 领取指定id红包
            红包列表    获取本群红包列表
            
            ── 猜签游戏 ──
            猜签 (3到5位数字,用`,`隔开) (金额)
                       模拟彩票
            
            ── 抽奖游戏 ──
            抽奖        默认奖池抽奖
            切换奖池 (奖池编号) 切换奖池
            抽奖 (奖池编号) 抽奖指定奖池
            奖池列表    获取奖池列表
            抽奖信息    获取个人抽奖统计
            好运榜      获取好运榜
            ═══════════════════════════════════
        """.trimIndent()
        )
    }

}