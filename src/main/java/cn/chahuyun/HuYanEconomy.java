package cn.chahuyun;

import cn.chahuyun.entity.GoldEconomyCurrency;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.console.plugin.jvm.JavaPlugin;
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescriptionBuilder;
import xyz.cssxsh.mirai.economy.EconomyDsl;
import xyz.cssxsh.mirai.economy.EconomyService;
import xyz.cssxsh.mirai.economy.service.BotEconomyContext;
import xyz.cssxsh.mirai.economy.service.EconomyAccount;
import xyz.cssxsh.mirai.economy.service.IEconomyService;

public final class HuYanEconomy extends JavaPlugin {
    public static final HuYanEconomy INSTANCE = new HuYanEconomy();

    private HuYanEconomy() {
        super(new JvmPluginDescriptionBuilder("cn.chahuyun.HuYanEconomy", "1.0.0")
                .name("HuYanEconomy")
                .info("壶言经济")
                .author("Moyuyanli")
                .build());
    }

    @Override
    public void onEnable() {
        getLogger().info("Plugin loaded!");
        Bot bot = Bot.getInstance(2061954151);
        //创建经济账户实例   后面填啥？
        IEconomyService economyService = EconomyService.Factory.create("xyz.cssxsh.mirai.economy.EconomyService");
        //一种货币  金币
        GoldEconomyCurrency gold = new GoldEconomyCurrency();
        //注册货币
        economyService.register(gold,false);
        //获取一个上下文   是不是约等于  获取一家银行  a
        BotEconomyContext context = economyService.context(bot);
        //获取一个账户
        EconomyAccount account = context.getService().account("t1", "test1");
        //在 a 银行中的 t1 的 金币 余额
        double v = context.get(account, gold);
        //给 a 银行中的 t1 的 金币 +20
        context.plusAssign(account,gold,20);
    }
}