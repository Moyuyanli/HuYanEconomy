package cn.chahuyun.economy.entity;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 用户因子
 *
 * @author Moyuyanli
 * @date 2024/9/26 9:27
 */
@Entity(name = "UserFactor")
@Table
@Getter
@Setter
public class UserFactor {

    @Id
    private Long id;

    /**
     * 暴躁值<br>
     * 打他md
     */
    private Double irritable = 0.3;

    /**
     * 武力值<br>
     * 抢劫成功附加概率
     */
    @Column(name = "`force`")
    private Double force = 0.1;

    /**
     * 闪避值<br>
     * 各种地方的闪避、逃跑概率
     */
    private Double dodge = 0.1;

    /**
     * 反抗因子<br>
     * md,跟你爆了！
     */
    private Double resistance = 0.3;

    /**
     * json存储格式
     */
    private String buff = "[]";

    public String getBuffJson(String buff) {
        return findBuff(buff);
    }

    public UserFactor setBuffJson(String buff, String value) {
        Integer index = findBuffIndex(buff);

        JSONObject json;

        if (index == null) {
            json = JSONUtil.createObj();
        } else {
            json = findBuffJson(buff);
        }

        json.set("name", buff);
        json.set("value", value);

        JSONArray array = JSONUtil.parseArray(this.buff);

        if (index == null) {
            array.add(json);
        } else {
            array.add(index, json);
        }

        this.buff = array.put(json).toString();
        return this;
    }

    public String findBuff(String buff) {
        JSONArray array = JSONUtil.parseArray(this.buff);
        for (JSONObject next : array.jsonIter()) {
            if (next.get("name").equals(buff)) {
                return next.getStr("value");
            }
        }
        return null;
    }

    public JSONObject findBuffJson(String buff) {
        JSONArray array = JSONUtil.parseArray(this.buff);
        for (JSONObject next : array.jsonIter()) {
            if (next.get("name").equals(buff)) {
                return next;
            }
        }
        return null;
    }

    public Integer findBuffIndex(String buff) {
        JSONArray array = JSONUtil.parseArray(this.buff);
        int index = 0;
        for (JSONObject next : array.jsonIter()) {
            index++;
            if (next.get("name").equals(buff)) {
                return index;
            }
        }
        return null;
    }

}
