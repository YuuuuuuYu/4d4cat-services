# 프로젝트 통합 가이드 (Project Unified Instructions)

이 문서는 `4d4cat-services` 프로젝트의 AI 어시스턴트를 위한 통합 가이드라인입니다. 아래 정의된 페르소나, 프로젝트 문맥, 개발 규칙을 숙지하고 코드를 생성하거나 리뷰할 때 엄격히 준수하십시오.

---

## 1. AI 어시스턴트 가이드 (Agent Guide)

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
-   **프로그래밍 언어:** Java 21
-   **프레임워크:** Spring Boot 3.4.12
-   **빌드 도구:** Gradle
-   **주요 라이브러리:**
    -   `Spring Web`: REST API 및 웹 기능 개발
    -   `Spring AOP`: 관점 지향 프로그래밍 (로깅 등 공통 기능)
    -   `Spring WebFlux (WebClient)`: 비동기 HTTP 통신 (Discord 웹훅)
    -   `SpringDoc OpenAPI`: API 문서 자동화 (Swagger UI)
    -   `Lombok`: 보일러플레이트 코드 감소
    -   `MessageSource`: 메시지 중앙 관리 (YAML 기반)
-   **외부 서비스:**
    -   **Pixabay API:** Pixabay와의 연동을 통해 비디오 및 음악 데이터를 가져옵니다. `PIXABAY_KEY` 환경 변수를 통해 API 키를 설정해야 합니다.
    -   **Discord Webhook:** 애플리케이션의 주요 이벤트(데이터 초기화 성공/실패 등)를 실시간으로 알립니다. `DISCORD_WEBHOOK_URL` 환경 변수를 통해 웹훅 URL을 설정해야 합니다.

### 2.2. 개발 환경 (`build.gradle` 기반)
-   **의존성 관리:** `Gradle` 및 `io.spring.dependency-management` 플러그인
-   **주요 의존성:**
    -   `spring-boot-starter-web`: RESTful API 개발을 위한 핵심 모듈
    -   `spring-boot-starter-aop`: AOP(관점 지향 프로그래밍) 지원
    -   `spring-boot-starter-webflux`: `WebClient` 사용을 위한 비동기 웹 스택
    -   `springdoc-openapi-starter-webmvc-ui:2.8.14`: API 문서를 위한 Swagger UI 통합
    -   `net.rakugakibox.util:yaml-resource-bundle:1.1`: YAML 기반 `MessageSource` 지원
    -   `lombok`: 어노테이션 기반으로 상용구 코드를 자동 생성
    -   `spring-boot-devtools`: 개발 시 코드 변경 사항 자동 재시작 지원
    -   `spring-boot-starter-test`: JUnit 5 기반의 테스트 환경 지원

### 2.3. 도메인별 워크플로우

#### 2.3.1. Pixabay 도메인 (외부 API 연동)
-   **목적:** 외부 Pixabay API를 호출하여 비디오와 음악 검색 결과를 제공합니다.
-   **워크플로우:**
    1.  **Request:** 클라이언트가 검색어(`q`), 페이지(`page`) 등의 파라미터와 함께 `/api/v1/pixabay/videos` 또는 `/api/v1/pixabay/musics` 엔드포인트로 API를 요청합니다.
    2.  **Controller:** `PixabayController`가 요청을 수신하여 `PixabayVideoService` 또는 `PixabayMusicService`를 호출합니다.
    3.  **Service:** 서비스 계층은 `RestTemplate`을 사용하여 Pixabay API 서버에 실제 HTTP 요청을 보냅니다. 이 과정에서 `PIXABAY_KEY`를 인증에 사용합니다.
    4.  **Response:** API 응답을 받은 후, 서비스는 이를 `PixabayResponse` 또는 `CustomPixabayMusicResponse` 같은 맞춤형 DTO로 가공하여 컨트롤러에 반환하고, 클라이언트는 최종 결과를 JSON 형태로 받습니다.

