package cn.chahuyun.economy.utils;

import net.mamoe.mirai.message.data.*;

/**
 * 消息工具类<p>
 * 对于一些常用的消息构造，进行一个简单构造<p>
 *
 * @author Moyuyanli
 * @date 2022/12/21 11:23
 */
public class MessageUtil {

    private MessageUtil() {
    }

    /**
     * 格式化的消息<p>
     *
     * @param format 消息格式
     * @param params 参数
     * @return PlainText 文本消息
     * @author Moyuyanli
     * @date 2022/12/21 11:30
     */
    public static PlainText formatMessage(String format, Object... params) {
        return new PlainText(String.format(format, params));
    }

    /**
     * 格式化的消息<p>
     *
     * @param format 消息格式
     * @param params 参数
     * @return MessageChain 消息链
     * @author Moyuyanli
     * @date 2022/12/21 11:30
     */
    public static MessageChain formatMessageChain(String format, Object... params) {
        return new MessageChainBuilder().append(String.format(format, params)).build();
    }

    /**
     * 带引用的格式化的消息<p>
     *
     * @param citation 引用消息
     * @param format   消息格式
     * @param params   参数
     * @return MessageChain 消息链
     * @author Moyuyanli
     * @date 2022/12/21 11:30
     */
    public static MessageChain formatMessageChain(MessageChain citation, String format, Object... params) {
        return new MessageChainBuilder().append(new QuoteReply(citation)).append(String.format(format, params)).build();
    }

    /**
     * 带at的格式化的消息<p>
     *
     * @param at     at用户
     * @param format 消息格式
     * @param params 参数
     * @return MessageChain 消息链
     * @author Moyuyanli
     * @date 2022/12/21 11:30
     */
    public static MessageChain formatMessageChain(long at, String format, Object... params) {
        return new MessageChainBuilder().append(new At(at)).append(String.format(format, params)).build();
    }


    /**
     * 带引用的消息构造器<p>
     *
     * @param citation 引用消息
     * @return MessageChainBuild 构造消息器
     * @author Moyuyanli
     * @date 2022/12/21 11:30
     */
    public static MessageChainBuilder quoteReply(MessageChain citation) {
        return new MessageChainBuilder().append(new QuoteReply(citation));
    }

    /**
     * 格式化的消息<p>
     *
     * @param format 消息格式
     * @param params 参数
     * @return MessageChainBuild 构造消息器
     * @author Moyuyanli
     * @date 2022/12/21 11:30
     */
    public static MessageChainBuilder formatMessageBuild(String format, Object... params) {
        return new MessageChainBuilder().append(String.format(format, params));
    }

    /**
     * 带引用的格式化的消息<p>
     *
     * @param citation 引用消息
     * @param format   消息格式
     * @param params   参数
     * @return MessageChainBuild 构造消息器
     * @author Moyuyanli
     * @date 2022/12/21 11:30
     */
    public static MessageChainBuilder formatMessageBuild(MessageChain citation, String format, Object... params) {
        return new MessageChainBuilder().append(new QuoteReply(citation)).append(String.format(format, params));
    }

    /**
     * 带at的格式化的消息<p>
     *
     * @param at     at用户
     * @param format 消息格式
     * @param params 参数
     * @return MessageChainBuild 构造消息器
     * @author Moyuyanli
     * @date 2022/12/21 11:30
     */
    public static MessageChainBuilder formatMessageBuild(long at, String format, Object... params) {
        return new MessageChainBuilder().append(new At(at)).append(String.format(format, params));
    }


}
