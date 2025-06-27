package com.services.common.exception;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.ResponseErrorHandler;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CustomResponseErrorHandler implements ResponseErrorHandler {

    @Override
    public boolean hasError(ClientHttpResponse response) throws IOException {
        return response.getStatusCode().isError();
    }

    @Override
    public void handleError(URI url, HttpMethod method, ClientHttpResponse response) throws IOException {
        String errorBody = StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);
        
        if (response.getStatusCode().value() == 404) {
            log.warn("404 Not Found for URL: {}, Method: {}, Error Body: {}", url, method, errorBody);
            throw new NotFoundException(ErrorCode.DATA_NOT_FOUND);
        }
        
        log.error("Error occurred while calling URL: {}, Method: {}, Status Code: {}, Error Body: {}",
                url, method, response.getStatusCode(), errorBody);
        throw new InternalServerException(ErrorCode.INTERNAL_SERVER_ERROR);
    }
}
