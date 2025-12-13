# 프로젝트 분석: 4d4cat-services

이 문서는 프로젝트의 파일 구조, 기술 스택, 개발 환경 및 도메인별 워크플로우에 대한 요약입니다.

## 1. 기술 스택 및 환경

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

## 2. 개발 환경 (`build.gradle` 기반)

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

## 3. 도메인별 워크플로우

이 프로젝트는 크게 `Pixabay`, `Message`, `Common` 세 가지 도메인으로 구성됩니다.

### 3.1. Pixabay 도메인 (외부 API 연동)

-   **목적:** 외부 Pixabay API를 호출하여 비디오와 음악 검색 결과를 제공합니다.
-   **워크플로우:**
    1.  **Request:** 클라이언트가 검색어(`q`), 페이지(`page`) 등의 파라미터와 함께 `/api/v1/pixabay/videos` 또는 `/api/v1/pixabay/musics` 엔드포인트로 API를 요청합니다.
    2.  **Controller:** `PixabayController`가 요청을 수신하여 `PixabayVideoService` 또는 `PixabayMusicService`를 호출합니다.
    3.  **Service:** 서비스 계층은 `RestTemplate`을 사용하여 Pixabay API 서버에 실제 HTTP 요청을 보냅니다. 이 과정에서 `PIXABAY_KEY`를 인증에 사용합니다.
    4.  **Response:** API 응답을 받은 후, 서비스는 이를 `PixabayResponse` 또는 `CustomPixabayMusicResponse` 같은 맞춤형 DTO로 가공하여 컨트롤러에 반환하고, 클라이언트는 최종 결과를 JSON 형태로 받습니다.

### 3.2. Message 도메인 (메시지 저장 및 조회)

-   **목적:** 사용자가 제공한 메시지를 임시로 저장하고 조회하는 기능을 제공합니다.
-   **워크플로우:**
    1.  **메시지 저장 (POST `/api/v1/messages`):**
        -   클라이언트가 `content`를 담아 메시지 저장을 요청합니다.
        -   `MessageService`는 내용 유효성을 검사한 후, 클라이언트 IP와 함께 `Message` 객체를 생성하여 인메모리 `messageStore`에 `"lastMessage"`라는 키로 저장합니다. (이전 메시지는 덮어쓰여짐)
    2.  **메시지 조회 (GET `/api/v1/messages`):**
        -   클라이언트가 메시지 조회를 요청합니다.
        -   `MessageService`는 `messageStore`에서 `"lastMessage"` 키로 저장된 메시지를 찾아 `content`를 반환합니다. 메시지가 없으면 빈 문자열을 반환합니다.

### 3.3. Common 도메인 (공통 기능)

-   **목적:** 여러 도메인에서 공통으로 사용되는 기능을 제공합니다.
-   **주요 기능:**
    -   **`DataInitializationService`**: 애플리케이션 시작 시(`@EventListener(ApplicationReadyEvent.class)`) 외부 API로부터 데이터를 병렬로 가져와 `DataStorage`에 로드합니다. `CompletableFuture`와 `exceptionally`를 사용하여 개별 API 호출 실패가 전체 초기화 프로세스를 중단시키지 않도록 내결함성을 갖추었습니다.
    -   **`DataInitializationAspect`**: `DataInitializationService`의 실행을 감싸, 실행 시간 측정 및 성공/실패 결과를 Discord 웹훅으로 알립니다. 특히 예외 발생 시, `MessageSource`를 통해 `ErrorCode`에 맞는 상세한 에러 내용을 포함하여 전송하며, 예측하지 못한 예외는 `INTERNAL_SERVER_ERROR`로 처리하여 알립니다.
    -   **`GlobalExceptionHandler`**: 애플리케이션 전역에서 발생하는 예외(e.g., `NotFoundException`, `BadRequestException`)를 처리합니다. `MessageSource`를 사용하여 에러 코드에 맞는 메시지를 조회하고, 일관된 형식의 에러 응답(`BaseResponse`)을 반환합니다.
    -   **`RestTemplateConfig`**: 외부 API 통신을 위한 `RestTemplate` 빈을 설정하고, 커스텀 에러 핸들러(`CustomResponseErrorHandler`)를 등록합니다.
    -   **`DiscordWebhookService`**: `WebClient`를 사용하여 Discord 웹훅으로 비동기 메시지를 전송하는 로직을 담당합니다.
    -   **`DataStorage`**: API 응답 등에서 필요한 데이터를 임시로 저장하는 인메모리 저장소입니다.
    -   **`MessageSourceConfig`**: `messages.yml` 파일을 읽어 `MessageSource` 빈을 설정합니다. 이를 통해 애플리케이션의 모든 메시지를 중앙에서 관리할 수 있습니다.

