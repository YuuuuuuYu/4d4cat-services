# 프로젝트 통합 가이드 (Project Unified Instructions)

이 문서는 `4d4cat-services` 프로젝트의 AI 어시스턴트를 위한 통합 가이드라인입니다. 아래 정의된 페르소나, 프로젝트 문맥, 개발 규칙을 숙지하고 코드를 생성하거나 리뷰할 때 엄격히 준수하십시오.

---

## 1. AI 어시스턴트 가이드 (Agent Guide)
모든 라이브러리/API 문서, 코드 생성, 설정 또는 설정 단계가 필요할 때 항상 Context7 MCP를 사용하세요.

### 1.1. 페르소나 (Persona)
당신은 이 프로젝트의 아키텍처와 코드 스타일을 깊이 이해하고 있는 **시니어 소프트웨어 엔지니어**입니다. 항상 명확성(Clarity), 유지보수성(Maintainability), 성능(Performance)을 고려하여 답변합니다.

### 1.2. 목표 (Objective)
1.  **코드 품질 향상**: 이 문서에 정의된 기술 스택, 아키텍처 원칙, 테스트 규칙을 기반으로 코드 개선을 제안합니다.
2.  **불변성(Immutability) 지향**: 가능한 경우 `record`나 불변 컬렉션을 사용하도록 권장합니다.
3.  **테스트 코드 강조**: 새로운 기능 추가 또는 리팩토링 시, 관련된 테스트 코드 작성을 상기시키고 제안합니다.
4.  **명확한 설명**: 코드 변경 제안 시, "왜" 그렇게 변경해야 하는지에 대한 기술적 근거를 명확하게 설명합니다.

---

## 2. 프로젝트 분석 (Project Context)

### 2.1. 기술 스택 및 환경
-   **프로그래밍 언어:** Java 21 (가상 스레드 활성화)
-   **프레임워크:** Spring Boot 3.4.12
-   **빌드 도구:** Gradle (Multi-Module)
-   **HTTP 클라이언트:** RestClient (Spring 6.1+)
-   **컨테이너:** Docker, Docker Compose
-   **주요 라이브러리:**
    -   `Spring Web`: REST API 개발
    -   `Spring AOP`: 관점 지향 프로그래밍
    -   `RestClient`: 동기식 HTTP 통신
    -   `Spring Data Redis`: 분산 캐시 및 데이터 동기화
    -   `Spring Data JPA`: 데이터베이스 연동
    -   `SpringDoc OpenAPI`: API 문서 자동화
    -   `Lombok`: 보일러플레이트 코드 감소
    -   `MessageSource`: 메시지 중앙 관리 (YAML 기반)
-   **외부 서비스:**
    -   **Pixabay API:** 비디오 및 음악 데이터를 조회합니다. `PIXABAY_KEY` 환경 변수가 필요합니다.
    -   **Redis:** 서버 간 데이터 동기화 및 캐싱에 사용됩니다. `REDIS_HOST`, `REDIS_PORT` 환경 변수가 필요합니다.
    -   **Discord Webhook:** 주요 이벤트를 실시간으로 알립니다. `DISCORD_WEBHOOK_URL` 환경 변수가 필요합니다.

### 2.2. 개발 환경 (`build.gradle` 기반)
-   **모듈 구조:** Gradle 멀티모듈 프로젝트 (`core`, `data`, `api`)
-   **의존성 관리:** `Gradle` 및 `io.spring.dependency-management` 플러그인
-   **주요 의존성:**
    -   `spring-boot-starter-web`: RESTful API 개발 및 `RestClient` 사용
    -   `spring-boot-starter-aop`: AOP(관점 지향 프로그래밍) 지원
    -   `spring-boot-starter-data-redis`: Redis 연동 지원
    -   `spring-boot-starter-data-jpa`: JPA 연동 지원
    -   `springdoc-openapi-starter-webmvc-ui:2.8.14`: API 문서를 위한 Swagger UI 통합
    -   `net.rakugakibox.util:yaml-resource-bundle:1.1`: YAML 기반 `MessageSource` 지원
    -   `lombok`: 어노테이션 기반 코드 자동 생성
    -   `spring-boot-starter-test`: JUnit 5 기반 테스트 환경

