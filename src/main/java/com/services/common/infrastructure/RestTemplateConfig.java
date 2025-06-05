package com.services.common.infrastructure;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import com.services.common.exception.CustomResponseErrorHandler;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        
        // JSON 변환기 설정
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setSupportedMediaTypes(Arrays.asList(MediaType.APPLICATION_JSON));
        restTemplate.getMessageConverters().add(converter);

        // 에러 핸들러 설정
        restTemplate.setErrorHandler(new CustomResponseErrorHandler());

        return restTemplate;
    }
}
