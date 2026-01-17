package cn.chahuyun.economy.privatebank

import cn.chahuyun.economy.entity.privatebank.PrivateBankFoxBond
import cn.chahuyun.economy.entity.privatebank.PrivateBankFoxBondBid
import cn.chahuyun.economy.entity.privatebank.PrivateBankFoxBondHolding
import cn.chahuyun.economy.utils.*
import cn.hutool.core.date.DateUtil
import cn.hutool.core.util.RandomUtil
import net.mamoe.mirai.contact.User
import java.util.*

object PrivateBankFoxBondService {

    private fun baseBankInterestPercent(): Double = PrivateBankService.baseInterestPercent()

    private fun buildIssueTimes(now: Date): Pair<Date, Date> {
        val start = DateUtil.beginOfDay(now)
        val bidStart = DateUtil.offsetHour(start, 8)
        val bidEnd = DateUtil.offsetHour(start, 18)
        return bidStart to bidEnd
    }

    private fun nextBondCode(now: Date, seq: Int): String {
        return "FB-${DateUtil.format(now, "yyyyMMdd")}-${seq.toString().padStart(3, '0')}"
    }

    fun issueBondsForToday(now: Date = Date()): List<PrivateBankFoxBond> {
        val (bidStartAt, bidEndAt) = buildIssueTimes(now)
        val day = DateUtil.dayOfMonth(now)
        if (day != 1 && day != 15) return emptyList()

        // 避免重复发行：若当天已经有 FB-yyyyMMdd-xxx 则认为已发行
        val prefix = "FB-${DateUtil.format(now, "yyyyMMdd")}-"
        val existing = PrivateBankRepository.listFoxBonds().any { it.code.startsWith(prefix) }
        if (existing) return emptyList()

        val bonds = mutableListOf<PrivateBankFoxBond>()

        fun addBond(seq: Int, faceMin: Double, faceMax: Double, termMin: Int, termMax: Int) {
            val face = ShareUtils.rounding(RandomUtil.randomDouble(faceMin, faceMax))
            val term = RandomUtil.randomInt(termMin, termMax + 1)
            val rate = RandomUtil.randomDouble(2.0, 5.01)
            bonds += PrivateBankFoxBond(
                code = nextBondCode(now, seq),
                faceValue = face,
                baseRate = rate,
                termDays = term,
                bidStartAt = bidStartAt,
                bidEndAt = bidEndAt,
                status = "BIDDING",
                createdAt = now
            )
        }

        // 3 基础卷：100M-300M，7-14d
        var seq = 1
        repeat(3) { addBond(seq++, 100_000_000.0, 300_000_000.0, 7, 14) }
        // 6 标准卷：500M-900M，14-21d
        repeat(6) { addBond(seq++, 500_000_000.0, 900_000_000.0, 14, 21) }
        // 1 大鳄卷：1G-5G，21-30d
        addBond(seq, 1_000_000_000.0, 5_000_000_000.0, 21, 30)

        bonds.forEach { PrivateBankRepository.saveFoxBond(it) }
        return bonds
    }

    fun listActiveBonds(now: Date = Date()): List<PrivateBankFoxBond> {
        return PrivateBankRepository.listFoxBonds()
            .filter { it.status == "BIDDING" && it.bidEndAt.after(now) }
            .sortedBy { it.code }
    }

