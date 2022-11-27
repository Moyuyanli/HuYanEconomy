package cn.chahuyun.entity;

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
@Entity
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
    private long userId;
    /**
     * 道具编码
     */
    private String propsCode;
    /**
     * 道具id
     */
    private long propId;

    public UserBackpack() {
    }

    public UserBackpack(int userId, String propsCode, int propId) {
        this.userId = userId;
        this.propsCode = propsCode;
        this.propId = propId;
    }
}
