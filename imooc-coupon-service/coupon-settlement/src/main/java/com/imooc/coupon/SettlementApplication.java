package com.imooc.coupon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

/**
 * <h1> 优惠券结算微服务的启动入口 </h1>
 */
@SpringBootApplication
@EnableEurekaClient
public class SettlementApplication {
    public static void main(String[] args) {
        SpringApplication.run(SettlementApplication.class,args);
    }
}
