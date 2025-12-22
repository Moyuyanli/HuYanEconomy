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
     * 今日时间
     */
    private Date nowTime;

    /**
     * 被抢劫次数
     */
    private Integer beRobNumber;

    /**
     * 抢劫成功次数
     */
    private Integer robSuccess;

    /**
     * 打人成功次数
     */
    private Integer hitSuccess;

    public Date getNowTime() {
        return nowTime;
    }
}
