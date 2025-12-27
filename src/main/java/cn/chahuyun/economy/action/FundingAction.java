package cn.chahuyun.economy.action;


import cn.chahuyun.authorize.EventComponent;
import cn.chahuyun.authorize.MessageAuthorize;
import cn.chahuyun.authorize.constant.MessageMatchingEnum;
import cn.chahuyun.economy.entity.UserInfo;
import cn.chahuyun.economy.manager.UserCoreManager;
import cn.chahuyun.economy.utils.EconomyUtil;
import cn.chahuyun.hibernateplus.HibernateFactory;
import net.mamoe.mirai.event.events.FriendMessageEvent;
import net.mamoe.mirai.message.data.MessageChain;
import xyz.cssxsh.mirai.economy.EconomyService;
import xyz.cssxsh.mirai.economy.service.EconomyAccount;

import java.util.UUID;

@EventComponent
public class FundingAction {


    @MessageAuthorize(text = "#fund bind \\d+", messageMatching = MessageMatchingEnum.REGULAR)
    public void fundBind(FriendMessageEvent event) {
        MessageChain message = event.getMessage();
        String content = message.contentToString();

        String qqId = content.split(" ")[2];
        UserInfo user = UserCoreManager.getUserInfo(Long.parseLong(qqId));

        if (user == null) {
            event.getSender().sendMessage("未找到该用户");
            return;
        }

        if (user.getFunding() != null) {
            event.getSender().sendMessage("该用户已绑定");
            return;
        }


        String uuid = UUID.randomUUID().toString();
        user.setFunding(uuid);
        HibernateFactory.merge(user);

        event.getSender().sendMessage("fund bind " + qqId + " " + uuid);
    }

    @MessageAuthorize(text = "#fund get \\S+ \\d+", messageMatching = MessageMatchingEnum.REGULAR)
    public void fundGet(FriendMessageEvent event) {
        MessageChain message = event.getMessage();
        String content = message.contentToString();

        String uuid = content.split(" ")[2];
        UserInfo user = UserCoreManager.getUserInfo(uuid);

        if (user == null) {
            event.getSender().sendMessage("未找到该用户");
            return;
        }

        String string = content.split(" ")[3];
        int amount = Integer.parseInt(string);


        EconomyAccount account = EconomyService.INSTANCE.account(user.getId(), null);

        if (EconomyUtil.plusMoneyToBankForAccount(account, -amount)) {
            event.getSender().sendMessage(String.format("fund get %s %d success", uuid, amount));
        } else {
            event.getSender().sendMessage(String.format("fund get %s %d fail", uuid, amount));
        }
    }


}