#### 2.3.2. Message 도메인 (메시지 저장 및 조회)
-   **목적:** 사용자가 제공한 메시지를 임시로 저장하고 조회하는 기능을 제공합니다.
-   **워크플로우:**
    1.  **메시지 저장 (POST `/api/v1/messages`):**
        -   클라이언트가 `content`를 담아 메시지 저장을 요청합니다.
        -   `MessageService`는 내용 유효성을 검사한 후, 클라이언트 IP와 함께 `Message` 객체를 생성하여 인메모리 `messageStore`에 `"lastMessage"`라는 키로 저장합니다. (이전 메시지는 덮어쓰여짐)
    2.  **메시지 조회 (GET `/api/v1/messages`):**
        -   클라이언트가 메시지 조회를 요청합니다.
        -   `MessageService`는 `messageStore`에서 `"lastMessage"` 키로 저장된 메시지를 찾아 `content`를 반환합니다. 메시지가 없으면 빈 문자열을 반환합니다.

#### 2.3.3. Common 도메인 (공통 기능)
-   **목적:** 여러 도메인에서 공통으로 사용되는 기능을 제공합니다.
-   **주요 기능:**
    -   **`DataInitializationService`**: 애플리케이션 시작 시(`@EventListener(ApplicationReadyEvent.class)`) 외부 API로부터 데이터를 병렬로 가져와 `DataStorage`에 로드합니다. `CompletableFuture`와 `exceptionally`를 사용하여 개별 API 호출 실패가 전체 초기화 프로세스를 중단시키지 않도록 내결함성을 갖추었습니다.
    -   **`DataInitializationAspect`**: `DataInitializationService`의 실행을 감싸, 실행 시간 측정 및 성공/실패 결과를 Discord 웹훅으로 알립니다. 특히 예외 발생 시, `MessageSource`를 통해 `ErrorCode`에 맞는 상세한 에러 내용을 포함하여 전송하며, 예측하지 못한 예외는 `INTERNAL_SERVER_ERROR`로 처리하여 알립니다.
    -   **`GlobalExceptionHandler`**: 애플리케이션 전역에서 발생하는 예외(e.g., `NotFoundException`, `BadRequestException`)를 처리합니다. `MessageSource`를 사용하여 에러 코드에 맞는 메시지를 조회하고, 일관된 형식의 에러 응답(`BaseResponse`)을 반환합니다.
    -   **`RestTemplateConfig`**: 외부 API 통신을 위한 `RestTemplate` 빈을 설정하고, 커스텀 에러 핸들러(`CustomResponseErrorHandler`)를 등록합니다.
    -   **`DiscordWebhookService`**: `WebClient`를 사용하여 Discord 웹훅으로 비동기 메시지를 전송하는 로직을 담당합니다.
    -   **`DataStorage`**: API 응답 등에서 필요한 데이터를 임시로 저장하는 인메모리 저장소입니다.
    -   **`MessageSourceConfig`**: `messages.yml` 파일을 읽어 `MessageSource` 빈을 설정합니다. 이를 통해 애플리케이션의 모든 메시지를 중앙에서 관리할 수 있습니다.

### 2.4. 빌드 및 배포 (CI/CD)
-   **지속적 통합 (CI):** `main` 브랜치 PR 생성 시 트리거. Java 21 환경 설정 -> 테스트 실행(`gradlew test`) -> 결과 게시.
-   **지속적 배포 (CD):** `main` 브랜치 병합 시 트리거. `bootJar` 빌드 -> SCP 전송 -> SSH 접속 후 기존 프로세스 종료 및 새 JAR 실행.

### 2.5. 아키텍처 및 코드 스타일 가이드

