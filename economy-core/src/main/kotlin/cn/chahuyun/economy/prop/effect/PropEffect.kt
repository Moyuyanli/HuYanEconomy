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
    /** 当前处理器负责的道具 code 集合。 */
    val codes: Set<String>

    /** 执行道具效果；返回失败结果表示正常业务失败，不应直接抛给用户入口。 */
    suspend fun use(prop: BaseProp, event: UseEvent): UseResult
}

/**
 * 道具效果注册中心。
 *
 * 道具模型通过 code 查找处理器，新增道具效果时只需注册新的 PropEffectHandler，
 * 不需要修改 PropsCard/FunctionProps/FishBait 等模型类。
 */
object PropEffectRegistry {

    private val handlers = ConcurrentHashMap<String, PropEffectHandler>()

    fun register(handler: PropEffectHandler): Boolean {
        var success = true
        handler.codes.forEach { code ->
            // putIfAbsent 防止两个处理器抢占同一个 code；返回 false 让调用方在初始化日志中发现冲突。
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
