package cn.chahuyun.economy.utils;

import cn.chahuyun.config.EconomyConfig;
import cn.chahuyun.economy.HuYanEconomy;
import cn.chahuyun.hibernateplus.Configuration;
import cn.chahuyun.hibernateplus.DriveType;
import cn.chahuyun.hibernateplus.HibernatePlusService;

import java.nio.file.Path;

/**
 * 说明
 *
 * @author Moyuyanli
 * @Description :hibernate
 * @Date 2022/7/30 22:47
 */
public class HibernateUtil {


    private HibernateUtil() {

    }

    /**
     * Hibernate初始化
     *
     * @param economy 插件
     * @author Moyuyanli
     * @date 2022/7/30 23:04
     */
    public static void init(HuYanEconomy economy) {
        EconomyConfig config = HuYanEconomy.config;

        Configuration configuration = HibernatePlusService.createConfiguration(economy.getClass());
        configuration.setPackageName("cn.chahuyun.economy.entity");


        DriveType dataType = config.getDataType();
        configuration.setDriveType(dataType);
        Path dataFolderPath = economy.getDataFolderPath();
        switch (dataType) {
            case MYSQL:
                configuration.setAddress(config.getMysqlUrl());
                configuration.setUser(config.getMysqlUser());
                configuration.setPassword(config.getMysqlPassword());
                break;
            case H2:
                configuration.setAddress(dataFolderPath.resolve("HuYanEconomy.h2").toString());
                break;
            case SQLITE:
                configuration.setAddress(dataFolderPath.resolve("HuYanEconomy").toString());
                break;
        }

        HibernatePlusService.loadingService(configuration);
    }


}
