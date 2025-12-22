package cn.chahuyun.economy.model.bank;

import cn.chahuyun.economy.model.bank.action.BankAction;
import lombok.Getter;
import lombok.Setter;
import net.mamoe.mirai.contact.User;

/**
 * @author Erzbir
 * @Date: 2022/11/29 22:11
 */
@Getter
@Setter
public class AbstractBank {
    private User user;

    private BankAction bankAction;

}

