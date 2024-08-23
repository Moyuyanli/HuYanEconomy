package cn.chahuyun.economy.plugin;

import cn.chahuyun.economy.entity.yiyan.YiYan;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
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

    private YiYanManager() {
    }

    public static void init() {
        CompletableFuture.runAsync(() -> {
            for (int i = 0; i < 5; i++) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
                getYiYanRequest();
            }
        });
    }

    private static void getYiYanRequest() {
        CompletableFuture.runAsync(() -> {
            try {
                YiYan yiYan = JSONUtil.parseObj(HttpUtil.get("https://v1.hitokoto.cn")).toBean(YiYan.class);
                yiyanList.add(yiYan);
                lock.lock();
                condition.signalAll(); // 通知所有等待的线程
            } finally {
                lock.unlock();
            }
        });
    }

    /**
     * 获取一言
     *
     * @return 一言
     */
    public static synchronized YiYan getYiyan() {
        lock.lock();
        try {
            while (yiyanList.isEmpty()) {
                // 如果列表为空，等待直到列表中有元素可用
                condition.await();
            }
            YiYan yiYan = yiyanList.get(0);
            yiyanList.remove(0);
            // 发起新的异步请求以补充列表
            getYiYanRequest();
            return yiYan;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }
}