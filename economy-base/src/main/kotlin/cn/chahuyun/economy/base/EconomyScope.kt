package cn.chahuyun.economy.base

/**
 * Scope 的脱离实体句柄。
 *
 * 业务层只使用该不可变对象，不直接持有 Hibernate 实体，避免实体和 Session
 * 被带出 EconomyCore 的事务边界。
 */
data class EconomyScope(val id: Long, val name: String, val code: String)
