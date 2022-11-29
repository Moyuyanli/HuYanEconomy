package cn.chahuyun.entity.bank;

/**
 * @author Erzbir
 * @Date: 2022/11/29 21:52
 * 取款功能
 */
public class Withdraw extends BankAction {
    @Override
    public Boolean execute() {
        return null;
    }

    public Withdraw(Long id) {
        this.id = id;
    }
}
