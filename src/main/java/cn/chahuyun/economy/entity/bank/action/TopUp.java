package cn.chahuyun.economy.entity.bank.action;


import net.mamoe.mirai.contact.User;

/**
 * @author Erzbir
 * @Date: 2022/11/29 21:53
 * 存款功能
 */
public class TopUp extends BankAction {
    private int money;

    public TopUp(User user) {
        this.setUser(user);
    }

    @Override
    public void execute() {

    }
}