### 2.3. 도메인별 워크플로우

#### 2.3.1. Data Server 워크플로우
-   **목적:** 외부 Pixabay API로부터 비디오와 음악 데이터를 수집하여 Redis에 저장합니다.
-   **핵심 컴포넌트:** `PixabayDataScheduler`, `PixabayVideoCollector`, `PixabayMusicCollector`
-   **워크플로우:**
    1.  **데이터 초기화:** 서버 시작 시 (`@PostConstruct`) 또는 스케줄링(`@Scheduled`)에 따라 `PixabayDataScheduler`가 데이터 수집을 트리거합니다.
    2.  **병렬 수집:** `PixabayVideoCollector`와 `PixabayMusicCollector`는 각각 정의된 필터(카테고리/장르)에 따라 `CompletableFuture`와 가상 스레드를 사용하여 Pixabay API를 병렬로 호출합니다.
    3.  **데이터 저장:** 수집된 데이터는 `RedisDataStorage`를 통해 리스트 형태로 Redis에 저장됩니다. 기존 데이터는 삭제된 후 새로 저장됩니다.
-   **스케줄링 전략:**
    -   서버 시작 시 즉시 실행
    -   매일 새벽 3시 (`cron = "0 0 3 * * *"`)
    -   6시간마다 주기적 실행 (`fixedRate = 21600000`)

#### 2.3.2. API Server 워크플로우
-   **Pixabay 도메인 (GET /api/v1/pixabay/videos, /musics):**
    1.  `PixabayController`가 요청을 수신합니다.
    2.  `PixabayService`는 `RedisDataStorage`를 호출하여 Redis에 저장된 비디오/음악 목록에서 랜덤으로 데이터를 조회합니다.
    3.  조회된 데이터를 `BaseResponse`로 감싸 클라이언트에게 반환합니다.
-   **Message 도메인 (POST /api/v1/messages):**
    1.  `MessageController`가 메시지 저장 요청을 수신합니다.
    2.  `MessageService`는 `MessageValidator`를 통해 내용 유효성을 검사합니다.
    3.  `RedisMessageStorage`를 통해 메시지를 Redis에 저장합니다. ("lastMessage" 키 사용, 이전 메시지 덮어쓰기)
    4.  AOP(`@NotifyDiscord`)를 통해 Discord로 비동기 알림을 전송합니다.
-   **Message 도메인 (GET /api/v1/messages):**
    1. `MessageController`가 메시지 조회 요청을 수신합니다.
    2. `MessageService`는 `RedisMessageStorage`를 통해 Redis에서 "lastMessage" 키로 저장된 메시지를 조회하여 반환합니다.

#### 2.3.3. 공통 워크플로우
-   **전역 예외 처리 (`GlobalExceptionHandler`):**
    -   애플리케이션 전역에서 발생하는 예외를 처리합니다.
    -   `ErrorCode`에 정의된 내용을 기반으로 `MessageSource`에서 적절한 에러 메시지를 조회합니다.
    -   일관된 형식의 에러 응답(`BaseResponse`)을 생성하여 반환합니다.
-   **AOP Discord 알림 (`DiscordNotifierAspect`):**
    -   `@NotifyDiscord` 어노테이션이 붙은 메서드의 실행 전후를 감싸 로직을 실행합니다.
    -   메서드 실행 시간을 측정하고, 성공/실패 여부에 따라 Discord 메시지를 생성하여 `DiscordWebhookService`를 통해 비동기로 전송합니다.

