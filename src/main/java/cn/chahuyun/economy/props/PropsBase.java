package cn.chahuyun.economy.props;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 道具基本<p>
 * 所有道具都应该继承这个类 实现 [Serializable]<p>
 *
 * @author Moyuyanli
 * @date 2022/11/14 8:52
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
/*
 这里应该不是一个可存数据库的类型

//@Entity(name = "PropsBase")
//@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
//@Table(name = "PropsBase",
//        uniqueConstraints = {@UniqueConstraint(columnNames = "code")})
//    @Column(columnDefinition = "date default now()")
 */
public abstract class PropsBase implements Serializable {

    /**
     * 道具种类id
     */
    private String code;
    /**
     * 道具名称
     */
    private String name;
    /**
     * 道具描述
     */
    private String description;
    /**
     * 道具价值
     */
    private int cost;
    /**
     * 是否可以叠加物品
     */
    private boolean stack;
    /**
     * 道具数量单位
     */
    private String unit;
    /**
     * 是否可复用
     */
    private boolean reuse;
    /**
     * 数量
     */
    private Integer num;
    /**
     * 获得时间
     */
    private Date getTime = new Date();
    /**
     * 能否过期
     */
    private boolean canItExpire;
    /**
     * 过期时间
     */
    private Date expiredTime;

    @Override
    public String toString() {
        return "道具名称:" + name +
                "\n道具数量:" + num +
                "\n道具描述:" + description;
    }

    /**
     * 使用该道具
     */
    public abstract void use();
}


