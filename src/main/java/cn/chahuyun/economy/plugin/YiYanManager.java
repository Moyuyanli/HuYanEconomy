package cn.chahuyun.economy.plugin;

import cn.chahuyun.economy.entity.yiyan.YiYan;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 一言管理
 *
 * @author Moyuyanli
 * @date 2024/8/13 9:38
 */
public class YiYanManager {

    private static final List<YiYan> yiyanList = new ArrayList<>(5);

    private static final Lock lock = new ReentrantLock();
    private static final Condition condition = lock.newCondition();

    /**
     * 队列数量
     */
    private static Integer queue = 0;

    /**
     * 请求队列启动
     */
    private static AtomicBoolean start = new AtomicBoolean();

    private YiYanManager() {
    }

    public static void init() {
        queue = 5;
        start.set(false);
        CompletableFuture.runAsync(YiYanManager::request);
    }

    /**
     * 启动请求一言请求队列
     */
    private synchronized static void request() {
        if (start.get()) return;
        start.set(true);
        while (queue > 0) {
            getYiYanRequest();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            } finally {
                if (yiyanList.size() >= 8) {
                    queue = 0;
                } else {
                    queue--;
                }
            }
        }
        start.set(false);
    }


    private static void getYiYanRequest() {
        CompletableFuture.runAsync(() -> {
            YiYan yiYan = JSONUtil.parseObj(HttpUtil.get("https://v1.hitokoto.cn")).toBean(YiYan.class);
            yiyanList.add(yiYan);
            // 通知一个
            condition.signal();
        });
    }

    /**
     * 获取一言
     *
     * @return 一言
     */
    public static YiYan getYiyan() {
        lock.lock();
        try {
            while (yiyanList.isEmpty()) {
                // 如果列表为空，等待直到列表中有元素可用
                condition.await();
            }
            YiYan yiYan = yiyanList.get(0);
            yiyanList.remove(0);
            // 发起新的异步请求以补充列表
            queue++;
            if (!start.get()) {
                CompletableFuture.runAsync(YiYanManager::request);
            }
            return yiYan;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }
}