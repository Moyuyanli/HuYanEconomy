package cn.chahuyun.economy.plugin

import cn.chahuyun.economy.model.yiyan.YiYan
import cn.chahuyun.economy.scheduler.HuYanScheduler
import cn.chahuyun.economy.utils.Log
import cn.hutool.http.HttpUtil
import cn.hutool.json.JSONUtil
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 一言管理
 */
object YiYanManager {
    private val yiyanQueue: BlockingQueue<YiYan> = LinkedBlockingQueue(5)
    private val isShutdown = AtomicBoolean(false)

    @JvmStatic
    fun init() {
        requestOneYiYan()
    }

    private fun requestOneYiYan() {
        if (isShutdown.get()) return

        try {
            val yiYan = JSONUtil.parseObj(HttpUtil.get("https://v1.hitokoto.cn")).toBean(YiYan::class.java)
            yiyanQueue.put(yiYan)
        } catch (e: Exception) {
            yiyanQueue.offer(YiYan(-1, "这里是小狐狸哒~", "kemomimi", "小狐狸语录"))
        } finally {
            if (!isShutdown.get()) {
                HuYanScheduler.scheduleOnce("yiyan", 1, TimeUnit.SECONDS) { requestOneYiYan() }
            }
        }
    }

    @JvmStatic
    fun getYiyan(): YiYan {
        val yiYan = yiyanQueue.poll()
        if (yiYan == null) {
            return YiYan(-1, "这里是小狐狸哒~", "kemomimi", "小狐狸语录")
        }
        return yiYan
    }

    /**
     * 优雅地关闭
     */
    @JvmStatic
    fun shutdown() {
        if (isShutdown.compareAndSet(false, true)) {
            HuYanScheduler.cancel("yiyan")
            Log.info("一言管理:已关闭")
        }
    }
}
