package cn.chahuyun.economy.privatebank

import cn.chahuyun.economy.converter.v1.*
import cn.chahuyun.economy.converter.v2.*
import cn.chahuyun.economy.data.proxy.DataSourceStrategyImpl
import cn.chahuyun.economy.data.proxy.DataVersion
import cn.chahuyun.economy.data.proxy.EntityProxyRegistry
import cn.chahuyun.economy.entity.privatebank.*
import cn.chahuyun.economy.entity.v2.privatebank.*
import cn.chahuyun.economy.model.privatebank.*
import cn.chahuyun.hibernateplus.HibernateFactory

object PrivateBankRepository {

    private val depositConverter = PrivateBankDepositV1Converter()
    private val reviewConverter = PrivateBankReviewV1Converter()
    private val bondIssueConverter = PrivateBankGovBondIssueV1Converter()
    private val bondHoldingConverter = PrivateBankGovBondHoldingV1Converter()
    private val loanOfferConverter = PrivateBankLoanOfferV1Converter()
    private val loanConverter = PrivateBankLoanV1Converter()
    private val foxBondConverter = PrivateBankFoxBondV1Converter()
    private val foxBondBidConverter = PrivateBankFoxBondBidV1Converter()
    private val foxBondHoldingConverter = PrivateBankFoxBondHoldingV1Converter()
    private val depositV2Converter = PrivateBankDepositV2Converter()
    private val reviewV2Converter = PrivateBankReviewV2Converter()
    private val bondIssueV2Converter = PrivateBankGovBondIssueV2Converter()
    private val bondHoldingV2Converter = PrivateBankGovBondHoldingV2Converter()
    private val loanOfferV2Converter = PrivateBankLoanOfferV2Converter()
    private val loanV2Converter = PrivateBankLoanV2Converter()
    private val foxBondV2Converter = PrivateBankFoxBondV2Converter()
    private val foxBondBidV2Converter = PrivateBankFoxBondBidV2Converter()
    private val foxBondHoldingV2Converter = PrivateBankFoxBondHoldingV2Converter()

    private val isV2: Boolean
        get() = DataSourceStrategyImpl.getVersion("privatebank") == DataVersion.V2

    fun findBankByCode(code: String): PrivateBankDto? =
        privateBankProxy.findByKey(code)

    fun listBanks(): List<PrivateBankDto> =
        privateBankProxy.findAll()

    fun saveBank(bank: PrivateBankDto): PrivateBankDto =
        privateBankProxy.save(bank)

    fun findDeposit(bankCode: String, userQq: Long): PrivateBankDepositDto? {
        val params = hashMapOf<String, Any?>()
        params["bankCode"] = bankCode
        params["userQq"] = userQq
        if (!isV2) return HibernateFactory.selectOne(PrivateBankDeposit::class.java, params)?.let(depositConverter::toDto)
        return HibernateFactory.selectList(PrivateBankDepositEntity::class.java, "bankCode", bankCode)
            .firstOrNull { it.userQq == userQq }
            ?.let(depositV2Converter::toDto)
    }

    fun listDeposits(bankCode: String): List<PrivateBankDepositDto> =
        if (isV2) {
            depositV2Converter.toDtoList(HibernateFactory.selectList(PrivateBankDepositEntity::class.java, "bankCode", bankCode))
        } else {
            depositConverter.toDtoList(HibernateFactory.selectList(PrivateBankDeposit::class.java, "bankCode", bankCode))
        }

    fun saveDeposit(deposit: PrivateBankDepositDto): PrivateBankDepositDto =
        if (isV2) {
            val entity = depositV2Converter.toEntity(deposit)
            val existing = if (deposit.id != 0) {
                HibernateFactory.selectOneById(PrivateBankDepositEntity::class.java, deposit.id.toLong())
            } else {
                HibernateFactory.selectList(PrivateBankDepositEntity::class.java, "bankCode", deposit.bankCode)
                    .firstOrNull { it.userQq == deposit.userQq }
            }
            if (existing != null) entity.id = existing.id
            depositV2Converter.toDto(HibernateFactory.merge(entity))
        } else {
            depositConverter.toDto(HibernateFactory.merge(depositConverter.toEntity(deposit)))
        }

    fun addReview(review: PrivateBankReviewDto): PrivateBankReviewDto =
        if (isV2) {
            reviewV2Converter.toDto(HibernateFactory.merge(reviewV2Converter.toEntity(review)))
        } else {
            reviewConverter.toDto(HibernateFactory.merge(reviewConverter.toEntity(review)))
        }

    fun listReviews(bankCode: String): List<PrivateBankReviewDto> =
        if (isV2) {
            reviewV2Converter.toDtoList(HibernateFactory.selectList(PrivateBankReviewEntity::class.java, "bankCode", bankCode))
        } else {
            reviewConverter.toDtoList(HibernateFactory.selectList(PrivateBankReview::class.java, "bankCode", bankCode))
        }

