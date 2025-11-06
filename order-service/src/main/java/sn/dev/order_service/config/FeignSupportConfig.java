package sn.dev.order_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import feign.form.spring.SpringFormEncoder;

@Configuration
public class FeignSupportConfig {
    @Bean
    public SpringFormEncoder feignFormEncoder() {
        return new SpringFormEncoder();
    }
}
