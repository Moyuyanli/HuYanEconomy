package cn.chahuyun.economy.entity.bank.action;

import cn.chahuyun.economy.util.EconomyUtil;
import net.mamoe.mirai.contact.User;

/**
 * @author Erzbir
 * @Date: 2022/11/29 21:51
 * 转帐功能
 */
public class Transfer extends BankAction {
    private final User toUser;
    private final int money;

    public Transfer(User originUser, User toUser, int money) {
        this.toUser = toUser;
        this.setUser(originUser);
        this.money = money;
    }

    @Override
    public void execute() throws Exception {
        if (money <= 0) {
            throw new Exception("不能为负");
        } else if (getUser().getId() == toUser.getId()) {
            throw new Exception("不能给自己转帐");
        } else if (EconomyUtil.getMoneyByUser(getUser()) < money) {
            throw new Exception("余额不足");
        }
        EconomyUtil.turnUserToUser(getUser(), toUser, money);
    }
}
