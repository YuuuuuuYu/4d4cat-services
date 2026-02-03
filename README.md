[![Java](https://img.shields.io/badge/Java-21-blue.svg)](https://www.java.com)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.12-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED.svg)](https://docs.docker.com/compose/)

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

### Core Platform
- **Java 21** - 최신 LTS 버전, Virtual Threads 지원
- **Spring Boot 3.4.12** - 엔터프라이즈 애플리케이션 프레임워크
- **Spring Framework 6.1+** - 의존성 주입, 트랜잭션 관리

### Spring Ecosystem
- **Spring Web** - RESTful API 개발
- **Spring Data JPA** - ORM 기반 데이터베이스 연동
- **Spring Data Redis** - 분산 캐싱 및 세션 관리
- **Spring AOP** - 횡단 관심사 처리 (로깅, 모니터링, 알림)
- **Spring Actuator** - 헬스체크, 메트릭 노출

### External Libraries
- **RestClient** - Spring 6.1+ 동기식 HTTP 클라이언트
- **SpringDoc OpenAPI 2.8.14** - API 문서 자동 생성 (Swagger UI)
- **Lombok** - 보일러플레이트 코드 감소

### Infrastructure & DevOps
- **Docker & Docker Compose** - 컨테이너화 및 로컬 개발 환경
- **Redis** - 인메모리 데이터 스토어, 캐시
- **Nginx** - 리버스 프록시, 로드 밸런싱
- **Prometheus** - 메트릭 수집 및 모니터링

### Build & Quality
- **Gradle** - 멀티모듈 빌드 도구
- **JUnit 5** - 단위 테스트 프레임워크
- **Mockito** - 모킹 라이브러리
- **Spotless** - 코드 포맷팅 (Google Java Format)
- **GitHub Actions** - CI/CD 자동화

### Development Tools
- **Virtual Threads** - Java 21 경량 스레드 기반 동시성
- **Claude & Gemini AI** - 코드 리뷰 및 개선 지원

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

### 멀티모듈 구조
```
services/
├── core/                  # 공통 라이브러리 (DTO, 예외, Redis, AOP, 알림)
├── api/                   # API 서버 (REST API, JPA)
├── data/                  # 데이터 수집 서버 (스케줄러, Pixabay API 호출)
```

**모듈별 역할:**
- **core**: 공통 예외, Redis 저장소, Pixabay DTO, AOP (Discord 알림), 유틸리티
- **api**: REST API 제공, 사용자 요청 처리
- **data**: 외부 API 데이터 수집, Redis 저장, 스케줄링

### 서비스 아키텍처

**로컬 개발 환경:**
```
┌─────────────────────────────────────────────────────────────┐
│                      Docker Compose                         │
├─────────────┬─────────────┬─────────────┬───────────────────┤
│     api     │    data     │    Redis    │     Prometheus    │
│   :8080     │    :8081    │    :6379    │       :9090       │
└─────────────┴─────────────┴─────────────┴───────────────────┘
```

**프로덕션 환경:**
```
                    ┌─────────────────┐
                    │  Nginx (Main)   │
                    │  Load Balancer  │
                    └────────┬────────┘
                             │
              ┌──────────────┼──────────────┐
              │              │              │
      ┌───────▼──────┐  ┌────▼─────┐  ┌─────▼────────┐
      │  API (Main)  │  │ API (Sub)│  │    Data      │
      │   :8080      │  │  :8080   │  │   :8081      │
      └───────┬──────┘  └────┬─────┘  └─────┬────────┘
              │              │              │
              │              │              │ Redis :6379
              └──────────────┴──────────────┘
```

### 패키지 구조 (Feature-based)

**api 모듈:**
```
api/
├── pixabay/              # Pixabay API (Controller + Service)
├── message/              # Message 기능 (Controller + Service)
├── omniwatch/            # OmniWatch 분석 (추후 구현 예정)
├── config/               # Spring 설정 (CORS, JPA 등)
├── presentation/         # 공통 예외 처리 (GlobalExceptionHandler)
└── util/                 # 유틸리티
```

**data 모듈:**
```
data/
├── collector/            # Pixabay API 데이터 수집
├── scheduler/            # 주기적 데이터 갱신
└── DataServerApplication
```

**core 모듈:**
```
core/
├── aop/                  # AOP (Discord 알림 어노테이션 및 Aspect)
├── dto/                  # 공통 DTO (Pixabay 응답, API 응답)
├── exception/            # 공통 예외 클래스
├── notification/         # 알림 관련 (Discord 웹훅, DataCollectionResult)
├── infrastructure/       # Redis 설정 및 저장소
└── util/                 # 공통 유틸리티
```

**설계 철학:**
- **기능별 응집**: 관련 코드를 한 패키지에 모아 직관적으로 구성
- **수직 슬라이싱**: Controller-Service를 기능별로 수직 분리
- **YAGNI 원칙**: 현재 규모에 적합한 단순한 구조 유지

### 🔧 핵심 설계 패턴
- **Template Method**: 공통 초기화 로직 재사용 (`PixabayDataCollector`)
- **Singleton**: 글로벌 데이터 캐시 관리 (Redis)
- **Strategy**: API별 다른 매개변수 생성 전략
- **Optional**: 안전한 null 처리
- **AOP**: `@NotifyDiscord` 어노테이션을 통한 횡단 관심사 처리
  - core 모듈에서 제공하는 공통 AOP
  - data-server: 데이터 수집 완료 시 자동 알림
  - api-server: 메시지 저장 시 자동 알림
  - 실행 시간 측정 및 Discord 웹훅 실시간 알림

### ⚡ 성능 최적화
- **분산 캐싱**: Redis 기반 데이터 캐싱으로 서버 간 상태 공유
- **가상 스레드**: Java 21 Virtual Threads를 활용한 경량 동시성 처리
- **RestClient**: Spring 6.1+ RestClient를 통한 간결한 HTTP 통신
- **내결함성**: 개별 API 실패가 전체 시스템에 영향 주지 않음 (Optional 기반 실패 처리)
- **단순한 에러 처리**: Retry 없이 실패 시 즉시 다음 작업 진행, 시스템 복잡도 감소
- **계층화된 예외 처리**: `NotFoundException`, `BadGatewayException` 우아한 처리
- **컨테이너화**: Docker Compose로 일관된 배포 환경 보장
- **메트릭 수집**: Prometheus를 통한 모니터링 데이터 수집
- **AOP 기반 알림**: 주요 작업의 실행 시간 추적 및 Discord 실시간 알림

## 📊 데이터 플로우

1. **Data 서버 시작**: 스케줄러를 통해 주기적으로 Pixabay API 호출
2. **AOP Intercept**: `@NotifyDiscord` 어노테이션으로 데이터 수집 시작 감지
3. **데이터 수집**: 32개 음악 장르 + 20개 비디오 카테고리 병렬 수집 (Virtual Threads)
4. **Redis 저장**: 수집된 데이터를 Redis에 캐싱 (서버 간 공유)
5. **Discord 알림**: 수집 완료 시 통계 정보와 함께 비동기 알림 전송
6. **API 서버 요청**: Redis에서 랜덤 데이터 선택하여 즉시 응답

## 📈 향후 개선 계획

### 기능
- 더 구체적인 키워드 기반 필터링
- 음악 길이별/분위기별 필터링
- 일별/주간 트렌드 데이터 제공

### 기술
- API Key Vault 도입
- Redis Cluster 구성
- Alert Manager 연동
- Blue-Green 배포

## 🚀 실행 방법

### 사전 준비
```bash
# 환경 변수 설정
cp .env.example .env

# .env 파일 편집 (필수)
# PIXABAY_KEY=your_api_key
# DISCORD_WEBHOOK_URL=your_webhook_url
```

### Docker Compose (권장)
```bash
# 전체 서비스 실행
docker-compose up -d --build

# 로그 확인
docker-compose logs -f

# 특정 서비스 로그
docker-compose logs -f api
docker-compose logs -f data

# 서비스 중지
docker-compose down
```

### 개별 모듈 빌드 및 실행
```bash
# 전체 빌드
./gradlew clean build

# 모듈별 빌드
./gradlew :core:build
./gradlew :api:bootJar
./gradlew :data:bootJar

# 개별 실행
./gradlew :api:bootRun
./gradlew :data:bootRun
```

### 테스트
```bash
# 전체 테스트
./gradlew test

# 특정 모듈 테스트
./gradlew :api:test
./gradlew :data:test

# 코드 포맷팅
./gradlew spotlessApply
```

### 접속 정보
| 서비스 | URL | 설명 |
|--------|-----|------|
| API Server | http://localhost:8080 | REST API 엔드포인트 |
| Data Server | http://localhost:8081 | 데이터 수집 서버 |
| Swagger UI | http://localhost:8080/api-docs/swagger-ui.html | API 문서 |
| API Health | http://localhost:8080/actuator/health | API 헬스체크 |
| Data Health | http://localhost:8081/actuator/health | Data 헬스체크 |
| Prometheus | http://localhost:9090 | 메트릭 수집 |
| Grafana | http://localhost:3000 | 모니터링 대시보드 |
| Redis | localhost:6379 | 캐시 스토어 |

### API 테스트 예시
```bash
# 헬스체크
curl http://localhost:8080/actuator/health

# 랜덤 비디오
curl http://localhost:8080/video?q=nature

# 랜덤 음악
curl http://localhost:8080/music?q=relax

# 메시지 조회
curl http://localhost:8080/message

# 메시지 저장
curl -X POST http://localhost:8080/message \
  -H "Content-Type: application/json" \
  -d '{"content": "Hello World"}'
```

## 🔄 CI/CD

### GitHub Actions Workflows

**CI - Pull Request:**
- 모든 PR에 대해 자동 빌드 및 테스트 실행
- 테스트 결과 자동 리포팅

**CD - API Server (Main/Sub):**
- `main` 브랜치 병합 시 자동 배포
- Docker 이미지 빌드 → Docker Hub 푸시
- Oracle 서버 2대에 병렬 배포
- 헬스체크 및 자동 롤백

**CD - Data Server:**
- `core/`, `data/` 변경 감지 시 자동 배포
- Redis 컨테이너 자동 확인/실행
- 배포 후 헬스체크

### 배포 전략
- **멀티 서버 배포**: Main/Sub API 서버 병렬 배포
- **무중단 배포**: 헬스체크 기반 점진적 트래픽 전환
- **이미지 태깅**: Git SHA 기반 버전 관리
- **자동 정리**: 사용하지 않는 이미지 자동 삭제
