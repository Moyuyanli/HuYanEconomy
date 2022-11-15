package cn.chahuyun.entity;

import jakarta.persistence.*;

/**
 * 用户背包
 *
 * @author Moyuyanli
 * @date 2022/11/15 9:02
 */
@Entity
@Table
public class UserBackpack {
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
    private long cardId;

    public UserBackpack() {
    }

    public UserBackpack(int userId, String propsCode, int cardId) {
        this.userId = userId;
        this.propsCode = propsCode;
        this.cardId = cardId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getPropsCode() {
        return propsCode;
    }

    public void setPropsCode(String propsCode) {
        this.propsCode = propsCode;
    }

    public Long getCardId() {
        return cardId;
    }

    public void setCardId(int cardId) {
        this.cardId = cardId;
    }
}
