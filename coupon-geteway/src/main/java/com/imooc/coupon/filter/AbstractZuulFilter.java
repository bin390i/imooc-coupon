package com.imooc.coupon.filter;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;

/**
 * 通用的抽象过滤器类
 */
@SuppressWarnings("all")
public abstract class AbstractZuulFilter extends ZuulFilter {

    RequestContext context;

    private static final String NEXT="next";
    /**
     * a "true" return from this method means that the run() method should be invoked
     *
     * @return true if the run() method should be invoked. false will not invoke the run() method
     */
    @Override
    public boolean shouldFilter() {
        context = RequestContext.getCurrentContext();
        return (boolean) context.getOrDefault(NEXT,true);
    }

    /**
     * if shouldFilter() is true, this method will be invoked. this method is the core method of a ZuulFilter
     *
     * @return Some arbitrary artifact may be returned. Current implementation ignores it.
     * @throws ZuulException if an error occurs during execution.
     */
    @Override
    public Object run() throws ZuulException {
        context = RequestContext.getCurrentContext();
        return cRun();
    }

    protected abstract Object cRun();

    Object fail(int code, String msg) {
        context.set(NEXT, false);
        context.setSendZuulResponse(false);
        context.getResponse().setContentType("text/html;charset=UTF-8");
        context.setResponseStatusCode(code);
        context.setResponseBody(String.format("{result:%s!}",msg));
        //context.setResponseBody(String.format("{\"result\": \"%s!\"}", msg));
        return null;
    }

    Object success() {
        context.set(NEXT, true);
        return null;
    }
}