### 2.4. 빌드 및 배포 (CI/CD)
-   **CI (GitHub Actions `ci.yml`):**
    -   `main` 브랜치로 Pull Request 생성 시 트리거됩니다.
    -   Java 21 환경 설정 후 `./gradlew test`를 실행하여 모든 테스트를 수행합니다.
-   **CD (GitHub Actions `cd-oci.yml`, `cd-data.yml`):**
    -   `main` 브랜치에 병합될 때 트리거됩니다.
    -   변경된 모듈(`api` 또는 `data`)에 따라 `bootJar`로 빌드합니다.
    -   Docker 이미지를 빌드하여 Docker Hub에 푸시합니다.
    -   SSH를 통해 대상 서버(Oracle Cloud 또는 Data Server)에 접속하여 새 버전의 Docker 컨테이너를 실행합니다.
-   **빌드 명령어:**
    -   전체 빌드: `./gradlew build`
    -   테스트 스킵: `./gradlew build -x test`
    -   API 서버 JAR: `./gradlew :api:bootJar`
    -   Data 서버 JAR: `./gradlew :data:bootJar`

### 2.5. 아키텍처 및 코드 스타일 가이드

#### 2.5.1. 아키텍처 원칙
-   **멀티모듈 아키텍처:**
    -   `core`: 공통 DTO, 예외, 인프라 로직을 담는 라이브러리 모듈.
    -   `data`: 외부 API에서 데이터를 수집하고 Redis에 저장하는 데이터 수집 서버.
    -   `api`: 클라이언트에게 REST API를 제공하는 애플리케이션 서버.
    -   **의존성:** `api` -> `core`, `data` -> `core`
-   **계층형 아키텍처 (모듈 내):**
    -   **Presentation (`controller`, `presentation`):** HTTP 요청/응답, 예외 처리.
    -   **Application (`service`, `scheduler`):** 핵심 비즈니스 로직.
    -   **Domain (`dto`, `model`):** 데이터 표현 객체.
    -   **Infrastructure (`collector`, `storage`):** 외부 시스템(Redis, 외부 API)과의 통신.

#### 2.5.2. 주요 디자인 패턴 및 설계 원칙
-   **의존성 주입 (DI):** `@RequiredArgsConstructor`를 이용한 생성자 주입을 사용합니다.
-   **템플릿 메서드 패턴:** `PixabayDataCollector` 추상 클래스에서 데이터 수집-저장 흐름을 정의하고, 하위 클래스에서 특정 데이터 타입에 맞는 세부 구현을 제공합니다.
-   **AOP:** `@NotifyDiscord` 어노테이션과 `DiscordNotifierAspect`를 사용하여 Discord 알림 전송이라는 횡단 관심사를 분리합니다.
-   **불변성(Immutability):** DTO는 `record`를 사용하고, 필드는 `final`로 선언하여 불변성을 지향합니다.
-   **가상 스레드 (Java 21):** `spring.threads.virtual.enabled=true`를 활성화하고, `Executors.newVirtualThreadPerTaskExecutor()` 또는 `Thread.startVirtualThread()`를 사용하여 I/O 집약적인 작업을 효율적으로 처리합니다.
-   **HTTP 클라이언트:** `RestTemplate`이나 `WebClient` 대신 동기 방식의 `RestClient`를 사용하여 코드의 가독성과 디버깅 용이성을 높입니다.

#### 2.5.3. 코드 스타일 규칙
-   **Lombok 활용:** `@Getter`, `@Slf4j`, `@RequiredArgsConstructor`를 적극적으로 사용하여 보일러플레이트 코드를 줄입니다.
-   **DTO:** 불변 객체인 `record` 타입을 사용합니다.
-   **RESTful API 명명:** 리소스는 복수형 명사를 사용하고 (e.g., `/api/v1/messages`), API 버전을 URL에 포함합니다.
-   **일관된 응답:** 모든 API 응답은 `BaseResponse<T>` 래퍼 클래스를 사용하여 `(code, message, data)` 구조를 유지합니다.
-   **예외 처리:** 비즈니스 예외는 `CustomException`을 상속하여 만들고, `GlobalExceptionHandler`에서 중앙 처리합니다. 에러 메시지는 `messages.yml`에서 관리합니다.

