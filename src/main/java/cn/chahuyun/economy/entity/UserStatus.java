package cn.chahuyun.economy.entity;

import cn.chahuyun.economy.constant.UserLocation;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 用户状态
 *
 * @author Moyuyanli
 * @date 2024/9/5 16:44
 */
@Entity
@Table(name = "user_status")
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
    private UserLocation place;



}
