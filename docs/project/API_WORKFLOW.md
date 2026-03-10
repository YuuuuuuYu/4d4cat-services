# API 워크플로우 가이드

## 1. 개요

`4d4cat-services` 프로젝트는 마이크로서비스 지향의 멀티 모듈 아키텍처로 구성되어 있으며, 외부 데이터 수집, 가공, 저장 및 제공을 위한 효율적인 파이프라인을 구축하고 있습니다.

### 주요 모듈 구성
- **`core`**: 공통 도메인 엔티티, DTO, 예외 처리, Redis/Discord 인프라 설정 및 AOP 기능을 포함하는 핵심 라이브러리입니다.
- **`data`**: Pixabay 등 외부 API로부터 데이터를 수집하고 Redis에 동기화하는 스케줄러 및 수집기 역할을 수행합니다. (Producer)
- **`api`**: Redis 및 RDB(MySQL/H2)에 저장된 데이터를 클라이언트에게 REST API로 제공합니다. (Consumer)

---

## 2. 핵심 아키텍처 및 기술 스택

### 2.1. 데이터 저장소 전략
- **Redis (In-memory)**: Pixabay 비디오/음악 데이터와 같이 빠른 무작위 액세스가 필요한 데이터를 `Set` 구조로 저장하여 높은 성능을 보장합니다.
- **RDB (MySQL/H2)**: OmniWatch 도메인의 브랜드, 소재, 태그 등 정형화된 메타데이터를 저장하며 JPA/Hibernate를 통해 관리합니다.

### 2.2. 성능 최적화: 가상 스레드 (Virtual Threads)
- Java 21의 가상 스레드를 적극 활용하여 I/O 차단(Blocking) 구간에서의 처리량을 극대화합니다.
- `data` 모듈의 데이터 수집 시 병렬 HTTP 호출 및 `api` 모듈의 비동기 Discord 알림 전송에 적용되어 있습니다.

### 2.3. 전역 관심사 (Cross-cutting Concerns)
- **`@NotifyDiscord` (AOP)**: 주요 작업(데이터 수집, 메시지 저장 등)의 성공/실패 여부를 Discord 웹훅으로 실시간 전송합니다.
- **모니터링**: Micrometer를 통해 Prometheus 메트릭을 노출하며, 에러 발생 빈도와 작업 소요 시간을 추적합니다.
- **로그**: CloudWatch Logs Appender를 통해 운영 환경의 로그를 중앙 집중화합니다.

---

## 3. 도메인별 워크플로우

### 3.1. Pixabay 데이터 수집 및 제공 (Data Domain)

#### 3.1.1. 데이터 수집 프로세스 (data 모듈)
`data` 서버는 시작 시 또는 매일 새벽 3시에 데이터를 수집하여 Redis를 최신 상태로 유지합니다.

```text
[ 스케줄러 / 시작 이벤트 ]
          |
          v
[ 가상 스레드(Virtual Thread) 실행 ]
          |
          v
[ PixabayDataCollector: 필터 정의 ]
          |
          v
[ Staggered Parallel Fetch (1초 간격) ]
          |
          v
[ Pixabay API 호출 (외부 서비스) ]
          |
          v
[ 데이터 결합 및 가공 ]
          |
          v
[ Redis Pipelined sAdd (벌크 저장) ]
          |
          v
[ @NotifyDiscord: 작업 결과 알림 ]
```

- **Staggered Parallel Fetch**: 외부 API의 Rate Limit을 준수하기 위해 1초 간격으로 가상 스레드 작업을 순차적으로 제출(Submit)합니다.
- **Redis Pipelining**: 대량의 데이터를 저장할 때 네트워크 왕복 시간을 줄이기 위해 파이프라인을 사용합니다.

#### 3.1.2. 데이터 제공 프로세스 (api 모듈)
클라이언트가 데이터를 요청하면 Redis에서 무작위로 요소를 선택하여 반환합니다.

```text
[ Client ] ----( GET /video or /music )----> [ Controller ]
                                                 |
                                                 v
[ Redis ] <---( SrandomMember )--- [ RedisDataStorage ] <--- [ Service ]
    |                                            |              |
    +-----------( Optional<T> )------------------+              |
                                                                v
[ Client ] <---( 200 OK: BaseResponse )------------------- [ DTO 변환 ]
```

---

### 3.2. 메시지 도메인 (Message Domain)

사용자의 간단한 텍스트 메시지를 검증하고 Redis에 최신 상태로 유지합니다.

```text
[ 클라이언트: POST /message ]
           |
           v
[ MessageValidator (검증) ] --( 실패 )--> [ 400 Bad Request ]
           |
           +----( 성공 )----> [ Redis 저장 (SET) ]
                                    |
                                    v
[ Discord 알림 (AOP) ] <--- [ @NotifyDiscord ]
                                    |
                                    v
[ 클라이언트 ] <-----( 200 OK: Response )-----+
```

- **저장 (POST)**: 한글 2자, 영문/숫자 1자로 계산하여 최대 30자까지 허용하며, 특수문자를 제한하는 `MessageValidator`를 거칩니다.
- **알림**: 메시지 저장 성공 시 `@NotifyDiscord`를 통해 작업 로그가 Discord로 전송됩니다.

---

### 3.3. OmniWatch 도메인 (JPA Domain)

시계 정보(Watch)를 관리하는 도메인으로, 슬러그(Slug) 기반 조회 및 연관된 브랜드, 소재, 작업물 정보를 관리합니다.

```text
[ Client (GET /watch/{slug}) ] ----> [ Controller ]
                                         |
                                         v
[ DB (MySQL/H2) ] <---( Query )--- [ JPA Repository ] <--- [ Service ]
        |                                 |                   |
        +----( Brand/Material/Tag )-------+                   |
                                                              v
[ Client ] <---( 200 OK: DTO )-------------------------- [ DTO 변환 ]
```

- **JPA Auditing**: 모든 엔티티의 생성/수정 시간을 자동으로 기록합니다.
- **연관 관계**: 시계 하나에 대해 다수의 태그, 소재, 작업물이 1:N 관계로 매핑되어 복합적인 정보를 구성합니다.

---

## 4. 예외 처리 가이드

`GlobalExceptionHandler`는 모든 커스텀 예외를 감지하여 표준 응답 형식을 생성합니다.

```text
[ 예외 발생 (40x, 50x) ]
           |
           v
[ GlobalExceptionHandler ]
           |
           +----[ messages.yml: 에러 메시지 매핑 ]
           |
           +----[ Prometheus: 에러 메트릭 카운팅 ]
           |
           v
[ BaseResponse (JSON 응답) ]
           |
           v
[ 클라이언트 (에러 코드 + 메시지) ]
```

- **HTTP Status 400**: 잘못된 요청 (메시지 유효성 실패 등)
- **HTTP Status 404**: 데이터 없음 (Redis 히트 실패 등)
- **HTTP Status 502**: 외부 서비스 오류 (Pixabay/Discord API 실패 등)

각 에러는 `messages.yml`에 정의된 다국어(기본 ko_KR) 메시지와 함께 응답되며, Prometheus 에러 카운터가 1 증가합니다.

```json
{
  "status": 404,
  "data": null,
  "error": {
    "code": "PV1000",
    "message": "비디오 데이터를 찾을 수 없습니다."
  },
  "timestamp": "2026-03-10T..."
}
```
