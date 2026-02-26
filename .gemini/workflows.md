# Domain Workflows

## Data Server 워크플로우

### Pixabay 데이터 수집 (PixabayDataScheduler)
```
[Server Startup] → @PostConstruct
                        ↓
              PixabayDataScheduler.initializeData()
                        ↓
        ┌───────────────┴───────────────┐
        ▼                               ▼
PixabayVideoCollector          PixabayMusicCollector
@NotifyDiscord("Pixabay 비디오 수집")  @NotifyDiscord("Pixabay 음악 수집")
        │                               │
        ↓ (AOP Intercept)               ↓ (AOP Intercept)
DiscordNotifierAspect          DiscordNotifierAspect
        │                               │
        ▼                               ▼
CompletableFuture 병렬 처리    CompletableFuture 병렬 처리
(20개 카테고리)               (32개 장르)
        │                               │
        ▼                               ▼
   Pixabay API                     Pixabay API
        │                               │
        ▼                               ▼
RedisDataStorage.setListData()  RedisDataStorage.setListData()
        │                               │
        ▼                               ▼
DataCollectionResult 반환      DataCollectionResult 반환
        │                               │
        ▼                               ▼
Discord 성공 알림 전송         Discord 성공 알림 전송
(비동기, Virtual Thread)       (비동기, Virtual Thread)
        │                               │
        └───────────────┬───────────────┘
                        ▼
                     [Redis]
```

### 스케줄링 전략
- `@PostConstruct`: 서버 시작 시 즉시 수집
- `@Scheduled(cron = "0 0 3 * * *")`: 매일 새벽 3시
- `@Scheduled(fixedRate = 21600000)`: 6시간마다 갱신

### 처리 과정
1. 스케줄러가 Collector의 `collectAndStore()` 호출
2. **AOP Intercept**: `@NotifyDiscord` 어노테이션 감지, 시작 로깅
3. Collector가 필터별로 병렬 API 호출 (Virtual Thread Pool)
   - 각 필터는 독립적으로 실행
   - 실패 시 retry 없이 즉시 Optional.empty() 반환
   - 개별 실패가 전체 수집에 영향 주지 않음
4. 응답 데이터를 DTO로 변환
5. `RedisDataStorage`를 통해 Redis에 저장
6. **AOP After**: `DataCollectionResult` 반환값 기반으로 Discord 알림 전송
   - 총 아이템 수, 성공/실패 필터 수, 소요 시간 포함
   - Virtual Thread로 비동기 전송 (메인 워크플로우 지연 없음)

## API Server 워크플로우

### Pixabay 도메인

#### 랜덤 비디오 조회 (GET /video)
```
[Client] → [PixabayController] → [PixabayService]
                                       ↓
                    RedisDataStorage.getRandomElement()
                                       ↓
                                   [Redis]
                                       ↓
[Client] ← [BaseResponse] ← [PixabayVideoResult]
```

#### 랜덤 음악 조회 (GET /music)
```
[Client] → [PixabayController] → [PixabayService]
                                       ↓
                    RedisDataStorage.getRandomElement()
                                       ↓
                                   [Redis]
                                       ↓
[Client] ← [BaseResponse] ← [PixabayMusicResult]
```

### Message 도메인

#### 메시지 저장 (POST /message)
```
[Client] → [MessageController] → [MessageService]
                                       ↓
                         1. MessageValidator.isValid()
                         2. RedisMessageStorage.saveMessage()
                                       ↓
                                   [Redis]
                                       ↓
                    @NotifyDiscord → Discord 알림
                                       ↓
[Client] ← [200 OK]
```

#### 메시지 조회 (GET /message)
```
[Client] → [MessageController] → [MessageService]
                                       ↓
                    RedisMessageStorage.getMessage()
                                       ↓
                                   [Redis]
                                       ↓
[Client] ← [String content]
```

### 특징
- Redis 기반 저장소 (서버 간 동기화)
- 항상 마지막 메시지만 유지
- 저장 시 Discord 웹훅 알림

## 공통 워크플로우

### 전역 예외 처리 (GlobalExceptionHandler, api-server)
```
[Any Layer] → 예외 발생
                 ↓
       GlobalExceptionHandler
                 ↓
       1. ErrorCode 추출
       2. MessageSource로 메시지 조회
       3. BaseResponse 생성
                 ↓
[Client] ← JSON 에러 응답
```

### AOP Discord 알림 (core 모듈 - 공통)
```
@NotifyDiscord 어노테이션 메서드 실행
                 ↓
       DiscordNotifierAspect.notifyEvent() (core 모듈)
                 ↓
       1. 실행 시간 측정 시작
       2. 메서드 실행 (joinPoint.proceed())
       3. 결과에 따라 성공/실패 메시지 생성
          - DataCollectionResult: 통계 정보 포함
          - 일반 메서드: 실행 시간만 포함
       4. DiscordWebhookService.sendMessageAsync()
                 ↓
       [Discord Webhook] (비동기, Virtual Thread)
```

**사용 위치:**
- **data-server**: 데이터 수집 완료 시 (PixabayVideoCollector, PixabayMusicCollector)
- **api-server**: 메시지 저장 시 (MessageService)

### Redis 데이터 저장소 (core)
```java
// 리스트 데이터 저장 (data-server)
RedisDataStorage.setListData("pixabayVideos", videoList)

// 랜덤 요소 조회 (api-server)
RedisDataStorage.getRandomElement("pixabayVideos", PixabayVideoResult.class, ErrorCode)

// 메시지 저장/조회
RedisMessageStorage.saveMessage("Hello")
RedisMessageStorage.getMessage() → Optional<String>
```
