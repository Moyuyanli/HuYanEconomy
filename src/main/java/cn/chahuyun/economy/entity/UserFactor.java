package cn.chahuyun.economy.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 用户因子
 *
 * @author Moyuyanli
 * @date 2024/9/26 9:27
 */
@Entity(name = "UserFactor")
@Table
@Getter
@Setter
public class UserFactor {

    @Id
    private Long id;

    /**
     * 暴躁值<br>
     * 打他md
     */
    private Double irritable = 0.3;

    /**
     * 武力值<br>
     * 抢劫成功附加概率
     */
    @Column(name = "`force`")
    private Double force = 0.1;

    /**
     * 闪避值<br>
     * 各种地方的闪避、逃跑概率
     */
    private Double dodge = 0.1;

    /**
     * 反抗因子<br>
     * md,跟你爆了！
     */
    private Double resistance = 0.3;

}
