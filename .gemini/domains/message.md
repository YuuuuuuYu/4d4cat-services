# Message Domain Guide (Business Logic)

사용자의 메시지를 저장하고 조회하며, 실시간 알림을 제공하는 비즈니스 도메인입니다.

## 📁 주요 관련 파일 (Domain Files)

### 1. 메시지 처리 로직 (api 모듈)
- `com.services.api.message.MessageController`: `POST /message`, `GET /message` 엔드포인트.
- `com.services.api.message.MessageService`: 저장 시 `@NotifyDiscord` 어노테이션을 사용하여 알림 트리거.
- **인프라 상세**: [API Module 가이드](../modules/api.md) 참조.

### 2. 저장 및 검증 로직 (core 모듈)
- `com.services.core.infrastructure.RedisMessageStorage`: 최신 메시지 하나를 Redis에 저장하는 인프라 캡슐화.
- `com.services.core.message.MessageValidator`: 비즈니스 규칙에 따른 메시지 유효성 검사.
- `com.services.core.message.MessageRequest`: 메시지 입력 명세.
- **인프라 상세**: [Core Module 가이드](../modules/core.md) 참조.

## ⚙️ 상세 비즈니스 워크플로우

### 1. 메시지 등록 및 통지 프로세스
1. **입력 및 검증**: 사용자가 전송한 메시지를 `MessageValidator`를 통해 검사합니다.
2. **최신화**: Redis에 `message:last` 키로 저장하여 기존 메시지를 대체합니다.
3. **알림 발송**: `DiscordNotifierAspect`가 동작하여 저장된 메시지 내용을 관리자에게 즉시 전송합니다.

### 2. 메시지 조회 프로세스
1. **요청 수신**: 최신 메시지 조회 요청을 수신합니다.
2. **데이터 추출**: Redis에서 현재 저장된 단일 메시지 문자열을 가져옵니다.
3. **응답**: 조회된 메시지가 없을 경우 빈 값을 반환하거나 예외를 발생시킵니다.

## ⚠️ 비즈니스 규칙 및 주의 사항
- 메시지는 항상 가장 최근의 1개만 유지됩니다.
- 알림은 가상 스레드를 통해 비동기적으로 발송되므로 사용자 응답 속도에 영향을 주지 않습니다.
- 메시지 내용이 비어있거나 특정 금지어(추후 확장 가능)를 포함하는 경우 저장이 거부됩니다.
