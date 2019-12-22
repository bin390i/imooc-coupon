package com.imooc.coupon.advice;

import com.imooc.coupon.annotation.IgnoreResponseAdvice;
import com.imooc.coupon.vo.CommonResponse;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 统一响应
 */
@SuppressWarnings("all")
@RestControllerAdvice
public class CommonResponseDataAdvice implements ResponseBodyAdvice {
    /**
     * 判断是否对相应进行处理
     * @param methodParameter
     * @param aclass
     * @return boolean
     */
    @Override
    public boolean supports(MethodParameter methodParameter, Class aclass) {
        if (methodParameter.getDeclaringClass().isAnnotationPresent(IgnoreResponseAdvice.class)){
            return false;
        }
        if (methodParameter.getMethod().getAnnotation(IgnoreResponseAdvice.class)!=null){
            return false;
        }
        //对响应进行处理，执行beforeBodyWrite
        return true;
    }

    /**
     * 响应返回之前处理
     *
     * @param ob                  the body to be written
     * @param methodParameter     the return type of the controller method
     * @param mediaType   the content type selected through content negotiation
     * @param aclass the converter type selected to write to the response
     * @param serverHttpRequest   the current request
     * @param serverHttpResponse    the current response
     * @return the body that was passed in or a modified (possibly new) instance
     */
    @Override
    public Object beforeBodyWrite(Object ob, MethodParameter methodParameter, MediaType mediaType,
                                  Class aclass, ServerHttpRequest serverHttpRequest, ServerHttpResponse serverHttpResponse) {
        CommonResponse<Object> commonResponse = new CommonResponse<>(0,"");
        //如果 ob 是空，响应不需要设置 data
        if (ob==null){
          return   commonResponse;
        }else if (ob instanceof CommonResponse){ //如果 ob 已经是 CommonResponse, 不需要再次处理
            commonResponse = (CommonResponse<Object>) ob;
        }else{//否则, 把响应对象作为 CommonResponse 的 data 部分
            commonResponse.setData(ob);
        }
        return commonResponse;
    }
}
