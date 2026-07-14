package cn.chahuyun.economy.privatebank

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

object PrivateBankLocks {
    private val locks = ConcurrentHashMap<String, ReentrantLock>()

    fun <T> withBankLock(bankCode: String, action: () -> T): T =
        locks.computeIfAbsent(bankCode) { ReentrantLock() }.withLock(action)
}
