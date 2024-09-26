package cn.chahuyun.economy.entity;

import cn.chahuyun.economy.constant.UserLocation;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * 用户状态
 *
 * @author Moyuyanli
 * @date 2024/9/5 16:44
 */
@Entity
@Table(name = "user_status")
@Getter
@Setter
public class UserStatus {

    /**
     * 用户qq
     * 应该跟UserInfo一一对应
     */
    @Id
    private Long id;

    /**
     * 用户所处位置
     */
    private UserLocation place = UserLocation.HOME;

    /**
     * 复原时间 单位分钟
     */
    private Integer recoveryTime = 0;

    /**
     * 开始时间
     */
    private Date startTime;


}
