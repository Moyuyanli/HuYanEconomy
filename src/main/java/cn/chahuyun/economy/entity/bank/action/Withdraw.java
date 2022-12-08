package cn.chahuyun.economy.entity.bank.action;

import net.mamoe.mirai.contact.User;

/**
 * @author Erzbir
 * @Date: 2022/11/29 21:52
 * 取款功能
 */
public class Withdraw extends BankAction {
    private int money;

    public Withdraw(User user) {
        this.setUser(user);
    }

    @Override
    public void execute() {

    }
}
