package com.services.data.pixabay;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.services.core.infrastructure.RedisDataStorage;
import com.services.core.notification.DataCollectionResult;
import com.services.core.pixabay.dto.PixabayResponse;
import com.services.core.pixabay.dto.PixabayVideoResult;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestClient;

@ExtendWith(MockitoExtension.class)
class PixabayVideoCollectorTest {

  @Mock private RestClient restClient;
  @Mock private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;
  @Mock private RestClient.RequestHeadersSpec requestHeadersSpec;
  @Mock private RestClient.ResponseSpec responseSpec;
  @Mock private Environment environment;
  @Mock private RedisDataStorage redisDataStorage;
  @Mock private MeterRegistry registry;
  @Mock private Counter counter;

  @InjectMocks private PixabayVideoCollector collector;

  @Test
  @DisplayName("collectAndStore - 성공적으로 데이터를 수집하고 저장")
  void collectAndStore_shouldFetchAndStoreData() {
    // Given
    when(environment.getProperty(anyString())).thenReturn("https://api.pixabay.com");
    when(restClient.get()).thenReturn(requestHeadersUriSpec);
    when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
    when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    
    PixabayResponse<PixabayVideoResult> pixabayResponse = 
        PixabayResponse.of("1", "1", List.of(PixabayVideoResult.builder().id(1).build()));
    when(responseSpec.body(any(ParameterizedTypeReference.class))).thenReturn(pixabayResponse);
    
    when(registry.counter(anyString(), any(String[].class))).thenReturn(counter);

    // When
    DataCollectionResult result = collector.collectAndStore();

    // Then
    assertThat(result).isNotNull();
    assertThat(result.totalItems()).isEqualTo(20); 
    
    verify(redisDataStorage).setData(eq("pixabayVideos"), anyList());
  }
}
