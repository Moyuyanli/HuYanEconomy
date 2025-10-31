package cn.chahuyun.economy.entity;

import cn.chahuyun.economy.prop.PropBase;
import cn.chahuyun.economy.prop.PropsManager;
import jakarta.persistence.*;
import lombok.Setter;

import java.io.Serializable;

/**
 * 用户背包
 *
 * @author Moyuyanli
 * @date 2022/11/15 9:02
 */
@SuppressWarnings("LombokGetterMayBeUsed")
@Entity(name = "UserBackpack")
@Table
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

    public Long getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getPropCode() {
        return propCode;
    }

    public String getPropKind() {
        return propKind;
    }

    public Long getPropId() {
        return propId;
    }

    //    @ManyToOne
//    private UserInfo userInfo;
    public UserBackpack() {
    }

    public UserBackpack(String userId, String propCode, String propKind, Long propId) {
        this.userId = userId;
        this.propCode = propCode;
        this.propKind = propKind;
        this.propId = propId;
    }

    /**
     * 获取改背包道具
     * @param tClass 类型
     * @return 道具
     */
    public <T extends PropBase> T getProp(Class<T> tClass) {
        return PropsManager.getProp(this, tClass);
    }

}
