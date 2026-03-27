# Core Module Guide (Infrastructure)

`core` 모듈은 프로젝트 전체에서 사용하는 공통 기반 기술과 인프라를 담당합니다.

## 🏗️ 핵심 인프라 구현 (Infrastructure)

### 1. RedisDataStorage (Repository 패턴)
Redis Set 자료구조를 활용한 고성능 저장소 구현입니다.
- **Set 데이터 저장**: Pipelined `SADD`를 통해 대량의 데이터를 효율적으로 저장하며, 원자적인 갱신을 위해 기존 키를 삭제 후 재생성합니다.
- **랜덤 요소 조회**: `SRANDMEMBER` 명령을 사용하여 O(1) 성능의 랜덤 데이터 추출을 지원합니다.
```java
@Component
public class RedisDataStorage {
    public <T> void setData(String key, Collection<T> data) { ... }
    public <T> T getRandomElement(String key, Class<T> type, ErrorCode errorCode) { ... }
}
```

### 2. DiscordNotifierAspect (AOP)
횡단 관심사인 실시간 알림을 처리하는 핵심 로직입니다.
- **어노테이션**: `@NotifyDiscord(taskName = "태스크명")`
- **로직**: 메서드 실행 전후로 실행 시간을 측정하고, 결과(성공/실패/통계)를 Discord 웹훅으로 비동기 전송합니다.
```java
@Aspect
@Component
public class DiscordNotifierAspect {
    @Around("@annotation(notifyDiscord)")
    public Object notifyEvent(ProceedingJoinPoint joinPoint, NotifyDiscord notifyDiscord) {
        // 실행 시간 측정 및 결과에 따른 Discord Webhook 발송 (Virtual Thread 사용)
        return joinPoint.proceed();
    }
}
```

### 3. 공통 예외 및 응답 포맷 (Exception & DTO)
- **BaseResponse<T>**: 모든 API의 표준 응답 규격 (code, message, data).
- **ErrorCode**: 도메인 전체에서 사용하는 비즈니스 예외 코드 관리.
- **CustomException**: `ErrorCode`를 포함하는 런타임 예외 계층.

## ⚙️ 주요 라이브러리 및 기술
- **Spring Data Redis**: 분산 데이터 관리.
- **Spring AOP**: 횡단 관심사(알림) 분리.
- **Virtual Threads (Java 21)**: 비동기 알림 전송 최적화.
- **Micrometer**: Redis 접근 및 성능 지표 측정.

## 🔗 의존성 및 영향도 (Dependency Map)
- **의존하는 모듈**: 없음 (최하위 공통 모듈)
- **영향을 받는 모듈**: `data`, `api` (core 수정 시 두 모듈 모두 컴파일/테스트 필수)
