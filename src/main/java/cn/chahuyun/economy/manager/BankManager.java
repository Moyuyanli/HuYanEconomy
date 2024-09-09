package cn.chahuyun.economy.manager;

import cn.chahuyun.authorize.EventComponent;
import cn.chahuyun.authorize.MessageAuthorize;
import cn.chahuyun.authorize.constant.MessageMatchingEnum;
import cn.chahuyun.economy.HuYanEconomy;
import cn.chahuyun.economy.entity.UserInfo;
import cn.chahuyun.economy.entity.bank.BankInfo;
import cn.chahuyun.economy.utils.EconomyUtil;
import cn.chahuyun.economy.utils.Log;
import cn.chahuyun.economy.utils.MessageUtil;
import cn.chahuyun.hibernateplus.HibernateFactory;
import cn.hutool.core.date.DateUtil;
import cn.hutool.cron.CronUtil;
import cn.hutool.cron.task.Task;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.User;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.ForwardMessageBuilder;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.MessageChainBuilder;
import net.mamoe.mirai.message.data.PlainText;
import xyz.cssxsh.mirai.economy.service.EconomyAccount;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 银行管理<p>
 * 存款|取款<p>
 *
 * @author Moyuyanli
 * @date 2022/11/14 12:26
 */
@EventComponent
public class BankManager {

    /**
     * 初始化银行<p>
     * 应当开启银行利息定时器<p>
     *
     * @author Moyuyanli
     * @date 2022/12/21 11:03
     */
    public static void init() {
        BankInfo one = HibernateFactory.selectOne(BankInfo.class, 1);
        if (one == null) {
            BankInfo bankInfo = new BankInfo("global", "主银行", "经济服务", HuYanEconomy.config.getOwner(), 0);
            HibernateFactory.merge(bankInfo);
        }
        List<BankInfo> bankInfos = null;
        try {
            bankInfos = HibernateFactory.selectList(BankInfo.class);
        } catch (Exception e) {
            Log.error("银行管理:利息加载出错!", e);
        }
        BankInterestTask bankInterestTask = new BankInterestTask("bank", bankInfos);
        CronUtil.schedule("bank", "0 0 4 * * ?", bankInterestTask);
    }

    /**
     * 存款<p>
     *
     * @param event 消息事件
     * @author Moyuyanli
     * @date 2022/12/21 11:04
     */
    @MessageAuthorize(text = "存款 \\d+|deposit \\d+", messageMatching = MessageMatchingEnum.REGULAR)
    public static void deposit(MessageEvent event) {
        UserInfo userInfo = UserManager.getUserInfo(event.getSender());
        User user = userInfo.getUser();

        Contact subject = event.getSubject();

        MessageChain message = event.getMessage();
        MessageChainBuilder singleMessages = MessageUtil.quoteReply(message);
        String code = message.serializeToMiraiCode();


        int money = Integer.parseInt(code.split(" ")[1]);
        double moneyByUser = EconomyUtil.getMoneyByUser(user);
        if (moneyByUser - money < 0) {
            singleMessages.append(String.format("你的金币不够%s了", money));
            subject.sendMessage(singleMessages.build());
            return;
        }

        if (EconomyUtil.turnUserToBank(user, money)) {
            singleMessages.append("存款成功!");
            subject.sendMessage(singleMessages.build());
        } else {
            singleMessages.append("存款失败!");
            subject.sendMessage(singleMessages.build());
            Log.error("银行管理:存款失败!");
        }
    }

    /**
     * 取款<p>
     *
     * @param event 消息事件
     * @author Moyuyanli
     * @date 2022/12/21 11:04
     */
    @MessageAuthorize(text = "取款 \\d+|withdraw \\d+", messageMatching = MessageMatchingEnum.REGULAR)
    public static void withdrawal(MessageEvent event) {
        UserInfo userInfo = UserManager.getUserInfo(event.getSender());
        User user = userInfo.getUser();

        Contact subject = event.getSubject();

        MessageChain message = event.getMessage();
        MessageChainBuilder singleMessages = MessageUtil.quoteReply(message);
        String code = message.serializeToMiraiCode();


        int money = Integer.parseInt(code.split(" ")[1]);
        double moneyByBank = EconomyUtil.getMoneyByBank(user);
        if (moneyByBank - money < 0) {
            singleMessages.append(String.format("你的银行余额不够%s枚金币了", money));
            subject.sendMessage(singleMessages.build());
            return;
        }

        if (EconomyUtil.turnBankToUser(user, money)) {
            singleMessages.append("取款成功!");
            subject.sendMessage(singleMessages.build());
        } else {
            singleMessages.append("取款失败!");
            subject.sendMessage(singleMessages.build());
            Log.error("银行管理:取款失败!");
        }
    }

