package cn.chahuyun.entity.bank;

/**
 * @author Erzbir
 * @Date: 2022/11/29 21:55
 */
public class Bank extends AbstractBank{
    public static final Bank INSTANCE = new Bank();

    private Bank() {
        super();
    }

    public static void main(String[] args) {
        // 取款示例
        Bank bank = Bank.INSTANCE;
        bank.setBankAction(new TopUp(191231232L));
        bank.getBankAction().execute();
    }

}
