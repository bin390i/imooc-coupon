package com.imooc.coupon.executor;

import com.alibaba.fastjson.JSON;
import com.imooc.coupon.vo.GoodsInfo;
import com.imooc.coupon.vo.SettlementInfo;
import org.apache.commons.collections4.CollectionUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <h1>规则执行器抽象类, 定义通用方法</h1>
 */
public abstract class AbstractExecutor {

    /**
     * <h2>校验商品类型与优惠券是否匹配</h2>
     * 需要注意:
     * 1. 这里实现的单品类优惠券的校验, 多品类优惠券重载此方法
     * 2. 商品只需要有一个优惠券要求的商品类型去匹配就可以
     * @param settlement {@link SettlementInfo}
     * @return true or false
     * */
    @SuppressWarnings("all")
    protected boolean isGoodsTypeSatisfy(SettlementInfo settlement){

        List<Integer> templateGoodsType = JSON.parseObject(settlement.getCouponAndTemplateInfos().get(0)
        .getTemplate().getRule().getUsage().getGoodsType(),List.class);

        List<Integer> goodsType = settlement.getGoodsInfos().stream()
                .map(GoodsInfo::getType)
                .collect(Collectors.toList());

        return CollectionUtils.isNotEmpty(
                CollectionUtils.intersection(templateGoodsType,goodsType) //取交集
        );
    }

    /**
     * <h2>处理商品类型与优惠券限制不匹配的情况</h2>
     * @param settlement {@link SettlementInfo} 用户传递的结算信息
     * @param goodsSum 商品总价(原价)
     * @return {@link SettlementInfo} 已经修改过的结算信息
     * */
    protected SettlementInfo processGoodsTypeNotSatisfy(SettlementInfo settlement, double goodsSum){
        boolean isGoodsTypeSatisfy = isGoodsTypeSatisfy(settlement);
        // 当商品类型不满足时, 直接返回总价, 并清空优惠券
        if (!isGoodsTypeSatisfy){
            settlement.setCost(goodsSum);
            settlement.setCouponAndTemplateInfos(Collections.emptyList());
            return settlement;
        }
        return null;
    }

    /**
     * <h1>商品总价</h1>
     * @param goodsInfos
     * @return goodsSum
     */
    protected double goodsCostSum(List<GoodsInfo> goodsInfos){
        return goodsInfos.stream()
                .mapToDouble(g->g.getPrice() * g.getCount())
                .sum();
    }

    /**
     * <h1>返回最小支付费用</h1>
     * @return 0.1
     */
    protected double minCost(){
        return 0.1;
    }

    /**
     * <h1>保留2位小数</h1>
     * @param value
     * @return 0.00
     */
    protected double retain2Decimals(double value){
        return new BigDecimal(value).setScale(2,BigDecimal.ROUND_HALF_UP).doubleValue();
    }
}
