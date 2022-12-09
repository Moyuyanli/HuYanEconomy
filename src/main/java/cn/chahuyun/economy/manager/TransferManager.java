package cn.chahuyun.economy.manager;

import cn.chahuyun.economy.entity.bank.Bank;
import cn.chahuyun.economy.entity.bank.action.Transfer;
import net.mamoe.mirai.contact.User;

/**
 * 转账管理<p>
 * 转账|抢劫<p>
 *
 * @author Moyuyanli
 * @date 2022/11/14 12:27
 */
public class TransferManager {
    public static String transfer(User originUser, User toUser, int money) {
        try {
            Bank.INSTANCE.execute(new Transfer(originUser, toUser, money));
        } catch (Exception e) {
            return e.getMessage();
        }
        return "转帐成功";
    }
}
