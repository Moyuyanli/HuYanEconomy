package cn.chahuyun.economy.entity.rob;

import cn.chahuyun.hibernateplus.HibernateFactory;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import net.mamoe.mirai.contact.User;

import java.util.Date;

@Getter
@Setter
@Entity(name = "RobInfo")
@Table(name = "RobInfo")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RobInfo {
    /**
     * 用户QQ号
     */
    @Id
    private Long userId;
    /**
     * 上次抢劫时间
     */
    private Date lastRobTime;
    /**
     * 抢劫冷却时间
     */
    private Long cooling;
    /**
     * 是否在监狱中
     */
    private boolean isInJail;

    /**
     * 存在位置<p>
     * <li>0.正常</li>
     * <li>1.监狱</li>
     * <li>2.医院</li>
     */
    private int type;

    public Long getCooling() {
        return cooling;
    }

}
