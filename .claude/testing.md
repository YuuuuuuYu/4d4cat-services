# Testing Guide

## 공통 규칙

### 테스트 클래스명
`[테스트 대상 클래스명]Test`
```java
PixabayControllerTest.java
MessageServiceTest.java
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
    Message result = service.save(content);
    
    // Then (검증)
    assertThat(result.getContent()).isEqualTo(content);
}
```

## Controller 테스트

```java
@WebMvcTest(PixabayController.class)
class PixabayControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private PixabayVideoService service;
    
    @Test
    @DisplayName("GET /api/v1/pixabay/videos - 성공")
    void getVideo_shouldReturnVideoData() throws Exception {
        // Given
        when(service.getRandomElement()).thenReturn(videoResult);
        
        // When & Then
        mockMvc.perform(get("/api/v1/pixabay/videos"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("SUCCESS"))
            .andExpect(jsonPath("$.data.id").value(1));
    }
}
```

## Service 테스트

```java
@ExtendWith(MockitoExtension.class)
class MessageServiceTest {
    @Mock private MessageRepository repository;
    @InjectMocks private MessageService service;
    
    @Test
    @DisplayName("메시지 저장 - 성공")
    void saveMessage_shouldReturnSavedMessage() {
        // Given
        String content = "Hello";
        when(repository.save(any())).thenReturn(message);
        
        // When
        Message result = service.save(content);
        
        // Then
        assertThat(result.getContent()).isEqualTo(content);
        verify(repository).save(any());
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