    /**
     * 查看利率
     *
     * @param event 消息事件
     * @author Moyuyanli
     * @date 2022/12/23 16:08
     */
    @MessageAuthorize(text = {"本周利率", "银行利率"})
    public static void viewBankInterest(MessageEvent event) {
        Log.info("银行指令");

        BankInfo bankInfo = HibernateFactory.selectOne(BankInfo.class, 1);
        event.getSubject().sendMessage(MessageUtil.formatMessageChain(event.getMessage(), "本周银行利率是%s%%", bankInfo.getInterest()));
    }

    /**
     * 富豪榜
     *
     * @param event 消息
     */
    @MessageAuthorize(text = {"富豪榜", "经济排行"})
    public static void viewRegalTop(MessageEvent event) {
        Log.info("经济指令");

        Contact subject = event.getSubject();
        Bot bot = event.getBot();

        ForwardMessageBuilder builder = new ForwardMessageBuilder(subject);
        builder.add(bot, new PlainText("以下是银行存款排行榜:"));

        Map<EconomyAccount, Double> accountByBank = EconomyUtil.getAccountByBank();

        LinkedHashMap<EconomyAccount, Double> collect = accountByBank.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).limit(10).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (oldValue, newValue) -> oldValue, LinkedHashMap::new));

        int index = 1;
        for (Map.Entry<EconomyAccount, Double> entry : collect.entrySet()) {
            UserInfo userInfo = UserManager.getUserInfo(entry.getKey());
            Group group = bot.getGroup(userInfo.getRegisterGroup());
            String name;
            if (group == null) {
                name = String.valueOf(userInfo.getRegisterGroup());
            } else {
                name = group.getName();
            }
            PlainText plainText = MessageUtil.formatMessage("top:%d%n" + "用户:%s%n" + "注册群:%s%n" + "存款:%.1f", index++, userInfo.getName(), name, entry.getValue());
            builder.add(bot, plainText);
        }

        subject.sendMessage(builder.build());
    }

}


/**
 * 银行的利息管理
 *
 * @author Moyuyanli
 * @date 2022/12/23 9:22
 */
class BankInterestTask implements Task {


    private final String id;
    /**
     * 银行信息
     */
    private final List<BankInfo> bankList;

    public BankInterestTask(String id, List<BankInfo> bankList) {
        this.id = id;
        this.bankList = bankList;
    }

    /**
     * 执行作业
     * <p>
     * 作业的具体实现需考虑异常情况，默认情况下任务异常在监听中统一监听处理，如果不加入监听，异常会被忽略<br>
     * 因此最好自行捕获异常后处理
     */
    @Override
    public void execute() {
        for (BankInfo bankInfo : bankList) {
            if (bankInfo.isInterestSwitch()) {
                if (DateUtil.thisDayOfWeek() == 2) {
                    bankInfo.setInterest(BankInfo.randomInterest());
                    HibernateFactory.merge(bankInfo);
                }
            }
            if (bankInfo.getId() == 1) {
                int interest = bankInfo.getInterest();
                Map<EconomyAccount, Double> accountByBank = EconomyUtil.getAccountByBank();
                for (Map.Entry<EconomyAccount, Double> entry : accountByBank.entrySet()) {
                    UserInfo userInfo = UserManager.getUserInfo(entry.getKey());
                    double v = entry.getValue() * (interest / 100.0);
                    v = Double.parseDouble(String.format("%.1f", v));
                    if (EconomyUtil.plusMoneyToBankForAccount(entry.getKey(), v)) {
                        userInfo.setBankEarnings(v);
                        HibernateFactory.merge(userInfo);
                    } else {
                        Log.error("银行利息管理:" + id + "添加利息出错");
                    }
                }
            }
        }
    }
}
