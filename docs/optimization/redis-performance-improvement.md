# Redis 성능 최적화 및 수집 로직 개선

본 문서는 2026년 2월 3일(커밋: `cf9deab`) 이후 진행된 Redis 관련 데이터 수집 및 조회 로직의 개선 사항을 분석한 결과입니다.

## 0. 요약
### 잘못된 설계 방향
- 기존의 랜덤 인덱스 기반 조회 방식은 Redis 자료구조 특성상 데이터 탐색 부하를 가중시킴
### Redis 구조 개선 (List → Set)
- 이를 해결하기 위해 조회 효율이 극대화된 Set 구조로 전환

## 1. 개요
기존 시스템에서 발생하던 두 가지 주요 문제를 해결했습니다.
- **수집 성능**: Pixabay Music 데이터 수집 시 100-130초 소요되던 시간을 **90-100초대로 단축**
- **조회 지연**: API 호출 시 간헐적으로 발생하던 **5초 이상의 지연 현상(Spike)을 제거**

---

## 2. Redis 데이터 조회 로직 개선 (조회 지연 해결)

### 개선 전 (List 구조 및 LINDEX 사용)
- **자료구조**: Redis `List` (`RPUSH`)
- **조회 방식**: Java에서 랜덤 인덱스를 생성한 후 Redis의 `LINDEX` 명령으로 조회
- **문제점**: `LINDEX`는 **O(N)** 의 시간 복잡도를 가집니다. 데이터가 많아질수록 특정 요소를 찾는 속도가 느려지며, 빈번한 호출 시 Redis 부하가 급증하여 간헐적인 5초 지연을 유발했습니다.
- **의도**: 현재 시간(Timestamp)을 이용한 랜덤 요소를 유지하려고 했는데, Redis의 특징을 제대로 활용하지 못했습니다.

```java
// [기존 코드: RandomUtils.java]
public static int generateRandomInt(int max) {
    long timestamp = System.currentTimeMillis();
    return (int) (timestamp % max);
}

// [기존 코드: RedisDataStorage_old.java]
public <T> Optional<T> getRandomElement(String key, Class<T> elementType) {
    Long size = redisTemplate.opsForList().size(key);
    if (size == null || size == 0) return Optional.empty();
    
    // O(N) 연산 발생: 리스트 크기가 커질수록 성능 저하 및 자원 경합 유발
    int randomIndex = RandomUtils.generateRandomInt(size.intValue());
    Object element = redisTemplate.opsForList().index(key, randomIndex);
    return Optional.ofNullable((T) element);
}
```

### 개선 후 (Set 구조 및 SRANDMEMBER 도입)
- **자료구조**: Redis `Set` (`SADD`)
- **조회 방식**: Redis 자체 명령인 **`SRANDMEMBER`** 를 사용하여 직접 랜덤 요소를 추출
- **개선 결과**: `SRANDMEMBER`는 **O(1)** 에 가까운 성능을 보장하며, Redis 내부에서 최적화된 방식으로 랜덤 샘플링을 수행합니다. 이로 인해 조회 지연이 완전히 사라졌습니다.

```java
// [개선 코드: RedisDataStorage.java]
public <T> Optional<T> getRandomElement(String key, Class<T> elementType) {
    // O(1) 연산: Redis 엔진 레벨에서 직접 랜덤 멤버 반환 (초고속)
    Object element = redisTemplate.opsForSet().randomMember(key);
    
    // 모니터링 메트릭 추가 (Observability 강화)
    String status = (element == null) ? "miss" : "hit";
    registry.counter("redis.random.access", "key", key, "status", status).increment();
    
    return Optional.ofNullable((T) element);
}
```

---

## 3. 데이터 수집 성능 개선 (수집 시간 40초 단축)

수집 성능 향상은 **실행 시점 최적화**, **가상 스레드 활용**, 그리고 **자료구조 효율화**의 결과입니다.

### 3.1. 실행 시점 및 스레드 모델 전환

#### 개선 전: @PostConstruct + 동기 방식
- **문제점**: Spring Bean 초기화 단계에서 실행되어 애플리케이션 시작을 차단(Blocking)하고, 초기화 자원 경합을 유발했습니다.

```java
// [기존 코드: PixabayDataScheduler_old.java]
@PostConstruct
public void initializeData() {
    log.info("=== Starting initial data collection ===");
    collectAllData(); // 메인 스레드 점유, 동기 실행 (130초 소요)
    log.info("=== Initial data collection completed ===");
}

@Scheduled(fixedRate = 21600000, initialDelay = 21600000) // 불필요한 주기적 수집 존재
public void scheduledPeriodicCollection() { collectAllData(); }
```

#### 개선 후: ApplicationReadyEvent + 가상 스레드 비동기 방식
- **개선점**: 서버가 모든 설정을 마치고 안정화된(`Ready`) 시점에 **가상 스레드**를 생성하여 백그라운드에서 실행합니다. I/O 대기 효율이 극대화되었습니다.

```java
// [개선 코드: PixabayDataScheduler.java]
@EventListener(ApplicationReadyEvent.class)
public void initializeData() {
    // 가상 스레드에서 비동기로 실행하여 시작 시점 부하 제거 및 병렬 처리 극대화
    Thread.startVirtualThread(() -> {
        log.info("=== Starting initial data collection in virtual thread ===");
        collectAllData(); // 90초대로 단축
    });
}
// 불필요한 fixedRate 스케줄 삭제로 자원 낭비 제거
```

### 3.2. Redis 저장 자료구조 최적화

#### 개선 전: List (RPUSH)
- 순서를 유지해야 하므로 내부 포인터 처리가 추가되며, 중복 데이터 유입 시 별도의 처리가 필요했습니다.

```java
// [기존 코드: RedisDataStorage_old.java]
private <T> RedisCallback<Object> createPipelineCallback(String key, List<T> data) {
    return connection -> {
        // ...
        for (T item : data) {
            connection.rPush(keyBytes, valueBytes); // List 추가 연산
        }
        return null;
    };
}
```

#### 개선 후: Set (SADD)
- 해시 테이블 기반으로 대량 데이터 쓰기(`Pipelining`)가 더 효율적이며, Redis 레벨에서 자동으로 중복을 제거합니다.

```java
// [개선 코드: RedisDataStorage.java]
private <T> RedisCallback<Object> createPipelineCallback(String key, Collection<T> data) {
    return connection -> {
        // ...
        for (T item : data) {
            connection.sAdd(keyBytes, valueBytes); // Set 추가 연산 (중복 자동 제거)
        }
        return null;
    };
}
```

---

## 4. 기타 개선 사항 (모니터링 및 유연성)
- **메트릭 강화**: `Micrometer`를 도입하여 Redis 파이프라인 지속 시간(`redis.pipeline.duration`)을 측정합니다.
- **코드 범용성**: `setListData` (List 전용)에서 `setData` (Collection 범용)로 리팩토링하여 다양한 자료구조 대응이 가능해졌습니다.

---

## 5. 결론
이번 개선을 통해 **Redis 자료구조의 최적화(List -> Set)** 와 **가상 스레드 기반의 비동기 수집 모델**이 시스템 안정성에 결정적인 영향을 미쳤음을 확인했습니다.
- **조회**: O(N) → O(1) 성능 개선으로 간헐적 지연 제거.
- **수집**: 병렬 I/O 활용으로 시간 30% 단축 및 애플리케이션 기동성 향상.
