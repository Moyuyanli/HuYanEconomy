package cn.chahuyun.economy.exception

/**
 * 操作异常
 */
class Operation : RuntimeException {
    @JvmField
    var remove: Boolean = false

    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
    constructor(cause: Throwable) : super(cause)
    constructor(message: String, cause: Throwable, enableSuppression: Boolean, writableStackTrace: Boolean) :
        super(message, cause, enableSuppression, writableStackTrace)

    constructor(message: String, remove: Boolean) : super(message) {
        this.remove = remove
    }

    constructor(remove: Boolean) {
        this.remove = remove
    }
}
