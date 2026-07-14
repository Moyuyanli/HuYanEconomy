package cn.chahuyun.economy.data.repository

import cn.chahuyun.economy.converter.v1.*
import cn.chahuyun.economy.converter.v2.*
import cn.chahuyun.economy.data.proxy.DataSourceStrategyImpl
import cn.chahuyun.economy.data.proxy.DataVersion
import cn.chahuyun.economy.entity.privatebank.*
import cn.chahuyun.economy.entity.v2.privatebank.*
import cn.chahuyun.economy.model.privatebank.*

object PrivateBankRepository {

    private val privateBankConverter = PrivateBankV1Converter()
    private val depositConverter = PrivateBankDepositV1Converter()
    private val reviewConverter = PrivateBankReviewV1Converter()
    private val bondIssueConverter = PrivateBankGovBondIssueV1Converter()
    private val bondHoldingConverter = PrivateBankGovBondHoldingV1Converter()
    private val loanOfferConverter = PrivateBankLoanOfferV1Converter()
    private val loanConverter = PrivateBankLoanV1Converter()
    private val mainBankDebtConverter = PrivateBankMainBankDebtV1Converter()
    private val foxBondConverter = PrivateBankFoxBondV1Converter()
    private val foxBondBidConverter = PrivateBankFoxBondBidV1Converter()
    private val foxBondHoldingConverter = PrivateBankFoxBondHoldingV1Converter()
    private val privateBankV2Converter = PrivateBankV2Converter()
    private val depositV2Converter = PrivateBankDepositV2Converter()
    private val reviewV2Converter = PrivateBankReviewV2Converter()
    private val bondIssueV2Converter = PrivateBankGovBondIssueV2Converter()
    private val bondHoldingV2Converter = PrivateBankGovBondHoldingV2Converter()
    private val loanOfferV2Converter = PrivateBankLoanOfferV2Converter()
    private val loanV2Converter = PrivateBankLoanV2Converter()
    private val mainBankDebtV2Converter = PrivateBankMainBankDebtV2Converter()
    private val foxBondV2Converter = PrivateBankFoxBondV2Converter()
    private val foxBondBidV2Converter = PrivateBankFoxBondBidV2Converter()
    private val foxBondHoldingV2Converter = PrivateBankFoxBondHoldingV2Converter()

    private val isV2: Boolean
        get() = DataSourceStrategyImpl.getVersion("privatebank") == DataVersion.V2

    @JvmStatic
    fun findBankEntityById(id: Int): PrivateBank? =
        HibernateDataStore.selectOneById(PrivateBank::class.java, id)

    @JvmStatic
    fun findBankEntityByCode(code: String): PrivateBank? =
        HibernateDataStore.selectOne(PrivateBank::class.java, "code", code)

    @JvmStatic
    fun listBankEntities(): List<PrivateBank> =
        HibernateDataStore.selectList(PrivateBank::class.java)

    @JvmStatic
    fun saveBankEntity(entity: PrivateBank): PrivateBank =
        HibernateDataStore.merge(entity)

    @JvmStatic
    fun deleteBankEntityById(id: Int): Boolean {
        val entity = findBankEntityById(id) ?: return false
        HibernateDataStore.delete(entity)
        return true
    }

    @JvmStatic
    fun findBankEntityV2ById(id: Long): PrivateBankEntity? =
        HibernateDataStore.selectOneById(PrivateBankEntity::class.java, id)

    @JvmStatic
    fun findBankEntityV2ByCode(code: String): PrivateBankEntity? =
        HibernateDataStore.selectOne(PrivateBankEntity::class.java, "code", code)

    @JvmStatic
    fun listBankEntitiesV2(): List<PrivateBankEntity> =
        HibernateDataStore.selectList(PrivateBankEntity::class.java)

    @JvmStatic
    fun saveBankEntityV2(entity: PrivateBankEntity): PrivateBankEntity =
        HibernateDataStore.merge(entity)

    @JvmStatic
    fun deleteBankEntityV2ById(id: Long): Boolean {
        val entity = findBankEntityV2ById(id) ?: return false
        HibernateDataStore.delete(entity)
        return true
    }

    fun findBankByCode(code: String): PrivateBankDto? =
        if (isV2) {
            HibernateDataStore.selectOne(PrivateBankEntity::class.java, "code", code)
                ?.let(privateBankV2Converter::toDto)
        } else {
            HibernateDataStore.selectOne(PrivateBank::class.java, "code", code)
                ?.let(privateBankConverter::toDto)
        }

