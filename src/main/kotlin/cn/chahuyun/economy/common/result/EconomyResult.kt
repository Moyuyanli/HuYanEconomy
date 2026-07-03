package cn.chahuyun.economy.common.result

/**
 * Lightweight result type for cross-module workflows.
 *
 * Domain modules can use this when they need to return a value plus a user-facing
 * message without throwing exceptions for normal business failure.
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
