package cn.chahuyun.economy.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.awt.*;
import java.io.Serializable;
import java.util.Date;

/**
 * 称号信息<p>
 *
 * @author Moyuyanli
 * @date 2022/12/5 17:01
 */
@Getter
@Setter
@Entity(name = "TitleInfo")
@Table
public class TitleInfo implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;
    /**
     * 所属者用户id
     */
    private long userId;
    /**
     * 使用状态
     */
    private boolean status;
    /**
     * 称号
     */
    private String title;
    /**
     * 称号颜色
     */
    private Color color;
    /**
     * 称号到期时间
     */
    private Date dueTime;


}
