package cn.chahuyun.economy.plugin;

import cn.chahuyun.economy.HuYanEconomy;
import cn.chahuyun.economy.entity.fish.Fish;
import cn.chahuyun.hibernateplus.HibernateFactory;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;

import java.io.InputStream;
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
        List<Fish> fishList = HibernateFactory.selectList(Fish.class);
        if (fishList == null || fishList.isEmpty()) {
            reloadFish();
            return;
        }
        readFish(fishList);
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
        InputStream resourceAsStream = instance.getResourceAsStream("fish.xls");
        Map<String, String> map = new HashMap<>();
        map.put("等级", "level");
        map.put("名称", "name");
        map.put("描述", "description");
        map.put("单价", "price");
        map.put("最小尺寸", "dimensionsMin");
        map.put("最大尺寸", "dimensionsMax");
        map.put("尺寸1阶", "dimensions1");
        map.put("尺寸2阶", "dimensions2");
        map.put("尺寸3阶", "dimensions3");
        map.put("尺寸4阶", "dimensions4");
        map.put("难度", "difficulty");
        map.put("特殊标记", "special");
        ExcelReader reader = ExcelUtil.getReader(resourceAsStream);
        List<Fish> fishList = reader.setHeaderAlias(map).readAll(Fish.class);
        for (Fish fish : fishList) {
            HibernateFactory.merge(fish);
        }
        readFish(fishList);
    }


    private static void readFish(List<Fish> list) {
        for (Fish fish : list) {
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
