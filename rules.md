# 프로젝트 규칙 (Project Rules)

이 문서는 `4d4cat-services` 프로젝트의 원활한 협업을 위한 세부 규칙을 정의합니다.

## 1. Git 사용 규칙

### 1.1. 브랜치 이름 전략

- **feature**: `feature/기능-요약` (예: `feature/clock-domain-search`)
- **fix**: `fix/수정-내용-요약` (예: `fix/pixabay-api-error`)
- **refactor**: `refactor/리팩토링-범위` (예: `refactor/datainitialization-service`)

### 1.2. 커밋 메시지 형식

Angular Commit Convention을 따릅니다.

```
<type>: <subject>
<BLANK LINE>
<body>
```

- **type**: `feat`, `fix`, `refactor`, `style`, `docs`, `test`, `chore` 등

## 2. 테스트 코드 작성 규칙

### 2.1. 공통 규칙

- **테스트 클래스명**: `[테스트 대상 클래스명]Test` (예: `PixabayControllerTest`)
- **테스트 메소드명**: `[시나리오]_[예상 결과]` 형식의 스네이크 케이스를 사용합니다. (예: `getVideo_shouldReturnVideoData`)
- **`@DisplayName`**: 각 테스트 메소드에 시나리오를 한글로 명확하게 설명합니다. (예: `GET /video - 비디오 데이터 성공 응답`)
- **Given-When-Then 구조**: `// Given`, `// When`, `// Then` 주석으로 테스트 단계를 명확히 구분합니다.

### 2.2. 계층별 테스트 전략

#### 2.2.1. Presentation (Controller) 계층 테스트

- **목표**: API 엔드포인트의 요청/응답을 테스트합니다.
- **주요 어노테이션**: `@WebMvcTest`, `@SpringBootTest` + `@AutoConfigureMockMvc`
- **핵심 도구**: `MockMvc`
- **특징**:
    - `MockMvc`를 사용하여 실제 HTTP 요청처럼 테스트를 수행합니다.
    - `perform()`으로 요청을 보내고, `andExpect()`를 체이닝하여 상태 코드, 헤더, 응답 본문 등을 검증합니다.
    - Service 등 의존성은 `@MockBean` 또는 `@MockitoBean`으로 Mock 처리합니다.

```java
@WebMvcTest(PixabayController.class)
class PixabayControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private PixabayVideoService pixabayVideoService;

    @Test
    @DisplayName("GET /video - 비디오 데이터 성공 응답")
    void getVideo_shouldReturnVideoData() throws Exception {
        // Given
        when(pixabayVideoService.getRandomElement()).thenReturn(videoResult);

        // When & Then
        mockMvc.perform(get("/video"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(1));
    }
}
```

#### 2.2.2. Application (Service) 계층 테스트

- **목표**: 비즈니스 로직의 정확성을 단위 테스트합니다.
- **주요 어노테이션**: `@ExtendWith(MockitoExtension.class)`
- **핵심 도구**: `Mockito`, `AssertJ`
- **특징**:
    - `@Mock`으로 의존성을 Mock 객체로 만들고, `@InjectMocks`로 테스트 대상 Service에 주입합니다.
    - `MockMvc`를 사용하지 않고, Service 메소드를 직접 호출하여 결과를 검증합니다.
    - 검증 시 `assertThat` (AssertJ), `verify` (Mockito) 등을 사용합니다.
    - 예외 발생은 `assertThatThrownBy`로 검증합니다.

```java
@ExtendWith(MockitoExtension.class)
class PixabayMusicServiceTest {
    @Mock private RestTemplate restTemplate;
    @InjectMocks private PixabayMusicService pixabayMusicService;

    @Test
    @DisplayName("음악 데이터 반환")
    void getRandomElement_shouldReturnsMusicData() {
        // Given
        when(dataStorage.getRandomElement(...)).thenReturn(musicResult);
        
        // When
        PixabayMusicResult result = pixabayMusicService.getRandomElement();

        // Then
        assertThat(result).isEqualTo(musicResult);
    }
}
```
