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


    /**
     * 设置或更新指定名称的buff的值
     *
     * @param buffName buff名称
     * @param value    buff的值
     * @return 当前对象实例，支持链式调用
     */
    public UserFactor setBuffValue(String buffName, String value) {
        JSONArray array = JSONUtil.parseArray(this.buff);
        boolean found = false;

        // 尝试找到并更新现有的buff
        for (int i = 0; i < array.size(); i++) {
            JSONObject obj = array.getJSONObject(i);
            if (buffName.equals(obj.getStr("name"))) {
                obj.set("value", value);
                found = true;
                break;
            }
        }

        // 如果没有找到，则添加新的buff
        if (!found) {
            JSONObject newBuff = JSONUtil.createObj()
                    .set("name", buffName)
                    .set("value", value);
            array.add(newBuff);
        }

        this.buff = array.toString();
        return this;
    }

    /**
     * 获取指定名称的buff的值
     *
     * @param buffName buff名称
     * @return buff的值, 如果不存在则返回null
     */
    public String getBuffValue(String buffName) {
        JSONArray array = JSONUtil.parseArray(this.buff);
        for (JSONObject obj : array.jsonIter()) {
            if (buffName.equals(obj.getStr("name"))) {
                return obj.getStr("value");
            }
        }
        return null; // 或者可以返回一个默认值
    }

}
