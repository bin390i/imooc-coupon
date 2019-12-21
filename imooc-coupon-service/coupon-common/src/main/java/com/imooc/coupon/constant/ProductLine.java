package com.imooc.coupon.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;
import java.util.stream.Stream;

/**
 * 产品线枚举
 */
@Getter
@AllArgsConstructor
@SuppressWarnings("all")
public enum ProductLine {

    DAMAO("大猫", 1),
    DABAO("大宝", 2);

    /**产品线分类描述*/
    private String description;

    /**产品线分类券码*/
    private Integer code;

    public static ProductLine of(Integer code){

        Objects.requireNonNull(code,"ProductLine code is null");

        return Stream.of(values())
                .filter(bean->bean.code.intValue()==code.intValue())
                .findAny()
                .orElseThrow(()-> new IllegalArgumentException(code + " not exists!"));
    }
}
