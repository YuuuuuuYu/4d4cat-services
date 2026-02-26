# Architecture & Design Patterns

## 멀티모듈 아키텍처

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│ Pixabay API │────▶│ Data Server │────▶│    Redis    │
└─────────────┘     │  (1대)      │     │  (Docker)   │
                    └─────────────┘     └──────┬──────┘
                                               │
                    ┌─────────────┐            │
                    │ API Server  │◀───────────┘
                    │ (N대)       │  조회/저장
                    └──────┬──────┘
                           │ 메트릭 수집
                           ▼
              ┌───────────────────────────┐
              │ Monitoring Stack          │
              │ (Prometheus + Grafana)    │
              └───────────────────────────┘
```

### 모듈 의존성
```
api-server ──▶ core
data-server ──▶ core
monitoring-server ──▶ (metrics from api-server)
```

## 모듈별 계층 구조

### core 모듈 (공통 라이브러리)
```
com.services.core/
├── aop/             # AOP (Discord 알림 어노테이션 및 Aspect)
│   ├── NotifyDiscord.java          (어노테이션)
│   └── DiscordNotifierAspect.java  (AOP)
├── config/          # Redis 설정
├── dto/             # 공통 DTO (BaseResponse, ApiResponse)
├── exception/       # 공통 예외 클래스
├── infrastructure/  # Redis 저장소, ApiMetadata
├── message/         # 메시지 DTO, Validator
├── notification/    # 알림 관련
│   ├── DataCollectionResult.java   (데이터 수집 결과 DTO)
│   └── discord/     # Discord 웹훅
│       ├── DiscordWebhookService.java
│       ├── DiscordWebhookPayload.java
│       ├── Embed.java
│       └── Footer.java
├── pixabay/dto/     # Pixabay DTO
└── util/            # 유틸리티
```

### data-server 모듈
```
com.services.data/
├── config/          # RestClient 설정
├── pixabay/         # 데이터 수집기
│   ├── PixabayDataCollector.java    (추상 클래스)
│   ├── PixabayVideoCollector.java   (@NotifyDiscord 사용)
│   └── PixabayMusicCollector.java   (@NotifyDiscord 사용)
└── scheduler/       # 스케줄러
    └── PixabayDataScheduler.java
```

### api-server 모듈
```
com.services.api/
├── config/          # CORS, MessageSource 설정
├── message/         # Message API (@NotifyDiscord 사용)
├── omniwatch/       # JPA 엔티티
├── pixabay/         # Pixabay API
├── presentation/    # GlobalExceptionHandler
└── util/            # WebUtils
```

### monitoring 모듈 (모니터링 대시보드 및 수집기)
```
com.services.monitoring/
└── ...              # Prometheus metric 수집 및 Grafana 대시보드
```
`monitoring` 모듈은 애플리케이션의 메트릭을 수집하고 시각화하는 역할을 담당합니다. Prometheus와 Grafana를 활용하여 시스템 전반의 상태를 모니터링합니다.

**Prometheus/Grafana 통합:**
`api-server` 모듈은 `spring-boot-starter-actuator`와 `micrometer-registry-prometheus`를 통해 `/actuator/prometheus` 엔드포인트에 메트릭을 노출합니다. Prometheus는 이 엔드포인트를 주기적으로 스크랩하여 메트릭을 수집하고, Grafana는 Prometheus에 저장된 메트릭을 사용하여 시각화된 대시보드를 제공합니다.


## 주요 디자인 패턴

### 의존성 주입 (DI)
```java
@RequiredArgsConstructor  // 생성자 주입
public class PixabayController {
    private final PixabayService service;  // final 필드
}
```

### 템플릿 메서드 패턴 (data-server)
```java
// 추상 클래스에서 공통 로직 정의
public abstract class PixabayDataCollector<T, R> {
    public void collectAndStore() {
        List<T> dataList = fetchAllData();
        redisDataStorage.setListData(getStorageKey(), dataList);
    }

    protected abstract String getStorageKey();
    protected abstract List<String> getFilters();

    // API 호출 (단순화된 에러 처리)
    protected Optional<R> fetchDataForFilter(String filter) {
        try {
            String uri = buildUri(filter).toUriString();
            R result = restClient.get().uri(uri).retrieve().body(getResponseTypeReference());
            return Optional.ofNullable(result);
        } catch (Exception e) {
            log.error("Failed to fetch data for filter '{}'", filter, e);
            return Optional.empty(); // 실패 시 retry 없이 즉시 반환
        }
    }
}

