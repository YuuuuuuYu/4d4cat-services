# Project Overview

## 기본 정보
- **프로젝트명:** 4d4cat-services
- **목적:** 외부 API 연동 및 데이터 관리를 위한 백엔드 서비스
- **구조:** Gradle 멀티모듈

## 기술 스택
- **언어:** Java 21 (가상 스레드 활성화)
- **프레임워크:** Spring Boot 3.4.12
- **빌드 도구:** Gradle (Multi-Module)
- **저장소:** Redis (데이터 동기화 및 캐싱), MySQL (JPA 영속성)
- **컨테이너:** Docker, Docker Compose
- **모니터링:** Prometheus, Grafana

## 서비스 도메인 (Domains)

각 서비스의 구체적인 구현과 가이드는 아래 문서를 참조하십시오.

- **Pixabay:** [데이터 수집 및 랜덤 조회](.gemini/domains/pixabay.md)
- **Message:** [메시지 저장 및 Discord 알림](.gemini/domains/message.md)
- **OmniWatch:** [시계 정보 JPA 관리](.gemini/domains/omniwatch.md)
- **Monitoring:** [시스템 지표 및 시각화](.gemini/domains/monitoring.md)

## 모듈 역할 (Module Roles)

```
4d4cat-services/
├── core/           # 모든 모듈의 공통 기반 (Exception, AOP, Redis Storage 등)
├── data/           # 외부 데이터 수집 및 초기화 담당 (Collector, Scheduler)
├── api/            # 외부 API 제공 및 비즈니스 로직 처리 (Controller, JPA)
└── monitoring/     # 시스템 통합 가시성 확보 (Actuator, Prometheus 연동)
```

## 주요 공통 기술 및 연동

### 가상 스레드 (Virtual Threads)
- I/O 집약적 작업(수집 API 호출, Discord 웹훅 전송)의 성능 최적화를 위해 프로젝트 전반에 가상 스레드가 활성화되어 있습니다.

### Redis (Centralized Storage)
- 서버 간 데이터 동기화 및 빠른 랜덤 추출을 위해 Redis Set과 String 자료구조를 활용합니다.

### Discord Webhook (Unified Notification)
- `core` 모듈의 `@NotifyDiscord` 어노테이션을 통해 주요 서비스 이벤트를 실시간으로 통지합니다.

### 전역 예외 처리 및 응답 포맷
- 모든 API 응답은 `BaseResponse` 형식을 준수하며, `GlobalExceptionHandler`를 통해 중앙 집중식으로 예외를 처리합니다.

## 필수 환경 변수 및 설정
상세한 배포 가이드와 환경 변수 설정은 [Build & Deployment](.gemini/deployment.md) 문서를 참조하십시오.
