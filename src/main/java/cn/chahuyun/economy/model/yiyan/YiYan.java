package cn.chahuyun.economy.model.yiyan;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 一言模型
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class YiYan {

    private Integer id;
    private String hitokoto;
    private String author;
    private String from;

}

