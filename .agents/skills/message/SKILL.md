# Skill: Message (Notification & Discord Integration)

이 스킬은 사용자의 메시지를 저장/조회하고 실시간 Discord 알림을 발송하는 `Message` 서비스 관련 작업을 수행할 때 탑재하는 가이드라인입니다.

---

## 1. 도메인 개요 및 비즈니스 핵심
- **목표:** 사용자가 입력한 메시지를 유효성 검사 후 저장하고, 저장 성공 시 가상 스레드를 활용해 관리자 Discord 채널로 실시간 알림을 전송하는 것을 목표로 합니다. 메시지는 항상 가장 최근의 단 1개만 저장 및 유지됩니다.

---

## 2. 주요 관련 파일

### 2.1 api 모듈 (API 및 서비스)
- `com.services.api.message.MessageController`: `POST /message`, `GET /message` 엔드포인트 제공.
- `com.services.api.message.MessageService`: 저장 시 `@NotifyDiscord` 어노테이션을 사용하여 비동기 알림 발송을 트리거합니다.

### 2.2 core 모듈 (데이터 명세 및 영속성)
- `com.services.core.infrastructure.RedisMessageStorage`: 최신 메시지 하나를 Redis에 저장하는 인프라 캡슐화.
- `com.services.core.message.MessageValidator`: 비즈니스 규칙에 따른 메시지 유효성 검사.
- `com.services.core.message.MessageRequest`: 메시지 입력 명세 (Record).

---

## 3. 핵심 처리 지침 & 전문가 규칙

### 3.1 Discord 알림 연동 및 AOP 적용
- **AOP 연동:** 메시지 저장 로직 수정 시 `@NotifyDiscord` 어노테이션의 `taskName`이 올바르게 설정되었는지 확인하십시오.
- **AspectConfig 확인:** `api` 모듈의 `AspectConfig`에 `DiscordNotifierAspect`가 빈으로 정상 등록되어 있어야 AOP가 올바르게 작동합니다.
- **비동기 발송:** 알림 전송은 Java 21 가상 스레드 환경에서 동작하여 사용자 응답 속도에 지장을 주지 않습니다.

### 3.2 메시지 검증 및 Redis 캡슐화
- **유효성 검증:** 메시지는 저장 전 반드시 `MessageValidator`를 통해 철저히 검증되어야 합니다 (빈 문자열 검사, 최대 길이 검사, 금지어 필터링 등).
- **인프라 추상화:** 직접 `RedisTemplate`을 사용해 Redis 키에 접근하는 것을 금지하며, 반드시 `RedisMessageStorage`를 통해 추상화된 메서드를 사용하고 키 명명 규칙(`message:last`)을 준수해야 합니다.

---

## 4. 자가 검증 체크리스트
- [ ] 메시지 저장 시 Discord 알림이 정상적으로 트리거되는가?
- [ ] `MessageValidator`가 입력값에 대한 예외 처리를 수행하는가?
- [ ] Redis의 단일 키 구조(`message:last`)를 유지하여 최신성이 보장되는가?
- [ ] 가상 스레드를 통해 알림 전송이 비동기적으로 이루어지는가?

---

## 5. 테스트 검증 전략 (Testing Strategy)
- **Mocking 대상:** `RedisMessageStorage`, `DiscordWebhookService`
- **검증 포인트:**
  - 빈 문자열이나 너무 긴 문자열 입력 시 `MessageValidator`가 `BadRequestException`을 발생시키는지 검증합니다.
  - 정상적인 메시지 저장 시 `RedisMessageStorage.saveMessage()`가 호출되는지 `verify()`를 통해 확인합니다.
  - 통합 테스트에서 AOP(`@NotifyDiscord`)가 동작하여 알림 전송 로직이 트리거되는지 검증합니다.
