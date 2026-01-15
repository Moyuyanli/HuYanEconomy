package cn.chahuyun.economy.privatebank

import cn.chahuyun.economy.entity.privatebank.*
import cn.chahuyun.hibernateplus.HibernateFactory

object PrivateBankRepository {

    fun findBankByCode(code: String): PrivateBank? =
        HibernateFactory.selectOne(PrivateBank::class.java, "code", code)

    fun listBanks(): List<PrivateBank> = HibernateFactory.selectList(PrivateBank::class.java)

    fun saveBank(bank: PrivateBank): PrivateBank = HibernateFactory.merge(bank)

    fun findDeposit(bankCode: String, userQq: Long): PrivateBankDeposit? {
        val params = hashMapOf<String, Any?>()
        params["bankCode"] = bankCode
        params["userQq"] = userQq
        return HibernateFactory.selectOne(PrivateBankDeposit::class.java, params)
    }

    fun listDeposits(bankCode: String): List<PrivateBankDeposit> =
        HibernateFactory.selectList(PrivateBankDeposit::class.java, "bankCode", bankCode)

    fun saveDeposit(deposit: PrivateBankDeposit): PrivateBankDeposit = HibernateFactory.merge(deposit)

    fun addReview(review: PrivateBankReview): PrivateBankReview = HibernateFactory.merge(review)

    fun listReviews(bankCode: String): List<PrivateBankReview> =
        HibernateFactory.selectList(PrivateBankReview::class.java, "bankCode", bankCode)

    fun findBondIssueByWeek(weekKey: String): PrivateBankGovBondIssue? =
        HibernateFactory.selectOne(PrivateBankGovBondIssue::class.java, "weekKey", weekKey)

    fun saveBondIssue(issue: PrivateBankGovBondIssue): PrivateBankGovBondIssue = HibernateFactory.merge(issue)

    fun listBondHoldings(bankCode: String): List<PrivateBankGovBondHolding> =
        HibernateFactory.selectList(PrivateBankGovBondHolding::class.java, "bankCode", bankCode)

    fun findBondHolding(id: Int): PrivateBankGovBondHolding? = HibernateFactory.selectOneById(PrivateBankGovBondHolding::class.java, id)

    fun saveBondHolding(holding: PrivateBankGovBondHolding): PrivateBankGovBondHolding = HibernateFactory.merge(holding)

    fun listLoanOffers(bankCode: String): List<PrivateBankLoanOffer> =
        HibernateFactory.selectList(PrivateBankLoanOffer::class.java, "bankCode", bankCode)

    fun findLoanOffer(id: Int): PrivateBankLoanOffer? = HibernateFactory.selectOneById(PrivateBankLoanOffer::class.java, id)

    fun saveLoanOffer(offer: PrivateBankLoanOffer): PrivateBankLoanOffer = HibernateFactory.merge(offer)

    fun listLoansByBorrower(borrowerQq: Long): List<PrivateBankLoan> =
        HibernateFactory.selectList(PrivateBankLoan::class.java, "borrowerQq", borrowerQq)

    fun findLoan(id: Int): PrivateBankLoan? = HibernateFactory.selectOneById(PrivateBankLoan::class.java, id)

    fun saveLoan(loan: PrivateBankLoan): PrivateBankLoan = HibernateFactory.merge(loan)
}