## 4. 빌드 및 배포 (CI/CD)

프로젝트는 GitHub Actions를 사용하여 CI/CD 파이프라인을 자동화합니다.

### 지속적 통합 (CI)

-   **트리거:** `main` 브랜치로 Pull Request가 생성될 때 실행됩니다.
-   **프로세스:**
    1.  Java 21 환경을 설정합니다.
    2.  `./gradlew test` 명령어로 테스트를 실행합니다.
    3.  테스트 결과를 게시합니다.

### 지속적 배포 (CD)

-   **트리거:** `main` 브랜치로 Pull Request가 병합(close)될 때 실행됩니다.
-   **프로세스:**
    1.  **빌드:** `./gradlew bootJar` 명령어로 실행 가능한 JAR 파일을 생성합니다.
    2.  **전송:** SCP를 사용하여 생성된 JAR 파일을 원격 서버로 복사합니다.
    3.  **배포:** SSH를 통해 원격 서버에 접속하여 다음을 수행합니다.
        a. 기존에 실행 중이던 애플리케이션 프로세스를 종료합니다.
        b. `java -jar` 명령어로 새로운 JAR 파일을 백그라운드에서 실행합니다.

## 5. 아키텍처 및 코드 스타일 가이드

이 섹션은 프로젝트의 일관된 코드 품질과 유지보수성 향상을 위한 아키텍처 원칙과 코드 스타일 규칙을 정의합니다.

### 5.1. 아키텍처 원칙

-   **계층형 아키텍처 (Layered Architecture)**
    -   프로젝트는 계층형 아키텍처를 따르며, 각 계층은 명확한 책임을 갖습니다. `com.services.common` 패키지 구조를 통해 이를 확인할 수 있습니다.
    -   **Presentation Layer (`presentation` 패키지):** 외부(클라이언트)와의 상호작용을 담당합니다.
        -   HTTP 요청 수신 및 응답, 데이터 형식 변환(JSON), 요청 값 검증을 처리합니다.
        -   예: `PixabayController`, `GlobalExceptionHandler`, `dto/ApiResponse`
    -   **Application Layer (`application` 패키지):** 핵심 비즈니스 로직을 구현합니다.
        -   서비스의 핵심 동작을 정의하고, 도메인 모델을 관리하며, 비즈니스 규칙을 적용합니다.
        -   예: `DataInitializationService`, `exception/ErrorCode`
    -   **Infrastructure Layer (`infrastructure` 패키지):** 기술적인 세부 사항과 외부 시스템 연동을 담당합니다.
        -   외부 API 통신(`RestTemplate`, `WebClient`), 데이터 저장소(`DataStorage`), 환경 설정(`@Configuration`) 등을 포함합니다.
        -   예: `config`, `discord`, `DataStorage`
    -   **Common/Util (`aop`, `util` 패키지):** 여러 계층에서 공통으로 사용되는 횡단 관심사(cross-cutting concerns) 및 유틸리티 기능을 제공합니다.
        -   예: `DataInitializationAspect` (AOP), `RandomUtils` (유틸리티)

### 5.2. 주요 디자인 패턴 및 설계 원칙

