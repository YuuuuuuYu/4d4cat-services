# Message Expert Skill Guide

이 서비스는 신속한 정보 공유와 알림 연동을 담당합니다. 관련 작업을 수행할 때 다음 지침을 준수하십시오.

## 🧠 핵심 처리 지침 (Core Instructions)

### 1. Discord 알림 연동
- 메시지 저장 로직 수정 시 `@NotifyDiscord` 어노테이션의 `taskName`이 올바르게 설정되었는지 확인하십시오.
- 알림 메시지의 가독성을 위해 `core` 모듈의 `DiscordWebhookPayload`와 `Embed` 구성을 적절히 활용하십시오.

### 2. 메시지 유효성 검증
- 모든 메시지는 저장 전 `MessageValidator`를 통해 철저히 검증되어야 합니다.
- 비즈니스 요구사항에 따라 최대 길이, 금지어 필터링 등의 로직을 추가할 때는 `core` 모듈의 `MessageValidator`를 확장하십시오.

### 3. Redis 저장소 캡슐화
- 메시지 저장/조회 시 직접 `RedisTemplate`을 사용하지 말고, 항상 `RedisMessageStorage`를 통해 추상화된 메서드를 사용하십시오.
- 키 명명 규칙(`message:last`)을 준수하여 데이터 정합성을 유지하십시오.

### 4. AOP 적용 확인
- `MessageService`에 `core` 모듈에서 제공하는 공통 AOP(`DiscordNotifierAspect`)가 정상적으로 적용되는지 확인하십시오.
- `api` 모듈의 `AspectConfig`에 `DiscordNotifierAspect`가 빈으로 등록되어 있어야 합니다.

## 🔍 작업 전 체크리스트
- [ ] 메시지 저장 시 Discord 알림이 정상적으로 트리거되는가?
- [ ] `MessageValidator`가 입력값에 대한 예외 처리를 수행하는가?
- [ ] Redis의 단일 키 구조를 유지하여 최신성 보장을 하고 있는가?
- [ ] 가상 스레드를 통해 알림 전송이 비동기적으로 이루어지는가?

## 🧪 테스트 전략 (Testing Strategy)
- **Mocking 대상**: `RedisMessageStorage`, `DiscordWebhookService`
- **검증 포인트**:
  - 빈 문자열이나 너무 긴 문자열 입력 시 `MessageValidator`가 `BadRequestException`을 발생시키는지 검증합니다.
  - 정상적인 메시지 저장 시 `RedisMessageStorage.saveMessage()`가 호출되는지 `verify()`를 통해 확인합니다.
  - 통합 테스트에서 AOP(`@NotifyDiscord`)가 동작하여 알림 전송 로직이 트리거되는지 검증합니다.
