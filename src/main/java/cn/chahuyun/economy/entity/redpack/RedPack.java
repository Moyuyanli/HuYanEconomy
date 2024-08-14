package cn.chahuyun.economy.entity.redpack;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity(name = "RedPack")
@Table(name = "RedPack")
@Getter
@Setter
public class RedPack {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;
    private String name;
    private long groupId;
    private long sender;
    private long money;
    private int number;
    private long createTime;
    private List<Long> receivers = new ArrayList<>();

    public RedPack() {
    }


    public RedPack(String name, long groupId, long sender, Long money, Integer number, Long createTime) {
        this.name = name;
        this.groupId = groupId;
        this.sender = sender;
        this.money = money;
        this.number = number;
        this.createTime = createTime;
    }
}
