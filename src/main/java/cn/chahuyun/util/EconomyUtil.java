package cn.chahuyun.util;

import cn.chahuyun.constant.Constant;
import net.mamoe.mirai.contact.User;
import xyz.cssxsh.mirai.economy.EconomyService;
import xyz.cssxsh.mirai.economy.service.*;

import static cn.chahuyun.HuYanEconomy.bot;

/**
 * 经济工具<p>
 * 使用前请先初始化 调用 [init] 方法<p>
 *
 * @author Moyuyanli
 * @date 2022/11/14 12:37
 */
public class EconomyUtil {

    //获取经济账户实例
    public static final IEconomyService economyService = EconomyService.INSTANCE;

    private EconomyUtil() {

    }

    /**
     * 初始化经济<p>
     *
     * @param
     * @return void
     * @author Moyuyanli
     * @date 2022/11/14 15:29
     */
    public static void init() {
        init(Constant.GOLD);
    }

    /**
     * 初始化经济<p>
     * 自定义加载货币
     *
     * @param currencies 货币集
     * @author Moyuyanli
     * @date 2022/11/14 15:28
     */
    public static void init(EconomyCurrency... currencies) {
        //注册货币
        try {
            for (EconomyCurrency currency : currencies) {
                economyService.register(currency, false);
            }
            Log.info("经济体系初始化成功!");
        } catch (UnsupportedOperationException e) {
            Log.error("经济体系初始化失败!", e);
        }
    }


    /**
     * 从用户 [钱包] 获取余额<p>
     * 默认货币 [金币] <p>
     *
     * @param user 用户
     * @return int 余额
     * @author Moyuyanli
     * @date 2022/11/14 12:39
     */
    public static double getMoneyByUser(User user) {
        return getMoneyByUser(user, Constant.GOLD);
    }


    /**
     * 从用户 [钱包] 获取 [货币] 余额<p>
     *
     * @param user     用户
     * @param currency 货币
     * @return int 余额
     * @author Moyuyanli
     * @date 2022/11/14 14:47
     */
    public static double getMoneyByUser(User user, EconomyCurrency currency) {
        //获取一个bot上下文
        try (BotEconomyContext context = economyService.context(bot)) {
            //在bot上下文里面找到这个用户
            UserEconomyAccount account = context.getService().account(user);
            //返回这个用户在bot上下文的某种货币的余额
            return context.get(account, currency);
        } catch (Exception e) {
            Log.error("经济获取出错:获取用户钱包余额");
        }
        return 0;
    }

    /**
     * 从 [银行] 获取余额<p>
     * 默认货币 [金币] <p>
     *
     * @param user 用户
     * @return int 余额
     * @author Moyuyanli
     * @date 2022/11/14 15:11
     */
    public static double getMoneyByBank(User user) {
        return getMoneyByBank(user, Constant.GOLD);
    }

    /**
     * 从 [银行] 获取 [货币] 余额<p>
     *
     * @param user     用户
     * @param currency 货币
     * @return int 余额
     * @author Moyuyanli
     * @date 2022/11/14 15:14
     */
    public static double getMoneyByBank(User user, EconomyCurrency currency) {
        try (GlobalEconomyContext global = economyService.global()) {
            UserEconomyAccount account = global.getService().account(user);
            return global.get(account, currency);
        } catch (Exception e) {
            Log.error("经济获取出错:获取用户银行余额");
            return 0;
        }
    }

    /**
     * 转账<p>
     * 用户 [钱包] 到 用户 [钱包]<p>
     * 默认货币 [金币] <p>
     *
     * @param user   转移前用户
     * @param toUser 转移后用户
     * @return boolean [true] 转移成功 [false] 转移失败
     * @author Moyuyanli
     * @date 2022/11/14 15:39
     */
    public static boolean turnUserToUser(User user, User toUser, double quantity) {
        return turnUserToUser(user, toUser, quantity, Constant.GOLD);
    }