// 하위 클래스에서 구체적인 구현
@Component
public class PixabayVideoCollector extends PixabayDataCollector<...> {
    @Override
    protected String getStorageKey() {
        return ApiMetadata.PIXABAY_VIDEOS.getKey();
    }
}
```

### Repository 패턴 (core)
```java
// Redis 저장소 추상화
@Component
public class RedisDataStorage {
    public <T> void setListData(String key, List<T> data) { ... }
    public <T> T getRandomElement(String key, Class<T> type, ErrorCode errorCode) { ... }
}

@Component
public class RedisMessageStorage {
    public void saveMessage(String content) { ... }
    public Optional<String> getMessage() { ... }
}
```

### 관점 지향 프로그래밍 (AOP, core 모듈)
```java
// core 모듈에서 제공하는 공통 AOP
@Aspect
@Component
public class DiscordNotifierAspect {
    @Around("@annotation(notifyDiscord)")
    public Object notifyEvent(ProceedingJoinPoint joinPoint, NotifyDiscord notifyDiscord) {
        // 메서드 실행 전: 시작 로깅, 시간 기록
        Object result = joinPoint.proceed();
        // 메서드 실행 후: Discord 알림 전송 (성공/실패)
        return result;
    }
}

// data-server에서 사용 예시
@Component
public class PixabayVideoCollector extends PixabayDataCollector {
    @Override
    @NotifyDiscord(taskName = "Pixabay 비디오 수집")
    public DataCollectionResult collectAndStore() {
        return super.collectAndStore();
    }
}

// api-server에서 사용 예시
@Service
public class MessageService {
    @NotifyDiscord(taskName = "메시지 저장")
    public void saveMessage(String content) {
        // ...
    }
}
```

### DTO 패턴
```java
// record 타입 사용 (불변)
public record PixabayVideoResult(
    Integer id,
    String pageURL,
    String tags
) implements Serializable {}
```

## 설계 원칙

### 불변성 (Immutability)
- `record` 타입 적극 활용
- `final` 필드 선언
- 불변 컬렉션 반환 (`List.of()`)

### SOLID 원칙
- **SRP:** 각 클래스는 하나의 책임만 (Collector는 수집만, Storage는 저장만)
- **OCP:** `PixabayDataCollector` 확장으로 새로운 데이터 타입 추가 가능
- **LSP:** 상위 타입을 하위 타입으로 대체 가능
- **ISP:** 클라이언트는 사용하지 않는 인터페이스에 의존하지 않음
- **DIP:** 구체적인 것이 아닌 추상화에 의존 (RedisDataStorage 인터페이스화 가능)

### Fail-Fast & Graceful Degradation
- 입력 검증은 빠르게 실패
- 외부 API 호출 실패는 우아하게 처리 (retry 없이 즉시 실패 처리)
- `CompletableFuture`를 통한 개별 실패 처리
- 개별 필터 실패가 전체 데이터 수집에 영향 주지 않음

## HTTP 클라이언트 및 동시성

### RestClient (Spring 6.1+)
모든 HTTP 통신에 `RestClient` 사용 (RestTemplate, WebClient 대체)
```java
// data-server: Pixabay API 호출
restClient.get().uri(uri).retrieve().body(responseType);

// api-server: Discord 웹훅 전송
restClient.post().contentType(MediaType.APPLICATION_JSON).body(payload).retrieve().toBodilessEntity();
```

### 가상 스레드 (Java 21)
`spring.threads.virtual.enabled=true` 설정으로 가상 스레드 활성화

```java
// data-server: 병렬 데이터 수집
try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
    List<CompletableFuture<Optional<R>>> futures = filters.stream()
        .map(filter -> CompletableFuture.supplyAsync(() -> fetchDataForFilter(filter), executor))
        .toList();
    // ...
}

// api-server: 비동기 Discord 알림 전송
Thread.startVirtualThread(() -> sendMessage(payload));
```

### 기존 방식 대비 장점
| 항목 | 이전 (WebClient/RestTemplate) | 현재 (RestClient + 가상스레드) |
|------|------------------------------|-------------------------------|
| 코드 스타일 | 리액티브/동기 혼재 | 동기식 통일 |
| 의존성 | webflux 필요 | 불필요 |
| 디버깅 | 어려움 | 쉬움 |
| 동시성 | 플랫폼 스레드풀 | 가상 스레드 (경량) |

## 데이터 흐름

### Data Server (데이터 수집)
```
[Pixabay API] → [PixabayDataCollector] → [RedisDataStorage] → [Redis]
```

### API Server (데이터 조회)
```
[Client] → [Controller] → [Service] → [RedisDataStorage] → [Redis]
                                            ↓
[Client] ← [BaseResponse] ← [Controller]
```

### Message 저장/조회
```
[Client] → [MessageController] → [MessageService] → [RedisMessageStorage] → [Redis]
```