    fun listBanks(): List<PrivateBankDto> =
        if (isV2) {
            privateBankV2Converter.toDtoList(HibernateDataStore.selectList(PrivateBankEntity::class.java))
        } else {
            privateBankConverter.toDtoList(HibernateDataStore.selectList(PrivateBank::class.java))
        }

    fun saveBank(bank: PrivateBankDto): PrivateBankDto =
        if (isV2) {
            val entity = privateBankV2Converter.toEntity(bank)
            val existing = if (bank.id != 0) {
                HibernateDataStore.selectOneById(PrivateBankEntity::class.java, bank.id.toLong())
            } else {
                HibernateDataStore.selectOne(PrivateBankEntity::class.java, "code", bank.code)
            }
            if (existing != null) {
                entity.id = existing.id
                entity.createdAt = existing.createdAt
            }
            privateBankV2Converter.toDto(HibernateDataStore.merge(entity))
        } else {
            privateBankConverter.toDto(HibernateDataStore.merge(privateBankConverter.toEntity(bank)))
        }

    fun findDeposit(bankCode: String, userQq: Long): PrivateBankDepositDto? {
        val params = hashMapOf<String, Any?>()
        params["bankCode"] = bankCode
        params["userQq"] = userQq
        if (!isV2) return HibernateDataStore.selectOne(PrivateBankDeposit::class.java, params)?.let(depositConverter::toDto)
        return HibernateDataStore.selectList(PrivateBankDepositEntity::class.java, "bankCode", bankCode)
            .firstOrNull { it.userQq == userQq }
            ?.let(depositV2Converter::toDto)
    }

    fun listDeposits(bankCode: String): List<PrivateBankDepositDto> =
        if (isV2) {
            depositV2Converter.toDtoList(HibernateDataStore.selectList(PrivateBankDepositEntity::class.java, "bankCode", bankCode))
        } else {
            depositConverter.toDtoList(HibernateDataStore.selectList(PrivateBankDeposit::class.java, "bankCode", bankCode))
        }

    fun saveDeposit(deposit: PrivateBankDepositDto): PrivateBankDepositDto =
        if (isV2) {
            val entity = depositV2Converter.toEntity(deposit)
            val existing = if (deposit.id != 0) {
                HibernateDataStore.selectOneById(PrivateBankDepositEntity::class.java, deposit.id.toLong())
            } else {
                HibernateDataStore.selectList(PrivateBankDepositEntity::class.java, "bankCode", deposit.bankCode)
                    .firstOrNull { it.userQq == deposit.userQq }
            }
            if (existing != null) entity.id = existing.id
            depositV2Converter.toDto(HibernateDataStore.merge(entity))
        } else {
            depositConverter.toDto(HibernateDataStore.merge(depositConverter.toEntity(deposit)))
        }

    fun addReview(review: PrivateBankReviewDto): PrivateBankReviewDto =
        if (isV2) {
            reviewV2Converter.toDto(HibernateDataStore.merge(reviewV2Converter.toEntity(review)))
        } else {
            reviewConverter.toDto(HibernateDataStore.merge(reviewConverter.toEntity(review)))
        }

    fun listReviews(bankCode: String): List<PrivateBankReviewDto> =
        if (isV2) {
            reviewV2Converter.toDtoList(HibernateDataStore.selectList(PrivateBankReviewEntity::class.java, "bankCode", bankCode))
        } else {
            reviewConverter.toDtoList(HibernateDataStore.selectList(PrivateBankReview::class.java, "bankCode", bankCode))
        }

    fun listReviewsByUser(bankCode: String, userQq: Long): List<PrivateBankReviewDto> {
        val params = hashMapOf<String, Any?>()
        params["bankCode"] = bankCode
        params["userQq"] = userQq
        if (!isV2) return reviewConverter.toDtoList(HibernateDataStore.selectList(PrivateBankReview::class.java, params))
        return reviewV2Converter.toDtoList(
            HibernateDataStore.selectList(PrivateBankReviewEntity::class.java, "bankCode", bankCode)
                .filter { it.userQq == userQq }
        )
    }