-   **의존성 주입 (Dependency Injection, DI):** Spring의 IoC 컨테이너를 통해 객체 간의 의존성을 외부에서 주입합니다. 이를 통해 클래스 간의 결합도를 낮추고 유연성과 테스트 용이성을 높입니다. 주로 `@RequiredArgsConstructor`를 이용한 생성자 주입 방식을 사용합니다.
-   **관점 지향 프로그래밍 (Aspect-Oriented Programming, AOP):** 로깅, 트랜잭션, 성능 측정과 같이 여러 모듈에 공통으로 필요한 기능(횡단 관심사)을 'Aspect'로 분리하여 관리합니다. `DataInitializationAspect`가 대표적인 예시입니다.
-   **데이터 전송 객체 (Data Transfer Object, DTO):** 계층 간 데이터 전송을 위해 특화된 객체를 사용합니다. 클라이언트 요청(`*Request`)과 응답(`*Response`)에 DTO를 사용하여 데이터 구조를 명확히 하고, 불필요한 정보 노출을 방지하며, 각 계층의 역할을 분리합니다.
-   **템플릿 메서드 (Template Method) 패턴**: `DataInitializationService` 추상 클래스가 데이터 초기화의 전체적인 흐름(템플릿)을 정의합니다. `PixabayVideoService`와 `PixabayMusicService`는 이를 상속받아 `getBaseUrl()`, `getFilters()` 등 각 서비스에 맞는 세부 단계를 구체화하여 코드 중복을 최소화하고 로직의 일관성을 유지합니다.
-   **전략 (Strategy) 패턴**: `ParameterBuilder` 인터페이스가 'URI 파라미터를 추가하는 행위'를 추상화(전략)합니다. `PixabayVideoRequest`, `PixabayMusicRequest` 등 각기 다른 DTO가 이 인터페이스를 구현하여, API별로 다른 파라미터 생성 로직을 캡슐화하고 교체 가능하도록 만듭니다.
-   **싱글턴 (Singleton) 패턴**: `DataStorage` 클래스는 애플리케이션 전역에서 사용되는 데이터 캐시를 단일 인스턴스로 관리하여 데이터 일관성을 보장하고 메모리 사용을 최적화합니다. Spring에서는 기본적으로 빈(Bean)을 싱글턴으로 관리하므로, `@Component`나 `@Service` 같은 어노테이션을 통해 자연스럽게 이 패턴이 적용됩니다.

### 5.3. 코드 스타일 규칙

-   **Lombok 적극 활용**
    -   `@Getter`, `@Slf4j` 등 Lombok 어노테이션을 적극적으로 사용하여 보일러플레이트 코드를 최소화합니다.
    -   의존성 주입은 `final` 필드와 `@RequiredArgsConstructor`를 사용한 **생성자 주입**을 기본으로 합니다.
        ```java
        @Service
        @RequiredArgsConstructor // final 필드에 대한 생성자를 자동 생성
        public class MessageService {
            private final DataStorage dataStorage; // final 키워드 사용
            // ...
        }
        ```

-   **DTO(Data Transfer Object) 사용**
    -   클라이언트 요청(`Request`) 및 응답(`Response`) 시에는 반드시 DTO를 사용합니다.
    -   Java 14 이상에서 도입된 `record` 타입을 사용하여 불변(immutable) DTO를 간결하게 정의하는 것을 권장합니다. (예: `PixabayResponse.java`)

-   **RESTful API 명명 규칙**
    -   **URL**: 리소스를 표현하는 명사는 **복수형**을 사용합니다. (예: `/api/v1/messages`)
    -   **버전 관리**: API 경로에 버전을 명시합니다. (예: `/api/v1/...`)

-   **일관된 API 응답 구조**
    -   모든 API 응답은 공통 인터페이스(`ApiResponse`)나 기본 클래스(`BaseResponse`)를 사용하여 반환합니다. 이를 통해 모든 응답이 일관된 구조를 갖도록 합니다.
    -   예외 발생 시 `GlobalExceptionHandler`에서 통일된 형식의 에러 메시지를 반환합니다.
