package cn.chahuyun.economy.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 用户属性
 *
 * @author Moyuyanli
 * @date 2024-10-17 10:45
 */
@Entity
@Table(name = "UserProperty")
@Getter
@Setter
public class UserProperty {

    @Id
    private Long id;

}
