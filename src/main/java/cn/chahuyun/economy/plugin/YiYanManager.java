package cn.chahuyun.economy.plugin;

import cn.chahuyun.economy.model.yiyan.YiYan;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 一言管理
 *
 * @author Moyuyanli
 * @date 2024/8/13 9:38
 */
public class YiYanManager {

    private static final BlockingQueue<YiYan> yiyanQueue = new LinkedBlockingQueue<>(5);
    /**
     * 定时线程池
     */
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    /**
     * 固定数量线程池
     */
    private static final ExecutorService executor = Executors.newFixedThreadPool(1);
    private static final AtomicBoolean isShutdown = new AtomicBoolean(false);

    private YiYanManager() {
    }

    /**
     * 开启一言请求
     */
    public static void init() {
        requestOneYiYan();
    }

    /**
     * 请求一条一言并添加到队列
     */
    private static void requestOneYiYan() {
        if (isShutdown.get()) return;

        CompletableFuture.runAsync(() -> {
            try {
                YiYan yiYan = JSONUtil.parseObj(HttpUtil.get("https://v1.hitokoto.cn")).toBean(YiYan.class);
                // 使用put方法，当队列满时会阻塞
                yiyanQueue.put(yiYan);
            } catch (Exception e) {
                yiyanQueue.offer(new YiYan(-1, "这里是小狐狸哒~", "kemomimi", "小狐狸语录"));
            } finally {
                if (!isShutdown.get()) {
                    // 如果没有关闭，安排下一次请求
                    scheduler.schedule(YiYanManager::requestOneYiYan, 1, TimeUnit.SECONDS);
                }
            }
        }, executor);
    }

    /**
     * 获取一言
     *
     * @return 一言
     */
    public static YiYan getYiyan() {
        YiYan yiYan = yiyanQueue.poll(); // 使用poll方法，如果队列为空则立即返回null
        if (yiYan == null) {
            return new YiYan(-1, "这里是小狐狸哒~", "kemomimi", "小狐狸语录");
        }
        return yiYan;
    }

    /**
     * 优雅地关闭所有线程
     */
    public static void shutdown() {
        if (isShutdown.compareAndSet(false, true)) {
            scheduler.shutdown();
            executor.shutdown();
            try {
                if (!scheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
                if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}