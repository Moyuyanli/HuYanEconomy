package cn.chahuyun.economy.power;

import net.mamoe.mirai.event.EventPriority;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 权限管理
 *
 * @author Moyuyanli
 * @date 2023/1/2 20:08
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Power {
    /**
     * 文本
     */
    String[] text();

    /**
     * 匹配方式
     */
    MatchingMenu matching() default MatchingMenu.TEXT;

    /**
     * 优先级
     */
    EventPriority priority() default EventPriority.NORMAL;

    /**
     * 权限
     */
    String[] permissions() default "null";


}
