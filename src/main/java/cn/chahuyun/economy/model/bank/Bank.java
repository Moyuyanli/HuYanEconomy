package cn.chahuyun.economy.model.bank;

import cn.chahuyun.economy.model.bank.action.BankAction;

/**
 * @author Erzbir
 * @Date: 2022/11/29 21:55
 */
public class Bank extends AbstractBank {
    public static final Bank INSTANCE = new Bank();

    private Bank() {
        super();
    }

    public void execute(BankAction action) throws Exception {
        action.execute();
    }

}

