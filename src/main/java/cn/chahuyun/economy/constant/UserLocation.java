package cn.chahuyun.economy.constant;

/**
 * @author Moyuyanli
 * @date 2024/9/5 16:49
 */
public enum UserLocation {

    HOME("家"),
    HOSPITAL("医院"),
    PRISON("监狱"),
    FISH_POND("鱼塘"),
    FACTORY("工厂"),
    OTHER1("其他1"),
    OTHER2("其他2"),
    OTHER3("其他3"),
    OTHER4("其他4"),
    OTHER5("其他5"),
    OTHER6("其他6"),
    ;


    private final String name;

    UserLocation(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
