# Architecture & Design Patterns

## 멀티모듈 아키텍처

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│ 외부 API     │────▶│ Data Server │────▶│    Redis    │
└─────────────┘     │  (수집)      │     │  (공통 저장소)│
                    └─────────────┘     └──────┬──────┘
                                               │
                    ┌─────────────┐            │
                    │ API Server  │◀───────────┘
                    │ (제공)       │  조회/저장
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
monitoring-server ──▶ (metrics from all servers)
```

## 모듈별 역할

### core 모듈 (공통 라이브러리)
- 모든 모듈에서 사용하는 공통 인프라, 예외, DTO, 유틸리티를 포함합니다.
- **핵심 패키지:** `aop`, `config`, `dto`, `exception`, `infrastructure`, `notification`, `util`

### data-server 모듈
- 외부 데이터를 수집하여 Redis에 동기화하는 역할을 수행합니다.
- **상세 도메인:** `pixabay` ([상세 가이드](.gemini/domains/pixabay.md))

### api-server 모듈
- 클라이언트에게 데이터를 제공하고 비즈니스 요청을 처리합니다.
- **상세 도메인:** `pixabay`, `message` ([상세 가이드](.gemini/domains/message.md)), `omniwatch` ([상세 가이드](.gemini/domains/omniwatch.md))

### monitoring 모듈
- 시스템 전반의 상태를 시각화하고 메트릭을 수집합니다.
- **상세 가이드:** [Monitoring 가이드](.gemini/domains/monitoring.md)


## 주요 디자인 패턴

### 의존성 주입 (DI)
- Spring의 `@RequiredArgsConstructor`를 이용한 생성자 주입 방식을 원칙으로 합니다.

### 템플릿 메서드 패턴
- 반복되는 데이터 수집 및 저장 흐름을 공통화합니다.
- **구현 예시:** `PixabayDataCollector` (상세 내용은 `pixabay.md` 참조)

### Repository 패턴 (Infrastructure 추상화)
- Redis 등 외부 저장소 접근 로직을 `core` 모듈의 인프라 계층으로 추상화합니다.
- **주요 클래스:** `RedisDataStorage`, `RedisMessageStorage`

### 관점 지향 프로그래밍 (AOP)
- 횡단 관심사(Discord 알림, 로깅 등)를 `@NotifyDiscord` 어노테이션으로 처리합니다.
- **핵심 클래스:** `DiscordNotifierAspect`

### DTO 및 Record 패턴
- 데이터 전달 시 Java 21의 `record`를 사용하여 불변성을 보장합니다.

## 설계 원칙

### 불변성 (Immutability)
- `record` 타입 및 `final` 키워드 적극 활용.

### SOLID 원칙 준수
- **SRP:** 수집, 저장, 조회 책임을 명확히 분리.
- **OCP:** 신규 데이터 타입 추가 시 추상 클래스 확장을 통해 대응.

### Fail-Fast & Graceful Degradation
- 외부 API 호출 실패가 전체 시스템에 영향을 주지 않도록 개별 태스크 단위로 에러를 격리합니다.

## HTTP 클라이언트 및 동시성

### RestClient (Spring 6.1+)
- 동기식 코드 스타일로 직관적인 HTTP 통신을 수행합니다.

### 가상 스레드 (Java 21)
- `spring.threads.virtual.enabled=true` 설정을 통해 I/O 집약적 작업의 효율을 극대화합니다.
- 병렬 데이터 수집 및 비동기 알림 전송에 활용됩니다.
