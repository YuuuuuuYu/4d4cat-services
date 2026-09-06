# API 및 배치 워크플로우 가이드 (API & Batch Workflows)

## 1. 개요 (Introduction)

`4d4cat-services` 프로젝트는 마이크로서비스 지향의 멀티 모듈 아키텍처로 구성되어 있으며, 외부 데이터 수집, 가공, 저장 및 제공을 위한 효율적인 파이프라인을 구축하고 있습니다.
본 가이드는 시스템을 구성하는 핵심 모듈들의 역할과 도메인별 데이터 흐름, 스케줄링 배치 작업, 그리고 보안 및 예외 처리 정책을 상세히 설명합니다.

### 1.1. 주요 모듈 구조 및 역할
- **`core`**: 공통 도메인 엔티티, Repository, DTO, 예외 처리, Redis/R2/PortOne/SendPulse 인프라 설정, AOP 기능을 포함하는 핵심 라이브러리입니다.
- **`data`**: 외부 API 및 RSS 피드 수집, 캐시 갱신, 통계 집계 및 구독 갱신/만료 스케줄링 등을 전담하는 배치/워커 모듈입니다. (Producer/Worker)
- **`api`**: RDB(PostgreSQL/H2)와 Redis에 저장된 데이터를 바탕으로, 클라이언트에게 인가 권한에 따른 REST API를 제공합니다. (Consumer)
- **`ApplyDays` (Frontend)**: React, Vite, Tailwind CSS v4 기반의 UI 웹 애플리케이션으로, 이미지 클라이언트단 최적화 및 API/R2 연동을 처리합니다.

---

## 2. 핵심 아키텍처 및 전역 관심사

### 2.1. 데이터 저장소 및 캐싱 전략
- **Redis (In-memory)**:
  - Pixabay 비디오/음악 데이터와 같이 임의 접근이 잦은 데이터를 `Set` 구조로 저장하여 고성능을 보장합니다.
  - 조회 성능 극대화를 위해 API 모듈의 기술 블로그 목록(`techblog:list:*`), 회사 정보 캐시(`companyList`, `companySearch`, `companySummary`, `publicSummary`) 등을 Redis 캐시 매니저를 통해 관리합니다.
  - 분산 환경에서의 스케줄러 중복 실행 방지를 위해 Redis 분산 락(Distributed Lock)을 적용합니다.
- **RDB (PostgreSQL 17.9 / H2)**:
  - 사용자, 지원 내역, 브랜드, 소재, 카테고리, 통계 데이터 등 정형화된 관계형 메타데이터를 저장하며 JPA/Hibernate를 통해 관리합니다.
  - 대량 통계 집계 성능 향상을 위해 PostgreSQL 17.9의 `JSON_TABLE` 함수를 활용한 Native Query를 적극 사용합니다.

### 2.2. 성능 최적화: 가상 스레드 (Virtual Threads)
- Java 21의 가상 스레드를 적극 활용하여 I/O 차단(Blocking) 구간에서의 처리량을 극대화합니다.
- **적용처**:
  - `data` 모듈: 기술 블로그 RSS 피드 병렬 수집 (`Executors.newVirtualThreadPerTaskExecutor()`).
  - `api` 모듈: 어드민의 지원서 벌크 승인/거절 처리 비동기 처리기 (`dbTaskExecutor`).
  - 공통: Discord 알림 웹훅 전송 시 비동기 스레드 실행.

### 2.3. 전역 보안 및 인가 권한 체계 (Role Hierarchy)
시스템은 사용자의 인증 상태에 따라 권한 수준을 분류하고 API 데이터 노출 범위를 차등 제어합니다.
- **ANONYMOUS (미인증)**:
  - 기업 목록 조회 시 고스트 정보 마스킹 (`ghostingCount`, `ghostingRate`, `avgResponseTime`을 `null`로 반환).
  - 기업 요약 통계 조회 시 전체 통계만 노출하며, 직무별(L1/L2) 세부 데이터 및 타임라인 접근은 차단됩니다.