    fun findBondIssueByWeek(weekKey: String): PrivateBankGovBondIssueDto? =
        if (isV2) {
            HibernateDataStore.selectOne(PrivateBankGovBondIssueEntity::class.java, "weekKey", weekKey)?.let(bondIssueV2Converter::toDto)
        } else {
            HibernateDataStore.selectOne(PrivateBankGovBondIssue::class.java, "weekKey", weekKey)?.let(bondIssueConverter::toDto)
        }

    fun findBondIssueByCode(code: String): PrivateBankGovBondIssueDto? =
        if (isV2) {
            HibernateDataStore.selectOne(PrivateBankGovBondIssueEntity::class.java, "code", code)?.let(bondIssueV2Converter::toDto)
                ?: HibernateDataStore.selectOne(PrivateBankGovBondIssueEntity::class.java, "weekKey", code)?.let(bondIssueV2Converter::toDto)
        } else {
            HibernateDataStore.selectOne(PrivateBankGovBondIssue::class.java, "code", code)?.let(bondIssueConverter::toDto)
                ?: HibernateDataStore.selectOne(PrivateBankGovBondIssue::class.java, "weekKey", code)?.let(bondIssueConverter::toDto)
        }

    fun listBondIssues(): List<PrivateBankGovBondIssueDto> =
        if (isV2) {
            bondIssueV2Converter.toDtoList(HibernateDataStore.selectList(PrivateBankGovBondIssueEntity::class.java))
        } else {
            bondIssueConverter.toDtoList(HibernateDataStore.selectList(PrivateBankGovBondIssue::class.java))
        }

    fun saveBondIssue(issue: PrivateBankGovBondIssueDto): PrivateBankGovBondIssueDto =
        if (isV2) {
            val entity = bondIssueV2Converter.toEntity(issue)
            val existing = if (issue.id != 0) {
                HibernateDataStore.selectOneById(PrivateBankGovBondIssueEntity::class.java, issue.id.toLong())
            } else {
                issue.code.takeIf { it.isNotBlank() }
                    ?.let { HibernateDataStore.selectOne(PrivateBankGovBondIssueEntity::class.java, "code", it) }
                    ?: HibernateDataStore.selectOne(PrivateBankGovBondIssueEntity::class.java, "weekKey", issue.weekKey)
            }
            if (existing != null) entity.id = existing.id
            bondIssueV2Converter.toDto(HibernateDataStore.merge(entity))
        } else {
            val entity = bondIssueConverter.toEntity(issue)
            val existing = if (issue.id != 0) {
                HibernateDataStore.selectOneById(PrivateBankGovBondIssue::class.java, issue.id)
            } else {
                issue.code.takeIf { it.isNotBlank() }
                    ?.let { HibernateDataStore.selectOne(PrivateBankGovBondIssue::class.java, "code", it) }
                    ?: HibernateDataStore.selectOne(PrivateBankGovBondIssue::class.java, "weekKey", issue.weekKey)
            }
            if (existing != null) entity.id = existing.id
            bondIssueConverter.toDto(HibernateDataStore.merge(entity))
        }

    fun listBondHoldings(bankCode: String): List<PrivateBankGovBondHoldingDto> =
        if (isV2) {
            bondHoldingV2Converter.toDtoList(HibernateDataStore.selectList(PrivateBankGovBondHoldingEntity::class.java, "bankCode", bankCode))
        } else {
            bondHoldingConverter.toDtoList(HibernateDataStore.selectList(PrivateBankGovBondHolding::class.java, "bankCode", bankCode))
        }

    fun findBondHolding(id: Int): PrivateBankGovBondHoldingDto? =
        if (isV2) {
            HibernateDataStore.selectOneById(PrivateBankGovBondHoldingEntity::class.java, id.toLong())?.let(bondHoldingV2Converter::toDto)
        } else {
            HibernateDataStore.selectOneById(PrivateBankGovBondHolding::class.java, id)?.let(bondHoldingConverter::toDto)
        }

    fun saveBondHolding(holding: PrivateBankGovBondHoldingDto): PrivateBankGovBondHoldingDto =
        if (isV2) {
            bondHoldingV2Converter.toDto(HibernateDataStore.merge(bondHoldingV2Converter.toEntity(holding)))
        } else {
            bondHoldingConverter.toDto(HibernateDataStore.merge(bondHoldingConverter.toEntity(holding)))
        }

    fun listLoanOffers(bankCode: String): List<PrivateBankLoanOfferDto> =
        if (isV2) {
            loanOfferV2Converter.toDtoList(HibernateDataStore.selectList(PrivateBankLoanOfferEntity::class.java, "bankCode", bankCode))
        } else {
            loanOfferConverter.toDtoList(HibernateDataStore.selectList(PrivateBankLoanOffer::class.java, "bankCode", bankCode))
        }

