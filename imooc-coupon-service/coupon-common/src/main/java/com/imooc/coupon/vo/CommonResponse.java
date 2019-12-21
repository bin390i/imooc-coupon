package com.imooc.coupon.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.util.Objects;

/**
 * 通用相应对象定义
 */
@Data
@AllArgsConstructor
public class CommonResponse<T> implements Serializable {

    private Integer code;
    private String msg;
    private T data;

   public CommonResponse(Integer code,String msg){
        this.code = code;
        this.msg = msg;
    }
}
