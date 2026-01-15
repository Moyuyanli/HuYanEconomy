package cn.chahuyun.economy.plugin

import cn.chahuyun.economy.model.yiyan.YiYan
import cn.hutool.http.HttpUtil
import cn.hutool.json.JSONUtil
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 一言管理
 */
object YiYanManager {
    private val yiyanQueue: BlockingQueue<YiYan> = LinkedBlockingQueue(5)
    private val scheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(1)
    private val executor: ExecutorService = Executors.newFixedThreadPool(1)
    private val isShutdown = AtomicBoolean(false)

    @JvmStatic
    fun init() {
        requestOneYiYan()
    }

    private fun requestOneYiYan() {
        if (isShutdown.get()) return

        CompletableFuture.runAsync({
            try {
                val yiYan = JSONUtil.parseObj(HttpUtil.get("https://v1.hitokoto.cn")).toBean(YiYan::class.java)
                yiyanQueue.put(yiYan)
            } catch (e: Exception) {
                yiyanQueue.offer(YiYan(-1, "这里是小狐狸哒~", "kemomimi", "小狐狸语录"))
            } finally {
                if (!isShutdown.get()) {
                    scheduler.schedule({ requestOneYiYan() }, 1, TimeUnit.SECONDS)
                }
            }
        }, executor)
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
     * 优雅地关闭所有线程
     */
    @JvmStatic
    fun shutdown() {
        if (isShutdown.compareAndSet(false, true)) {
            scheduler.shutdown()
            executor.shutdown()
            try {
                if (!scheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow()
                }
                if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                    executor.shutdownNow()
                }
            } catch (e: InterruptedException) {
                scheduler.shutdownNow()
                executor.shutdownNow()
                Thread.currentThread().interrupt()
            }
        }
    }
}
