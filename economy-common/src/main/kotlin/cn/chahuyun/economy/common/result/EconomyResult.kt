package cn.chahuyun.economy.common.result

/**
 * 跨模块业务结果类型。
 *
 * 适用于“业务失败是正常分支”的场景，例如余额不足、权限不满足、目标不存在。
 * 这类失败应通过 [Failure] 携带用户可读 message 返回，而不是抛异常穿过 action/usecase 边界。
 */
sealed class EconomyResult<out T> {
    abstract val message: String

    val isSuccess: Boolean
        get() = this is Success

    val isFailure: Boolean
        get() = this is Failure

    data class Success<out T>(
        val value: T,
        override val message: String = "",
    ) : EconomyResult<T>()

    data class Failure(
        override val message: String,
        val cause: Throwable? = null,
    ) : EconomyResult<Nothing>()

    inline fun onSuccess(block: (T) -> Unit): EconomyResult<T> {
        if (this is Success) block(value)
        return this
    }

    inline fun onFailure(block: (Failure) -> Unit): EconomyResult<T> {
        if (this is Failure) block(this)
        return this
    }

    fun getOrNull(): T? = when (this) {
        is Success -> value
        is Failure -> null
    }

    fun getOrElse(defaultValue: () -> @UnsafeVariance T): T = when (this) {
        is Success -> value
        is Failure -> defaultValue()
    }

    companion object {
        fun <T> success(value: T, message: String = ""): EconomyResult<T> = Success(value, message)

        fun failure(message: String, cause: Throwable? = null): EconomyResult<Nothing> = Failure(message, cause)
    }
}
