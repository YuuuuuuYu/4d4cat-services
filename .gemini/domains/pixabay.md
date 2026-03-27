# Pixabay Domain Guide (Business Logic)

Pixabay 외부 API 연동을 통해 미디어 데이터를 수집하고 랜덤하게 제공하는 비즈니스 도메인입니다.

## 📁 주요 관련 파일 (Domain Files)

### 1. 데이터 수집 로직 (data 모듈)
- `com.services.data.pixabay.PixabayVideoCollector`: 비디오 필터 정의 및 수집 로직.
- `com.services.data.pixabay.PixabayMusicCollector`: 음악 장르 정의 및 수집 로직.
- `com.services.data.scheduler.PixabayDataScheduler`: 수집 주기 관리.
- **인프라 상세**: [Data Module 가이드](../modules/data.md) 참조.

### 2. API 제공 로직 (api 모듈)
- `com.services.api.pixabay.PixabayController`: `/video`, `/music` 엔드포인트.
- `com.services.api.pixabay.PixabayService`: 랜덤 데이터 추출 로직.
- **인프라 상세**: [API Module 가이드](../modules/api.md) 참조.

### 3. 데이터 명세 및 공통 (core 모듈)
- `com.services.core.pixabay.dto.PixabayVideoResult`: 비디오 레코드 필드 정의.
- `com.services.core.pixabay.dto.PixabayMusicResult`: 음악 레코드 필드 정의.
- `com.services.core.infrastructure.ApiMetadata`: Redis 키 이름 정의.
- **인프라 상세**: [Core Module 가이드](../modules/core.md) 참조.

## ⚙️ 상세 비즈니스 워크플로우

### 1. 미디어 수집 및 갱신 프로세스
1. **필터 로드**: 각 수집기(Collector)에서 정의된 카테고리/장르 목록을 로드합니다.
2. **병렬 요청**: 외부 Pixabay API에 대해 필터별로 데이터를 요청합니다.
3. **정제 및 저장**: 수집된 원본 데이터를 서비스 규격에 맞게 변환하여 Redis Set에 저장합니다.
4. **결과 보고**: 총 수집 건수와 성공/실패 여부를 Discord로 전송합니다.

### 2. 랜덤 미디어 제공 프로세스
1. **요청 수신**: 클라이언트의 랜덤 비디오/음악 요청을 수신합니다.
2. **랜덤 추출**: Redis의 `SRANDMEMBER` 기능을 사용하여 즉시 랜덤 요소를 추출합니다.
3. **응답**: 표준 `BaseResponse` 규격에 담아 반환합니다.

## ⚠️ 비즈니스 규칙 및 주의 사항
- 데이터 갱신 시 기존 데이터는 모두 삭제되고 새로운 데이터로 교체됩니다.
- 새로운 카테고리를 추가하려면 `getFilters()` 메서드에 값을 추가해야 합니다.
- 수집 실패 시 로그를 남기고 다음 필터로 진행하며, 전체 프로세스는 멈추지 않습니다.
