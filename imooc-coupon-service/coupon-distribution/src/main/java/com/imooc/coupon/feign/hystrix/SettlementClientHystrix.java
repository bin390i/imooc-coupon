package com.imooc.coupon.feign.hystrix;

import com.imooc.coupon.exception.CouponException;
import com.imooc.coupon.feign.SettlementClient;
import com.imooc.coupon.vo.CommonResponse;
import com.imooc.coupon.vo.SettlementInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * <h1>结算微服务 feign接口 的熔断降级策略</h1>
 */
@Slf4j
@Component
public class SettlementClientHystrix implements SettlementClient {
    /**
     * <h2></h2>
     *
     * @param settlementInfo
     */
    @Override
    public CommonResponse<SettlementInfo> computeRule(SettlementInfo settlementInfo) throws CouponException {
        log.error("[eureka--client-coupon-settlemet] computeRule request error");
        settlementInfo.setEmploy(false);
        settlementInfo.setCoast(-1.0);

        return new CommonResponse<>(-1,
                "[eureka--client-coupon-settlemet] computeRule request error",
                settlementInfo);
    }
}
