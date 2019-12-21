package com.imooc.coupon.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Stream;

@Getter
@AllArgsConstructor
@SuppressWarnings("all")
public enum CouponCategory {

    MANJIAN("满减券","001"),
    ZHEKOU("折扣券", "002"),
    LIJIAN("立减券", "003");

    /**优惠券分类描述*/
    private String description;

    /**优惠分类券码*/
    private String code;

    public static CouponCategory of(String code){
        Objects.requireNonNull(code,"CouponCategory code is null");
        return   Stream.of(values())
                .filter(bean->bean.code.equals(code))
                .findAny()
                .orElseThrow(()->new IllegalArgumentException(code+" not exists!"));
    }

}
