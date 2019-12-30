package com.imooc.coupon.feign.hystrix;

import com.imooc.coupon.feign.TemplateClient;
import com.imooc.coupon.vo.CommonResponse;
import com.imooc.coupon.vo.CouponTemplateSDK;
import feign.hystrix.FallbackFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class TemplateClientHystrixFallbackFactory implements FallbackFactory<TemplateClient> {

    @Override
    public TemplateClient create(Throwable throwable) {
        log.error("fallback reason:{}",throwable.getMessage());
        return new TemplateClient(){

            /**
             * <h2>查找所有可用的优惠券模板</h2>
             */
            @Override
            public CommonResponse<List<CouponTemplateSDK>> findAllUsableTemplate() {
                log.error("[eureka-client-coupon-template >>] findAllUsableTemplate request error");
                return new CommonResponse<>(-1,
                        "[eureka-client-coupon-template] findAllUsableTemplate request error",
                        Collections.emptyList());
            }

            /**
             * <h2>获取模板 ids 到 CouponTemplateSDK 的映射</h2>
             *
             * @param ids
             */
            @Override
            public CommonResponse<Map<Integer, CouponTemplateSDK>> findIds2TemplateSDK(Collection<Integer> ids) {
                log.error("[eureka-client-coupon-template >>] findIds2TemplateSDK request error");
                return new CommonResponse<>(
                        -1,
                        "[eureka-client-coupon-template] findIds2TemplateSDK request error",
                        Collections.emptyMap()
                );
            }
        };
    }
}
