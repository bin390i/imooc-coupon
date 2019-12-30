package com.imooc.coupon.controller;

import com.alibaba.fastjson.JSON;
import com.imooc.coupon.annotation.IgnoreResponseAdvice;
import com.imooc.coupon.entity.Coupon;
import com.imooc.coupon.exception.CouponException;
import com.imooc.coupon.service.IUserService;
import com.imooc.coupon.vo.AcquireTemplateRequest;
import com.imooc.coupon.vo.CouponTemplateSDK;
import com.imooc.coupon.vo.SettlementInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <h1> 用户服务 controller </h1>
 */
@RestController
@Slf4j
public class UserServiceController {

    private final IUserService userService;

    @Autowired
    public UserServiceController(IUserService userService) {
        this.userService = userService;
    }

    /**
     * <h2> 根据用户状态查找优惠券 </h2>
     * @param userId
     * @param status
     * @return
     * @throws CouponException
     */
    @GetMapping("/coupons")
    @IgnoreResponseAdvice
    public List<Coupon> findCouponsByStatus(Long userId , Integer status) throws CouponException{
        log.info("find coupons by status: {} {}",userId,status);
        List<Coupon> coupons = userService.findCouponsByStatus(userId, status);
        log.debug("findCouponsByStatus response date ==> {}",JSON.toJSONString(coupons));
        return coupons;
    }

    /**
     * <h2> 根据用户 ID 查找当前可用的优惠券模板 </h2>
     * @param userId
     * @return
     * @throws CouponException
     */
    @GetMapping("/template")
    public List<CouponTemplateSDK> findAvailableTemplate(Long userId) throws CouponException {
        log.info("find available template :{} ",userId);
        return userService.findAvailableTemplate(userId);
    }

    @PostMapping("/acquire/template")
    public Coupon acuireTemplate(@RequestBody AcquireTemplateRequest request) throws CouponException{
        log.info("acquire template: {}", JSON.toJSONString(request));
        return userService.acquireTemplate(request);
    }

    /**
     * <h2> 结算(核销)优惠券 </h2>
     * @param info
     * @return
     * @throws CouponException
     */
    @PostMapping("/settlement")
    public SettlementInfo settlement(SettlementInfo info) throws CouponException{
        log.info("settlement: {}",JSON.toJSONString(info));
        return userService.settlement(info);
    }
}
