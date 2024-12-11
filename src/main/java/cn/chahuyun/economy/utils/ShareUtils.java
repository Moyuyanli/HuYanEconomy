package cn.chahuyun.economy.utils;


import cn.chahuyun.economy.HuYanEconomy;
import cn.chahuyun.economy.config.EconomyConfig;
import cn.hutool.core.util.RandomUtil;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.contact.User;
import net.mamoe.mirai.event.GlobalEventChannel;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.SingleMessage;

import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 公共工具
 *
 * @author Moyuyanli
 * @date 2022/12/8 16:23
 */
public class ShareUtils {

    private static final Set<CountDownLatch> latchCache = new CopyOnWriteArraySet<>();
    private static ExecutorService executorService;

    private ShareUtils() {
    }

    /**
     * 初始化消息线程池
     */
    public static void init() {
        executorService = Executors.newFixedThreadPool(EconomyConfig.INSTANCE.getNextMessageExecutorsNumber());
    }

    /**
     * 获取用户的下一次消息事件
     *
     * @param user    用户
     * @param subject 载体
     * @return MessageEvent or null
     * @author Moyuyanli
     * @date 2022/8/20 12:37
     */
    public static MessageEvent getNextMessageEventFromUser(User user, Contact subject) {
        AtomicReference<MessageEvent> result = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        // 将 latch 添加到缓存中
        synchronized (latchCache) { // 使用类锁保证线程安全
            latchCache.add(latch);
        }

        CompletableFuture.runAsync(() -> {
            GlobalEventChannel.INSTANCE.parentScope(HuYanEconomy.INSTANCE)
                    .filterIsInstance(MessageEvent.class)
                    .filter(filter -> filter.getSubject().getId() == subject.getId() && filter.getSender().getId() == user.getId())
                    .subscribeOnce(MessageEvent.class, event -> {
                        result.set(event);
                        latch.countDown();
                    });
        },executorService);

        try {
            if (latch.await(10, TimeUnit.MINUTES)) {
                return result.get();
            } else {
                Log.debug("获取用户下一条消息超时");
                return null;
            }
        } catch (InterruptedException e) {
            Log.debug("获取用户下一条消息被中断");
            Thread.currentThread().interrupt(); // 恢复中断状态
            return null;
        } finally {
            // 确保在任务完成后移除 latch
            synchronized (latchCache) {
                latchCache.remove(latch);
            }
        }
    }

    /**
     * 替换字符串中的变量
     *
     * @param s        原始字符串
     * @param variable 变量数组
     * @param content  替换内容数组
     * @return 替换后的字符串
     */
    public static String replacer(String s, String[] variable, Object... content) {
        for (int i = 0; i < variable.length; i++) {
            s = s.replace(variable[i], content[i].toString());
        }
        return s;
    }

    /**
     * 获取消息中的at人<p/>
     * 包括成功at消息和[@xxx]消息
     *
     * @param event 群消息事件
     * @return at群成员，可能为空
     */
    public static Member getAtMember(GroupMessageEvent event) {
        MessageChain message = event.getMessage();
        for (SingleMessage singleMessage : message) {
            if (singleMessage instanceof At) {
                return event.getGroup().get(((At) singleMessage).getTarget());
            }
        }
        String content = message.contentToString();
        Matcher matcher = Pattern.compile("@\\d{5,11}").matcher(content);
        if (matcher.find()) {
            return event.getGroup().get(Long.parseLong(matcher.group().substring(1)));
        }
        return null;
    }

    /**
     * 四舍五入后，保留一位小数
     *
     * @param value 值
     * @return double, 保留一位小数
     */
    public static double rounding(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    /**
     * 将百分比的Double转换为int(*100)
     *
     * @param value 值
     * @return 转换为int的值
     */
    public static int percentageToInt(double value) {
        return (int) Math.round(value * 100);
    }

    /**
     * 随机比较
     * @param probability 成功概率 0~100
     * @return true 结果
     */
    public static boolean randomCompare(int probability) {
        if (probability < 0 || probability > 100) {
            throw new IllegalArgumentException("概率只有0~100");
        }

        int randomed = RandomUtil.randomInt(1, 101);

        return randomed <= probability;
    }

    /**
     * 优雅地关闭所有线程并取消所有任务
     */
    public static void shutdown() {
        // 首先通知所有等待的 latch
        for (CountDownLatch latch : latchCache) {
            latch.countDown();
        }
        latchCache.clear(); // 清空缓存，防止内存泄漏

        // 如果有 executorService，关闭它
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
            try {
                if (!executorService.awaitTermination(3, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}
