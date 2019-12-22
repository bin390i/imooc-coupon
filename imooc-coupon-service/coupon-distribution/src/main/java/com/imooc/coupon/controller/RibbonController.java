package com.imooc.coupon.controller;

import com.imooc.coupon.annotation.IgnoreResponseAdvice;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * <h1> Ribbon 应用 controller </h1>
 */
@RestController
@Slf4j
public class RibbonController {

    /** rest 客户端 */
    private final RestTemplate restTemplate;

    @Autowired
    public RibbonController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @GetMapping("/info")
    @IgnoreResponseAdvice
    public TemplateInfo getTemplateInfo(){
        String url = "http://eureka-client-coupon-template" +
                "/coupon-template/info";
        return restTemplate.getForEntity(url,TemplateInfo.class).getBody();
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    private static class TemplateInfo {
        private Integer code;
        private String message;
        private List<Map<String,Object>> data;

    }
}