- **USER (일반 회원)**:
  - 기업 목록의 무통보 관련 상세 수치를 조회할 수 있습니다.
  - 기업 요약 통계에서 전체 통계와 함께 직무 대분류 통계(L1)까지 조회 가능합니다.
- **REVIEWER (인증 회원)**:
  - 1건 이상의 지원서가 승인 완료(`APPROVED`)된 회원은 자동으로 권한이 승격됩니다.
  - 기업 요약 통계에서 대분류(L1) 및 중분류 통계(L2)까지 접근할 수 있습니다.
- **SUBSCRIBER / ADMIN (프리미엄 구독자 및 관리자)**:
  - 가장 상위 권한으로, 기업 타임라인 상세 이력(`/companies/{slug}/timeline`) 및 세부 전형 리스트 조회(`/companies/{slug}/details`) 등의 프리미엄 데이터에 완전한 접근 권한을 획득합니다.

### 2.4. 전역 관심사 (Cross-cutting Concerns)
- **`@NotifyDiscord` (AOP)**: 주요 작업의 성공/실패 여부를 Discord 웹훅으로 실시간 전송합니다.
  - **채널 분리 규칙**:
    - 데이터 수집 및 연동 작업: `DiscordChannel.DATA`
    - 시스템 상태 체크, 이미지 클린업, 만료 처리 등 헬스케어 작업: `DiscordChannel.MONITORING`
    - 통계 집계 배치 및 플랫폼 일간/주간 통계: `DiscordChannel.STATISTICS`
    - 일반 비즈니스 이벤트/일반 에러: `DiscordChannel.DEFAULT`
- **모니터링**: Micrometer 및 Prometheus 메트릭을 연동하여 작업 소요 시간(`task.execution.duration`), 실행 횟수(`task.execution.total`), 지원서 등록/조회/승인 카운터 등을 수집합니다.
- **에러 핸들링 & 다국어**: `GlobalExceptionHandler`와 `messages.yml`을 결합하여 표준화된 JSON 에러 응답(`BaseResponse`) 구조를 생성합니다.

---

## 3. 도메인별 상세 워크플로우

### 3.1. Pixabay 데이터 파이프라인 (Data Domain)

#### 3.1.1. 데이터 수집 프로세스 (data 모듈)
`data` 서버는 매일 새벽 3시에 스케줄러를 통해 데이터를 수집하여 Redis를 최신 상태로 유지합니다.

```text
[ 스케줄러 (매일 새벽 3시) ]
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
[ @NotifyDiscord (AOP): 작업 결과 알림 (DATA 채널) ]
```
- **Staggered Parallel Fetch**: 외부 API의 Rate Limit(요청 제한)을 방어하기 위해 1초 간격으로 가상 스레드 작업을 순차 제출(Submit)합니다.
- **Redis Pipelining**: 대량의 데이터를 저장할 때 네트워크 왕복 시간(RTT)을 최소화하기 위해 파이프라인을 사용합니다.

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

### 3.2. 기술 블로그 RSS 수집 및 제공 (Tech Blog Domain)

#### 3.2.1. 기술 블로그 RSS 피드 수집 (data 모듈)
스케줄러(`TechBlogDataScheduler`)가 주기적으로 실행되어 가상 스레드를 기반으로 RSS 피드를 병렬 수집합니다.

```text
[ 스케줄러 (8시, 13시, 22시) ]
           |
           v
[ 가상 스레드 병렬 실행 ]
           |
           v
[ TechBlogRssCollector ] --( Company 피드 URL 조회 )--> [ RDB ]
           |
           +---> [ RSS 피드 요청 (HTTP GET) ]
           |
           +---> [ XML 파이프라인 (Rome SyndFeed) ]
           |     (XML 유효하지 않은 특수 문자 제거 처리)
           |
           v
[ 신규 포스트 등록 (TechBlogPost & Tag 저장) ]
           |
           v
[ 기존 RSS 피드에 더이상 없는 미노출 포스트 삭제 ]
           |
           v
[ Redis 캐시 일괄 삭제 (techblog:list:* 무효화) ]
           |
           v
[ @NotifyDiscord (AOP): 수집 결과 전송 (DATA 채널) ]
```
- **병렬 수집**: 가상 스레드 풀(`newVirtualThreadPerTaskExecutor`)을 활용하여 각 기업의 피드를 동시에 fetch하고 파싱하므로 속도가 매우 빠릅니다.
- **물리 삭제 스케줄러**: 매일 새벽 12시 5분에 물리적으로 삭제 처리된 포스트들을 일괄 정밀 정리합니다.

