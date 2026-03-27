# Pixabay Expert Skill Guide

이 서비스는 고성능 데이터 수집 및 랜덤 데이터 조회를 목표로 합니다. 관련 작업을 수행할 때 다음 지침을 엄격히 준수하십시오.

## 🧠 핵심 처리 지침 (Core Instructions)

### 1. 가상 스레드 기반 병렬 처리
- 데이터 수집 로직(`PixabayDataCollector`) 수정 시 반드시 **Java 21 가상 스레드(Virtual Threads)**를 활용하십시오.
- `Executors.newVirtualThreadPerTaskExecutor()`를 사용하여 I/O 블로킹을 최소화해야 합니다.

### 2. API Rate Limit 준수
- Pixabay API의 속도 제한을 고려하여, 병렬 작업 제출 시 `STAGGER_DELAY_MS`를 사용하여 순차적으로 태스크를 투입해야 합니다.
- 새로운 수집기 구현 시에도 이 staggered delay 메커니즘을 유지하십시오.

### 3. Redis 최적화 (Set 자료구조)
- 모든 Pixabay 데이터는 Redis의 **Set** 자료구조에 저장되어야 합니다.
- 조회 시 `RedisDataStorage.getRandomElement()`를 사용하여 `SRANDMEMBER` 명령이 실행되도록 함으로써 O(1)의 성능을 보장하십시오.

### 4. 에러 복원력 (Error Resilience)
- 개별 카테고리/필터 수집 실패가 전체 수집 프로세스를 중단시키지 않아야 합니다.
- `fetchDataForFilter`는 실패 시 에러를 로깅하고 `Optional.empty()`를 반환하여 우아하게 실패를 처리해야 합니다.

## 🔍 작업 전 체크리스트
- [ ] 수집 로직이 가상 스레드 풀을 사용하는가?
- [ ] `STAGGER_DELAY_MS`가 적용되어 API 요청 부하를 분산하고 있는가?
- [ ] Redis 저장 시 `SADD` 기반의 Set 구조를 사용하는가?
- [ ] `@NotifyDiscord`를 통해 수집 결과(성공/실패 수)가 제대로 전달되는가?

## 🧪 테스트 전략 (Testing Strategy)
- **Mocking 대상**: `RestClient` (외부 API 호출), `RedisDataStorage` (데이터베이스 연동)
- **검증 포인트**:
  - `fetchDataForFilter`가 예외 발생 시 `Optional.empty()`를 반환하는지 검증합니다.
  - 병렬 처리 결과(성공/실패 필터 개수)가 `DataCollectionResult`에 정확히 합산되는지 확인합니다.
  - 가상 스레드 환경이 아닌 일반 스레드 환경에서도 비즈니스 로직이 동일하게 동작하는지 단위 테스트합니다.
