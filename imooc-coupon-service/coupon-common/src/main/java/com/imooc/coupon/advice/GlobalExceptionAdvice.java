package com.imooc.coupon.advice;

import com.imooc.coupon.exception.CouponException;
import com.imooc.coupon.vo.CommonResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;

/**
 * 全局异常处理
 * spring 3.2中，新增了@ControllerAdvice 注解，可以用于定义@ExceptionHandler、@InitBinder、@ModelAttribute，
 * 并应用到所有@RequestMapping中
 */
@RestControllerAdvice
public class GlobalExceptionAdvice {

    /**
     * 统一对优惠券异常进行处理
     * ExceptionHandler  指定对哪个异常进行拦截并处理
     */
    @ExceptionHandler(value = CouponException.class)  //ExceptionHandler指定对哪个异常进行处理
    public CommonResponse<String> handlerCouponException(HttpServletRequest req , CouponException ex){
        CommonResponse<String> commonResponse = new CommonResponse<>(-1,"business error");
        commonResponse.setData(ex.getMessage());
        return commonResponse;
    }
}
