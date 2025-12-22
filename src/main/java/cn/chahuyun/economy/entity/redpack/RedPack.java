package cn.chahuyun.economy.entity.redpack;

import cn.chahuyun.hibernateplus.HibernateFactory;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.persistence.*;
import lombok.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Entity(name = "RedPack")
@Table(name = "RedPack")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RedPack {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;
    /**
     * 红包名称
     */
    private String name;
    /**
     * 群id
     */
    private Long groupId;
    /**
     * 发送者
     */
    private Long sender;
    /**
     * 金额
     */
    private Double money;
    /**
     * 个数
     */
    private Integer number;
    /**
     * 创建时间
     */
    private Date createTime;
    /**
     * 是否随机
     */
    private boolean isRandomPack;
    /**
     * 已领走的钱数
     */
    @Builder.Default
    private Double takenMoneys = 0.0;
    /**
     * 领取着
     */
    private String receivers;
    /**
     * 随机红包
     */
    private String randomRedPack;

    /**
     * 领取红包的人
     */
    @Transient
    @Builder.Default
    private List<Long> receiverList = new ArrayList<>();

    /**
     * 随机红包
     */
    @Transient
    @Builder.Default
    private List<Double> randomPackList = new ArrayList<>();

    public List<Long> getReceiverList() {
        if (StrUtil.isNotBlank(receivers)) {
            for (String s : receivers.split(",")) {
                receiverList.add(Long.parseLong(s));
            }
        }
        return receiverList;
    }

    public RedPack setReceiverList(List<Long> receiverList) {
        this.receiverList = receiverList;
        this.receivers = receiverList.stream()
                .map(Object::toString)
                .collect(Collectors.joining(","));
        return this;
    }

    public List<Double> getRandomPackList() {
        if (StrUtil.isNotBlank(randomRedPack)) {
            for (String s : randomRedPack.split(",")) {
                randomPackList.add(Double.parseDouble(s));
            }
        }
        return randomPackList;
    }

    public RedPack setRandomPackList(List<Double> randomPackList) {
        this.randomPackList = randomPackList;
        this.randomRedPack = randomPackList.stream()
                .map(it -> Math.round(it * 10.0) / 10.0)
                .map(Object::toString)
                .collect(Collectors.joining(","));
        return this;
    }

    /**
     * 获取随机红包
     *
     * @return 红包金额
     */
    @NotNull
    public Double getRandomPack() {
        if (randomRedPack.isBlank()) {
            throw new RuntimeException("红包已经被领干净了，但仍然在领取!");
        }
        if (getRandomPackList().isEmpty()) {
            throw new RuntimeException("红包已经被领干净了，但仍然在领取!");
        }
        int index = RandomUtil.randomInt(0, randomPackList.size());
        Double v = randomPackList.get(index);
        randomPackList.remove(v);
        setRandomPackList(randomPackList);
        HibernateFactory.merge(this);
        return v;
    }


    public boolean isRandomPack() {
        return isRandomPack;
    }

    public RedPack setRandomPack(boolean randomPack) {
        isRandomPack = randomPack;
        return this;
    }

    @Override
    public String toString() {
        return "RedPack{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", groupId=" + groupId +
                ", sender=" + sender +
                ", money=" + money +
                ", number=" + number +
                ", createTime=" + createTime +
                ", isRandomPack=" + isRandomPack +
                ", takenMoneys=" + takenMoneys +
                ", receivers='" + receivers + '\'' +
                ", randomRedPack='" + randomRedPack + '\'' +
                ", receiverList=" + receiverList +
                ", randomPackList=" + randomPackList +
                '}';
    }
}
