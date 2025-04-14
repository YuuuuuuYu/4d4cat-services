package com.services.common.infrastructure;

import org.apache.coyote.BadRequestException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Configuration
public class RestemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();

        // JSON 변환을 위한 MessageConverter 설정
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setSupportedMediaTypes(Arrays.asList(MediaType.APPLICATION_JSON));
        restTemplate.getMessageConverters().add(converter);

        // 응답 오류 처리를 위한 ErrorHandler 설정
        restTemplate.setErrorHandler(new ResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) throws IOException {
                return response.getStatusCode().isError();
            }

            @Override
            public void handleError(URI url, HttpMethod method, ClientHttpResponse response) throws IOException {
                String errorBody = StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);
                throw new BadRequestException(
                        String.format("API 호출 오류: %s %s, 상태 코드: %s, 응답: %s", method, url, response.getStatusCode(), errorBody)
                );
            }
        });

        return restTemplate;
    }
}
