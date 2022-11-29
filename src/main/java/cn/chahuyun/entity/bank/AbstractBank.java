package cn.chahuyun.entity.bank;

import lombok.Getter;
import lombok.Setter;

/**
 * @author Erzbir
 * @Date: 2022/11/29 22:11
 */
@Getter
@Setter
public class AbstractBank {
    private String name; // 预留, 如果有需要多个银行, 就一个银行绑定唯一标识

    private BankAction bankAction;

}
