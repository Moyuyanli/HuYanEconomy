package cn.chahuyun.economy.plugin;

import cn.chahuyun.economy.constant.PropsKind;
import cn.chahuyun.economy.entity.props.PropsCard;
import cn.chahuyun.economy.prop.PropsManager;
import cn.chahuyun.economy.prop.PropsShop;

/**
 * 道具管理
 *
 * @author Moyuyanli
 * @date 2024/9/14 10:46
 */
public class PluginPropsManager {

    public static void init() {

        PropsManager.registerProp(PropsKind.card, PropsCard.class);

        PropsCard.PropsCardBuilder<?, ?> stack = PropsCard.builder()
                .kind(PropsKind.card)
                .unit("张")
                .canBuy(true)
                .reuse(false)
                .canItExpire(false)
                .stack(true);

        PropsCard signDouble = stack
                .code(PropsCard.SIGN_2)
                .name("签到双倍金币卡")
                .description("\"不要999，不要888，只要99金币，你的下一次签到将翻倍！\"")
                .cost(99).build();
        PropsCard signTriple = stack
                .code(PropsCard.SIGN_3)
                .name("签到三倍金币卡")
                .description("\"不要999，不要888，只要299金币，你的下一次签到将翻三倍！\"")
                .cost(299).build();

        PropsCard signIn = stack
                .code(PropsCard.SIGN_IN)
                .name("补签卡")
                .description("\"花123补签一次你的签到\"")
                .cost(123).build();

        PropsShop.addShop(PropsCard.SIGN_2, signDouble);
        PropsShop.addShop(PropsCard.SIGN_3, signTriple);
        PropsShop.addShop(PropsCard.SIGN_IN, signIn);

    }

}
