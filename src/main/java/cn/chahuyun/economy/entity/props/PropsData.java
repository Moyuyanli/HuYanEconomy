package cn.chahuyun.economy.entity.props;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.Date;

/**
 * 道具数据实体 (优化重构版)
 * 采用“混合存储”模式：核心字段提取为列，扩展数据保留 JSON
 * 
 * @author Moyuyanli
 */
@Getter
@Setter
@Table
@Entity(name = "PropsData")
public class PropsData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 道具大类
     */
    private String kind;

    /**
     * 道具编码
     */
    private String code;

    /**
     * 数量 (提取为列以优化查询)
     */
    @Column(columnDefinition = "integer default 1")
    private Integer num = 1;

    /**
     * 过期时间 (提取为列，支持索引查询)
     */
    private Date expiredTime;

    /**
     * 状态 (提取为列，例如：已激活/已穿戴)
     */
    @Column(columnDefinition = "boolean default false")
    private Boolean status = false;

    /**
     * 动态扩展数据 (JSON 存储)
     */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String data;
}
