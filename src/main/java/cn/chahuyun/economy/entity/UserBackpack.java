package cn.chahuyun.economy.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 用户背包
 *
 * @author Moyuyanli
 * @date 2022/11/15 9:02
 */
@Entity(name = "UserBackpack")
@Table
@Getter
@Setter
public class UserBackpack implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    /**
     * 用户id
     */
    private String userId;
    /**
     * 道具编码
     */
    private String propCode;
    /**
     * 道具类型
     */
    private String propKind;
    /**
     * 道具id
     */
    private Long propId;

    @ManyToOne
    private UserInfo userInfo;
    public UserBackpack() {
    }

    public UserBackpack(String userId, String propCode, String propKind, Long propId) {
        this.userId = userId;
        this.propCode = propCode;
        this.propKind = propKind;
        this.propId = propId;
    }
}