    fun findLoanOffer(id: Int): PrivateBankLoanOfferDto? =
        if (isV2) {
            HibernateDataStore.selectOneById(PrivateBankLoanOfferEntity::class.java, id.toLong())?.let(loanOfferV2Converter::toDto)
        } else {
            HibernateDataStore.selectOneById(PrivateBankLoanOffer::class.java, id)?.let(loanOfferConverter::toDto)
        }

    fun saveLoanOffer(offer: PrivateBankLoanOfferDto): PrivateBankLoanOfferDto =
        if (isV2) {
            loanOfferV2Converter.toDto(HibernateDataStore.merge(loanOfferV2Converter.toEntity(offer)))
        } else {
            loanOfferConverter.toDto(HibernateDataStore.merge(loanOfferConverter.toEntity(offer)))
        }

    fun listLoansByBorrower(borrowerQq: Long): List<PrivateBankLoanDto> =
        if (isV2) {
            loanV2Converter.toDtoList(HibernateDataStore.selectList(PrivateBankLoanEntity::class.java, "borrowerQq", borrowerQq))
        } else {
            loanConverter.toDtoList(HibernateDataStore.selectList(PrivateBankLoan::class.java, "borrowerQq", borrowerQq))
        }

    fun listLoansByBank(bankCode: String): List<PrivateBankLoanDto> =
        if (isV2) {
            loanV2Converter.toDtoList(HibernateDataStore.selectList(PrivateBankLoanEntity::class.java, "bankCode", bankCode))
        } else {
            loanConverter.toDtoList(HibernateDataStore.selectList(PrivateBankLoan::class.java, "bankCode", bankCode))
        }

    fun listUnrepaidLoans(): List<PrivateBankLoanDto> {
        return if (isV2) {
            loanV2Converter.toDtoList(HibernateDataStore.selectList(PrivateBankLoanEntity::class.java).filter { it.repaidAt == 0L })
        } else {
            val all = HibernateDataStore.selectList(PrivateBankLoan::class.java)
            loanConverter.toDtoList(all.filter { it.repaidAt == null })
        }
    }

    fun findLoan(id: Int): PrivateBankLoanDto? =
        if (isV2) {
            HibernateDataStore.selectOneById(PrivateBankLoanEntity::class.java, id.toLong())?.let(loanV2Converter::toDto)
        } else {
            HibernateDataStore.selectOneById(PrivateBankLoan::class.java, id)?.let(loanConverter::toDto)
        }

    fun saveLoan(loan: PrivateBankLoanDto): PrivateBankLoanDto =
        if (isV2) {
            loanV2Converter.toDto(HibernateDataStore.merge(loanV2Converter.toEntity(loan)))
        } else {
            loanConverter.toDto(HibernateDataStore.merge(loanConverter.toEntity(loan)))
        }

    fun findMainBankDebt(bankCode: String): PrivateBankMainBankDebtDto? =
        if (isV2) {
            HibernateDataStore.selectOne(PrivateBankMainBankDebtEntity::class.java, "bankCode", bankCode)
                ?.let(mainBankDebtV2Converter::toDto)
        } else {
            HibernateDataStore.selectOne(PrivateBankMainBankDebt::class.java, "bankCode", bankCode)
                ?.let(mainBankDebtConverter::toDto)
        }

    fun listMainBankDebts(): List<PrivateBankMainBankDebtDto> =
        if (isV2) {
            mainBankDebtV2Converter.toDtoList(HibernateDataStore.selectList(PrivateBankMainBankDebtEntity::class.java))
        } else {
            mainBankDebtConverter.toDtoList(HibernateDataStore.selectList(PrivateBankMainBankDebt::class.java))
        }

