package cn.chahuyun.entity.bank;

/**
 * @author Erzbir
 * @Date: 2022/11/29 21:51
 * 转帐功能
 */
public class Transfer extends BankAction {
    @Override
    public Boolean execute() {
        return null;
    }

    public Transfer(Long id) {
        this.id = id;
    }
}
