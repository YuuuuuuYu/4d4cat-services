# Domain Workflows

본 문서는 프로젝트 전반에 걸친 공통 워크플로우를 다룹니다. 각 도메인별 상세 워크플로우는 도메인 가이드를 참조하십시오.

## 서비스별 상세 워크플로우 가이드

- **Pixabay (수집/조회):** [상세 가이드](.gemini/domains/pixabay.md)
- **Message (저장/알림):** [상세 가이드](.gemini/domains/message.md)
- **OmniWatch (JPA/속성):** [상세 가이드](.gemini/domains/omniwatch.md)
- **Monitoring (지표/시각화):** [상세 가이드](.gemini/domains/monitoring.md)

## 공통 워크플로우

### 전역 예외 처리 (GlobalExceptionHandler)
모든 모듈에서 발생하는 예외는 전역 핸들러를 통해 일관된 응답 형식으로 변환됩니다.
```
[Any Layer] → 예외 발생
                 ↓
       GlobalExceptionHandler
                 ↓
       1. ErrorCode 추출
       2. MessageSource로 메시지 조회
       3. BaseResponse 생성
                 ↓
[Client] ← JSON 에러 응답
```

### AOP Discord 알림 (core 모듈)
`@NotifyDiscord` 어노테이션이 붙은 메서드는 실행 전후로 Discord 알림을 트리거합니다.
```
@NotifyDiscord 어노테이션 메서드 실행
                 ↓
       DiscordNotifierAspect.notifyEvent()
                 ↓
       1. 실행 시간 측정 시작
       2. 메서드 실행 (joinPoint.proceed())
       3. 성공/실패 결과에 따라 페이로드 생성
       4. DiscordWebhookService.sendMessageAsync()
                 ↓
       [Discord Webhook] (비동기, Virtual Thread)
```

### Redis 데이터 저장 및 조회 추상화
모든 데이터 저장소 연동은 `core` 모듈의 인프라 계층을 통해 캡슐화됩니다.
- **Set 데이터 (O(1) 랜덤):** `RedisDataStorage` 활용
- **단일 메시지 데이터:** `RedisMessageStorage` 활용

## 스케줄링 및 이벤트 핸들링

- **ApplicationReadyEvent:** 서버 구동 완료 시 초기화 작업 수행 (예: 데이터 초기 수집)
- **@Scheduled:** 주기적인 데이터 동기화 및 갱신 (예: 6시간 주기, 매일 새벽 3시 등)