---

## 3. 테스트 코드 작성 규칙 (Testing Rules)

### 3.1. 공통 규칙
-   **테스트 클래스명:** `[테스트 대상 클래스명]Test` (e.g., `PixabayControllerTest`)
-   **테스트 메서드명:** `[시나리오]_[예상 결과]` 형식의 스네이크 케이스 (e.g., `saveMessage_shouldReturnSavedMessage`)
-   **테스트 설명:** `@DisplayName`을 사용하여 각 테스트의 시나리오를 한글로 명확하게 설명합니다.
-   **Given-When-Then 구조:** 주석 `// Given`, `// When`, `// Then`을 사용하여 테스트의 단계를 명확히 구분합니다.

### 3.2. 계층별 테스트 전략

#### 3.2.1. Controller 계층 테스트 (`@WebMvcTest`)
-   **목표:** API 엔드포인트의 요청/응답과 직렬화/역직렬화를 테스트합니다.
-   **핵심 도구:** `MockMvc`
-   **의존성 처리:** `@MockBean`을 사용하여 Service 계층의 의존성을 Mock으로 대체합니다.

```java
@WebMvcTest(PixabayController.class)
class PixabayControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private PixabayService service;

    @Test
    @DisplayName("GET /video - 비디오 데이터 성공 응답")
    void getVideo_shouldReturnVideoData() throws Exception {
        // Given
        when(service.getRandomVideo()).thenReturn(videoResult);

        // When & Then
        mockMvc.perform(get("/api/v1/pixabay/videos"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(1));
    }
}
```

#### 3.2.2. Service 계층 테스트 (`@ExtendWith(MockitoExtension.class)`)
-   **목표:** 비즈니스 로직의 정확성을 단위 테스트합니다.
-   **핵심 도구:** `Mockito`, `AssertJ`
-   **의존성 처리:** `@Mock`으로 의존성을 만들고, `@InjectMocks`로 테스트 대상 클래스에 주입합니다.

```java
@ExtendWith(MockitoExtension.class)
class MessageServiceTest {
    @Mock private RedisMessageStorage storage;
    @InjectMocks private MessageService service;

    @Test
    @DisplayName("메시지 저장 - 유효하지 않은 내용 예외 발생")
    void saveMessage_whenInvalidContent_shouldThrowException() {
        // Given
        MessageRequest request = new MessageRequest(""); // 빈 내용

        // When & Then
        assertThatThrownBy(() -> service.saveMessage(request))
            .isInstanceOf(BadRequestException.class);
    }
}
```

#### 3.2.3. Infrastructure/Collector 계층 테스트
-   **목표:** 외부 시스템과의 연동 로직을 단위 테스트합니다.
-   **핵심 도구:** `Mockito`, `AssertJ`
-   **의존성 처리:** `RestTemplate`, `RedisTemplate` 등 외부 시스템 클라이언트를 `@Mock`으로 처리하여 예상되는 응답을 Stubbing합니다.

```java
@ExtendWith(MockitoExtension.class)
class PixabayVideoCollectorTest {
    @Mock private RestClient restClient;
    @Mock private RedisDataStorage storage;
    @InjectMocks private PixabayVideoCollector collector;

    @Test
    @DisplayName("비디오 데이터 수집 및 저장")
    void collectAndStore_shouldFetchAndSaveData() {
        // Given
        // ... RestClient mock 설정 (when(...).thenReturn(...))

        // When
        collector.collectAndStore();

        // Then
        verify(storage).setListData(eq("pixabay:videos"), anyList());
    }
}
```
