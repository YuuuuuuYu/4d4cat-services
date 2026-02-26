# Redis 랜덤 데이터 조회 방식 분석

> 작성일: 2026-02-10
> 분석 대상: Pixabay 데이터 (music, video) Redis 조회 로직

---

## 1. Redis 용도에 맞게 잘 설계했는가? (캐싱)

### 현재 설계 목적
- **외부 API 호출 최소화**: Pixabay API rate limit 회피 및 응답 속도 향상
- **데이터 사전 수집**: 스케줄러가 주기적으로 데이터를 수집하여 Redis에 저장
- **빠른 랜덤 조회**: API 요청 시 Redis에서 즉시 랜덤 데이터 반환

### 평가: **적절함**

| 평가 항목 | 결과 | 설명 |
|----------|------|------|
| 캐싱 목적 | ✅ | 외부 API 호출을 줄이고 응답 속도 향상 |
| 데이터 특성 | ✅ | 자주 읽고, 드물게 쓰는 패턴에 적합 |
| TTL 미설정 | ⚠️ | 스케줄러가 매일 갱신하므로 문제없으나, 명시적 TTL 권장 |
| 메모리 효율 | ✅ | JSON 직렬화로 구조화된 데이터 저장 |

### 데이터 흐름
```
[Pixabay API] → [Scheduler: 매일 03:00] → [Redis List] → [API 요청] → [랜덤 응답]
```

---

## 2. 지금 로직이 최적화 되어있는가?

### 현재 구현 (RedisDataStorage.java)
```java
public <T> Optional<T> getRandomElement(String key, Class<T> elementType) {
    // 1단계: 리스트 크기 조회 - O(1)
    Long size = redisTemplate.opsForList().size(key);

    // 2단계: 랜덤 인덱스 생성
    int randomIndex = RandomUtils.generateRandomInt(size.intValue());

    // 3단계: 인덱스로 요소 조회 - O(N)
    Object element = redisTemplate.opsForList().index(key, randomIndex);
    return Optional.of((T) element);
}
```

### 평가: **최적화되지 않음**

| 문제점 | 현재 | 최적 |
|--------|------|------|
| Redis 호출 횟수 | 2회 (LLEN + LINDEX) | 1회 (LRANDMEMBER) |
| LINDEX 시간복잡도 | O(N) | O(1) |
| 네트워크 왕복 | 2 RTT | 1 RTT |
| 랜덤 품질 | `currentTimeMillis() % max` (편향) | SecureRandom (균등) |

---

## 2-1. 최적화 방안 및 성능 비교

### 최적화 코드

```java
// 개선된 버전 (Redis 6.2+ 필요)
public <T> Optional<T> getRandomElement(String key, Class<T> elementType) {
    Object element = redisTemplate.opsForList().randomElement(key);  // LRANDMEMBER
    return element == null ? Optional.empty() : Optional.of((T) element);
}
```

### 성능 비교 (이론적 분석)

#### Redis 명령어별 시간복잡도

| 명령어 | 시간복잡도 | 설명 |
|--------|-----------|------|
| LLEN | O(1) | 리스트 길이 조회 |
| LINDEX | O(N) | N = 인덱스 위치까지의 거리 |
| LRANDMEMBER | O(1) 또는 O(N) | N = 반환할 요소 개수 (1개면 O(1)) |

#### 데이터 크기별 예상 응답 시간

| 데이터 크기 | 현재 방식 (LLEN + LINDEX) | 최적화 (LRANDMEMBER) | 개선율 |
|------------|--------------------------|---------------------|--------|
| 100개 | ~0.3ms | ~0.15ms | **50%** |
| 1,000개 | ~0.5ms | ~0.15ms | **70%** |
| 10,000개 | ~2ms | ~0.15ms | **92%** |
| 100,000개 | ~15ms | ~0.15ms | **99%** |

> **참고**: 위 수치는 로컬 Redis 기준 추정치. 네트워크 지연(RTT)이 추가되면 차이가 더 커짐.

#### 네트워크 영향

```
현재: Client → [LLEN] → Redis → Client → [LINDEX] → Redis → Client
      ├─────── RTT 1 ──────┤      ├─────── RTT 2 ──────┤

최적화: Client → [LRANDMEMBER] → Redis → Client
        ├──────────── RTT 1 ────────────┤
```

- **로컬 환경 RTT**: ~0.1ms
- **운영 환경 RTT (같은 VPC)**: ~0.5ms ~ 1ms
- **운영 환경 RTT (다른 리전)**: ~10ms ~ 50ms

**결론**: 데이터 1,000개 이상, 또는 네트워크 지연이 있는 환경에서 체감 성능 차이 발생

---

## 2-2. 현재 설계 유지 시 성능 한계점

### 환경 사양

| 환경 | CPU | Memory | Redis |
|------|-----|--------|-------|
| 로컬 | 멀티코어 | 16GB+ | 로컬 Redis |
| 운영 | 1 vCPU | 4GB | 별도 Redis 인스턴스 (2대) |

### 현재 데이터 규모 (추정)

| 키 | 데이터 크기 | 메모리 사용량 (추정) |
|----|-----------|-------------------|
| pixabayMusic | ~500개 (32장르 × ~15개) | ~500KB |
| pixabayVideos | ~400개 (20카테고리 × ~20개) | ~600KB |
| **합계** | ~900개 | **~1.1MB** |

### 성능 이슈 발생 시점 분석

#### 로컬 환경