    fun saveMainBankDebt(debt: PrivateBankMainBankDebtDto): PrivateBankMainBankDebtDto =
        if (isV2) {
            val entity = mainBankDebtV2Converter.toEntity(debt)
            val existing = if (debt.id != 0) {
                HibernateDataStore.selectOneById(PrivateBankMainBankDebtEntity::class.java, debt.id.toLong())
            } else {
                HibernateDataStore.selectOne(PrivateBankMainBankDebtEntity::class.java, "bankCode", debt.bankCode)
            }
            if (existing != null) {
                entity.id = existing.id
                entity.createdAt = existing.createdAt
            }
            mainBankDebtV2Converter.toDto(HibernateDataStore.merge(entity))
        } else {
            val entity = mainBankDebtConverter.toEntity(debt)
            val existing = if (debt.id != 0) {
                HibernateDataStore.selectOneById(PrivateBankMainBankDebt::class.java, debt.id)
            } else {
                HibernateDataStore.selectOne(PrivateBankMainBankDebt::class.java, "bankCode", debt.bankCode)
            }
            if (existing != null) {
                entity.id = existing.id
                entity.createdAt = existing.createdAt
            }
            mainBankDebtConverter.toDto(HibernateDataStore.merge(entity))
        }

    // ===== 狐债 =====

    fun findFoxBondByCode(code: String): PrivateBankFoxBondDto? =
        if (isV2) {
            HibernateDataStore.selectOne(PrivateBankFoxBondEntity::class.java, "code", code)?.let(foxBondV2Converter::toDto)
        } else {
            HibernateDataStore.selectOne(PrivateBankFoxBond::class.java, "code", code)?.let(foxBondConverter::toDto)
        }

    fun saveFoxBond(bond: PrivateBankFoxBondDto): PrivateBankFoxBondDto =
        if (isV2) {
            val entity = foxBondV2Converter.toEntity(bond)
            val existing = if (bond.id != 0) HibernateDataStore.selectOneById(PrivateBankFoxBondEntity::class.java, bond.id.toLong()) else HibernateDataStore.selectOne(PrivateBankFoxBondEntity::class.java, "code", bond.code)
            if (existing != null) entity.id = existing.id
            foxBondV2Converter.toDto(HibernateDataStore.merge(entity))
        } else {
            foxBondConverter.toDto(HibernateDataStore.merge(foxBondConverter.toEntity(bond)))
        }

    fun listFoxBonds(): List<PrivateBankFoxBondDto> =
        if (isV2) {
            foxBondV2Converter.toDtoList(HibernateDataStore.selectList(PrivateBankFoxBondEntity::class.java))
        } else {
            foxBondConverter.toDtoList(HibernateDataStore.selectList(PrivateBankFoxBond::class.java))
        }

    fun listAllFoxBondBids(): List<PrivateBankFoxBondBidDto> =
        if (isV2) {
            foxBondBidV2Converter.toDtoList(HibernateDataStore.selectList(PrivateBankFoxBondBidEntity::class.java))
        } else {
            foxBondBidConverter.toDtoList(HibernateDataStore.selectList(PrivateBankFoxBondBid::class.java))
        }

    fun saveFoxBondBid(bid: PrivateBankFoxBondBidDto): PrivateBankFoxBondBidDto =
        if (isV2) {
            val entity = foxBondBidV2Converter.toEntity(bid)
            val existing = if (bid.id != 0) {
                HibernateDataStore.selectOneById(PrivateBankFoxBondBidEntity::class.java, bid.id.toLong())
            } else {
                HibernateDataStore.selectList(PrivateBankFoxBondBidEntity::class.java, "bondCode", bid.bondCode)
                    .firstOrNull { it.bankCode == bid.bankCode }
            }
            if (existing != null) entity.id = existing.id
            foxBondBidV2Converter.toDto(HibernateDataStore.merge(entity))
        } else {
            foxBondBidConverter.toDto(HibernateDataStore.merge(foxBondBidConverter.toEntity(bid)))
        }

    fun listFoxBondBids(bondCode: String): List<PrivateBankFoxBondBidDto> =
        if (isV2) {
            foxBondBidV2Converter.toDtoList(HibernateDataStore.selectList(PrivateBankFoxBondBidEntity::class.java, "bondCode", bondCode))
        } else {
            foxBondBidConverter.toDtoList(HibernateDataStore.selectList(PrivateBankFoxBondBid::class.java, "bondCode", bondCode))
        }

    fun saveFoxBondHolding(holding: PrivateBankFoxBondHoldingDto): PrivateBankFoxBondHoldingDto =
        if (isV2) {
            foxBondHoldingV2Converter.toDto(HibernateDataStore.merge(foxBondHoldingV2Converter.toEntity(holding)))
        } else {
            foxBondHoldingConverter.toDto(HibernateDataStore.merge(foxBondHoldingConverter.toEntity(holding)))
        }

