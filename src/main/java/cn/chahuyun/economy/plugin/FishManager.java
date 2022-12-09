package cn.chahuyun.economy.plugin;

import cn.chahuyun.economy.HuYanEconomy;
import cn.chahuyun.economy.entity.fish.Fish;
import cn.chahuyun.economy.util.HibernateUtil;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 鱼管理
 *
 * @author Moyuyanli
 * @date 2022/12/9 14:53
 */
public class FishManager {

    /**
     * 归类整理的鱼
     */
    private static final Map<Integer, List<Fish>> fishMap = new HashMap<>();

    private FishManager() {
    }

    /**
     * 初始化鱼管理
     */
    public static void init() {
        List<Fish> fishList = HibernateUtil.factory.fromSession(session -> {
            HibernateCriteriaBuilder builder = session.getCriteriaBuilder();
            JpaCriteriaQuery<Fish> query = builder.createQuery(Fish.class);
            query.select(query.from(Fish.class));
            return session.createQuery(query).list();
        });

        if (fishList == null || fishList.size() == 0) {
            reloadFish();
            return;
        }
        for (Fish fish : fishList) {
            int level = fish.getLevel();
            if (fishMap.containsKey(level)) {
                fishMap.get(level).add(fish);
            } else {
                fishMap.put(level, new ArrayList<>() {{
                    add(fish);
                }});
            }
        }
    }

    /**
     * 获取对应的鱼等级
     *
     * @param fishLevel 鱼的等级
     * @return 对应等级的鱼集合
     * @author Moyuyanli
     * @date 2022/12/9 15:49
     */
    public static List<Fish> getLevelFishList(int fishLevel) {
        return fishMap.get(fishLevel);
    }

    /**
     * 从resources里面读取鱼数据
     */
    private static void reloadFish() {
        HuYanEconomy instance = HuYanEconomy.INSTANCE;
        ExcelReader reader = ExcelUtil.getReader(instance.getResourceAsStream("fish.excel"));
        Map<String, String> map = new HashMap<>();
        map.put("level", "等级");
        map.put("name", "名称");
        map.put("description", "描述");
        map.put("price", "单价");
        map.put("dimensionsMin", "最小尺寸");
        map.put("dimensionsMax", "最大尺寸");
        map.put("difficulty", "难度");
        map.put("special", "特殊标记");
        List<Fish> fishList = reader.setHeaderAlias(map).readAll(Fish.class);
        for (Fish fish : fishList) {
            HibernateUtil.factory.fromTransaction(session -> session.merge(fish));
        }
        for (Fish fish : fishList) {
            int level = fish.getLevel();
            if (fishMap.containsKey(level)) {
                fishMap.get(level).add(fish);
            } else {
                fishMap.put(level, new ArrayList<>() {{
                    add(fish);
                }});
            }
        }
    }
}