    fun listReviewsByUser(bankCode: String, userQq: Long): List<PrivateBankReviewDto> {
        val params = hashMapOf<String, Any?>()
        params["bankCode"] = bankCode
        params["userQq"] = userQq
        if (!isV2) return reviewConverter.toDtoList(HibernateFactory.selectList(PrivateBankReview::class.java, params))
        return reviewV2Converter.toDtoList(
            HibernateFactory.selectList(PrivateBankReviewEntity::class.java, "bankCode", bankCode)
                .filter { it.userQq == userQq }
        )
    }

    fun findBondIssueByWeek(weekKey: String): PrivateBankGovBondIssueDto? =
        if (isV2) {
            HibernateFactory.selectOne(PrivateBankGovBondIssueEntity::class.java, "weekKey", weekKey)?.let(bondIssueV2Converter::toDto)
        } else {
            HibernateFactory.selectOne(PrivateBankGovBondIssue::class.java, "weekKey", weekKey)?.let(bondIssueConverter::toDto)
        }

    fun saveBondIssue(issue: PrivateBankGovBondIssueDto): PrivateBankGovBondIssueDto =
        if (isV2) {
            val entity = bondIssueV2Converter.toEntity(issue)
            val existing = if (issue.id != 0) HibernateFactory.selectOneById(PrivateBankGovBondIssueEntity::class.java, issue.id.toLong()) else HibernateFactory.selectOne(PrivateBankGovBondIssueEntity::class.java, "weekKey", issue.weekKey)
            if (existing != null) entity.id = existing.id
            bondIssueV2Converter.toDto(HibernateFactory.merge(entity))
        } else {
            bondIssueConverter.toDto(HibernateFactory.merge(bondIssueConverter.toEntity(issue)))
        }

    fun listBondHoldings(bankCode: String): List<PrivateBankGovBondHoldingDto> =
        if (isV2) {
            bondHoldingV2Converter.toDtoList(HibernateFactory.selectList(PrivateBankGovBondHoldingEntity::class.java, "bankCode", bankCode))
        } else {
            bondHoldingConverter.toDtoList(HibernateFactory.selectList(PrivateBankGovBondHolding::class.java, "bankCode", bankCode))
        }

    fun findBondHolding(id: Int): PrivateBankGovBondHoldingDto? =
        if (isV2) {
            HibernateFactory.selectOneById(PrivateBankGovBondHoldingEntity::class.java, id.toLong())?.let(bondHoldingV2Converter::toDto)
        } else {
            HibernateFactory.selectOneById(PrivateBankGovBondHolding::class.java, id)?.let(bondHoldingConverter::toDto)
        }

    fun saveBondHolding(holding: PrivateBankGovBondHoldingDto): PrivateBankGovBondHoldingDto =
        if (isV2) {
            bondHoldingV2Converter.toDto(HibernateFactory.merge(bondHoldingV2Converter.toEntity(holding)))
        } else {
            bondHoldingConverter.toDto(HibernateFactory.merge(bondHoldingConverter.toEntity(holding)))
        }

    fun listLoanOffers(bankCode: String): List<PrivateBankLoanOfferDto> =
        if (isV2) {
            loanOfferV2Converter.toDtoList(HibernateFactory.selectList(PrivateBankLoanOfferEntity::class.java, "bankCode", bankCode))
        } else {
            loanOfferConverter.toDtoList(HibernateFactory.selectList(PrivateBankLoanOffer::class.java, "bankCode", bankCode))
        }

    fun findLoanOffer(id: Int): PrivateBankLoanOfferDto? =
        if (isV2) {
            HibernateFactory.selectOneById(PrivateBankLoanOfferEntity::class.java, id.toLong())?.let(loanOfferV2Converter::toDto)
        } else {
            HibernateFactory.selectOneById(PrivateBankLoanOffer::class.java, id)?.let(loanOfferConverter::toDto)
        }

    fun saveLoanOffer(offer: PrivateBankLoanOfferDto): PrivateBankLoanOfferDto =
        if (isV2) {
            loanOfferV2Converter.toDto(HibernateFactory.merge(loanOfferV2Converter.toEntity(offer)))
        } else {
            loanOfferConverter.toDto(HibernateFactory.merge(loanOfferConverter.toEntity(offer)))
        }

    fun listLoansByBorrower(borrowerQq: Long): List<PrivateBankLoanDto> =
        if (isV2) {
            loanV2Converter.toDtoList(HibernateFactory.selectList(PrivateBankLoanEntity::class.java, "borrowerQq", borrowerQq))
        } else {
            loanConverter.toDtoList(HibernateFactory.selectList(PrivateBankLoan::class.java, "borrowerQq", borrowerQq))
        }

