package com.imooc.coupon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.web.client.RestTemplate;

/**
 * <h1>分发微服务的启动入口</h1>
 */
@SpringBootApplication
@EnableEurekaClient    // 标记应用为 Eureka Client
@EnableFeignClients    // 开启 feign ,允许应用访问其它微服务
@EnableCircuitBreaker  //开启断路器
@EnableJpaAuditing     //开启JPA的审计功能
public class DistributionApplication {
    public static void main(String[] args) {
        SpringApplication.run(DistributionApplication.class,args);
    }

    @Bean
    @LoadBalanced  //开启负载均衡
    RestTemplate restTemplate(){
        return new RestTemplate();
    }
}