    fun listFoxBondHoldings(bankCode: String): List<PrivateBankFoxBondHoldingDto> =
        if (isV2) {
            foxBondHoldingV2Converter.toDtoList(HibernateDataStore.selectList(PrivateBankFoxBondHoldingEntity::class.java, "bankCode", bankCode))
        } else {
            foxBondHoldingConverter.toDtoList(HibernateDataStore.selectList(PrivateBankFoxBondHolding::class.java, "bankCode", bankCode))
        }

    fun listAllFoxBondHoldings(): List<PrivateBankFoxBondHoldingDto> =
        if (isV2) {
            foxBondHoldingV2Converter.toDtoList(HibernateDataStore.selectList(PrivateBankFoxBondHoldingEntity::class.java))
        } else {
            foxBondHoldingConverter.toDtoList(HibernateDataStore.selectList(PrivateBankFoxBondHolding::class.java))
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

        migrate("deposit", depositConverter.toDtoList(HibernateDataStore.selectList(PrivateBankDeposit::class.java))) { dto ->
            val entity = depositV2Converter.toEntity(dto)
            val existing = if (dto.id != 0) HibernateDataStore.selectOneById(PrivateBankDepositEntity::class.java, dto.id.toLong()) else null
            if (existing != null) entity.id = existing.id
            depositV2Converter.toDto(HibernateDataStore.merge(entity))
        }
        migrate("review", reviewConverter.toDtoList(HibernateDataStore.selectList(PrivateBankReview::class.java))) { dto ->
            reviewV2Converter.toDto(HibernateDataStore.merge(reviewV2Converter.toEntity(dto)))
        }
        migrate("govBondIssue", bondIssueConverter.toDtoList(HibernateDataStore.selectList(PrivateBankGovBondIssue::class.java))) { dto ->
            val entity = bondIssueV2Converter.toEntity(dto)
            val existing = if (dto.id != 0) HibernateDataStore.selectOneById(PrivateBankGovBondIssueEntity::class.java, dto.id.toLong()) else null
            if (existing != null) entity.id = existing.id
            bondIssueV2Converter.toDto(HibernateDataStore.merge(entity))
        }
        migrate("govBondHolding", bondHoldingConverter.toDtoList(HibernateDataStore.selectList(PrivateBankGovBondHolding::class.java))) { dto ->
            bondHoldingV2Converter.toDto(HibernateDataStore.merge(bondHoldingV2Converter.toEntity(dto)))
        }
        migrate("loanOffer", loanOfferConverter.toDtoList(HibernateDataStore.selectList(PrivateBankLoanOffer::class.java))) { dto ->
            loanOfferV2Converter.toDto(HibernateDataStore.merge(loanOfferV2Converter.toEntity(dto)))
        }
        migrate("loan", loanConverter.toDtoList(HibernateDataStore.selectList(PrivateBankLoan::class.java))) { dto ->
            loanV2Converter.toDto(HibernateDataStore.merge(loanV2Converter.toEntity(dto)))
        }
        migrate("mainBankDebt", mainBankDebtConverter.toDtoList(HibernateDataStore.selectList(PrivateBankMainBankDebt::class.java))) { dto ->
            mainBankDebtV2Converter.toDto(HibernateDataStore.merge(mainBankDebtV2Converter.toEntity(dto)))
        }
        migrate("foxBond", foxBondConverter.toDtoList(HibernateDataStore.selectList(PrivateBankFoxBond::class.java))) { dto ->
            val entity = foxBondV2Converter.toEntity(dto)
            val existing = if (dto.id != 0) HibernateDataStore.selectOneById(PrivateBankFoxBondEntity::class.java, dto.id.toLong()) else null
            if (existing != null) entity.id = existing.id
            foxBondV2Converter.toDto(HibernateDataStore.merge(entity))
        }
        migrate("foxBondBid", foxBondBidConverter.toDtoList(HibernateDataStore.selectList(PrivateBankFoxBondBid::class.java))) { dto ->
            foxBondBidV2Converter.toDto(HibernateDataStore.merge(foxBondBidV2Converter.toEntity(dto)))
        }
        migrate("foxBondHolding", foxBondHoldingConverter.toDtoList(HibernateDataStore.selectList(PrivateBankFoxBondHolding::class.java))) { dto ->
            foxBondHoldingV2Converter.toDto(HibernateDataStore.merge(foxBondHoldingV2Converter.toEntity(dto)))
        }

        return migrated to errors
    }
}
