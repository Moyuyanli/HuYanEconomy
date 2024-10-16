package cn.chahuyun.economy.prop;

import cn.chahuyun.economy.entity.UserInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

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
@SuperBuilder(toBuilder = true)
public abstract class PropBase implements Serializable {

    /**
     * 道具种类id
     */
    private String kind;
    /**
     * 类型code
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
     * 能否购买
     */
    @Builder.Default
    private boolean canBuy = false;
    /**
     * 道具价值
     */
    @Builder.Default
    private int cost = 0;
    /**
     * 是否可以叠加物品
     */
    @Builder.Default
    private boolean stack = false;
    /**
     * 道具数量单位
     */
    private String unit;
    /**
     * 是否可复用
     */
    @Builder.Default
    private boolean reuse = false;
    /**
     * 数量
     */
    @Builder.Default
    private Integer num = 0;
    /**
     * 获得时间
     */
    @Builder.Default
    private Date getTime = new Date();

    /**
     * 能否过期
     */
    @Builder.Default
    private boolean canItExpire = false;

    /**
     * 过期时间(天)
     */
    @Builder.Default
    private Integer expire = -1;
    /**
     * 过期时间
     */
    private Date expiredTime;

    public PropBase(String kind, String code, String name) {
        this.kind = kind;
        this.code = code;
        this.name = name;
    }


    @Override
    public String toString() {
        return "道具名称:" + name +
                "\n道具数量:" + num +
                "\n道具描述:" + description;
    }

    /**
     * 商店显示描述
     *
     * @return 商店显示结果
     */
    public abstract String toShopInfo();


    /**
     * 使用该道具
     */
    public abstract void use(UserInfo user);
}


