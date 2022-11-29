package cn.chahuyun.entity.bank;

/**
 * @author Erzbir
 * @Date: 2022/11/29 21:53
 * 存款功能
 */
public class TopUp extends BankAction{
    @Override
    public Boolean execute() {
        return null;
    }

    public TopUp(Long id) {
        this.id = id;
    }
}
