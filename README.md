**이런 기능이 있으면 어떨까?** 라는 생각으로 시작하여 여러 기능들을 제공하는 서비스입니다.

## 🚀 주요 기능

### 1. Random
#### Random Video
- 🎞️ Pixabay Video API를 이용한 랜덤 영상 제공
- 📂 20개 카테고리별 영상 노출 (backgrounds, fashion, nature, people, emotions 등)
- 📱 다양한 해상도 지원 (tiny, small, medium, large)

#### Random Music
- 🎵 커스텀 Music API를 이용한 랜덤 음원 제공
- 🎼 32개 장르의 다양한 음악 (electronic, upbeat, beats, ambient, rock, folk 등)
- 🖼️ 썸네일 이미지 지원

### 2. Message
- 💬 마지막 메시지 저장 및 조회 기능
- 📝 메시지 유효성 검증 (빈 문자열, null 체크)

### 3. Keyword
#### 실시간 트렌드
- 🔍 구글 트렌드를 이용한 실시간 검색어 제공

## 🏗️ 기술 스택

- **Java 21** - 최신 자바 기능 활용
- **Spring Boot 3.4.4** - 웹 애플리케이션 프레임워크
- **Spring AOP** - 횡단 관심사 처리 (로깅, 모니터링)
- **JUnit 5 + Mockito** - 테스트 프레임워크
- **Gradle** - 빌드 도구
- **SpringDoc OpenAPI** - API 문서 자동 생성
- **AI** - 코드 개선 (Claude, Gemini)

## 📡 API 엔드포인트

| Method | Endpoint | Description | Response Type |
|--------|----------|-------------|---------------|
| GET | `/` | 서비스 상태 확인 | `BaseResponse<String>` |
| GET | `/video` | 랜덤 비디오 반환 | `BaseResponse<PixabayVideoResult>` |  
| GET | `/music` | 랜덤 음악 반환 | `BaseResponse<PixabayMusicResult>` |
| GET | `/message` | 저장된 마지막 메시지 조회 | `String` |
| POST | `/message` | 새로운 메시지 저장 | `200 OK` |

### 응답 형식 예시

**성공 응답:**
```json
{
  "status": 200,
  "data": {
    "id": 12345,
    "tags": "nature, landscape",
    "webformatURL": "https://...",
    "duration": 30
  },
  "error": null,
  "timestamp": "2025-08-08T16:10:30.123456"
}
```

**에러 응답:**
```json
{
  "status": 404,
  "data": null,
  "error": {
    "code": "PM1000",
    "message": "PixabayMusic data not found"
  },
  "timestamp": "2025-08-08T16:10:30.123456"
}
```

## 🏛️ 아키텍처 특징

### 레이어드 아키텍처
```
Presentation Layer (Controllers, DTOs, GlobalExceptionHandler)
    ↓
Application Layer (Services, Exceptions, Business Logic)
    ↓  
Infrastructure Layer (DataStorage, Config, RestTemplate)
    ↓
Cross-cutting Concerns (AOP Aspects, Utilities)
```

### 🔧 핵심 설계 패턴
- **Template Method**: 공통 초기화 로직 재사용
- **Singleton**: 글로벌 데이터 캐시 관리
- **Strategy**: API별 다른 매개변수 생성 전략
- **Optional**: 안전한 null 처리

### ⚡ 성능 최적화
- **병렬 처리**: 10개 스레드 풀을 통한 동시 API 호출
- **내결함성**: `exceptionally()` 메서드로 개별 API 실패가 전체 시스템에 영향 주지 않음
- **계층화된 예외 처리**: `NotFoundException`, `BadGatewayException` 우아한 처리
- **메모리 캐싱**: ConcurrentHashMap 기반 스레드 안전 캐싱
- **AOP 모니터링**: 실행 시간 추적 및 성능 분석

## 📊 데이터 플로우

1. **애플리케이션 시작**: Spring ApplicationReadyEvent 트리거
2. **병렬 초기화**: 32개 음악 장르 + 20개 비디오 카테고리 동시 로드
3. **데이터 캐싱**: 모든 성공한 데이터를 ConcurrentHashMap에 저장
4. **요청 처리**: 캐시에서 랜덤 선택하여 즉시 응답

## 📈 향후 개선 계획

### Random Video
- 더 구체적인 키워드 기반 필터링

### Random Music
- 태그 데이터 구체화 및 확장
- 음악 길이별 필터링

### 실시간 트렌드
- 일별/주간 트렌드 데이터 제공
- 네이버, 다음 등 다른 검색 키워드 추가

### 기술적 개선
- Redis를 통한 외부 캐시 도입
- OpenAPI 문서화 개선
- API Key 관리 및 보안 강화
- 실시간 데이터 업데이트