    /**
     * 转账<p>
     * 用户 [钱包] 到 用户 [钱包]<p>
     * 自定义货币 <p>
     *
     * @param user     转移前用户
     * @param toUser   转移后用户
     * @param currency 货币
     * @return boolean [true] 转移成功 [false] 转移失败
     * @author Moyuyanli
     * @date 2022/11/14 15:39
     */
    public static boolean turnUserToUser(User user, User toUser, double quantity, EconomyCurrency currency) {
        try (BotEconomyContext context = economyService.context(bot)) {
            UserEconomyAccount account = context.getService().account(user);
            UserEconomyAccount toAccount = context.getService().account(toUser);
            double userMoney = context.get(account, currency);
            if (userMoney - quantity < 0) {
                return false;
            }
            context.minusAssign(account, currency, quantity);
            context.plusAssign(toAccount, currency, quantity);
            return true;
        } catch (Exception e) {
            Log.error("经济转移出错:用户->用户");
            return false;
        }
    }

    /**
     * 转账<p>
     * 从 用户 [钱包] 到 [银行]<p>
     * 默认货币 [金币] <p>
     *
     * @param user     用户
     * @param quantity 转移数量
     * @return boolean [true] 转移成功 [false] 转移失败
     * @author Moyuyanli
     * @date 2022/11/14 15:53
     */
    public static boolean turnUserToBank(User user, double quantity) {
        return turnUserToBank(user, quantity, Constant.GOLD);
    }

    /**
     * 转账<p>
     * 从 用户 [钱包] 到 [银行]<p>
     * 自定义货币<p>
     *
     * @param user     用户
     * @param quantity 转移数量
     * @param currency 货币
     * @return boolean [true] 转移成功 [false] 转移失败
     * @author Moyuyanli
     * @date 2022/11/14 15:53
     */
    public static boolean turnUserToBank(User user, double quantity, EconomyCurrency currency) {
        try (BotEconomyContext context = economyService.context(bot); GlobalEconomyContext global = economyService.global()) {
            UserEconomyAccount account = context.getService().account(user);
            double money = context.get(account, currency);
            if (money - quantity < 0) {
                return false;
            }
            context.minusAssign(account, currency, quantity);
            UserEconomyAccount backAccount = global.getService().account(user);
            global.plusAssign(backAccount, currency, quantity);
            return true;
        } catch (Exception e) {
            Log.error("经济转移出错:用户->银行");
            return false;
        }
    }

    /**
     * 转账<p>
     * 从 [银行] 到 用户 [钱包]<p>
     * 默认货币 [金币] <p>
     *
     * @param user     用户
     * @param quantity 转移数量
     * @return boolean [true] 转移成功 [false] 转移失败
     * @author Moyuyanli
     * @date 2022/11/14 16:04
     */
    public static boolean turnBankToUser(User user, double quantity) {
        return turnBankToUser(user, quantity, Constant.GOLD);
    }

    /**
     * 转账<p>
     * 从 [银行] 到 用户 [钱包]<p>
     * 自定义货币<p>
     *
     * @param user     用户
     * @param quantity 转移数量
     * @param currency 货币
     * @return boolean [true] 转移成功 [false] 转移失败
     * @author Moyuyanli
     * @date 2022/11/14 16:06
     */
    public static boolean turnBankToUser(User user, double quantity, EconomyCurrency currency) {
        try (GlobalEconomyContext global = economyService.global(); BotEconomyContext context = economyService.context(bot);) {
            UserEconomyAccount backAccount = global.getService().account(user);
            double bankMoney = global.get(backAccount, currency);
            if (bankMoney - quantity < 0) {
                return false;
            }
            global.minusAssign(backAccount, currency, quantity);
            UserEconomyAccount account = context.getService().account(user);
            context.plusAssign(account, currency, quantity);
            return true;
        } catch (Exception e) {
            Log.error("经济转移出错:银行->用户");
            return false;
        }
    }

}
