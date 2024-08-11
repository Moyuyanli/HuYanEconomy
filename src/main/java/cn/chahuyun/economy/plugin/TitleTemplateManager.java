package cn.chahuyun.economy.plugin;

import cn.chahuyun.economy.entity.TitleInfo;
import cn.chahuyun.economy.entity.UserInfo;
import cn.chahuyun.economy.entity.title.TitleTemplate;
import cn.chahuyun.economy.manager.TitleManager;
import cn.hutool.core.date.DateUtil;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Moyuyanli
 * @Date 2024/8/11 11:35
 */
public final class TitleTemplateManager {

    /**
     * 称号模板map
     */
    private static final Map<String, TitleTemplate> titleTemplateMap = new HashMap<>(3);

    /**
     * 注册一个称号模板。
     * <p>称号模板需要继承 {@link TitleTemplate} ，并且使用方法也在其中！</p>
     * <p>使用案例请看 {@link  TitleManager#init()}</p>
     *
     * @param template 要注册的称号模板。
     * @param <T>      称号模板的具体类型，必须是 {@link TitleTemplate} 的子类。
     * @return 如果注册成功返回 true，否则返回 false（例如，如果模板代码已经存在）。
     */
    public static <T extends TitleTemplate> boolean registerTitleTemplate(T template) {
        String titleCode = template.getTemplateCode();
        if (titleTemplateMap.containsKey(titleCode)) {
            return false;
        }
        titleTemplateMap.put(titleCode, template);
        return true;
    }

    /**
     * 批量注册多个称号模板。
     *
     * <p>此方法会遍历给定的模板数组，并对每个模板调用 {@link #registerTitleTemplate(T)} 方法。</p>
     * <p>如果有重复的模板代码，重复的模板将不会被注册。</p>
     *
     * @param template 称号模板数组，每个元素必须是 {@link TitleTemplate} 的子类。
     * @param <T>      称号模板的具体类型，必须是 {@link TitleTemplate} 的子类。
     */
    @SafeVarargs
    public static <T extends TitleTemplate> void registerTitleTemplate(T... template) {
        for (T t : template) {
            registerTitleTemplate(t);
        }
    }

    /**
     * 根据称号模版code创建一个称号，并绑定到对应的用户上。<br>
     *
     * @param templateCode 称号code
     * @param userInfo     用户信息
     * @return 生成的称号, code不存在时为null
     */
    public static TitleInfo createTitle(String templateCode, UserInfo userInfo) {
        if (!titleTemplateMap.containsKey(templateCode)) {
            return null;
        }
        TitleTemplate template = titleTemplateMap.get(templateCode);
        Date validityPeriod = null;
        if (template.getValidityPeriod() > 0) {
            validityPeriod = DateUtil.offsetDay(new Date(), template.getValidityPeriod());
        }
        return template.createTitleInfo(userInfo).setCode(template.getTemplateCode()).setDueTime(validityPeriod);
    }

    /**
     * 获取所有可以购买的称号。
     *
     * @return 能购买的称号模板
     */
    public static List<TitleTemplate> getCanBuyTemplate() {
        return titleTemplateMap.values().stream().filter(TitleTemplate::getCanIBuy).collect(Collectors.toList());
    }

}

