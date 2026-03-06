# Redis 커넥션 풀(Connection Pool) 최적화 가이드

## 문제 상황

### 증상
- 간헐적인 Redis 연결 타임아웃 발생
- 트래픽 증가 시 응답 시간(Latency) 급증
- 가상 스레드(Virtual Threads) 사용 환경에서 병목 현상 발생 가능성

### 영향 범위
- Redis를 사용하는 모든 API (`/video`, `/music`, `/message` 등)
- 시스템 전체의 동시 처리량(Throughput) 저하

---

## 원인 분석 및 조정 기준

### 1. 가상 스레드(Virtual Threads) 환경의 특성
현재 프로젝트는 `spring.threads.virtual.enabled: true` 설정을 통해 가상 스레드를 사용하고 있습니다.
- **특징**: 수천 개의 스레드가 동시에 실행될 수 있으며, I/O 작업(Redis 호출) 시 스레드 차단 비용이 매우 낮음.
- **문제**: 기존의 플랫폼 스레드 방식보다 훨씬 더 많은 동시 Redis 요청이 발생할 수 있으나, 커넥션 풀이 작으면(`maxTotal: 20`) 가상 스레드들이 커넥션을 얻기 위해 대기하게 되어 병목이 발생함.

### 2. 풀 크기 결정 기준 (Three Pillars)

#### ① 동시 처리량 (Concurrency)
- **공식**: `Expected RPS (초당 요청 수) × Average Redis Latency (초)`
- 예: 초당 500개 요청이 오고, Redis 응답이 0.05초(50ms)라면 `500 * 0.05 = 25`개의 커넥션이 상시 사용됨. 피크 타임을 고려해 이 수치의 2~3배를 `maxTotal`로 잡는 것이 안전함.

#### ② Redis 서버 리소스 (Server Capacity)
- Redis 서버의 `maxclients` 설정 확인 (기본값 보통 10,000).
- **공식**: `(API 인스턴스 수 × 인스턴스당 maxTotal) < Redis 서버 maxclients`
- 전체 애플리케이션 인스턴스가 사용하는 커넥션 합계가 Redis 서버가 수용 가능한 범위를 넘지 않아야 함.

#### ③ 네트워크 레이턴시 및 타임아웃
- 네트워크 RTT(Round Trip Time)가 길수록 커넥션 점유 시간이 길어짐.
- `commandTimeout`과 `connectTimeout` 설정에 따라 풀 대기 시간이 길어지면 연쇄적으로 에러가 발생할 수 있음.

---

## 권장 설정 및 개선 방안

### 1. 권장 파라미터 (Virtual Threads 환경 기준)

| 설정 항목 | 권장 값 | 설명 |
|-----------|---------|------|
| **maxTotal** | **64 ~ 128** | 가상 스레드의 높은 동시성을 수용하기 위해 상향 조정 |
| **maxIdle** | **32 ~ 64** | 트래픽 서지(Surge) 발생 시 즉시 대응할 수 있도록 유지 |
| **minIdle** | **16 ~ 32** | 커넥션 생성 비용(Cold Start)을 방지하기 위한 최소 유지 |
| **testOnBorrow**| **true** | 사용 전 유효성 검사를 통해 Broken Connection 방지 |

### 2. 코드 적용 예시 (`RedisConfig.java`)

```java
@Bean
public RedisConnectionFactory redisConnectionFactory() {
    GenericObjectPoolConfig<Object> poolConfig = new GenericObjectPoolConfig<>();
    poolConfig.setMaxTotal(64);    // 20 -> 64 상향
    poolConfig.setMaxIdle(32);     // 10 -> 32 상향
    poolConfig.setMinIdle(16);     // 5 -> 16 상향
    poolConfig.setTestOnBorrow(true);
    poolConfig.setTestWhileIdle(true);
    poolConfig.setTimeBetweenEvictionRuns(Duration.ofSeconds(30));

    // ... 나머지 설정 동일
}
```

---

## 모니터링 및 검증

### 1. 핵심 지표 (Metrics)
Prometheus + Grafana를 통해 아래 지표를 실시간 모니터링해야 합니다.
- `redis.pool.active.connections`: 현재 사용 중인 커넥션 수
- `redis.pool.idle.connections`: 풀에서 대기 중인 커넥션 수
- `redis.pool.pending.threads`: 커넥션을 얻기 위해 대기 중인 스레드 수 (0에 가까워야 함)

### 2. 부하 테스트를 통한 검증
- `performance-test.sh`를 활용하여 동시 접속자 수를 단계적으로 늘리며 `pending.threads`가 발생하는 임계점을 확인하십시오.
- 특정 시점부터 Latency가 급격히 증가한다면 `maxTotal`을 추가로 상향 조정해야 합니다.

---

## 결론 및 요약

1. 가상 스레드를 사용하는 현재 환경에서는 **기존보다 더 큰 커넥션 풀이 필요**합니다.
2. `maxTotal`은 **예상 최대 동시 요청 수의 2배 이상**으로 설정하는 것을 권장합니다.
3. 무조건 크게 잡는 것이 답은 아니며, **Redis 서버의 전체 수용량(`maxclients`) 내에서 조정**해야 합니다.
4. 주기적인 **모니터링**을 통해 유휴 커넥션과 대기 스레드 수의 균형을 맞추는 작업이 필요합니다.

---
**작성일**: 2026-03-06
**작성자**: Gemini CLI (Engineering Team)
**상태**: 📖 가이드 제공 완료
