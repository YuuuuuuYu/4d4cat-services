[![Java](https://img.shields.io/badge/Java-21-blue.svg?logo=openjdk&logoColor=white)](https://www.java.com)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.12-brightgreen.svg?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1.svg?logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-DC382D.svg?logo=redis&logoColor=white)](https://redis.io/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED.svg?logo=docker&logoColor=white)](https://docs.docker.com/compose/)
[![Prometheus](https://img.shields.io/badge/Prometheus-E6522C.svg?logo=prometheus&logoColor=white)](https://prometheus.io/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

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
#### Trend
- 🔍 구글 트렌드를 이용한 실시간 검색어 제공

#### Tech Blog
- 📰 주요 기술 블로그(Naver, Kakao, Toss 등) RSS/Atom 피드 수집
- 🔍 회사별, 태그별 필터링 기능 제공
- 📜 커서 기반(Cursor-based) 무한 스크롤 지원
- 📈 아티클 클릭 수 통계 집계

## 🏗️ 기술 스택

### Core Platform
- **Java 21** - 최신 LTS 버전, Virtual Threads 지원
- **Spring Boot 3.4.12** - 엔터프라이즈 애플리케이션 프레임워크
- **Spring Framework 6.1+** - 의존성 주입, 트랜잭션 관리

### Spring Ecosystem
- **Spring Web** - RESTful API 개발
- **Spring Data JPA & QueryDSL** - 동적 쿼리 및 ORM 기반 데이터베이스 연동
- **Spring Data Redis** - 분산 캐싱 및 세션 관리
- **Spring AOP** - 횡단 관심사 처리 (로깅, 모니터링, 알림)
- **Spring Actuator** - 헬스체크, 메트릭 노출

### External Libraries
- **RestClient** - Spring 6.1+ 동기식 HTTP 클라이언트
- **SpringDoc OpenAPI 2.8.14** - API 문서 자동 생성 (Swagger UI)
- **Lombok** - 보일러플레이트 코드 감소

### Infrastructure & DevOps
- **Docker & Docker Compose** - 컨테이너화 및 로컬 개발 환경
- **PostgreSQL** - 영속성 저장소
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
| GET | `/techblogs` | 기술 블로그 목록 조회 (필터링/페이징) | `BaseResponse<TechBlogListResponse>` |
| GET | `/techblogs/companies` | 활성화된 기술 블로그 회사 목록 | `BaseResponse<List<TechBlogCompanyResponse>>` |
| POST | `/techblogs/{id}/click` | 기술 블로그 게시물 클릭 수 증가 | `BaseResponse<Void>` |

### 응답 형식 예시

**성공 응답:**
```json
{
  "status": 200,
  "data": {
    "items": [
      {
        "id": 1,
        "companyName": "Naver D2",
        "title": "Java 21 Virtual Threads 적용기",
        "url": "https://...",
        "publishedAt": "2024-04-17T10:00:00",
        "tags": ["Java", "VirtualThreads"]
      }
    ],
    "nextCursor": "2024-04-17T10:00:00_1",
    "hasNext": true
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
├── core/                  # 공통 라이브러리 (DTO, 예외, Redis, AOP, 알림, 도메인 엔티티)
├── api/                   # API 서버 (REST API, JPA/QueryDSL)
├── data/                  # 데이터 수집 서버 (RSS 수집기, Pixabay API 호출)
└── monitoring/            # 모니터링 서버 (Actuator, Prometheus)
```

**모듈별 역할:**
- **core**: 공통 예외, Redis 저장소, Pixabay/TechBlog DTO & Entity, AOP, 유틸리티
- **api**: REST API 제공, QueryDSL 기반 동적 필터링 및 커서 페이징 처리
- **data**: 외부 API(Pixabay) 및 RSS(TechBlog) 데이터 수집, 가상 스레드 기반 병렬 처리
- **monitoring**: 서비스 메트릭 수집 및 Prometheus 엔드포인트 제공

### 서비스 아키텍처

**로컬 개발 환경:**
```
┌─────────────────────────────────────────────────────────────────────────────┐
│                             Docker Compose                                  │
├─────────────┬─────────────┬─────────────┬──────────────┬────────────────────┤
│     api     │    data     │    Redis    │  monitoring  │     Prometheus     │
│   :8080     │    :8081    │    :6379    │    :8082     │       :9090        │
└─────────────┴─────────────┴─────────────┴──────────────┴────────────────────┘
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
              │              │              │ Redis :6379 / DB
              └──────────────┴──────────────┘
```

### 패키지 구조 (Feature-based)

**api 모듈:**
```
api/
├── pixabay/              # Pixabay API (Controller + Service)
├── techblog/             # Tech Blog (Controller + QueryService + QueryDSL)
├── message/              # Message 기능 (Controller + Service)
├── omniwatch/            # OmniWatch 분석 (추후 구현 예정)
├── config/               # Spring 설정 (CORS, JPA, Redis 등)
├── presentation/         # 공통 예외 처리 (GlobalExceptionHandler)
└── util/                 # 유틸리티
```

**data 모듈:**
```
data/
├── pixabay/              # Pixabay API 데이터 수집
├── techblog/             # RSS/Atom 피드 수집 (Collector + Scheduler)
├── scheduler/            # 공통 스케줄링 로직
└── DataServerApplication
```

**core 모듈:**
```
core/
├── aop/                  # AOP (Discord 알림, 로깅)
├── dto/                  # 공통 DTO
├── techblog/             # Tech Blog 엔티티 및 리포지토리
├── pixabay/              # Pixabay 관련 도메인 로직
├── exception/            # 공통 예외 클래스
├── infrastructure/       # Redis 설정 및 저장소
└── util/                 # 공통 유틸리티
```

**monitoring 모듈:**
```
monitoring/
└── src/main/java/com/services/monitoring/MonitoringApplication.java
```

**설계 철학:**
- **기능별 응집**: 관련 코드를 한 패키지에 모아 직관적으로 구성
- **수직 슬라이싱**: Controller-Service를 기능별로 수직 분리
- **커서 기반 페이징**: 대용량 데이터에서 성능을 보장하는 페이징 방식 채택
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
- **분산 캐싱**: Redis 기반 데이터 캐싱으로 서버 간 상태 공유 (Tech Blog API 12시간 캐시)
- **가상 스레드**: Java 21 Virtual Threads를 활용하여 RSS 피드 수집 시 I/O 병목 최소화
- **QueryDSL & Indexing**: 효율적인 동적 쿼리 및 커서 기반 페이징을 통한 DB 조회 최적화
- **RestClient**: Spring 6.1+ RestClient를 통한 간결한 HTTP 통신
- **내결함성**: 개별 API 실패가 전체 시스템에 영향 주지 않음 (Optional 기반 실패 처리)
- **단순한 에러 처리**: Retry 없이 실패 시 즉시 다음 작업 진행, 시스템 복잡도 감소
- **컨테이너화**: Docker Compose로 일관된 배포 환경 보장
- **메트릭 수집**: Prometheus를 통한 모니터링 데이터 수집 (Tech Blog 조회수, 캐시 히트율 등)

## 📊 데이터 플로우

### 1. Pixabay 데이터
- **Data 서버 시작**: 스케줄러를 통해 주기적으로 Pixabay API 호출
- **AOP Intercept**: `@NotifyDiscord` 어노테이션으로 데이터 수집 시작 감지
- **데이터 수집**: 32개 음악 장르 + 20개 비디오 카테고리 병렬 수집 (Virtual Threads)
- **Redis 저장**: 수집된 데이터를 Redis에 캐싱 (서버 간 공유)
- **Discord 알림**: 수집 완료 시 통계 정보와 함께 비동기 알림 전송

### 2. Tech Blog 데이터
- **정기 수집**: 스케줄러가 지정된 시간(08:00, 13:00, 22:00)에 RSS/Atom 피드 수집
- **병렬 파싱**: 가상 스레드를 활용하여 여러 회사의 기술 블로그 피드를 동시에 파싱
- **중복 제거 및 저장**: 신규 게시물만 DB에 저장하고, 사라진 게시물은 Soft Delete 처리
- **조회 API**: 사용자가 요청 시 Redis 캐시 확인 후 없을 경우 QueryDSL로 DB 조회 (커서 페이징)

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
./gradlew :monitoring:bootJar

# 개별 실행
./gradlew :api:bootRun
./gradlew :data:bootRun
./gradlew :monitoring:bootRun
```

### 테스트
```bash
# 전체 테스트
./gradlew test

# 특정 모듈 테스트
./gradlew :api:test
./gradlew :data:test
./gradlew :monitoring:test

# 코드 포맷팅
./gradlew spotlessApply
```

### 접속 정보
| 서비스 | URL | 설명 |
|--------|-----|------|
| API Server | http://localhost:8080 | REST API 엔드포인트 |
| Data Server | http://localhost:8081 | 데이터 수집 서버 |
| Monitoring Server | http://localhost:8082 | 모니터링 서버 |
| Swagger UI | http://localhost:8080/api-docs/swagger-ui.html | API 문서 |
| API Health | http://localhost:8080/actuator/health | API 헬스체크 |
| Data Health | http://localhost:8081/actuator/health | Data 헬스체크 |
| Monitoring Health | http://localhost:8082/actuator/health | Monitoring 헬스체크 |
| Prometheus | http://localhost:9090 | 메트릭 수집 |
| Grafana | http://localhost:3000 | 모니터링 대시보드 |
| Redis | localhost:6379 | 캐시 스토어 |

### API 테스트 예시
```bash
# 헬스체크
curl http://localhost:8080/actuator/health

# 기술 블로그 목록 (페이징)
curl "http://localhost:8080/techblogs?limit=5"

# 특정 회사 필터링
curl "http://localhost:8080/techblogs?companySlug=naver"

# 게시물 클릭 수 증가
curl -X POST http://localhost:8080/techblogs/1/click

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

**CD - Monitoring Server:**
- `core/`, `monitoring/` 변경 감지 시 자동 배포
- Monitoring, Prometheus, Grafana 컨테이너 자동 배포
- 배포 후 헬스체크

### 배포 전략
- **멀티 서버 배포**: Main/Sub API 서버 병렬 배포
- **무중단 배포**: 헬스체크 기반 점진적 트래픽 전환
- **이미지 태깅**: Git SHA 기반 버전 관리
- **자동 정리**: 사용하지 않는 이미지 자동 삭제