| 지표 | 안전 구간 | 주의 구간 | 위험 구간 |
|-----|----------|----------|----------|
| 데이터 크기 | < 10,000개 | 10,000 ~ 50,000개 | > 50,000개 |
| 동시 요청 | < 100 req/s | 100 ~ 500 req/s | > 500 req/s |
| 응답 시간 | < 5ms | 5 ~ 20ms | > 20ms |

**로컬에서 성능 이슈 발생 조건**:
- 데이터 50,000개 이상 + 동시 요청 100 req/s 이상

#### 운영 환경 (1 vCPU, 4GB Memory × 2대)

| 지표 | 안전 구간 | 주의 구간 | 위험 구간 |
|-----|----------|----------|----------|
| 데이터 크기 | < 5,000개 | 5,000 ~ 20,000개 | > 20,000개 |
| 동시 요청 | < 50 req/s | 50 ~ 200 req/s | > 200 req/s |
| 응답 시간 | < 10ms | 10 ~ 50ms | > 50ms |
| Redis 연결 | < 15 | 15 ~ 20 | > 20 (maxTotal 도달) |

**운영에서 성능 이슈 발생 조건**:

1. **CPU 병목** (1 vCPU 한계)
   - 동시 요청 200 req/s 이상
   - JSON 역직렬화 부하 누적

2. **메모리 병목** (4GB 한계)
   - Redis 데이터 2GB 이상 (현재 1.1MB로 여유 충분)
   - JVM Heap + Redis 메모리 합계 3.5GB 초과

3. **Connection Pool 고갈**
   - 현재 설정: maxTotal=20, maxIdle=10
   - 동시 요청 20개 초과 시 대기 발생

4. **네트워크 지연 누적**
   - 2회 Redis 호출 × 높은 동시성 = RTT 누적

### 병목 발생 시나리오

```
시나리오: 100 req/s, 데이터 10,000개, 운영 환경

1. 각 요청당 Redis 호출 2회 → 200 Redis ops/s
2. LINDEX 평균 O(5000) 연산 → Redis CPU 부하 증가
3. Connection Pool 20개 중 대부분 사용 중
4. 평균 응답 시간 30ms → 50ms로 증가
5. Timeout 발생 시작 (commandTimeout: 3초 설정이나 누적 지연)
```

---

## 3. 최종 정리

### 현재 상태 요약

| 항목 | 상태 | 비고 |
|------|------|------|
| Redis 캐싱 설계 | ✅ 적절 | 용도에 맞게 사용 중 |
| 조회 로직 최적화 | ⚠️ 개선 가능 | LRANDMEMBER 사용 권장 |
| 현재 데이터 규모 | ✅ 안전 | ~900개, ~1.1MB |
| 현재 트래픽 규모 | ✅ 안전 (추정) | 낮은 트래픽 가정 |

### 앞으로 발생 가능한 이슈

| 이슈 | 발생 조건 | 영향 | 대응 방안 |
|------|----------|------|----------|
| LINDEX 성능 저하 | 데이터 10,000개+ | 응답 지연 | LRANDMEMBER로 변경 |
| Connection Pool 고갈 | 동시 요청 20개+ | 요청 대기/실패 | maxTotal 증가 또는 최적화 |
| Redis 메모리 부족 | 데이터 수GB | OOM, 데이터 손실 | maxmemory 정책 설정 |
| 스케줄러 중복 실행 | 서버 2대 동시 실행 | 데이터 충돌 | 분산 락 또는 단일 인스턴스 실행 |
| 랜덤 편향 | 특정 시간대 요청 집중 | 동일 데이터 반복 반환 | SecureRandom 사용 |

### 모니터링 필수 지표

#### Redis 지표
```
# 필수 모니터링 항목
- redis_connected_clients        # 연결된 클라이언트 수
- redis_used_memory_bytes        # 메모리 사용량
- redis_commands_processed_total # 초당 명령어 처리량
- redis_keyspace_hits/misses     # 캐시 히트율
- redis_slowlog                  # 느린 쿼리 로그
```

#### 애플리케이션 지표
```
# 필수 모니터링 항목
- http_request_duration_seconds  # API 응답 시간
- redis_pool_active_connections  # 활성 연결 수
- redis_pool_idle_connections    # 유휴 연결 수
- redis_pool_pending_requests    # 대기 중인 요청
- jvm_memory_used_bytes          # JVM 메모리 사용량
```

#### 알람 임계값 권장

| 지표 | Warning | Critical |
|------|---------|----------|
| API 응답 시간 (p95) | > 100ms | > 500ms |
| Redis 연결 수 | > 15 | > 18 |
| Redis 메모리 | > 70% | > 90% |
| Redis 명령 지연 | > 10ms | > 50ms |

### 권장 액션 (우선순위순)

1. **[낮음]** 현재 트래픽/데이터 규모에서는 변경 불필요
2. **[중간]** 데이터 5,000개 이상 예상 시 LRANDMEMBER로 마이그레이션
3. **[중간]** Redis 모니터링 대시보드 구축 (Prometheus + Grafana)
4. **[낮음]** RandomUtils를 SecureRandom으로 변경 (랜덤 품질 개선)

---

## 참고: 관련 파일

| 파일 | 설명 |
|------|------|
| `core/.../infrastructure/RedisDataStorage.java` | 랜덤 조회 로직 |
| `core/.../util/RandomUtils.java` | 랜덤 인덱스 생성 |
| `core/.../config/RedisConfig.java` | Redis 연결 설정 |
| `data/.../scheduler/PixabayDataScheduler.java` | 데이터 수집 스케줄러 |
