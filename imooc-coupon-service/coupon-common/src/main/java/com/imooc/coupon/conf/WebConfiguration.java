package com.imooc.coupon.conf;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * 定制 HTTP 消息转换器
 */
@Configuration
public class WebConfiguration implements WebMvcConfigurer  {

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.clear();
        //直接指定使用哪种消息转换器,而不需要springboot为我们挑选合适的转换器
        converters.add(new MappingJackson2HttpMessageConverter());
    }
}
