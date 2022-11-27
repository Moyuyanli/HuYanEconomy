package cn.chahuyun.entity;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 鱼道具
 *
 * @author Moyuyanli
 * @date 2022/11/14 12:35
 */
@Getter
@Setter
public class PropsFish extends PropsBase<PropsFish> implements Serializable {

    @Override
    public PropsFish getProp() {
        return null;
    }
}