#### 3.2.2. 기술 블로그 조회 (api 모듈)
클라이언트가 기술 블로그를 조회할 때 최적의 속도를 내기 위해 캐싱과 페이지네이션을 사용합니다.
- **커서 기반 페이지네이션 (No-offset)**: 대량 데이터 조회 시의 인덱싱 효율을 위해 `cursorId`를 이용한 페이징을 지원합니다.
- **Redis 캐싱**: 쿼리 및 페이지 파라미터별 조회 결과는 `techblog:list:...` 키로 Redis에 12시간 동안 캐싱됩니다.

---

### 3.3. ApplyDays 지원 및 검증 파이프라인 (Candidate Verification)

사용자가 지원 이력(Application)을 등록하고 증빙 서류 이미지를 제출하면, 관리자의 검증을 거쳐 권한이 부여되는 핵심 도메인입니다.

```text
[ 사용자: 지원서 등록 ]
       |
       v
[ API: 회사 매칭 및 전형 날짜 분석 ]
       |---> DB에 회사 미존재 시 'SUGGESTED' 상태로 임시 생성 (회사 제안 메커니즘)
       |---> 전형 단계별 날짜간 차이(appliedAt 대비)를 계산하여 duration_days 파싱
       |
       v
[ API: PENDING 상태의 VerificationRequest 자동 생성 ]
       |
       +---> [ 사용자: Presigned URL 요청 ]
       |            |
       |            v
       |     [ API: 이미지 수 검증 (최대 10장 한도) ]
       |            |
       |            v
       |     [ API: Presigned URL 발급 및 VerificationImage 엔티티 임시 저장 ]
       |            |
       |            v
       |     [ Client: 브라우저에서 WebP 최적화 & 리사이징 수행 후 R2로 직접 업로드 ]
       |
       v
[ 어드민: 승인(Approve) 또는 거절(Reject) 처리 ]
       |
       v  (비동기 가상 스레드 트랜잭션 격리 처리 - AdminApplyDaysCommandService)
       |
       +----( 승인 시 )----> [ API: 지원서 APPROVED 업데이트 및 10자리 랜덤 비밀번호 생성 ]
       |                     [ API: 회원 권한 검증 -> ROLE_USER인 경우 ROLE_REVIEWER 승격 ]
       |                     [ API: DB 내 NotificationQueue에 메일 발송 건 PENDING 적재 ]
       |
       +----( 거절 시 )----> [ API: 지원서 REJECTED 업데이트 및 거절 사유 기록 ]
                             [ API: DB 내 NotificationQueue에 메일 발송 건 PENDING 적재 ]
```

- **클라이언트 측 이미지 최적화**: 서버 자원 소모 방지 및 네트워크 업로드 병목 해결을 위해 사용자의 브라우저에서 WebP 변환 및 1024px 리사이징을 선행한 후 R2로 다이렉트 업로드합니다.
- **비동기 트랜잭션 처리**: 어드민의 승인/거절(특히 벌크 처리)은 API 메인 스레드를 블로킹하지 않도록 가상 스레드 풀(`dbTaskExecutor`) 내부에서 비동기적으로 트랜잭션을 획득하여 병렬 수행됩니다.
- **이미지 자동 정리 (Cleanup)**: 삭제된 지원서에 매핑된 인증용 이미지는 매일 새벽 3시 `VerificationImageCleanupScheduler`에 의해 Cloudflare R2에서 삭제된 후, DB에서 Hard-Delete 배치 처리됩니다.

