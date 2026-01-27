# Domain Workflows

## Pixabay 도메인

### 워크플로우
```
[Client] → [Controller] → [Service] → [RestTemplate] → [Pixabay API]
                                           ↓
                          [DTO 변환] ← [API Response]
                                           ↓
[Client] ← [BaseResponse] ← [Controller]
```

### 엔드포인트
- `GET /api/v1/pixabay/videos` - 비디오 검색
- `GET /api/v1/pixabay/musics` - 음악 검색

### 처리 과정
1. 클라이언트 요청 (검색어, 페이지 파라미터)
2. Controller가 Service 호출
3. Service는 `RestTemplate`로 Pixabay API 호출
4. API 응답을 DTO로 변환
5. `BaseResponse`로 래핑하여 클라이언트에 반환

## Message 도메인

### 메시지 저장 (POST /api/v1/messages)
```
[Client] → [Controller] → [Service]
                              ↓
                    1. 내용 유효성 검사
                    2. Message 객체 생성
                    3. messageStore.put("lastMessage", message)
                              ↓
[Client] ← [BaseResponse] ← [Controller]
```

### 메시지 조회 (GET /api/v1/messages)
```
[Client] → [Controller] → [Service] → messageStore.get("lastMessage")
                                            ↓
[Client] ← [BaseResponse] ← [content 추출]
```

### 특징
- 인메모리 저장소 사용
- 항상 마지막 메시지만 유지
- 클라이언트 IP 자동 수집

## Common 도메인

### 데이터 초기화 (DataInitializationService)
```
[Application Startup] → ApplicationReadyEvent
                              ↓
                    initializeData() 실행
                              ↓
            CompletableFuture 병렬 처리
            ├─ 비디오 데이터
            ├─ 음악 데이터
            └─ 기타 데이터
                              ↓
                      DataStorage 저장
```

**특징:**
- 애플리케이션 시작 시 자동 실행
- `CompletableFuture`를 통한 병렬 처리
- 개별 API 실패 시 `exceptionally()`로 복구

### AOP 로깅 (DataInitializationAspect)
- 실행 시간 측정
- 성공/실패 결과를 Discord 웹훅으로 알림
- `MessageSource`를 통해 에러 메시지 조회

### 전역 예외 처리 (GlobalExceptionHandler)
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

### Discord 웹훅 (DiscordWebhookService)
- `WebClient`를 사용한 비동기 전송
- Fire-and-forget 방식 (재시도 없음)

### 데이터 저장소 (DataStorage)
- 인메모리 저장소
- Spring Bean으로 싱글턴 관리
- Thread-safe 고려 필요 시 `ConcurrentHashMap` 사용
