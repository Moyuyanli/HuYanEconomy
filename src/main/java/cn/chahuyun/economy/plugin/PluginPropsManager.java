package cn.chahuyun.economy.plugin;

import cn.chahuyun.economy.constant.PropsKind;
import cn.chahuyun.economy.entity.fish.FishBait;
import cn.chahuyun.economy.entity.props.FunctionProps;
import cn.chahuyun.economy.entity.props.PropsCard;
import cn.chahuyun.economy.entity.props.PropsData;
import cn.chahuyun.economy.prop.PropBase;
import cn.chahuyun.economy.prop.PropsManager;
import cn.chahuyun.economy.prop.PropsShop;
import cn.chahuyun.economy.utils.Log;
import cn.chahuyun.hibernateplus.HibernateFactory;
import cn.hutool.core.date.DateUtil;
import cn.hutool.cron.CronUtil;
import cn.hutool.cron.task.Task;

import java.util.Date;
import java.util.List;

/**
 * 道具管理
 *
 * @author Moyuyanli
 * @date 2024/9/14 10:46
 */
public class PluginPropsManager {

    public static void init() {

        if (PropsManager.registerProp(PropsKind.card, PropsCard.class)) {
            Log.debug("card 注册成功!");
        }
        if (PropsManager.registerProp(PropsKind.functionProp, FunctionProps.class)) {
            Log.debug("functionProp 注册成功!");
        }
        if (PropsManager.registerProp(PropsKind.fishBait, FishBait.class)) {
            Log.debug("fishBait 注册成功!");
        }


        PropsCard.PropsCardBuilder<?, ?> stack = PropsCard.builder()
                .kind(PropsKind.card)
                .unit("张")
                .canBuy(true)
                .reuse(false)
                .canItExpire(false)
                .stack(false);

        PropsCard signDouble = stack
                .code(PropsCard.SIGN_2)
                .name("签到双倍金币卡")
                .description("\"不要999，不要888，只要88金币，你的下一次签到将翻倍！\"")
                .cost(88).build();
        PropsCard signTriple = stack
                .code(PropsCard.SIGN_3)
                .name("签到三倍金币卡")
                .description("\"不要999，不要888，只要188金币，你的下一次签到将翻三倍！\"")
                .cost(188).build();

        PropsCard signIn = stack
                .code(PropsCard.SIGN_IN)
                .name("补签卡")
                .description("\"花123补签一次你的签到\"")
                .cost(123).build();

        PropsCard health = stack
                .code(PropsCard.HEALTH)
                .name("医保卡")
                .description("少年，你还在为付不起医药费而发愁吗？？？")
                .cost(5888)
                .build();

        PropsCard.PropsCardBuilder<?, ?> expireStack = PropsCard.builder()
                .kind(PropsKind.card)
                .unit("张")
                .canBuy(true)
                .reuse(false)
                .canItExpire(true)
                .expire(30)
                .stack(false);

        PropsCard monthly = expireStack
                .code(PropsCard.MONTHLY)
                .name("签到月卡")
                .description("持续一个月的5倍经济，无法与签到卡同时生效!")
                .cost(9999)
                .build();


        PropsShop.addShop(PropsCard.SIGN_2, signDouble);
        PropsShop.addShop(PropsCard.SIGN_3, signTriple);
        PropsShop.addShop(PropsCard.SIGN_IN, signIn);
        PropsShop.addShop(PropsCard.HEALTH, health);

        PropsShop.addShop(PropsCard.MONTHLY, monthly);


        FunctionProps baton = FunctionProps.builder()
                .kind(PropsKind.functionProp)
                .unit("个")
                .canBuy(true)
                .reuse(true)
                .canItExpire(false)
                .stack(false)
                .code(FunctionProps.ELECTRIC_BATON)
                .name("便携电棍")
                .description("用于防身,或许有其他用途?")
                .cost(1888)
                .electricity(100).build();

        PropsShop.addShop(FunctionProps.ELECTRIC_BATON, baton);


        FishBait.FishBaitBuilder<?, ?> fishBait = FishBait.builder()
                .kind(PropsKind.fishBait)
                .unit("包")
                .canBuy(true)
                .reuse(true);

        FishBait bait_1 = fishBait.code(FishBait.BAIT_1)
                .num(25)
                .level(1)
                .quality(0.08f)
                .name("基础鱼饵")
                .cost(66)
                .description("最基础的鱼饵，量大管饱(鱼管饱)").build();
        FishBait bait_2 = fishBait.code(FishBait.BAIT_2)
                .num(20)
                .level(2)
                .quality(0.15f)
                .name("中级鱼饵")
                .cost(269)
                .description("中级鱼饵，闻着就有一股香味。").build();
        FishBait bait_3 = fishBait.code(FishBait.BAIT_3)
                .num(15)
                .level(3)
                .quality(0.25f)
                .name("高级鱼饵")
                .cost(588)
                .description("除了贵，全是优点").build();
        FishBait bait_l_1 = fishBait.code(FishBait.BAIT_L_1)
                .num(18)
                .level(4)
                .quality(0.05f)
                .name("特化型(香味)鱼饵")
                .cost(450)
                .description("袋子都封不住他的气味，看来传播性很好！").build();
        FishBait bait_q_1 = fishBait.code(FishBait.BAIT_Q_1)
                .num(18)
                .level(2)
                .quality(0.30f)
                .name("特化型(口味)鱼饵")
                .cost(350)
                .description("我家鱼吃了都说好吃！").build();


        PropsShop.addShop(FishBait.BAIT_1, bait_1);
        PropsShop.addShop(FishBait.BAIT_2, bait_2);
        PropsShop.addShop(FishBait.BAIT_3, bait_3);
        PropsShop.addShop(FishBait.BAIT_L_1, bait_l_1);
        PropsShop.addShop(FishBait.BAIT_Q_1, bait_q_1);


        CronUtil.schedule("0 0 4 * * ?", new PropExpireCheckTask());
    }

}


class PropExpireCheckTask implements Task {

    /**
     * 执行作业
     * <p>
     * 作业的具体实现需考虑异常情况，默认情况下任务异常在监听中统一监听处理，如果不加入监听，异常会被忽略<br>
     * 因此最好自行捕获异常后处理
     */
    @Override
    public void execute() {
        List<PropsData> collect = HibernateFactory.selectList(PropsData.class);

        for (PropsData data : collect) {
            PropBase base = PropsManager.deserialization(data, PropBase.class);
            if (!base.isCanItExpire()) {
                continue;
            }
            if (DateUtil.isSameDay(new Date(), base.getExpiredTime())) {
                PropsManager.destroyProsInBackpack(data);
            }
        }

    }
}