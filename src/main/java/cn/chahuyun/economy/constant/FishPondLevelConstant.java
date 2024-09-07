package cn.chahuyun.economy.constant;

/**
 * @author Moyuyanli
 * @Date 2024/9/7 18:50
 */
public enum FishPondLevelConstant {

    LV_2(5000, 0),
    LV_3(10000, 0),
    LV_4(15000, 0),
    LV_5(20000, 0),
    LV_6(25000, 0),
    LV_7(30000, 0),
    LV_8(50000, 10),
    LV_9(70000, 0),
    LV_10(10000, 0);

    /**
     * 升级金额
     */
    private final int amount;

    /**
     * 最低鱼竿等级
     */
    private final int minFishLevel;

    FishPondLevelConstant(int amount, int minFishLevel) {
        this.amount = amount;
        this.minFishLevel = minFishLevel;
    }

    public int getAmount() {
        return amount;
    }

    public int getMinFishLevel() {
        return minFishLevel;
    }
}
