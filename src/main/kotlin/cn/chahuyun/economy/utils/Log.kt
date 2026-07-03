package cn.chahuyun.economy.utils

/**
 * Common logger facade.
 *
 * Modules outside economy-main must not know about the Mirai plugin instance.
 * The plugin entry wires the real Mirai logger during startup.
 */
object Log {
    private var sink: Sink = ConsoleSink

    @JvmField
    var name: String = "壶言经济--"

    interface Sink {
        fun info(message: String)
        fun warning(message: String)
        fun error(message: String)
        fun error(message: String, throwable: Throwable)
        fun debug(message: String)
    }

    private object ConsoleSink : Sink {
        override fun info(message: String) = println(message)
        override fun warning(message: String) = println(message)
        override fun error(message: String) = System.err.println(message)
        override fun error(message: String, throwable: Throwable) {
            System.err.println(message)
            throwable.printStackTrace()
        }

        override fun debug(message: String) = println(message)
    }

    @JvmStatic
    fun configure(sink: Sink) {
        this.sink = sink
    }

    @JvmStatic
    fun configure(
        info: (String) -> Unit,
        warning: (String) -> Unit,
        error: (String) -> Unit,
        errorWithThrowable: (String, Throwable) -> Unit,
        debug: (String) -> Unit,
    ) {
        sink = object : Sink {
            override fun info(message: String) = info(message)
            override fun warning(message: String) = warning(message)
            override fun error(message: String) = error(message)
            override fun error(message: String, throwable: Throwable) = errorWithThrowable(message, throwable)
            override fun debug(message: String) = debug(message)
        }
    }

    @JvmStatic
    fun info(msg: String) {
        sink.info(name + msg)
    }

    @JvmStatic
    fun warning(msg: String) {
        sink.warning(name + msg)
    }

    @JvmStatic
    fun error(msg: String) {
        sink.error(name + msg)
    }

    @JvmStatic
    fun error(e: Throwable) {
        sink.error(name + (e.message ?: "unknown error"), e)
    }

    @JvmStatic
    fun error(msg: String, e: Throwable) {
        sink.error(name + msg)
        sink.error(name + (e.message ?: "unknown error"), e)
    }

    @JvmStatic
    fun debug(msg: String) {
        sink.debug(name + msg)
    }

    @JvmStatic
    fun debug(exception: Exception) {
        sink.debug(name + exception)
    }
}
