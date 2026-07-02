package cn.chahuyun.economy.prop.effect

import cn.chahuyun.economy.model.props.UseEvent
import cn.chahuyun.economy.prop.BaseProp
import cn.chahuyun.economy.prop.UseResult
import java.util.concurrent.ConcurrentHashMap

/**
 * 道具效果处理器。
 *
 * 道具对象只保存状态，具体效果由处理器承接，避免功能逻辑散落在道具模型里。
 */
interface PropEffectHandler {
    val codes: Set<String>

    suspend fun use(prop: BaseProp, event: UseEvent): UseResult
}

object PropEffectRegistry {

    private val handlers = ConcurrentHashMap<String, PropEffectHandler>()

    fun register(handler: PropEffectHandler): Boolean {
        var success = true
        handler.codes.forEach { code ->
            if (handlers.putIfAbsent(code, handler) != null) {
                success = false
            }
        }
        return success
    }

    fun registerAll(vararg handlers: PropEffectHandler) {
        handlers.forEach { register(it) }
    }

    fun get(code: String): PropEffectHandler? = handlers[code]

    suspend fun use(prop: BaseProp, event: UseEvent): UseResult? {
        return get(prop.code)?.use(prop, event)
    }
}
