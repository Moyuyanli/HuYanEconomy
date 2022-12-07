package cn.chahuyun.economy.entity.bank;

import lombok.Getter;
import lombok.Setter;

/**
 * @author Erzbir
 * @Date: 2022/11/29 21:55
 * 银行所有操作的父类
 */
@Getter
@Setter
public abstract class BankAction {
    Long id; // 一个操作绑定一个用户

    public abstract Boolean execute();
}
