package cn.chahuyun.economy.base

import cn.chahuyun.economy.base.entity.EconomyAccountEntity
import cn.chahuyun.economy.base.entity.EconomyScopeEntity
import cn.chahuyun.hibernateplus.HibernateFactory
import org.hibernate.LockMode
import org.hibernate.Session
import java.time.Instant

/**
 * 新经济系统核心。
 *
 * 该对象集中管理事务和 Session 生命周期，保证一次操作只使用一个 Session，
 * 业务层不会跨线程或协程持有 Hibernate Session。
 */
object EconomyCore {
    private const val DEFAULT_SCOPE = "DEFAULT"

    /** Creates the reserved default Scope during plugin startup. */
    fun init() {
        transaction {
            val existing = it.createQuery("from EconomyScopeEntity s where s.code = :code", EconomyScopeEntity::class.java)
                .setParameter("code", DEFAULT_SCOPE).setMaxResults(1).uniqueResult()
            if (existing == null) it.persist(EconomyScopeEntity(name = "默认", code = DEFAULT_SCOPE))
        }
    }

    /** 在指定 Scope 中获取账户；账户不存在时在当前事务中创建。 */
    fun take(account: String, scope: EconomyScope): EconomyAccount = transaction { session ->
        val normalized = normalizeAccount(account)
        val entity = session.createQuery(
            "from EconomyAccountEntity a where a.account = :account and a.scope.id = :scopeId",
            EconomyAccountEntity::class.java
        ).setParameter("account", normalized).setParameter("scopeId", scope.id).setMaxResults(1).uniqueResult()
            ?: EconomyAccountEntity(account = normalized, scope = session.find(EconomyScopeEntity::class.java, scope.id))
                .also { session.persist(it) }
        entity.toModel()
    }

    /** 按 code 查询 Scope。 */
    fun scope(code: String): EconomyScope? = transaction(readOnly = true) { session ->
        session.createQuery("from EconomyScopeEntity s where s.code = :code", EconomyScopeEntity::class.java)
            .setParameter("code", code.trim().uppercase()).setMaxResults(1).uniqueResult()?.toModel()
    }

    /** 按显示名称查询 Scope。 */
    fun scopeName(name: String): EconomyScope? = transaction(readOnly = true) { session ->
        session.createQuery("from EconomyScopeEntity s where s.name = :name", EconomyScopeEntity::class.java)
            .setParameter("name", name.trim()).setMaxResults(1).uniqueResult()?.toModel()
    }

    /** 创建 Scope；code 统一按大写保存并由数据库唯一约束保护。 */
    fun scopeCreate(name: String, code: String): EconomyScope = transaction { session ->
        val normalized = code.trim().uppercase()
        require(name.isNotBlank() && name.length <= 64) { "Scope 名称无效" }
        require(normalized.isNotBlank() && normalized.length <= 16) { "Scope code 无效" }
        require(session.createQuery("from EconomyScopeEntity s where s.code = :code", EconomyScopeEntity::class.java)
            .setParameter("code", normalized).setMaxResults(1).uniqueResult() == null) { "Scope 已存在" }
        EconomyScopeEntity(name = name.trim(), code = normalized).also(session::persist).toModel()
    }

    /** 删除未被任何账户引用的 Scope。DEFAULT Scope 永远不可删除。 */
    fun scopeDelete(scope: EconomyScope): Boolean = transaction { session ->
        require(scope.code != DEFAULT_SCOPE) { "DEFAULT Scope 不可删除" }
        val used = session.createQuery("select count(a.id) from EconomyAccountEntity a where a.scope.id = :id", Long::class.java)
            .setParameter("id", scope.id).singleResult > 0
        if (used) return@transaction false
        session.find(EconomyScopeEntity::class.java, scope.id)?.let { session.remove(it); true } ?: false
    }

    fun add(account: EconomyAccount, amount: EconomyBalance) = mutate(account, amount, false)
    fun subtract(account: EconomyAccount, amount: EconomyBalance) = mutate(account, amount, true)

    fun transfer(from: EconomyAccount, to: EconomyAccount, amount: EconomyBalance): EconomyActionResult {
        if (!valid(amount)) return EconomyActionResult(EconomyActionStatus.INVALID_AMOUNT)
        if (from.id == to.id) return EconomyActionResult(EconomyActionStatus.SAME_ACCOUNT)
        return try {
            transaction { session ->
                val firstId = minOf(from.id, to.id)
                val secondId = maxOf(from.id, to.id)
                session.find(EconomyAccountEntity::class.java, firstId, LockMode.PESSIMISTIC_WRITE)
                session.find(EconomyAccountEntity::class.java, secondId, LockMode.PESSIMISTIC_WRITE)
                val source = session.find(EconomyAccountEntity::class.java, from.id)
                    ?: return@transaction EconomyActionResult(EconomyActionStatus.FAILED)
                val target = session.find(EconomyAccountEntity::class.java, to.id)
                    ?: return@transaction EconomyActionResult(EconomyActionStatus.FAILED)
                if (source.balance < amount.value) return@transaction EconomyActionResult(EconomyActionStatus.INSUFFICIENT_BALANCE, EconomyBalance.of(source.balance))
                source.balance = source.balance.subtract(amount.value)
                target.balance = target.balance.add(amount.value)
                source.updateTime = Instant.now(); target.updateTime = Instant.now()
                EconomyActionResult(EconomyActionStatus.SUCCESS, EconomyBalance.of(source.balance))
            }
        } catch (_: Exception) { EconomyActionResult(EconomyActionStatus.FAILED) }
    }

    private fun mutate(account: EconomyAccount, amount: EconomyBalance, subtract: Boolean): EconomyActionResult {
        if (!valid(amount)) return EconomyActionResult(EconomyActionStatus.INVALID_AMOUNT)
        return try {
            transaction { session ->
                val entity = session.find(EconomyAccountEntity::class.java, account.id, LockMode.PESSIMISTIC_WRITE)
                    ?: return@transaction EconomyActionResult(EconomyActionStatus.FAILED)
                if (subtract && entity.balance < amount.value) return@transaction EconomyActionResult(EconomyActionStatus.INSUFFICIENT_BALANCE, EconomyBalance.of(entity.balance))
                entity.balance = if (subtract) entity.balance.subtract(amount.value) else entity.balance.add(amount.value)
                entity.updateTime = Instant.now()
                EconomyActionResult(EconomyActionStatus.SUCCESS, EconomyBalance.of(entity.balance))
            }
        } catch (_: Exception) { EconomyActionResult(EconomyActionStatus.FAILED) }
    }

    private fun valid(amount: EconomyBalance): Boolean = amount.isPositive()
    private fun normalizeAccount(account: String): String = account.trim().also {
        require(it.isNotEmpty() && it.length <= 64) { "账户标识无效" }
    }

    private fun <T> transaction(readOnly: Boolean = false, block: (Session) -> T): T =
        HibernateFactory.getSessionFactory().fromTransaction { session ->
            if (readOnly) session.defaultReadOnly = true
            block(session)
        }

    private fun EconomyScopeEntity.toModel() = EconomyScope(id, name, code)
    private fun EconomyAccountEntity.toModel(): EconomyAccount =
        EconomyAccount(id, account, scope!!.toModel(), description, balance)
}
