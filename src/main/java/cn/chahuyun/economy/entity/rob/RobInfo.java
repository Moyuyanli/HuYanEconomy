package cn.chahuyun.economy.entity.rob;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

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
    private Long cooldown;
    /**
     * 是否在监狱中
     */
    private boolean isInJail;

}
