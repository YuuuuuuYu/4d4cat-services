# Skill: Pixabay (Data Harvesting & Random Extraction)

이 스킬은 외부 Pixabay API를 연동하여 미디어 데이터를 수집하고 랜덤하게 제공하는 `Pixabay` 서비스 관련 작업을 수행할 때 탑재하는 가이드라인입니다.

---

## 1. 도메인 개요 및 비즈니스 핵심
- **목표:** Pixabay 외부 API 연동을 통해 비디오/음악 데이터를 주기적으로 수집하여 저장하고, 클라이언트의 랜덤 데이터 요청에 실시간으로 O(1) 수준으로 빠르게 응답하는 것을 목표로 합니다.

---

## 2. 주요 관련 파일

### 2.1 data 모듈 (데이터 수집 및 스케줄러)
- `com.services.data.pixabay.PixabayVideoCollector`: 비디오 필터 정의 및 수집 로직.
- `com.services.data.pixabay.PixabayMusicCollector`: 음악 장르 정의 및 수집 로직.
- `com.services.data.scheduler.PixabayDataScheduler`: 수집 주기 관리.

### 2.2 api 모듈 (API 및 서비스)
- `com.services.api.pixabay.PixabayController`: `/video`, `/music` 엔드포인트 제공.
- `com.services.api.pixabay.PixabayService`: 랜덤 데이터 추출 로직.

### 2.3 core 모듈 (데이터 명세 및 인프라)
- `com.services.core.pixabay.PixabayVideoResult`: 비디오 레코드 필드 정의 (Record).
- `com.services.core.pixabay.PixabayMusicResult`: 음악 레코드 필드 정의 (Record).
- `com.services.core.infrastructure.ApiMetadata`: Redis 키 이름 정의.

---

## 3. 핵심 처리 지침 & 전문가 규칙

### 3.1 가상 스레드 기반 병렬 처리 및 Rate Limit 준수
- **가상 스레드 사용:** 데이터 수집 로직(`PixabayDataCollector`) 수정 시 반드시 **Java 21 가상 스레드(Virtual Threads)**를 활용하십시오. I/O 블로킹을 최소화하기 위해 `Executors.newVirtualThreadPerTaskExecutor()`를 적용해야 합니다.
- **Rate Limit 준수:** Pixabay API의 속도 제한을 우회하기 위해, 병렬 작업 제출 시 `STAGGER_DELAY_MS` (1초) 지연 메커니즘을 두어 순차적으로 태스크를 투입해야 합니다.

### 3.2 Redis Set 자료구조 최적화
- 모든 Pixabay 수집 데이터는 Redis의 **Set** 자료구조에 저장되어야 합니다.
- 조회 성능 보장을 위해 `RedisDataStorage.getRandomElement()`를 사용하여 `SRANDMEMBER` 명령이 실행되도록 함으로써 O(1) 조회 속도를 보장하십시오.

### 3.3 에러 복원력 (Error Resilience)
- 개별 카테고리/필터의 API 수집 실패가 전체 수집 프로세스를 중단시키지 않아야 합니다.
- `fetchDataForFilter`는 실패 시 에러를 로깅하고 `Optional.empty()`를 반환하여 우아하게 실패를 처리해야 합니다.

---

## 4. 자가 검증 체크리스트
- [ ] 수집 로직이 가상 스레드 풀(`newVirtualThreadPerTaskExecutor`)을 사용하는가?
- [ ] `STAGGER_DELAY_MS`가 적용되어 API 요청 부하를 분산하고 있는가?
- [ ] Redis 저장 시 `SADD` 기반의 Set 구조를 사용하는가?
- [ ] `@NotifyDiscord`를 통해 수집 결과(성공/실패 수)가 제대로 전달되는가?

---

## 5. 테스트 검증 전략 (Testing Strategy)
- **Mocking 대상:** `RestClient` (외부 API 호출), `RedisDataStorage` (데이터베이스 연동)
- **검증 포인트:**
  - `fetchDataForFilter`가 예외 발생 시 `Optional.empty()`를 반환하고 로그를 남기는지 검증합니다.
  - 병렬 처리 결과(성공/실패 필터 개수)가 `DataCollectionResult`에 정확히 합산되는지 확인합니다.
  - 가상 스레드 환경이 아닌 일반 스레드 환경에서도 비즈니스 로직이 동일하게 동작하는지 단위 테스트합니다.
