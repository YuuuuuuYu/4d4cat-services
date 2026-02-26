# Coding Standards

## Lombok 활용

```java
@Slf4j                      // 로깅
@Getter                     // getter 자동 생성
@RequiredArgsConstructor    // final 필드 생성자 주입
```

## DTO는 record 사용

```java
// ✅ 권장
public record VideoResult(Long id, String url) {}

// ❌ 지양
@Getter @AllArgsConstructor
public class VideoResult { ... }
```

## RESTful API 네이밍

```java
// ✅ 권장
GET    /api/v1/messages          // 복수형 명사, 버전 명시
POST   /api/v1/messages

// ❌ 지양
GET    /api/v1/message           // 단수형
GET    /api/getMessage           // 동사 포함, 버전 누락
```

## 응답 형식 통일

```java
public record BaseResponse<T>(
    String code,
    String message,
    T data
) {
    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>("SUCCESS", "요청이 성공했습니다.", data);
    }
}
```

## 예외 처리

- 비즈니스 예외는 커스텀 예외 사용
- `GlobalExceptionHandler`에서 전역 처리
- 에러 메시지는 `MessageSource`에서 관리

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<BaseResponse<Void>> handleNotFoundException() {
        // ErrorCode → MessageSource → BaseResponse
    }
}
```

## 불변성

```java
// final 필드
private final MessageRepository repository;

// 불변 컬렉션
return List.of("item1", "item2");
return Collections.unmodifiableList(items);
```

## 의존성 주입

```java
// ✅ 권장: 생성자 주입
@RequiredArgsConstructor
public class Service {
    private final Repository repository;
}

// ❌ 지양: 필드 주입
@Autowired
private Repository repository;
```

## 로깅

```java
@Slf4j
public class Service {
    public void method() {
        log.debug("디버깅: param={}", param);
        log.info("정보: result={}", result);
        log.warn("경고: issue={}", issue);
        log.error("에러 발생", exception);
    }
}
```

## 네이밍 컨벤션

- **클래스:** PascalCase (`MessageService`)
- **메소드/변수:** camelCase (`saveMessage`, `userName`)
- **상수:** UPPER_SNAKE_CASE (`MAX_RETRY_COUNT`)
- **패키지:** 소문자 (`com.example.presentation`)

## 메소드 작성 원칙

- 단일 책임 원칙 (SRP)
- 최대 20-30줄 권장
- 파라미터는 3개 이하 (초과 시 DTO로 묶기)
- "무엇을"이 아닌 "왜"를 주석으로 설명

## 체크리스트

- [ ] Lombok을 적절히 사용했는가?
- [ ] DTO는 `record`로 작성했는가?
- [ ] 의존성 주입은 생성자 방식을 사용했는가?
- [ ] 모든 필드는 `final`로 선언했는가?
- [ ] API 응답은 `BaseResponse`로 통일했는가?
- [ ] 예외 처리는 `GlobalExceptionHandler`에서 하는가?
- [ ] 로깅은 `@Slf4j`를 사용했는가?
- [ ] 네이밍 컨벤션을 준수했는가?
