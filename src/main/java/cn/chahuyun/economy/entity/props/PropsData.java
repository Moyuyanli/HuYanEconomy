package cn.chahuyun.economy.entity.props;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 物品数据化
 * <p>
 * 这是需要存数据的
 *
 * @author Moyuyanli
 * @date 2024/9/13 16:05
 */
@Getter
@Setter
@Table
@Entity(name = "PropsData")
public class PropsData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String kind;

    private String code;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String data;


}