    fun submitBid(owner: User, bondCode: String, premium: Double, bidRate: Double): Pair<Boolean, String> {
        if (premium < 0) return false to "溢价金额必须 >= 0"
        if (bidRate <= 0) return false to "利率必须 > 0"

        val bond = PrivateBankRepository.findFoxBondByCode(bondCode.trim()) ?: return false to "未找到该狐卷：$bondCode"
        val now = Date()
        if (bond.status != "BIDDING" || now.before(bond.bidStartAt) || now.after(bond.bidEndAt)) {
            return false to "当前不在竞标时间内"
        }

        val bank = PrivateBankRepository.listBanks().firstOrNull { it.ownerQq == owner.id }
            ?: return false to "你还没有创建自己的银行"
        if (bank.isDefaulter()) return false to "失信期间禁止参与竞标"

        if (bidRate > bond.baseRate + 1e-9) {
            return false to "竞标利率不得高于原始利率（原始 ${FormatUtil.fixed(bond.baseRate, 2)}%/day）"
        }

        val minRate = baseBankInterestPercent()
        if (bidRate + 1e-9 < minRate) {
            return false to "竞标利率不得低于主银行基准均值（${FormatUtil.fixed(minRate, 1)}%）"
        }

        val reserve = EconomyUtil.getMoneyByBankFromId(bank.code, PrivateBankLedger.RESERVE_DESC)
        if (reserve + 1e-6 < bond.faceValue + premium) {
            val need = ShareUtils.rounding(bond.faceValue + premium)
            return false to "准备金不足：需 ${MoneyFormatUtil.format(need)}（面额 ${MoneyFormatUtil.format(bond.faceValue)} + 溢价 ${MoneyFormatUtil.format(premium)}），当前 ${MoneyFormatUtil.format(reserve)}"
        }

        // 同一银行对同一狐卷仅保留一条竞标（merge 会按 id 工作，因此这里手动复用已有记录）
        val existing = PrivateBankRepository.listFoxBondBids(bond.code).firstOrNull { it.bankCode == bank.code }
        val bid = existing ?: PrivateBankFoxBondBid(
            bondCode = bond.code,
            bankCode = bank.code,
            ownerQq = owner.id
        )
        bid.premium = ShareUtils.rounding(premium)
        bid.bidRate = bidRate
        bid.createdAt = now
        PrivateBankRepository.saveFoxBondBid(bid)

        return true to "竞标已提交：狐卷 ${bond.code} 面额=${MoneyFormatUtil.format(bond.faceValue)} 原始=${FormatUtil.fixed(bond.baseRate, 2)}%/day 你的溢价=${MoneyFormatUtil.format(premium)} 你的利率=${FormatUtil.fixed(bidRate, 2)}%/day"
    }

