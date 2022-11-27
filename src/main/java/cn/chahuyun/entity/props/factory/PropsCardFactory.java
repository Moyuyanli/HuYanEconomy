package cn.chahuyun.entity.props.factory;

import cn.chahuyun.entity.props.PropsCard;
import cn.chahuyun.util.HibernateUtil;

import java.util.Date;

/**
 * @author Erzbir
 * @Date: 2022/11/27 14:15
 */
public class PropsCardFactory implements PropsFactory {
    public static PropsCardFactory INSTANCE = new PropsCardFactory();

    private PropsCardFactory() {

    }

    @Override
    public PropsCard create(String code, String name, int cost, String description, boolean reuse, Date getTime, Date expiredTime, boolean status, boolean operation, Date enabledTime, String aging) {
        PropsCard card = new PropsCard(code, name, cost, description, reuse, getTime, expiredTime, status, operation, enabledTime, aging);
        card.setGetTime(new Date());
        PropsCard finalCard = card;
        //添加道具到数据库
        card = HibernateUtil.factory.fromTransaction(session -> session.merge(finalCard));
        return card;
    }

    @Override
    public PropsCard create() {
        return new PropsCard();
    }
}
