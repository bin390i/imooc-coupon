package com.imooc.coupon.service.impl;

import com.alibaba.fastjson.JSON;
import com.imooc.coupon.constant.Constant;
import com.imooc.coupon.constant.CouponStatus;
import com.imooc.coupon.dao.CouponDao;
import com.imooc.coupon.entity.Coupon;
import com.imooc.coupon.exception.CouponException;
import com.imooc.coupon.feign.SettlementClient;
import com.imooc.coupon.feign.TemplateClient;
import com.imooc.coupon.service.IRedisService;
import com.imooc.coupon.service.IUserService;
import com.imooc.coupon.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <h1>用户服务相关的实现</h1>
 * 所有的操作过程和状态都保存在redis中,并通过kafka将消息传递到mysql中
 * 问题:
 * 1. 为什么使用 kafka 而不是使用springBoot 中的异步处理?
 * 答: 因为异步任务可能会失败, 即使传递到kafak中的消息消费失败,也可以重新从kafka中获取消息去回溯记录,
 *      保证cache与存储的一致性
 */
@Slf4j
@Service
public class UserServiceImpl implements IUserService {


    /** Coupon Dao*/
    private final CouponDao couponDao;

    /** Redis 服务*/
    private final IRedisService redisService;

    /** 模板微服务客户端*/
    private final TemplateClient templateClient;

    /** 结算微服务客户端*/
    private final SettlementClient settlementClient;

