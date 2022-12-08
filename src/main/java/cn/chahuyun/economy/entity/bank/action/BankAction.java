package cn.chahuyun.economy.entity.bank.action;

import lombok.Getter;
import lombok.Setter;
import net.mamoe.mirai.contact.User;

/**
 * @author Erzbir
 * @Date: 2022/11/29 21:55
 * 银行所有操作的父类
 */
@Getter
@Setter
public abstract class BankAction {
    private User user; // 一个操作绑定一个用户

    public abstract void execute() throws Exception;
}
