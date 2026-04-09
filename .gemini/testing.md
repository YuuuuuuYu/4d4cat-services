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

### Test Fixture 활용
테스트 코드의 가독성을 높이고 객체 생성의 중복을 줄이기 위해 Test Fixture를 적극 활용합니다.
- 복잡한 객체 생성 로직은 테스트 클래스 내부에 두지 않고 별도의 Fixture 클래스로 분리합니다.
- Fixture 클래스는 `src/test/java/.../fixture/` 패키지 아래에 위치하며 `[도메인]TestFixtures` 명명 규칙을 따릅니다.
- 객체 생성 시 기본값이 채워진 팩토리 메서드를 제공하고, 필요한 경우 커스텀 값을 설정할 수 있도록 합니다.

예시 (`PixabayTestFixtures.java`):
```java
public class PixabayTestFixtures {

  public static PixabayVideoResult createDefaultVideoResult(int id) {
    return PixabayVideoResult.builder()
        .id(id)
        .pageURL("https://pixabay.com/videos/id-125/")
        .type("video")
        .tags("nature, video")
        .duration(120)
        // ... 생략
        .build();
  }
}
```

테스트 코드 내 사용 예시:
```java
@Test
void getVideo_shouldReturnVideoData() {
    // Given
    PixabayVideoResult video = PixabayTestFixtures.createDefaultVideoResult(1);
    when(service.getRandomVideo()).thenReturn(video);
    // ...
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
- [ ] 복잡한 테스트 객체 생성 시 Test Fixture를 활용하여 가독성을 높였는가?

## Redis Mocking (로컬 Redis 없이 테스트하기)

로컬에 Redis가 실행되고 있지 않은 환경에서도 테스트가 가능하도록 `TestRedisConfig`를 활용합니다.

### 1. TestRedisConfig 설정
`api` 및 `data` 모듈의 테스트 소스셋에 `TestRedisConfig`를 작성하여 `RedisDataStorage` 등을 Mock 빈으로 등록합니다.

```java
@TestConfiguration
public class TestRedisConfig {
    @Bean
    @Primary
    public RedisDataStorage redisDataStorage() {
        return Mockito.mock(RedisDataStorage.class);
    }
}
```

### 2. 테스트 클래스에서 활용
`@SpringBootTest`를 사용하는 통합 테스트에서 `@Import`를 통해 Mock 설정을 주입합니다.

```java
@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfig.class) // Mock Redis 설정 주입
class MyServiceTest {
    @Autowired private RedisDataStorage redisDataStorage;
    // ...
}
```
