package sn.dev.media_service.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
        // sensible defaults
        requestFactory.setConnectTimeout(10_000);
        requestFactory.setReadTimeout(30_000);
        return new RestTemplate(requestFactory);
    }
}

