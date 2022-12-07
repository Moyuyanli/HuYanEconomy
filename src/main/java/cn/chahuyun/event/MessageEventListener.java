package cn.chahuyun.event;

import cn.chahuyun.HuYanEconomy;
import cn.chahuyun.config.EconomyConfig;
import cn.chahuyun.manager.LotteryManager;
import cn.chahuyun.manager.PropsManager;
import cn.chahuyun.manager.SignManager;
import cn.chahuyun.manager.UserManager;
import cn.chahuyun.plugin.PluginManager;
import cn.chahuyun.util.Log;
import kotlin.coroutines.CoroutineContext;
import net.mamoe.mirai.contact.*;
import net.mamoe.mirai.event.EventHandler;
import net.mamoe.mirai.event.SimpleListenerHost;
import net.mamoe.mirai.event.events.EventCancelledException;
import net.mamoe.mirai.event.events.MessageEvent;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

/**
 * 说明
 *
 * @author Moyuyanli
 * @Description :消息检测
 * @Date 2022/7/9 18:11
 */
public class MessageEventListener extends SimpleListenerHost {


    @Override
    public void handleException(@NotNull CoroutineContext context, @NotNull Throwable exception) {
        if (exception instanceof EventCancelledException) {
            Log.error("发送消息被取消:", exception);
        } else if (exception instanceof BotIsBeingMutedException) {
            Log.error("你的机器人被禁言:", exception);
        } else if (exception instanceof MessageTooLargeException) {
            Log.error("发送消息过长:", exception);
        } else if (exception instanceof IllegalArgumentException) {
            Log.error("发送消息为空:", exception);
        }

        // 处理事件处理时抛出的异常
        Log.error(exception);
    }

    /**
     * 消息入口
     *
     * @param event 消息事件
     * @author Moyuyanli
     * @date 2022/11/14 12:34
     */
    @EventHandler()
    public void onMessage(@NotNull MessageEvent event) {
        EconomyConfig config = HuYanEconomy.config;

        User sender = event.getSender();
        //主人
        boolean owner = config.getOwner() == sender.getId();
        Contact subject = event.getSubject();
        Group group = null;
        if (subject instanceof Group) {
            group = (Group) subject;
        }

        String code = event.getMessage().serializeToMiraiCode();
        PropsManager propsManager = PluginManager.getPropsManager();

        switch (code) {
            case "测试":

                return;
            case "签到":
            case "打卡":
            case "sign":
                SignManager.sign(event);
                return;
            case "个人信息":
            case "info":
                UserManager.getUserInfo(event);
                return;
            case "背包":
            case "backpack":
                propsManager.viewUserBackpack(event);
                return;
            case "道具商店":
            case "shops":
                propsManager.propStore(event);
                return;
            case "开启 猜签":
                if (owner) {
                    if (group != null && !config.getGroup().contains(group.getId())) {
                        EconomyConfig.INSTANCE.getGroup().add(group.getId());
                    }
                    subject.sendMessage("本群的猜签功能已开启!");
                    break;
                }
            case "关闭 猜签":
                if (owner) {
                    if (group != null && config.getGroup().contains(group.getId())) {
                        EconomyConfig.INSTANCE.getGroup().remove(group.getId());
                    }
                    subject.sendMessage("本群的猜签功能已关闭!");
                    break;
                }
        }

        String buyPropRegex = "购买 (\\S+)( \\S+)?|buy (\\S+)( \\S+)?";
        if (Pattern.matches(buyPropRegex, code)) {
            Log.info("购买指令");
            propsManager.buyPropFromStore(event);
            return;
        }

        String userPropRegex = "使用 (\\S+)( \\S+)?|use (\\S+)( \\S+)?";
        if (Pattern.matches(userPropRegex, code)) {
            Log.info("使用指令");
            propsManager.userProp(event);
            return;
        }

        String buyLotteryRegex = "猜签 (\\d+)( \\d+)|lottery (\\d+)( \\d+)";
        if (Pattern.matches(buyLotteryRegex, code)) {
            Log.info("彩票指令");
            if (group != null && config.getGroup().contains(group.getId())) {
                LotteryManager.addLottery(event);
            }
            return;
        }

    }

}
