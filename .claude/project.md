# Project Overview

## 기본 정보
- **프로젝트명:** 4d4cat-services
- **목적:** 외부 API 연동 및 데이터 관리를 위한 백엔드 서비스

## 기술 스택
- **언어:** Java 21
- **프레임워크:** Spring Boot 3.4.12
- **빌드 도구:** Gradle

## 주요 라이브러리
- `Spring Web` - REST API 개발
- `Spring AOP` - 관점 지향 프로그래밍
- `Spring WebFlux (WebClient)` - 비동기 HTTP 통신
- `SpringDoc OpenAPI (2.8.14)` - API 문서 자동화 (Swagger UI)
- `Lombok` - 보일러플레이트 코드 감소
- `MessageSource` - 메시지 중앙 관리 (YAML 기반)

## 외부 서비스 연동

### Pixabay API
- **목적:** 비디오 및 음악 데이터 조회
- **인증:** 환경 변수 `PIXABAY_KEY` 필요
- **사용:** 애플리케이션 시작 시 데이터 초기화 및 실시간 검색

### Discord Webhook
- **목적:** 주요 이벤트 실시간 알림 (데이터 초기화 성공/실패 등)
- **설정:** 환경 변수 `DISCORD_WEBHOOK_URL` 필요
- **전송 방식:** `WebClient`를 통한 비동기 전송

## 필수 환경 변수

```bash
PIXABAY_KEY=your_pixabay_api_key
DISCORD_WEBHOOK_URL=your_discord_webhook_url
```
