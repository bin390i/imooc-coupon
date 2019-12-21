package com.imooc.coupon.service.impl;

import com.alibaba.fastjson.JSON;
import com.imooc.coupon.constant.Constant;
import com.imooc.coupon.constant.CouponStatus;
import com.imooc.coupon.dao.CouponDao;
import com.imooc.coupon.entity.Coupon;
import com.imooc.coupon.service.IKafkaService;
import com.imooc.coupon.vo.CouponKafkaMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * <h1>kafka相关服务实现</h1>
 * 核心思想: 将 cache  中的优惠券状态同步到 DB 中
 */
@Service
@Slf4j
public class KafkaServiceImpl implements IKafkaService {
    
    
    private final CouponDao couponDao;
    
    @Autowired
    public KafkaServiceImpl(CouponDao couponDao) {
        this.couponDao = couponDao;
    }


    /**
     * <h2>消费优惠券 Kafka 消息</h2>
     *
     * @param record {@link ConsumerRecord}
     */
    @Override
    @KafkaListener(topics = {Constant.Kafka.TOPIC}, groupId = "imooc-coupon-1")
    public void consumeCouponKafkaMessage(ConsumerRecord<?, ?> record) {
        Optional<?> kafkaMessage = Optional.ofNullable(record.value());
        if (kafkaMessage.isPresent()){
            Object messsage = kafkaMessage.get();
            CouponKafkaMessage couponKafkaMessage = JSON.parseObject(messsage.toString(), CouponKafkaMessage.class);
            log.info("receive couponKafkaMessage: {}",messsage.toString());
            CouponStatus couponStatus = CouponStatus.of(couponKafkaMessage.getStatus());
            switch (couponStatus){
                case USABLE: //此状态无需处理
                    break;
                case USED: processUsedCoupons(couponKafkaMessage,couponStatus);
                    break;
                case EXPIRED: processExpiredCoupons(couponKafkaMessage,couponStatus);
                    break;
            }
        }
    }

    /**
     * <h2>处理用户已使用的优惠券</h2>
     */
    private void processUsedCoupons(CouponKafkaMessage kafkaMessage ,CouponStatus status){
        //TODO 发短信或其它
        processCouponByStatus(kafkaMessage,status);
    }

    /**
     * <h2>处理用户已过期的优惠券</h2>
     * 重新包装一层,可以对不同的优惠券做不同的处理
     */
    private void processExpiredCoupons(CouponKafkaMessage kafkaMessage ,CouponStatus status){
        //TODO 发推送消息或其它
        processCouponByStatus(kafkaMessage,status);
    }

    /**
     * <h2>根据状态处理优惠券信息</h2>
     * @param kafkaMessage {@link CouponKafkaMessage} 优惠券 kafka 消息对象
     * @param status {@link CouponStatus} 优惠券状态
     */
    private void processCouponByStatus(CouponKafkaMessage kafkaMessage, CouponStatus status) {
        List<Coupon> coupons = couponDao.findAllById(kafkaMessage.getIds());
        if (CollectionUtils.isEmpty(coupons)
                || coupons.size() != kafkaMessage.getIds().size()){
            log.error("can't find right coupon info:{}",JSON.toJSONString(kafkaMessage));
            //TODO 发送邮件
            return;
        }
        coupons.forEach(c->c.setStatus(status));
        log.info("couponKafkaMessage op coupon count: {}",
                couponDao.saveAll(coupons).size());
    }


}
