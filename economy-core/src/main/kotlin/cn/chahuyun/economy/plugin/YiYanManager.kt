package cn.chahuyun.economy.plugin

import cn.chahuyun.economy.model.yiyan.YiYan
import cn.chahuyun.economy.scheduler.HuYanScheduler
import cn.chahuyun.economy.service.EconomyAsyncService
import cn.chahuyun.economy.utils.Log
import cn.hutool.http.HttpUtil
import cn.hutool.json.JSONUtil
import java.util.concurrent.BlockingQueue
import java.util.concurrent.Future
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 一言管理
 */
object YiYanManager {
    private const val YIYAN_CACHE_SIZE = 5
    private const val REFILL_DELAY_SECONDS = 1L
    private const val REQUEST_TIMEOUT_MILLIS = 5_000

    private val yiyanQueue: BlockingQueue<YiYan> = LinkedBlockingQueue(YIYAN_CACHE_SIZE)
    private val isShutdown = AtomicBoolean(false)
    @Volatile
    private var requestFuture: Future<*>? = null

    @JvmStatic
    fun init() {
        isShutdown.set(false)
        submitRequest()
    }

    private fun submitRequest() {
        if (isShutdown.get()) return
        requestFuture = EconomyAsyncService.submit("yiyan") {
            requestOneYiYan()
        }
    }

    private fun requestOneYiYan() {
        if (isShutdown.get()) return

        try {
            if (yiyanQueue.remainingCapacity() == 0) return

            val yiYan = JSONUtil.parseObj(HttpUtil.get("https://v1.hitokoto.cn", REQUEST_TIMEOUT_MILLIS))
                .toBean(YiYan::class.java)
            yiyanQueue.offer(yiYan)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        } catch (e: Exception) {
            yiyanQueue.offer(YiYan(-1, "这里是小狐狸哒~", "kemomimi", "小狐狸语录"))
        } finally {
            if (!isShutdown.get() && !Thread.currentThread().isInterrupted) {
                HuYanScheduler.scheduleOnce("yiyan", REFILL_DELAY_SECONDS, TimeUnit.SECONDS) { submitRequest() }
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
            requestFuture?.cancel(true)
            requestFuture = null
            Log.info("一言管理:已关闭")
        }
    }
}
