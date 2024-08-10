package cn.chahuyun.economy.plugin;

import cn.chahuyun.HuYanSession;
import cn.chahuyun.config.ConfigData;
import cn.chahuyun.economy.HuYanEconomy;
import cn.chahuyun.economy.constant.Constant;
import cn.chahuyun.economy.entity.props.PropsCard;
import cn.chahuyun.economy.manager.PropsManager;
import cn.chahuyun.economy.manager.PropsManagerImpl;
import cn.chahuyun.economy.manager.TitleManager;
import cn.chahuyun.economy.utils.Log;
import cn.hutool.core.io.FileUtil;
import cn.hutool.cron.CronUtil;
import lombok.Getter;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;

/**
 * 插件管理<p>
 *
 * @author Moyuyanli
 * @date 2022/11/15 16:03
 */
public class PluginManager {

    /**
     * 是否加载壶言会话插件
     */
    public static boolean isHuYanSessionPlugin;

    public static boolean isCustomImage;
    /**
     * 插件的道具管理
     * -- GETTER --
     * 获取道具管理实现
     */
    @Getter
    private static PropsManager propsManager = new PropsManagerImpl();

    private PluginManager() {
    }

    /**
     * 初始化插件道具系统
     *
     * @author Moyuyanli
     * @date 2022/11/23 10:50
     */
    public static void init() {
        //插件加载的时候启动调度器
        CronUtil.start();
        //加载道具
        PropsCard propsCard = new PropsCard(Constant.SIGN_DOUBLE_SINGLE_CARD, "签到双倍金币卡", 99, true, "张", "不要999，不要599，只要99金币，你的下一次签到将翻倍！", false, null, null, false, null);

        propsManager.registerProps(propsCard);
        try {
            //壶言会话
            HuYanEconomy.config.setOwner(Collections.singletonList(ConfigData.INSTANCE.getOwner()));
            Log.info("检测到壶言会话,已同步主人!");
            isHuYanSessionPlugin = true;
        } catch (NoClassDefFoundError e) {
            isHuYanSessionPlugin = false;
        }

        HuYanEconomy instance = HuYanEconomy.INSTANCE;
        Path path = instance.getDataFolderPath();
        File font = new File(path.resolve("font").toUri());
        if (!font.exists()) {
            font.mkdir();
            FileUtil.writeFromStream(instance.getResourceAsStream("Maple UI.ttf"), path.resolve("font/Maple UI.ttf").toFile());
        }
        File bottom = new File(path.resolve("bottom").toUri());
        if (!bottom.exists()) {
            bottom.mkdir();
            FileUtil.writeFromStream(instance.getResourceAsStream("bottom1.png"), path.resolve("bottom/bottom1.png").toFile());
            FileUtil.writeFromStream(instance.getResourceAsStream("bottom2.png"), path.resolve("bottom/bottom2.png").toFile());
            FileUtil.writeFromStream(instance.getResourceAsStream("bottom3.png"), path.resolve("bottom/bottom3.png").toFile());
            FileUtil.writeFromStream(instance.getResourceAsStream("bottom4.png"), path.resolve("bottom/bottom4.png").toFile());
            FileUtil.writeFromStream(instance.getResourceAsStream("bottom5.png"), path.resolve("bottom/bottom5.png").toFile());
            FileUtil.writeFromStream(instance.getResourceAsStream("bottom6.png"), path.resolve("bottom/bottom6.png").toFile());
            FileUtil.writeFromStream(instance.getResourceAsStream("bottom7.png"), path.resolve("bottom/bottom7.png").toFile());
            FileUtil.writeFromStream(instance.getResourceAsStream("bottom8.png"), path.resolve("bottom/bottom8.png").toFile());
        }

        try {
            ImageManager.init(instance);
            isCustomImage = true;
        } catch (IOException e) {
            instance.getLogger().error("自定义图片加载失败!");
            isCustomImage = false;
            throw new RuntimeException(e);
        } catch (FontFormatException e) {
            instance.getLogger().error("自定义字体加载失败!");
            isCustomImage = false;
            throw new RuntimeException(e);
        }

        TitleManager.init();
    }


    /**
     * 设置道具管理的实现
     *
     * @param propsManager 道具管理类
     */
    public static void setPropsManager(PropsManager propsManager) {
        PluginManager.propsManager = propsManager;
    }
}
