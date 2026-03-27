# Data Module Guide (Data Collection)

`data` 모듈은 외부 데이터 수집 및 초기화를 담당하는 전용 서버입니다.

## 🏗️ 수집기 인프라 (Collector Infrastructure)

### 1. 템플릿 메서드 패턴 (Abstract Base Class)
수집, 가공, 저장의 일관된 흐름을 `PixabayDataCollector`에 정의하여 중복 로직을 최소화합니다.
```java
public abstract class PixabayDataCollector<T, R extends ApiResponse<T>> {
    public DataCollectionResult collectAndStore() {
        FetchStatistics stats = fetchAllData();
        redisDataStorage.setData(getStorageKey(), stats.results());
        // 메트릭 측정 및 결과 통계 반환
    }
    // API 호출 및 필터별 가공 로직 추상화
    protected abstract List<String> getFilters();
    protected abstract Optional<R> fetchDataForFilter(String filter);
}
```

### 2. 가상 스레드 기반 병렬 수집 (Concurrency)
Java 21의 가상 스레드(Virtual Threads)를 활용하여 대량의 I/O 작업(HTTP 호출)을 병렬로 수행합니다.
- **Executor**: `Executors.newVirtualThreadPerTaskExecutor()` 사용.
- **Staggered Delay**: API Rate Limit을 방지하기 위해 `STAGGER_DELAY_MS` (1초)를 두고 태스크를 투입합니다.
- **격리**: 개별 필터 수집 실패가 전체 수집 프로세스에 영향을 주지 않도록 `Optional`로 결과를 감쌉니다.

## ⚙️ 주요 라이브러리 및 설정
- **RestClient (Spring 6.1+)**: 동기식 코드로 작성하되 가상 스레드를 통해 효율적인 비동기 성능 확보.
- **Spring Scheduling**: `@Scheduled` 기반의 주기적 갱신 및 `ApplicationReadyEvent`를 통한 초기 수집.
- **Environment**: 속성 파일(`.env`, `yml`)을 통해 API 키와 엔드포인트 설정.

## 📈 수집 워크플로우 (Common Phase)
1. **Fetch**: 가상 스레드로 필터별 API 호출.
2. **Transform**: 수집 결과를 공통 DTO 형태로 정규화.
3. **Save**: `core` 모듈의 `RedisDataStorage`를 통해 저장.
4. **Report**: 수행 결과(성공/실패 수, 시간)를 `DataCollectionResult`로 반환.

## 🔗 의존성 및 영향도 (Dependency Map)
- **의존하는 모듈**: `core` (RedisDataStorage, Exception, DTO 등 사용)
- **영향을 받는 모듈**: 없음 (타 모듈이 data를 직접 참조하지 않음, Redis를 통해 간접 연동)
