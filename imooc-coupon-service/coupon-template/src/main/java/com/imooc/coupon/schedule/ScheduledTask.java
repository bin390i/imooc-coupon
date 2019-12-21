package com.imooc.coupon.schedule;

import com.imooc.coupon.dao.CouponTemplateDao;
import com.imooc.coupon.entity.CouponTemplate;
import com.imooc.coupon.vo.TemplateRule;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * <h1>定时清理过期优惠券模板</h1>
 */
@Slf4j
@Component
public class ScheduledTask {


    private final CouponTemplateDao templateDao;

    @Autowired
    public ScheduledTask(CouponTemplateDao templateDao) {
        this.templateDao = templateDao;
    }

    /**
     * <h2>下线已过期的优惠券模板,1分钟执行一次</h2>
     * 
     */
    @Scheduled(fixedDelay = 60*60*1000)
    @SuppressWarnings("all")
    public void offlineCouponTemplate(){
        log.info("开始执行 offlineCouponTemplate");
        List<CouponTemplate> templates = templateDao.findAllByExpired(true);
        if (CollectionUtils.isEmpty(templates)){
            log.info("offlineCouponTemplate 执行完成");
            return;
        }

        List<CouponTemplate> expiredCouponTemplate = new ArrayList<>(templates.size());
        Date curDate = new Date();
        templates.stream()
        .forEach(
                t->{
                    TemplateRule rule = t.getRule();
                    if (rule.getExpiration().getDeadline()<curDate.getTime()){
                        t.setExpired(true);
                        expiredCouponTemplate.add(t);
                    }
                }
        );
        if (CollectionUtils.isNotEmpty(expiredCouponTemplate)){
            log.info("Expired CouponTemplate Num: {}",templateDao.saveAll(expiredCouponTemplate));
        }
        log.info("offlineCouponTemplate 执行完成");

    }
}
