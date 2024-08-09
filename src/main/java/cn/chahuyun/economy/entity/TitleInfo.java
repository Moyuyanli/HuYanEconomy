package cn.chahuyun.economy.entity;

import cn.chahuyun.economy.constant.TitleTemplate;
import cn.chahuyun.economy.utils.ImageUtil;
import jakarta.persistence.*;
import lombok.*;

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
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TitleInfo implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;
    /**
     * 所属者用户id
     */
    private long userId;
    /**
     *
     */
    private TitleTemplate type;
    /**
     * 使用状态
     */
    private boolean status;
    /**
     * 称号
     */
    private String title;
    /**
     * 是否影响名称
     */
    private boolean impactName;
    /**
     * 是否渐变
     */
    private boolean gradient;
    /**
     * 称号颜色
     */
    private String sColor;
    /**
     * 称号颜色
     */
    private String eColor;
    /**
     * 称号到期时间
     */
    private Date dueTime;

    public Color getStartColor() {
        return ImageUtil.hexColor(sColor);
    }

    public Color getEndColor() {
        return ImageUtil.hexColor(eColor);
    }

}