---

### 3.4. 구독 및 정기 결제 파이프라인 (Subscription & Billing)

사용자에게 프리미엄 권한을 부여하고 포트원(PortOne) 및 이메일(SendPulse) 연동을 통해 자동 갱신을 수행하는 파이프라인입니다.

```text
[ 사용자: PG 결제창 완료 ]
       |
       +---( 방법 A: Client -> API /pay 호출 )
       |
       +---( 방법 B: PortOne Webhook 수신 )
       |
       v
[ API: 결제 검증 및 활성화 (ApplyDaysSubscriptionCommandService) ]
       |
       +---> [ 1. 멱등성 검사 ] : paymentRepository 중복 확인
       |
       +---> [ 2. 트랜잭션 외부 API 검증 ] : PortOne API 호출로 위변조 검증 (DB 풀 보호)
       |
       +---> [ 3. 비관적 락 하에서 최종 멱등성 재검증 및 결제 정보 기록 ]
       |
       +---> [ 4. 구독 활성화 ] : ACTIVE 상태 변경, 시작/종료일 및 차기 결제일 설정
       |
       +---> [ 5. 권한 승격 ] : 회원 권한을 ROLE_SUBSCRIBER로 변경 (ADMIN 제외)
       |
       +---> [ 6. 알림 이벤트 발행 ] : 커밋 이후 비동기로 결제 성공 이메일/Discord 알림 발송
```

#### 3.4.1. 정기 결제 및 만료 스케줄링 워커 (Renewal & Expiration Batch)
매일 00:00:10에 `ApplyDaysSubscriptionScheduler`가 기동되어 대상자들을 처리합니다.

```text
[ 스케줄러 (매일 00:00:10) ]
           |
           v
[ Redis 분산 락 획득 (lock:subscription-billing) ]
           |
           v
[ Batch Processing (OOM 방지 및 트랜잭션 격리 구조) ]
           |
           +---> [ 1. 갱신 결제 대상 조회 (ACTIVE 이면서 nextBillingDate <= now) ]
           |     - Pageable Slice 단위로 조회하여 OOM 방지
           |     - 각 구독별로 PortOne payWithBillingKey API 호출 (트랜잭션 외부)
           |     - REQUIRES_NEW 격리 트랜잭션에서 결제 완료 및 정보 업데이트
           |     - 갱신 실패 시 -> 강등(Demotion) 및 실패 이메일 발송
           |
           +---> [ 2. 만료 대상 조회 (CANCELED 이면서 endDate < today) ]
                 - Pageable Slice 단위로 조회하여 OOM 방지
                 - REQUIRES_NEW 격리 트랜잭션에서 EXPIRED 처리
                 - 회원 권한 강등(Demotion) 및 만료 이메일 발송
           |
           v
[ Redis 분산 락 해제 ]
```
- **Transaction-After-API 패턴**: 외부 PG사 API 통신은 트랜잭션 외부에서 수행하고 DB 쓰기 작업만 트랜잭션 내부에서 수행하여, 외부 통신 지연으로 인해 DB 커넥션 풀이 마르는 문제를 원천 방어합니다.
- **역할 강등 제어 (Demote Role)**: 구독 만료/실패 시 회원의 권한을 강등합니다. 단, 사용자가 이전에 검증 완료(`APPROVED`)된 지원 내역이 1건이라도 존재하면 `ROLE_REVIEWER` 등급을 부여하고, 없을 경우에만 최하위 등급인 `ROLE_USER`로 강등합니다.

---

### 3.5. 통계 집계 배치 파이프라인 (Statistics Aggregation)

사용자들의 전형 정보를 집계하여 캐싱 통계 테이블에 적재하는 무거운 배치 작업입니다.