    fun settleExpiredBids(now: Date = Date()): Int {
        var settled = 0
        val bonds = PrivateBankRepository.listFoxBonds()
            .filter { it.status == "BIDDING" && it.bidEndAt.before(now) }

        for (bond in bonds) {
            val bids = PrivateBankRepository.listFoxBondBids(bond.code)
            if (bids.isEmpty()) {
                bond.status = "CANCELLED"
                PrivateBankRepository.saveFoxBond(bond)
                continue
            }

            val maxPremium = bids.maxOf { it.premium }.coerceAtLeast(1.0)
            val bankCodes = bids.map { it.bankCode }.distinct()
            val reserves = bankCodes.associateWith { EconomyUtil.getMoneyByBankFromId(it, PrivateBankLedger.RESERVE_DESC) }
            val maxReserve = reserves.values.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0

            fun starScore(bankCode: String): Double {
                val b = PrivateBankRepository.findBankByCode(bankCode) ?: return 0.2
                return (b.star.coerceIn(1, 5) / 5.0)
            }

            fun totalScore(bid: PrivateBankFoxBondBid): Double {
                val premiumScore = (bid.premium / maxPremium).coerceIn(0.0, 1.0)
                val concessionScore = ((bond.baseRate - bid.bidRate) / bond.baseRate).coerceIn(0.0, 1.0)
                val sScore = starScore(bid.bankCode).coerceIn(0.0, 1.0)
                val assetScore = ((reserves[bid.bankCode] ?: 0.0) / maxReserve).coerceIn(0.0, 1.0)
                return 0.35 * premiumScore + 0.25 * concessionScore + 0.25 * sScore + 0.15 * assetScore
            }

            val sorted = bids.sortedWith(
                compareByDescending<PrivateBankFoxBondBid> { totalScore(it) }
                    .thenByDescending { it.premium }
                    .thenBy { it.bidRate }
            )

            var winner: PrivateBankFoxBondBid? = null
            for (candidate in sorted) {
                val bank = PrivateBankRepository.findBankByCode(candidate.bankCode) ?: continue
                if (bank.isDefaulter()) continue
                val reserve = EconomyUtil.getMoneyByBankFromId(bank.code, PrivateBankLedger.RESERVE_DESC)
                if (reserve + 1e-6 < bond.faceValue + candidate.premium) continue
                winner = candidate
                break
            }

            if (winner == null) {
                bond.status = "CANCELLED"
                PrivateBankRepository.saveFoxBond(bond)
                continue
            }

            val bank = PrivateBankRepository.findBankByCode(winner.bankCode) ?: continue

            // 1) 扣除溢价（销毁）
            if (winner.premium > 0) {
                if (!EconomyUtil.plusMoneyToBankFromId(bank.code, PrivateBankLedger.RESERVE_DESC, -winner.premium)) {
                    Log.error("狐卷结算:扣除溢价失败 bank=${bank.code} bond=${bond.code}")
                    continue
                }
            }

            // 2) 锁定面额资金：从私银准备金(global) -> 狐卷锁仓(custom)
            val locked = EconomyUtil.turnGlobalBankAccountToPluginBankForId(
                fromUserId = bank.code,
                fromDescription = PrivateBankLedger.RESERVE_DESC,
                toUserId = bond.code,
                toDescription = PrivateBankLedger.FOX_BOND_LOCK_DESC,
                quantity = bond.faceValue
            )
            if (!locked) {
                // 回滚溢价（尽力）
                if (winner.premium > 0) {
                    EconomyUtil.plusMoneyToBankFromId(bank.code, PrivateBankLedger.RESERVE_DESC, winner.premium)
                }
                Log.error("狐卷结算:锁定资金失败 bank=${bank.code} bond=${bond.code}")
                continue
            }

            val dueAt = DateUtil.offsetDay(bond.bidEndAt, bond.termDays)
            PrivateBankRepository.saveFoxBondHolding(
                PrivateBankFoxBondHolding(
                    bondCode = bond.code,
                    bankCode = bank.code,
                    principal = bond.faceValue,
                    rate = winner.bidRate,
                    startedAt = bond.bidEndAt,
                    dueAt = dueAt
                )
            )

            bond.status = "HOLDING"
            bond.winnerBankCode = bank.code
            bond.winnerBidRate = winner.bidRate
            bond.winnerPremium = winner.premium
            PrivateBankRepository.saveFoxBond(bond)

            settled++
        }

        return settled
    }

    fun redeemMaturedHoldings(now: Date = Date()): Int {
        var redeemed = 0
        val holdings = PrivateBankRepository.listAllFoxBondHoldings()
            .filter { it.redeemedAt == null && it.dueAt.before(now) }

        for (h in holdings) {
            val bond = PrivateBankRepository.findFoxBondByCode(h.bondCode) ?: continue
            val bank = PrivateBankRepository.findBankByCode(h.bankCode) ?: continue

            // 解锁本金（从锁仓池扣减），再把“本金+收益”打回准备金
            val okUnlock = EconomyUtil.plusMoneyToPluginBankForId(bond.code, PrivateBankLedger.FOX_BOND_LOCK_DESC, -h.principal)
            if (!okUnlock) continue

            val payout = ShareUtils.rounding(h.principal * (1 + (h.rate / 100.0) * bond.termDays))
            EconomyUtil.plusMoneyToBankFromId(bank.code, PrivateBankLedger.RESERVE_DESC, payout)

            h.redeemedAt = now
            PrivateBankRepository.saveFoxBondHolding(h)

            bond.status = "FINISHED"
            PrivateBankRepository.saveFoxBond(bond)

            redeemed++
        }
        return redeemed
    }
}
