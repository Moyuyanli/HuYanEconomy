package cn.chahuyun.entity.props;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/**
 * 鱼道具
 *
 * @author Moyuyanli
 * @date 2022/11/14 12:35
 */
@Getter
@Setter
public class PropsFish extends PropsBase implements Serializable {
    private boolean status;
    private boolean operation = false;
    private Date enabledTime;
    private String aging;

    public PropsFish() {
    }

    public PropsFish(String code, String name, int cost, String description, boolean reuse, Date getTime, Date expiredTime, boolean status, boolean operation, Date enabledTime, String aging) {
        super(code, name, cost, description, reuse, getTime, expiredTime);
        this.status = status;
        this.operation = operation;
        this.enabledTime = enabledTime;
        this.aging = aging;
    }

    @Override
    public String toString() {
        return "卡名称:" + this.getName() +
                "\n价格:" + this.getCost() + "金币" +
                "\n状态:" + (status ? "使用中" : "未使用") +
                "\n描述:" + this.getDescription();
    }
}
