# Testing Guide

## 테스트 실행 명령어

### 전체 테스트
```bash
./gradlew test
```

### 모듈별 테스트
```bash
# Core 모듈 테스트
./gradlew :core:test

# Data Server 테스트
./gradlew :data-server:test

# API Server 테스트
./gradlew :api-server:test
```

### 특정 테스트 클래스 실행
```bash
./gradlew :api-server:test --tests "PixabayServiceTest"
```

## 공통 규칙

### 테스트 클래스명
`[테스트 대상 클래스명]Test`
```java
PixabayControllerTest.java
MessageServiceTest.java
RedisDataStorageTest.java
```

### 테스트 메소드명
`[시나리오]_[예상결과]` (스네이크 케이스)
```java
@Test
void saveMessage_shouldReturnSavedMessage() {}

@Test
void getVideo_whenDataNotFound_shouldThrowNotFoundException() {}
```

### 테스트 설명
```java
@Test
@DisplayName("GET /video - 비디오 데이터 성공 응답")
void getVideo_shouldReturnVideoData() {}
```

### Given-When-Then 구조
```java
@Test
void saveMessage_shouldWork() {
    // Given (준비)
    String content = "Hello";

    // When (실행)
    service.saveMessage(new MessageRequest(content));

    // Then (검증)
    String result = service.getMessage();
    assertThat(result).isEqualTo(content);
}
```

## 모듈별 테스트 예시

### Core 모듈 (단위 테스트)
```java
@ExtendWith(MockitoExtension.class)
class RedisDataStorageTest {
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ListOperations<String, Object> listOperations;
    @InjectMocks private RedisDataStorage storage;

    @Test
    @DisplayName("리스트 데이터 저장 - 성공")
    void setListData_shouldStoreData() {
        // Given
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        List<String> data = List.of("item1", "item2");

        // When
        storage.setListData("key", data);

        // Then
        verify(redisTemplate).delete("key");
        verify(listOperations).rightPushAll(eq("key"), any(Object[].class));
    }
}
```

### API Server (Controller 테스트)
```java
@WebMvcTest(PixabayController.class)
class PixabayControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private PixabayService service;

    @Test
    @DisplayName("GET /video - 성공")
    void getVideo_shouldReturnVideoData() throws Exception {
        // Given
        PixabayVideoResult video = PixabayVideoResult.builder()
            .id(1)
            .pageURL("https://example.com")
            .build();
        when(service.getRandomVideo()).thenReturn(video);

        // When & Then
        mockMvc.perform(get("/video"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.data.id").value(1));
    }
}
```

### API Server (Service 테스트)
```java
@ExtendWith(MockitoExtension.class)
class MessageServiceTest {
    @Mock private RedisMessageStorage storage;
    @InjectMocks private MessageService service;

    @Test
    @DisplayName("메시지 저장 - 성공")
    void saveMessage_shouldWork() {
        // Given
        MessageRequest request = new MessageRequest("Hello");

        // When
        service.saveMessage(request);

        // Then
        verify(storage).saveMessage("Hello");
    }

    @Test
    @DisplayName("메시지 저장 - 유효하지 않은 내용")
    void saveMessage_whenInvalidContent_shouldThrowException() {
        // Given
        MessageRequest request = new MessageRequest("");

        // When & Then
        assertThatThrownBy(() -> service.saveMessage(request))
            .isInstanceOf(BadRequestException.class);
    }
}
```

### Data Server (Collector 테스트)
```java
@ExtendWith(MockitoExtension.class)
class PixabayVideoCollectorTest {
    @Mock private RestTemplate restTemplate;
    @Mock private Environment environment;
    @Mock private RedisDataStorage storage;
    @InjectMocks private PixabayVideoCollector collector;

    @Test
    @DisplayName("비디오 데이터 수집 및 저장")
    void collectAndStore_shouldFetchAndSaveData() {
        // Given
        when(environment.getProperty(anyString())).thenReturn("https://api.pixabay.com");
        // ... RestTemplate mock 설정

        // When
        collector.collectAndStore();

        // Then
        verify(storage).setListData(eq("pixabayVideos"), anyList());
    }
}
```

## AssertJ 주요 메소드

```java
// 동등성
assertThat(actual).isEqualTo(expected);
assertThat(actual).isNotNull();

// 문자열
assertThat(str).contains("substring");
assertThat(str).startsWith("prefix");

// 컬렉션
assertThat(list).hasSize(3);
assertThat(list).contains("a", "b");

// 예외
assertThatThrownBy(() -> service.method())
    .isInstanceOf(NotFoundException.class)
    .hasMessage("Not found");
```

## Mockito 주요 기능

```java
// Stubbing
when(service.getData()).thenReturn(data);
when(service.getData(anyString())).thenReturn(data);
when(service.getData()).thenThrow(new RuntimeException());

// Verification
verify(service).getData();
verify(service, times(3)).getData();
verify(service, never()).getData();
```

## 테스트 체크리스트

- [ ] 모든 public 메소드에 테스트가 있는가?
- [ ] 테스트 메소드명이 명확한가?
- [ ] `@DisplayName`으로 한글 설명이 있는가?
- [ ] Given-When-Then 구조를 따르는가?
- [ ] 독립적으로 실행 가능한가?
- [ ] 예외 상황도 테스트했는가?
- [ ] Mock 객체의 호출을 검증했는가?
- [ ] Redis Mock이 필요한 경우 적절히 처리했는가?
