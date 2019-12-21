package com.imooc.coupon.service.impl;

import com.alibaba.fastjson.JSON;
import com.imooc.coupon.constant.Constant;
import com.imooc.coupon.constant.CouponStatus;
import com.imooc.coupon.entity.Coupon;
import com.imooc.coupon.exception.CouponException;
import com.imooc.coupon.service.IRedisService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * redis相关的操作服务接口
 */
@Service
@Slf4j
public class RedisServiceImpl implements IRedisService {


    private final StringRedisTemplate redisTemplate;

    @Autowired
    public RedisServiceImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * <h2>根据 userId 和状态找到缓存的优惠券列表数据</h2>
     *
     * @param userId 用户 id
     * @param status 优惠券状态 {@link CouponStatus}
     * @return {@link Coupon}s, 注意, 可能会返回 null, 代表从没有过记录
     */
    @Override
    public List<Coupon> getCachedCoupons(Long userId, Integer status) {
        log.info("get coupons from cache:{},{}",userId,status);
        String redisKey = status2RedisKey(status, userId);
        List<String> couponStr = redisTemplate.opsForHash().values(redisKey)
                .stream()
                .map(o -> Objects.toString(o,null))
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(couponStr)){
            saveEmptyCouponListToCache(userId, Collections.singletonList(status));
            return Collections.emptyList();
        }
        return couponStr.stream()
                .map(cs->JSON.parseObject(cs,Coupon.class))
                .collect(Collectors.toList());
    }

    /**
     * <h2>保存空的优惠券列表到缓存中</h2>
     * 目的:避免缓存穿透
     * @param userId 用户 id
     * @param status 优惠券状态列表
     */
    @Override
    @SuppressWarnings("all")
    public void saveEmptyCouponListToCache(Long userId, List<Integer> status) {
        log.info("save empty list to cache for user:{}, status:{}",userId, JSON.toJSONString(status));
        Map<String,String> invalidCouponMap = new HashMap<>();
        invalidCouponMap.put("-1",JSON.toJSONString(Coupon.invalidCoupon()));
        //使用sessionCallback 把数据命令放到 Redis  的 pipeline (可以一次性发送多条命令到redis中)
        SessionCallback<Object> sessionCallback = new SessionCallback<Object>() {
            @Override
            public  Object execute(RedisOperations operations) throws DataAccessException {
                status.forEach(s->{
                    String redisKey = status2RedisKey(s,userId);
                    operations.opsForHash().putAll(redisKey,invalidCouponMap);
                    //key: imooc_user_coupon_usable_1
                    //value: {-1: coupon objecy}
                });
                return null;
            }
        };
        log.info("pipeline exe result: {}", JSON.toJSONString(redisTemplate.executePipelined(sessionCallback)));
    }

    /**
     * <h2>尝试从 Cache 中获取一个优惠券码</h2>
     *
     * @param templateId 优惠券模板主键
     * @return 优惠券码
     */
    @Override
    public String tryToAcquireCouponCodeFromCache(Integer templateId) {
        String redisKey = String.format("%s%s",Constant.RedisPrefix.COUPON_TEMPLATE,templateId.toString());
        //左边pop 或 右边 pop没有影响
        String couponCode = redisTemplate.opsForList().leftPop(redisKey);
        log.info("acquire coupon code, templateId: {}, couponCode: {} ,redisKey: {}",
                templateId,couponCode,redisKey);
        return couponCode;
    }

    /**
     * <h2>将优惠券保存到 Cache 中</h2>
     *
     * @param userId  用户 id
     * @param coupons {@link Coupon}s
     * @param status  优惠券状态
     * @return 保存成功的个数
     */
    @Override
    public Integer addCouponToCache(Long userId, List<Coupon> coupons, Integer status) throws CouponException {
        log.info("access addCouponToCache method, params --> userId:{}, coupons:{}, status:{}",
                userId,JSON.toJSONString(coupons),status);
        Integer result = -1;
        CouponStatus couponStatus = CouponStatus.of(status);
        switch (couponStatus){
            case USABLE: addCouponToCacheForUsable(userId,coupons);
                break;
            case USED: addCouponToCacheForUsed(userId,coupons);
                break;
            case EXPIRED: addCouponToCaheForExpird(userId,coupons);
                break;
        }
        return result;
    }

    /**
     * <h2>用户可用优惠券存储到缓存中</h2>
     * @param userId
     * @param coupons
     * @return 受影响的优惠券个数
     */
    @SuppressWarnings("all")
    private int addCouponToCacheForUsable(Long userId, List<Coupon> coupons) throws CouponException {
        log.info("access addCouponToCacheForUsable,params-> userId: {},coupons:{}",
                userId,JSON.toJSONString(coupons));
        Map<String,String> needCacheObject = new HashMap<>(coupons.size());
        coupons.forEach(
                c->{
                    needCacheObject.put(c.getId().toString(),JSON.toJSONString(c));
                }
        );
        String redisKey = status2RedisKey(CouponStatus.USABLE.getCode(),userId);
        redisTemplate.opsForHash().putAll(redisKey,needCacheObject);
        redisTemplate.expire(redisKey,getRandomExpirationTime(1,2), TimeUnit.SECONDS);
        return needCacheObject.size();
    }

    /**
     * <h2>缓存用户已使用的优惠券</h2>
     * @param userId
     * @param coupons
     * @return 受影响的优惠券个数
     */
    @SuppressWarnings("all")
    private int addCouponToCacheForUsed(Long userId, List<Coupon> coupons) throws CouponException {
        log.info("access addCouponToCacheForUsed,params-> userId: {},coupons:{}",
            userId,JSON.toJSONString(coupons));
        Map<String,String> needCacheObject = new HashMap<>(coupons.size());
        String redisKeyForUsable = status2RedisKey(
                CouponStatus.USABLE.getCode(),userId
        );
        String redisKeyForUsed = status2RedisKey(
                CouponStatus.USED.getCode(),userId
        );
        //获取当前用户可用的优惠券
        List<Coupon> curUsableCoupon = getCachedCoupons(userId, CouponStatus.USABLE.getCode());
        //当前可用的优惠券个数一定是大于1
        assert curUsableCoupon.size()> coupons.size();
        coupons.forEach(
                c->needCacheObject.put(c.getId().toString(),JSON.toJSONString(c))
        );
        //校验当前的优惠券参数是否与cache中的匹配
        List<Integer> curUsables = curUsableCoupon.stream()
                .map(Coupon::getId)
                .collect(Collectors.toList());
        List<Integer> paramsIds = coupons.stream()
                .map(Coupon::getId)
                .collect(Collectors.toList());
        if (!CollectionUtils.isSubCollection(paramsIds,curUsables)){
             log.error("curCoupons is not equal to cache {}  {}   {}",
                     userId,JSON.toJSONString(curUsables),JSON.toJSONString(paramsIds));
              throw new CouponException("curCoupons is not equal to cache!");
        }
        List<String> needCleanKey = paramsIds.stream()
                .map(i->i.toString())
                .collect(Collectors.toList());
        SessionCallback<Object> sessionCallback = new SessionCallback<Object>() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                 //1. 已使用的优惠券 cache 缓存
                operations.opsForHash().putAll(redisKeyForUsed,needCacheObject);
                //2. 可用的优惠券 cache 清理
                operations.opsForHash().delete(redisKeyForUsable,needCleanKey.toArray());
                //3. 重置过期时间
                operations.expire(
                        redisKeyForUsable,
                        getRandomExpirationTime(1,2),
                        TimeUnit.SECONDS);
                operations.expire(
                        redisKeyForUsed,
                        getRandomExpirationTime(1,2),
                        TimeUnit.SECONDS);
                return null;
            }
        };
        log.info("pipeline exe result:{}",JSON.toJSONString(redisTemplate.executePipelined(sessionCallback)));
        return coupons.size();
    }

    /**
     * <h2>将过期优惠券加到缓存中</h2>
     * @param userId
     * @param coupons
     * @return 受影响的优惠券个数
     */
    @SuppressWarnings("all")
    private Integer addCouponToCaheForExpird(Long userId , List<Coupon> coupons) throws CouponException {
        //status 为 expired , 代表已有的优惠券过期了,影响到了俩个 cache
        //USABLE , EXPIRED
        log.info("access addCouponToCaheForExpird,params-> userId: {},coupons:{}",
                userId,JSON.toJSONString(coupons));
        Map<String,String> needCacheObject = new HashMap<>(coupons.size());
        String redisKeyForUsable = status2RedisKey(CouponStatus.USABLE.getCode(),userId);
        String redisKeyForExpired = status2RedisKey(CouponStatus.EXPIRED.getCode(),userId);
        List<Coupon> curUsableCoupons = getCachedCoupons(userId,CouponStatus.USABLE.getCode());
        assert curUsableCoupons.size() > coupons.size();
        coupons.forEach(
                c->needCacheObject.put(c.getId().toString() ,JSON.toJSONString(c))
        );
        //校验当前的优惠券参数是否与cache中的匹配
        List<Integer> curUsableIds = curUsableCoupons.stream()
                .map(Coupon::getId)
                .collect(Collectors.toList());
        List<Integer> paramsIds = coupons.stream()
                .map(Coupon::getId)
                .collect(Collectors.toList());
         if (!CollectionUtils.isSubCollection(paramsIds,curUsableIds)){
             log.error("curCoupons is not equal to cache:{} {} {}",
                     userId,JSON.toJSONString(curUsableIds),JSON.toJSONString(paramsIds));
             throw new CouponException("curCoupons is not equal to cache!");
         }
         List<String> needCleanKey = paramsIds.stream()
                 .map(i->i.toString())
                 .collect(Collectors.toList());
         SessionCallback<Object> sessionCallback = new SessionCallback<Object>() {
             @Override
             public Object execute(RedisOperations operations) throws DataAccessException {
                 //已过期的优惠券缓存
                 operations.opsForHash().putAll(redisKeyForExpired,needCacheObject);
                 //可用的优惠券需清除
                 operations.opsForHash().delete(redisKeyForUsable,needCleanKey.toArray());
                 //重置过期时间
                 operations.expire(redisKeyForUsable,getRandomExpirationTime(1,2), TimeUnit.SECONDS);
                 operations.expire(redisKeyForExpired ,getRandomExpirationTime(1,2), TimeUnit.SECONDS);
                 return null;
             }
         };
         log.info("pipleline exe result: {}",JSON.toJSONString(redisTemplate.executePipelined(sessionCallback)));
        return coupons.size();
    }

    /**
     * <h2>根据 status 获取到对应的 redis key</h2>
     */
    private String status2RedisKey(Integer status,Long userId){
        String rediskey =  null;
        CouponStatus couponStatus = CouponStatus.of(status);
        switch (couponStatus){
            case USABLE:
                rediskey = String.format("%s%s", Constant.RedisPrefix.USER_COUPON_USABLE,userId);
                break;
            case USED:
                rediskey = String.format("%s%s",Constant.RedisPrefix.USER_COUPON_USED,userId);
                break;
            case EXPIRED:
                rediskey = String.format("%s%s",Constant.RedisPrefix.USER_COUPON_EXPIRED,userId);
        }
        return rediskey;
    }

    /**
     * <h2>获取一个随机的过期时间</h2>
     * 避免缓存雪崩: redis key 在同一时间失效
     * @param min 最小小时数
     * @param max 最大小时数
     * @return [min,max] 中的随机秒数
     */
    private Long getRandomExpirationTime(int min , int max)  {
        if (min >= max) {
            log.error("params error:{} {}", min, max);
            throw new RuntimeException("getRandomExpirationTime params error");
        }
        return RandomUtils.nextLong(min * 60 * 60, max * 60 * 60);
    }
}
