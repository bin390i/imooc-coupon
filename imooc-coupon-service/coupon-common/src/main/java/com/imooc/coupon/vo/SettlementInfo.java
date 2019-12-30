package com.imooc.coupon.vo;

import com.sun.org.apache.xpath.internal.operations.Bool;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SettlementInfo {
    private Long userId;
    private List<GoodsInfo> goodsInfos;
    private List<SettlementInfo.CouponAndTemplateInfo> CouponAndTemplateInfos;

    /*是否使用当前优惠券*/
    private Boolean employ;

    /*折扣后的价格*/
    private Double cost;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
   public static class CouponAndTemplateInfo{
        /** coupon id*/
        private Integer id;

        /** CouponTemplateSDK */
        private CouponTemplateSDK template;
    }


}