    fun listLoansByBank(bankCode: String): List<PrivateBankLoanDto> =
        if (isV2) {
            loanV2Converter.toDtoList(HibernateFactory.selectList(PrivateBankLoanEntity::class.java, "bankCode", bankCode))
        } else {
            loanConverter.toDtoList(HibernateFactory.selectList(PrivateBankLoan::class.java, "bankCode", bankCode))
        }

    fun listUnrepaidLoans(): List<PrivateBankLoanDto> {
        return if (isV2) {
            loanV2Converter.toDtoList(HibernateFactory.selectList(PrivateBankLoanEntity::class.java).filter { it.repaidAt == 0L })
        } else {
            val all = HibernateFactory.selectList(PrivateBankLoan::class.java)
            loanConverter.toDtoList(all.filter { it.repaidAt == null })
        }
    }

    fun findLoan(id: Int): PrivateBankLoanDto? =
        if (isV2) {
            HibernateFactory.selectOneById(PrivateBankLoanEntity::class.java, id.toLong())?.let(loanV2Converter::toDto)
        } else {
            HibernateFactory.selectOneById(PrivateBankLoan::class.java, id)?.let(loanConverter::toDto)
        }

    fun saveLoan(loan: PrivateBankLoanDto): PrivateBankLoanDto =
        if (isV2) {
            loanV2Converter.toDto(HibernateFactory.merge(loanV2Converter.toEntity(loan)))
        } else {
            loanConverter.toDto(HibernateFactory.merge(loanConverter.toEntity(loan)))
        }

    // ===== 鐙愬嵎 =====

    fun findFoxBondByCode(code: String): PrivateBankFoxBondDto? =
        if (isV2) {
            HibernateFactory.selectOne(PrivateBankFoxBondEntity::class.java, "code", code)?.let(foxBondV2Converter::toDto)
        } else {
            HibernateFactory.selectOne(PrivateBankFoxBond::class.java, "code", code)?.let(foxBondConverter::toDto)
        }

    fun saveFoxBond(bond: PrivateBankFoxBondDto): PrivateBankFoxBondDto =
        if (isV2) {
            val entity = foxBondV2Converter.toEntity(bond)
            val existing = if (bond.id != 0) HibernateFactory.selectOneById(PrivateBankFoxBondEntity::class.java, bond.id.toLong()) else HibernateFactory.selectOne(PrivateBankFoxBondEntity::class.java, "code", bond.code)
            if (existing != null) entity.id = existing.id
            foxBondV2Converter.toDto(HibernateFactory.merge(entity))
        } else {
            foxBondConverter.toDto(HibernateFactory.merge(foxBondConverter.toEntity(bond)))
        }

    fun listFoxBonds(): List<PrivateBankFoxBondDto> =
        if (isV2) {
            foxBondV2Converter.toDtoList(HibernateFactory.selectList(PrivateBankFoxBondEntity::class.java))
        } else {
            foxBondConverter.toDtoList(HibernateFactory.selectList(PrivateBankFoxBond::class.java))
        }

    fun saveFoxBondBid(bid: PrivateBankFoxBondBidDto): PrivateBankFoxBondBidDto =
        if (isV2) {
            val entity = foxBondBidV2Converter.toEntity(bid)
            val existing = if (bid.id != 0) {
                HibernateFactory.selectOneById(PrivateBankFoxBondBidEntity::class.java, bid.id.toLong())
            } else {
                HibernateFactory.selectList(PrivateBankFoxBondBidEntity::class.java, "bondCode", bid.bondCode)
                    .firstOrNull { it.bankCode == bid.bankCode }
            }
            if (existing != null) entity.id = existing.id
            foxBondBidV2Converter.toDto(HibernateFactory.merge(entity))
        } else {
            foxBondBidConverter.toDto(HibernateFactory.merge(foxBondBidConverter.toEntity(bid)))
        }

    fun listFoxBondBids(bondCode: String): List<PrivateBankFoxBondBidDto> =
        if (isV2) {
            foxBondBidV2Converter.toDtoList(HibernateFactory.selectList(PrivateBankFoxBondBidEntity::class.java, "bondCode", bondCode))
        } else {
            foxBondBidConverter.toDtoList(HibernateFactory.selectList(PrivateBankFoxBondBid::class.java, "bondCode", bondCode))
        }

    fun saveFoxBondHolding(holding: PrivateBankFoxBondHoldingDto): PrivateBankFoxBondHoldingDto =
        if (isV2) {
            foxBondHoldingV2Converter.toDto(HibernateFactory.merge(foxBondHoldingV2Converter.toEntity(holding)))
        } else {
            foxBondHoldingConverter.toDto(HibernateFactory.merge(foxBondHoldingConverter.toEntity(holding)))
        }

