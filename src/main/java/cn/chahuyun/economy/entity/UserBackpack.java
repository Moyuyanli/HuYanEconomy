package cn.chahuyun.economy.entity;

import cn.chahuyun.economy.props.PropsBase;
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
    private String propsCode;
    /**
     * 道具id
     */
    private Long propId;
    /**
     * 道具子类映射
     */
    private String className;

    public UserBackpack() {
    }

    public UserBackpack(String userId, String propsCode, long propId, Class<? extends PropsBase> className) {
        this.userId = userId;
        this.propsCode = propsCode;
        this.propId = propId;
        this.className = className.getName();
    }

    public UserBackpack(UserInfo userInfo, PropsBase propsBase) {
        this.userId = userInfo.getId();
        this.propsCode = propsBase.getCode();
        this.propId = propsBase.getId();
        this.className = propsBase.getClass().getName();
    }

}
