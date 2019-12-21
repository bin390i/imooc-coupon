package com.imooc.coupon.filter;

import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import sun.rmi.runtime.Log;

import javax.servlet.http.HttpServletRequest;

/**
 * 限流过滤器
 */
@Component
@Slf4j
@SuppressWarnings("all")
public class RateLimiterFilter extends AbstractPreZuulFilter {
    // RateLimiter 谷歌提供的基于令牌桶的限流器
    //传递2.0，表示每秒可以获取到2个令牌
    RateLimiter rateLimiter = RateLimiter.create(2.0);

    @Override
    protected Object cRun() {
        HttpServletRequest request = context.getRequest();
        if (rateLimiter.tryAcquire()) {
            log.info("get rote token success");
            return success();
        } else {
            log.error("rote limit: {}", request.getRequestURI());
            return fail(402, "rote limit");
        }
    }

    /**
     * filterOrder() must also be defined for a filter. Filters may have the same  filterOrder if precedence is not
     * important for a filter. filterOrders do not need to be sequential.
     *
     * @return the int order of a filter
     */
    @Override
    public int filterOrder() {
        return 2;
    }
}