    fun listFoxBondHoldings(bankCode: String): List<PrivateBankFoxBondHoldingDto> =
        if (isV2) {
            foxBondHoldingV2Converter.toDtoList(HibernateFactory.selectList(PrivateBankFoxBondHoldingEntity::class.java, "bankCode", bankCode))
        } else {
            foxBondHoldingConverter.toDtoList(HibernateFactory.selectList(PrivateBankFoxBondHolding::class.java, "bankCode", bankCode))
        }

    fun listAllFoxBondHoldings(): List<PrivateBankFoxBondHoldingDto> =
        if (isV2) {
            foxBondHoldingV2Converter.toDtoList(HibernateFactory.selectList(PrivateBankFoxBondHoldingEntity::class.java))
        } else {
            foxBondHoldingConverter.toDtoList(HibernateFactory.selectList(PrivateBankFoxBondHolding::class.java))
        }

    fun migrateSubTablesToV2(): Pair<Int, List<String>> {
        var migrated = 0
        val errors = mutableListOf<String>()

        fun <D> migrate(label: String, items: List<D>, saver: (D) -> D) {
            items.forEach { item ->
                try {
                    saver(item)
                    migrated += 1
                } catch (e: Exception) {
                    errors += "$label: ${e.message ?: e::class.simpleName}"
                }
            }
        }

        migrate("deposit", depositConverter.toDtoList(HibernateFactory.selectList(PrivateBankDeposit::class.java))) { dto ->
            val entity = depositV2Converter.toEntity(dto)
            val existing = if (dto.id != 0) HibernateFactory.selectOneById(PrivateBankDepositEntity::class.java, dto.id.toLong()) else null
            if (existing != null) entity.id = existing.id
            depositV2Converter.toDto(HibernateFactory.merge(entity))
        }
        migrate("review", reviewConverter.toDtoList(HibernateFactory.selectList(PrivateBankReview::class.java))) { dto ->
            reviewV2Converter.toDto(HibernateFactory.merge(reviewV2Converter.toEntity(dto)))
        }
        migrate("govBondIssue", bondIssueConverter.toDtoList(HibernateFactory.selectList(PrivateBankGovBondIssue::class.java))) { dto ->
            val entity = bondIssueV2Converter.toEntity(dto)
            val existing = if (dto.id != 0) HibernateFactory.selectOneById(PrivateBankGovBondIssueEntity::class.java, dto.id.toLong()) else null
            if (existing != null) entity.id = existing.id
            bondIssueV2Converter.toDto(HibernateFactory.merge(entity))
        }
        migrate("govBondHolding", bondHoldingConverter.toDtoList(HibernateFactory.selectList(PrivateBankGovBondHolding::class.java))) { dto ->
            bondHoldingV2Converter.toDto(HibernateFactory.merge(bondHoldingV2Converter.toEntity(dto)))
        }
        migrate("loanOffer", loanOfferConverter.toDtoList(HibernateFactory.selectList(PrivateBankLoanOffer::class.java))) { dto ->
            loanOfferV2Converter.toDto(HibernateFactory.merge(loanOfferV2Converter.toEntity(dto)))
        }
        migrate("loan", loanConverter.toDtoList(HibernateFactory.selectList(PrivateBankLoan::class.java))) { dto ->
            loanV2Converter.toDto(HibernateFactory.merge(loanV2Converter.toEntity(dto)))
        }
        migrate("foxBond", foxBondConverter.toDtoList(HibernateFactory.selectList(PrivateBankFoxBond::class.java))) { dto ->
            val entity = foxBondV2Converter.toEntity(dto)
            val existing = if (dto.id != 0) HibernateFactory.selectOneById(PrivateBankFoxBondEntity::class.java, dto.id.toLong()) else null
            if (existing != null) entity.id = existing.id
            foxBondV2Converter.toDto(HibernateFactory.merge(entity))
        }
        migrate("foxBondBid", foxBondBidConverter.toDtoList(HibernateFactory.selectList(PrivateBankFoxBondBid::class.java))) { dto ->
            foxBondBidV2Converter.toDto(HibernateFactory.merge(foxBondBidV2Converter.toEntity(dto)))
        }
        migrate("foxBondHolding", foxBondHoldingConverter.toDtoList(HibernateFactory.selectList(PrivateBankFoxBondHolding::class.java))) { dto ->
            foxBondHoldingV2Converter.toDto(HibernateFactory.merge(foxBondHoldingV2Converter.toEntity(dto)))
        }

        return migrated to errors
    }

    private val privateBankProxy
        get() = EntityProxyRegistry.get<PrivateBankDto>("privatebank") ?: error("Private bank proxy is not initialized")
}