    /** Kafka 客户端*/
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    public UserServiceImpl(CouponDao couponDao, IRedisService redisService, TemplateClient templateClient, SettlementClient settlementClient, KafkaTemplate<String, String> kafkaTemplate) {
        this.couponDao = couponDao;
        this.redisService = redisService;
        this.templateClient = templateClient;
        this.settlementClient = settlementClient;
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * <h2>根据用户 id 和状态查询优惠券记录</h2>
     *
     * @param userId 用户 id
     * @param status 优惠券状态
     * @return {@link Coupon}s
     */
    @Override
    public List<Coupon> findCouponsByStatus(Long userId, Integer status) throws CouponException {
        List<Coupon> cachedCoupons = redisService.getCachedCoupons(userId, status);
        List<Coupon> preTarget;
        if (CollectionUtils.isNotEmpty(cachedCoupons) &&
            CollectionUtils.isNotEmpty(
                    cachedCoupons.stream()
                            .filter(c->c.getId()!=-1)
                            .collect(Collectors.toList()))
        ){
            preTarget = cachedCoupons.stream()
                    .filter(c->c.getId()!=-1)
                    .collect(Collectors.toList());
            log.debug("coupon cache is not empty:{} {} {}",userId,status,JSON.toJSONString(preTarget));
        }else {
            log.debug("coupon cache is empty, get coupon from db:{} {}",userId,status);
            List<Coupon> dbCoupon = couponDao.findAllByUserIdAndStatus(userId, CouponStatus.of(status));
            if (CollectionUtils.isEmpty(dbCoupon)){
                log.debug("current user not have coupon in db:{}, {}",userId,status);
                return dbCoupon;
            }
            //填充 dbCoupons 到 templateSdk 字段
            Map<Integer, CouponTemplateSDK> id2TemplateSDK = templateClient.findIds2TemplateSDK(
                    dbCoupon.stream()
                            .map(Coupon::getId)
                            .collect(Collectors.toList())
            ).getData();
            dbCoupon.forEach(c->{
                c.setTemplateSDK(id2TemplateSDK.get(c.getTemplateId()));
            });
            //数据库中存在记录
            preTarget =dbCoupon;
            //将记录写入cache
            redisService.addCouponToCache(userId,preTarget,status);
            //将无效优惠券剔除
            preTarget = preTarget.stream()
                    .filter(pt->pt.getId()!=-1)
                    .collect(Collectors.toList());
            //如果当前获取的是可用优惠券,还需对已过期的优惠券做延迟处理
           if (CouponStatus.of(status).equals(CouponStatus.USABLE)){
               CouponClassify couponClassify = CouponClassify.classify(preTarget);
               //如果已过期状态不为空,做延迟处理
               if (CollectionUtils.isNotEmpty(couponClassify.getExpired())){
                   log.info("add expired coupon to cache from  findCouponsByStatus: {} {}",userId,status);
                   redisService.addCouponToCache(userId,couponClassify.getExpired(),CouponStatus.EXPIRED.getCode());
                   //发送到 kafka 做异步处理 ,更改db
                   kafkaTemplate.send(Constant.Kafka.TOPIC,
                           JSON.toJSONString(new CouponKafkaMessage(
                                   CouponStatus.EXPIRED.getCode(),
                                   couponClassify.getExpired().stream()
                                   .map(Coupon::getId)
                                   .collect(Collectors.toList())
                           ))
                   );
                return couponClassify.getUsable();
               }
           }
        }
        List<Integer> templateIds = preTarget.stream().map(Coupon::getTemplateId).collect(Collectors.toList());
        log.debug("templateIds==> {}",JSON.toJSONString(templateIds));
        Map<Integer, CouponTemplateSDK> templateSDKMap = this.templateClient.findIds2TemplateSDK(templateIds).getData();
        preTarget.stream().forEach(c->c.setTemplateSDK(templateSDKMap.get(c.getTemplateId())));
        return  preTarget;
    }

    /**
     * <h2>根据用户 id 查找当前可以领取的优惠券模板</h2>
     *
     * @param userId 用户 id
     * @return {@link CouponTemplateSDK}s
     */
    @Override
    public List<CouponTemplateSDK> findAvailableTemplate(Long userId) throws CouponException {
        long curTime = new Date().getTime();
        List<CouponTemplateSDK> usableCouponTemplateSDK = templateClient.findAllUsableTemplate().getData();
        if (CollectionUtils.isEmpty(usableCouponTemplateSDK)){
            throw new CouponException("usableCouponTemplateSDK is empty");
        }
        //因为由定时任务过滤已过期的优惠券模板,所以存在未同步数据,需过滤已过期的优惠券模板
        usableCouponTemplateSDK.stream()
                .filter(t-> t.getRule().getExpiration().getDeadline()>=curTime)
                .collect(Collectors.toList());
        log.info("find usable template count: {}",usableCouponTemplateSDK.size());
        Map<Integer, Pair<Integer,CouponTemplateSDK>> limit2Tmplate = new HashMap<>(usableCouponTemplateSDK.size());
        usableCouponTemplateSDK.forEach(
                t->limit2Tmplate.put(t.getId(),Pair.of(t.getRule().getLimitation(),t))
        );
        List<CouponTemplateSDK> result = new ArrayList<>(limit2Tmplate.size());
        List<Coupon> userCurUsableCoupons = this.findCouponsByStatus(userId, CouponStatus.USABLE.getCode());
        log.info("current user has usable coupon:{} ,{}",userId, userCurUsableCoupons.size());
        Map<Integer,List<Coupon>> templateId2Coupon = userCurUsableCoupons.stream()
                .collect(Collectors.groupingBy(Coupon::getTemplateId));
        //根据 template rule 判断是否可领取优惠券
        limit2Tmplate.forEach(
                (k,v)->{
                    int limitation = v.getLeft();
                    CouponTemplateSDK couponTemplateSDK = v.getRight();
                    if (templateId2Coupon.containsKey(k) && templateId2Coupon.get(k).size() >= limitation){
                        return;
                    }
                    result.add(couponTemplateSDK);
                }
        );
        return result;
    }

    /**
     * <h2>用户领取优惠券</h2>
     *  1. 从TemplateClient 拿到对应的优惠券,并检查是否过期
     *  2. 根据 limiation 判断用户是否可领取
     *  3. save to db
     *  4. 填充 couponTemplateSDK
     *  5. save to cache
     * @param request {@link AcquireTemplateRequest}
     * @return {@link Coupon}
     */
    @Override
    public Coupon acquireTemplate(AcquireTemplateRequest request) throws CouponException {
        Map<Integer, CouponTemplateSDK> id2TemplateSDK =
                templateClient.findIds2TemplateSDK(Collections.singleton(request.getTemplateSDK().getId())).getData();
        log.info("打印数据===>{}",JSON.toJSONString(id2TemplateSDK));
        if (id2TemplateSDK.size() <= 0){
            log.error("can not acuiretemplate from templateClient:{}",request.getTemplateSDK().getId());
            throw new CouponException("can not acuiretemplate from templateClient");
        }
        //用户是否可以领取此张优惠券
        List<Coupon> userCurUsableCoupons = this.findCouponsByStatus(request.getUserId(), CouponStatus.USABLE.getCode());
        Map<Integer,List<Coupon>> templateId2Coupons = userCurUsableCoupons.stream()
                .collect(Collectors.groupingBy(Coupon::getTemplateId));
        if (templateId2Coupons.containsKey(request.getTemplateSDK().getId())
        && templateId2Coupons.get(request.getTemplateSDK().getId()).size() >=
                id2TemplateSDK.get(request.getTemplateSDK().getId()).getRule().getLimitation()){
            log.error("exceed template assign limitation:{}",
                    request.getTemplateSDK().getId());
            throw new CouponException("exceed template assign limitation");
        }
        //尝试获取优惠券码
        String couponCode = redisService.tryToAcquireCouponCodeFromCache(request.getTemplateSDK().getId());
        if (StringUtils.isEmpty(couponCode)){
            log.error("can not acquire coupon code: {}",request.getTemplateSDK().getId());
            throw new CouponException("can not acquire coupon code");
        }
        Coupon newCoupon =
                new Coupon(request.getTemplateSDK().getId(),request.getUserId(),couponCode,CouponStatus.USABLE);
        newCoupon = couponDao.save(newCoupon);
        //填充coupon对象 couponTemplateSDK, 一定放在缓存之前填充
        newCoupon.setTemplateSDK(request.getTemplateSDK());
        //放入缓存中
        redisService.addCouponToCache(request.getUserId(),
                Collections.singletonList(newCoupon),
                CouponStatus.USABLE.getCode());
        return newCoupon;
    }

    /**
     * <h2>结算(核销)优惠券</h2>
     * 这里需要注意:规则相关处理由 Settlement 系统去处理 ,这里仅仅做业务处理(校验过程)
     * @param info {@link SettlementInfo}
     * @return {@link SettlementInfo}
     */
    @Override
    public SettlementInfo settlement(SettlementInfo info) throws CouponException {
        //当没有传递优惠券时,直接返回商品总价
        List<SettlementInfo.CouponAndTemplateInfo> ctInfos = info.getCouponAndTemplateInfos();
        if (CollectionUtils.isEmpty(ctInfos)){
            log.info("empty info from SettlementInfo");
            double  goodsSum = 0.0;
            for (GoodsInfo g: info.getGoodsInfos()){
                goodsSum += g.getPrice() * g.getCount();
            }
            //没有优惠券,也就不存在优惠券的核销, SettlementInfo 其它字段无需修改
            info.setCost(retain2Decimals(goodsSum));
        }
        //校验优惠券是否属于用户自己
        List<Coupon> curUserUsableCoupons = findCouponsByStatus(info.getUserId(), CouponStatus.USABLE.getCode());
        Map<Integer,Coupon> couponId2Coupon = curUserUsableCoupons.stream()
                .collect(Collectors.toMap(Coupon::getId, Function.identity()));
        if (MapUtils.isEmpty(couponId2Coupon)
                || !CollectionUtils.isSubCollection(
                        ctInfos.stream().map(SettlementInfo.CouponAndTemplateInfo::getId)
                                .collect(Collectors.toList()), couponId2Coupon.keySet()
        )){
            log.error("coupon is not cur user in SettlementInfo");
            throw new CouponException("coupon is not cur user in SettlementInfo");
        }
        List<Coupon> settleCopons = new ArrayList<>(ctInfos.size());
        ctInfos.forEach(ci->settleCopons.add(couponId2Coupon.get(ci.getId())));
        //通过结算服务获取结算信息
        SettlementInfo processedInfo = settlementClient.computeRule(info).getData();
        if (processedInfo.getEmploy() && CollectionUtils.isNotEmpty(processedInfo.getCouponAndTemplateInfos())){
            log.info("settle user coupon :{} ,{}",info.getUserId(),JSON.toJSONString(settleCopons));
            //更新缓存
            redisService.addCouponToCache(info.getUserId(),settleCopons,CouponStatus.USED.getCode());
            //更新 DB
            kafkaTemplate.send(Constant.Kafka.TOPIC,
                    JSON.toJSONString(new CouponKafkaMessage(CouponStatus.USED.getCode(),
                            settleCopons.stream().map(Coupon::getId).collect(Collectors.toList()))
                    ));
        }
        return processedInfo;
    }

    /**
     * <h2>保留2位小数</h2>
     * @param value double值
     * @return model:0.00
     */
    private double retain2Decimals(double value){
        return new BigDecimal(value).setScale(2,BigDecimal.ROUND_HALF_UP).doubleValue();
       // return Double.parseDouble(new DecimalFormat("#.00").format(param));
    }
}