```text
[ 스케줄러 (매일 새벽 1시 10분) ]
           |
           v
[ 캐시 일괄 삭제 (companyList, companySearch, companySummary, publicSummary) ]
           |
           v
[ Spring Batch: applyDaysAggregationJob 실행 ]
           |
           v
[ Native Query: PostgreSQL 17.9 JSON_TABLE 집계 ]
           |  - application 테이블의 hiring_process (JSON) 데이터를 관계형 형태로 전개
           |  - 기업 전체(COMPANY), 대분류(CAT_L1), 중분류(CAT_L2) 레벨별 집계 데이터 생성
           |  - 총 리뷰 수, 무통보 수, 각 전형 단계별 평균 소요 일수(avg) 및 횟수(count)
           |
           v
[ apply_days_statistics 테이블 INSERT ... ON CONFLICT DO UPDATE ]
           |
           v
[ @NotifyDiscord (AOP): 집계 완료 통보 (STATISTICS 채널) ]
```

---

### 3.6. 알림 및 플랫폼 모니터링 (Notifications)

#### 3.6.1. 비동기 메일 발송 워커 (SendPulse Worker)
어드민이 지원서 검증 결과를 처리할 때 메일은 즉시 발송되지 않고, DB `NotificationQueue`에 적재됩니다.
- 스케줄러가 대기 중인(`PENDING`) 알림 큐 데이터를 가져와 **사용자(Member)별로 그룹화**합니다.
- SendPulse API의 Rate Limit을 준수하기 위해 **Resilience4j의 RateLimiter**를 거쳐 비동기적으로 한 명의 사용자에게 1개의 통합 결과 메일로 번들링하여 발송합니다.

#### 3.6.2. 일간/주간 플랫폼 통계 보고 (Platform Statistics Bot)
`RegistrationStatisticsScheduler`가 주기적으로 동작하여 플랫폼 활성 지표를 집계하고 디코드로 보고합니다.
- **일간 통계**: 매일 새벽 1시 (KST), 전날 하루 동안 가입한 신규 회원 수 및 유입 채널별 신규 지원서 등록 수를 Discord `STATISTICS` 채널에 전송합니다.
- **주간 통계**: 매주 월요일 오전 7시 (KST), 지난 일주일간의 동일 데이터를 누적 집계하여 시각화된 임베드 형태로 전송합니다.

---

## 4. 예외 처리 가이드 (Exception Handling)

시스템에서 정의된 모든 비즈니스 예외와 인프라 예외는 `GlobalExceptionHandler`에 의해 제어되며 다국어 메시지와 매핑되어 클라이언트에 반환됩니다.

```json
{
  "status": 400,
  "data": null,
  "error": {
    "code": "PV1000",
    "message": "비디오 데이터를 찾을 수 없습니다."
  },
  "timestamp": "2026-07-08T16:11:00.123456"
}
```

### 주요 HTTP 상태 코드 및 대응 에러 분류
- **400 Bad Request**:
  - `INVALID_REQUEST`: 유효하지 않은 입력 데이터 (형식 불일치 등)
  - `ALREADY_SUBSCRIBED`: 이미 구독 중인 상태에서 재구독 요청
  - `VERIFICATION_IMAGE_LIMIT_EXCEEDED`: 지원서당 등록 가능한 이미지 한도(10장) 초과
- **401 Unauthorized**:
  - `UNAUTHORIZED`: 인증 토큰 누락 또는 유효하지 않은 세션
- **403 Forbidden**:
  - `FORBIDDEN` / `UNAUTHORIZED_APPLICATION_ACCESS`: 본인 소유가 아닌 지원서 정보에 접근하거나 권한 수준(Role Hierarchy)을 위반하는 요청
- **404 Not Found**:
  - `USER_NOT_FOUND`: 가입되지 않은 이메일로 요청 시
  - `COMPANY_NOT_FOUND`: 존재하지 않는 기업 슬러그 조회 시
  - `SUBSCRIPTION_PLAN_NOT_FOUND`: 유효하지 않은 구독 상품 ID 지정 시
- **502 Bad Gateway**:
  - `RSS_FETCH_FAILED`: 외부 기술 블로그 피드 수신 지연/오류
  - `PAYMENT_FAILED`: PortOne API 호출 검증 실패 혹은 결제 금액 불일치
