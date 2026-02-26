# Project Overview

## 기본 정보
- **프로젝트명:** 4d4cat-services
- **목적:** 외부 API 연동 및 데이터 관리를 위한 백엔드 서비스
- **구조:** Gradle 멀티모듈

## 기술 스택
- **언어:** Java 21 (가상 스레드 활성화)
- **프레임워크:** Spring Boot 3.4.12
- **빌드 도구:** Gradle (Multi-Module)
- **컨테이너:** Docker, Docker Compose
- **HTTP 클라이언트:** RestClient (Spring 6.1+)
- **모니터링:** Prometheus, Grafana

## 모듈 구조

```
4d4cat-services/
├── core/           # 공통 라이브러리 모듈
├── data/           # 데이터 수집 서버 (port: 8081)
├── api/            # API 서버 (port: 8080)
└── monitoring/     # 모니터링 서버 (port: 8082)
```

### core 모듈
- 공통 예외 클래스 (`ErrorCode`, `CustomException` 등)
- 공통 DTO (`BaseResponse`, `ApiResponse`, `DataCollectionResult`)
- Redis 설정 및 저장소 (`RedisConfig`, `RedisDataStorage`, `RedisMessageStorage`)
- Pixabay DTO (`PixabayVideoResult`, `PixabayMusicResult`)
- AOP 및 알림 (`@NotifyDiscord`, `DiscordNotifierAspect`, `DiscordWebhookService`)
- 유틸리티 (`RandomUtils`)

### data 모듈
- Pixabay API 데이터 수집기 (`PixabayDataCollector`, `PixabayVideoCollector`, `PixabayMusicCollector`)
- 스케줄러 (`PixabayDataScheduler`)
- 서버 시작 시 데이터 초기화, 주기적 갱신
- `@NotifyDiscord` 어노테이션을 통한 데이터 수집 알림

### api 모듈
- REST API 컨트롤러 (`PixabayController`, `MessageController`)
- 서비스 (`PixabayService`, `MessageService`)
- JPA 엔티티 (`omniwatch` 패키지)
- `@NotifyDiscord` 어노테이션을 통한 메시지 저장 알림
- Prometheus 메트릭 노출 (`/actuator/prometheus`)

### monitoring 모듈
- Spring Boot 기반 애플리케이션
- Prometheus가 메트릭을 수집할 수 있도록 `/actuator/prometheus` 엔드포인트 노출
- Grafana와 연동하여 대시보드 시각화 환경 제공

## 주요 라이브러리

### 공통 (core)
- `Spring Data Redis` - 분산 캐시 및 데이터 동기화
- `Spring AOP` - 횡단 관심사 처리 (Discord 알림)
- `Spring Web` - RestClient (Discord 웹훅 전송)
- `Lombok` - 보일러플레이트 코드 감소

### data-server
- `Spring Web` - RestClient를 통한 API 호출
- `Spring Scheduling` - 주기적 데이터 수집
- `Virtual Threads` - 가상 스레드 기반 병렬 데이터 수집

### api-server
- `Spring Web` - REST API 개발
- `Spring Data JPA` - 데이터베이스 연동
- `SpringDoc OpenAPI (2.8.14)` - API 문서 자동화 (Swagger UI)
- `MessageSource` - 메시지 중앙 관리 (YAML 기반)

## 외부 서비스 연동

### Pixabay API
- **목적:** 비디오 및 음악 데이터 조회
- **인증:** 환경 변수 `PIXABAY_KEY` 필요
- **호출 위치:** data-server만 호출 (API 중복 호출 방지)

### Redis
- **목적:** 서버 간 데이터 동기화 및 캐싱
- **데이터:** Pixabay 비디오/음악 목록, 메시지
- **설정:** 환경 변수 `REDIS_HOST`, `REDIS_PORT`

### Discord Webhook
- **목적:** 주요 이벤트 실시간 알림 (데이터 수집, 메시지 저장 등)
- **설정:** 환경 변수 `DISCORD_WEBHOOK_URL` 필요
- **구현 위치:** core 모듈 (공통 AOP)
- **사용 위치:** data-server (데이터 수집), api-server (메시지 저장)

### Prometheus
- **목적:** 애플리케이션 메트릭 수집 및 저장
- **설정:** `prometheus/prometheus.yml` 파일을 통해 `api` 및 `monitoring` 서비스의 `/actuator/prometheus` 엔드포인트 스크랩

### Grafana
- **목적:** Prometheus를 통해 수집된 메트릭 시각화
- **설정:** Docker Compose를 통해 실행되며, Prometheus를 데이터 소스로 연결하여 대시보드 제공

## 필수 환경 변수

```bash
# Pixabay API (data-server)
PIXABAY_KEY=your_pixabay_api_key
PIXABAY_VIDEO_URL=https://pixabay.com/api/videos/
PIXABAY_MUSIC_URL=https://pixabay.com/api/music/

# Redis (core)
REDIS_HOST=localhost
REDIS_PORT=6379

# Discord (api-server)
DISCORD_WEBHOOK_URL=your_discord_webhook_url

# CORS (api-server)
CORS_ALLOWED_ORIGINS=http://localhost:3000

# Database (api-server)
OMNIWATCH_DB_URL=jdbc:mysql://localhost:3306/omniwatch
OMNIWATCH_DB_USERNAME=root
OMNIWATCH_DB_PASSWORD=password
```