#### 2.5.1. 아키텍처 원칙
-   **계층형 아키텍처 (Layered Architecture)**
    -   **Presentation Layer (`presentation`):** HTTP 요청 수신/응답, JSON 변환, 검증. (예: `PixabayController`)
    -   **Application Layer (`application`):** 핵심 비즈니스 로직, 도메인 모델 관리. (예: `DataInitializationService`)
    -   **Infrastructure Layer (`infrastructure`):** 외부 API 통신, 저장소, 환경 설정. (예: `DataStorage`)
    -   **Common/Util (`aop`, `util`):** 횡단 관심사 및 유틸리티. (예: `DataInitializationAspect`)

#### 2.5.2. 주요 디자인 패턴 및 설계 원칙
-   **의존성 주입 (DI):** `@RequiredArgsConstructor`를 이용한 생성자 주입 방식 사용.
-   **관점 지향 프로그래밍 (AOP):** 횡단 관심사(로깅 등)는 Aspect로 분리.
-   **DTO 사용:** 계층 간 데이터 전송 시 반드시 DTO 사용.
-   **템플릿 메서드 패턴:** `DataInitializationService`에서 전체 흐름 정의, 하위 클래스에서 세부 구현.
-   **전략 패턴:** `ParameterBuilder`를 통해 파라미터 생성 로직 캡슐화.
-   **싱글턴 패턴:** `DataStorage` 등을 빈(Bean)으로 관리하여 싱글턴 유지.

#### 2.5.3. 코드 스타일 규칙
-   **Lombok 적극 활용:** `@Getter`, `@Slf4j`, `@RequiredArgsConstructor`(final 필드 생성자 주입) 사용.
-   **DTO:** Java `record` 타입을 사용하여 불변(immutable) DTO 정의 권장.
-   **RESTful API 명명:** 리소스는 복수형 명사 사용(예: `/api/v1/messages`), 버전을 명시.
-   **일관된 응답:** 공통 인터페이스(`ApiResponse`)나 `BaseResponse`를 사용하여 일관된 구조 반환. `GlobalExceptionHandler`를 통한 전역 예외 처리.

---

## 3. 테스트 코드 작성 규칙 (Testing Rules)

### 3.1. 공통 규칙
-   **테스트 클래스명**: `[테스트 대상 클래스명]Test` (예: `PixabayControllerTest`)
-   **테스트 메소드명**: `[시나리오]_[예상 결과]` 형식의 스네이크 케이스 (예: `getVideo_shouldReturnVideoData`)
-   **`@DisplayName`**: 각 테스트 메소드에 시나리오를 한글로 명확하게 설명 (예: `GET /video - 비디오 데이터 성공 응답`)
-   **Given-When-Then 구조**: `// Given`, `// When`, `// Then` 주석으로 테스트 단계 명확히 구분.

### 3.2. 계층별 테스트 전략

#### 3.2.1. Presentation (Controller) 계층 테스트
-   **목표**: API 엔드포인트의 요청/응답 테스트.
-   **주요 어노테이션**: `@WebMvcTest`, `@SpringBootTest` + `@AutoConfigureMockMvc`
-   **핵심 도구**: `MockMvc`
-   **작성 방식**:
    -   `MockMvc`를 사용하여 실제 HTTP 요청처럼 테스트 수행.
    -   Service 등 의존성은 `@MockBean` 또는 `@MockitoBean`으로 Mock 처리.
    -   `perform()`으로 요청 전송, `andExpect()`로 검증.

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

#### 3.2.2. Application (Service) 계층 테스트
- **목표**: 비즈니스 로직의 정확성 단위 테스트.
- **주요 어노테이션**: `@ExtendWith(MockitoExtension.class)`
- **핵심 도구**: `Mockito`, `AssertJ`
- **작성 방식**:
  - `@Mock`으로 의존성 생성, `@InjectMocks`로 테스트 대상에 주입.
  - Service 메소드를 직접 호출하여 결과 검증.
  - `assertThat (AssertJ)`, `verify (Mockito)` 사용.

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
