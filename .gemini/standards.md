# Coding Standards

## 클래스 참조 및 Import 규약 (강력 준수)

- **FQN(전체 패키지 경로) 사용 절대 금지**: 코드 본문 내에서 `java.util.Date` 처럼 전체 경로를 쓰는 것을 금지합니다.
- **수정 절차**: 반드시 파일 상단에 `import`를 추가한 뒤, 본문에서는 클래스명만 사용하십시오.
- **예외 없음**: 단 한 번만 사용하는 클래스라도 반드시 `import` 규칙을 따릅니다.

```java
// ❌ 절대 금지 (FQN 사용)
java.util.Date now = new java.util.Date();

// ✅ 권장 (import 후 사용)
import java.util.Date;
...
Date now = new Date();
```

## Lombok 활용

```java
@Slf4j                      // 로깅
@Getter                     // getter 자동 생성
@RequiredArgsConstructor    // final 필드 생성자 주입
```

## 엔티티(Entity) 및 DTO 규약

### 1. DTO는 record 사용
- 데이터 전송 객체는 불변성을 보장하는 `record`를 사용합니다.

```java
// ✅ 권장
public record VideoResult(Long id, String url) {}

// ❌ 지양
@Getter @AllArgsConstructor
public class VideoResult { ... }
```

### 2. JPA 엔티티 상속 및 삭제 전략 (Soft/Physical Delete)
- **BaseEntity 상속**: 모든 엔티티는 `BaseEntity`를 상속받아 생성일, 수정일, `deleted` 필드를 공통으로 가져야 합니다.
- **삭제 전략 선택**:
    - **Soft Delete (핵심 엔티티)**: 게시글, 회사, 통계 등 보존 가치가 높은 엔티티는 클래스 레벨에 `@SQLRestriction("deleted = false")`와 `@SQLDelete`를 선언합니다.
    - **Physical Delete (종속 엔티티)**: 태그, 매핑 테이블 등 부모에 종속적이고 이력이 불필요한 엔티티는 어노테이션을 생략하여 물리 삭제합니다.
- **삭제 처리**: 서비스 계층에서 Soft Delete 엔티티를 삭제할 때는 `repository.delete()`를 호출하면 자동으로 상태가 변경(`deleted = true`)됩니다.
- **DTO 응답**: 컨트롤러 응답 시 엔티티를 직접 노출하지 말고, 반드시 DTO(record)로 변환하여 반환해야 합니다.

```java
// ✅ 권장: 핵심 엔티티 (Soft Delete)
@Entity
@SQLRestriction("deleted = false")
@SQLDelete(sql = "UPDATE techblog_post SET deleted = true WHERE id = ?")
public class TechBlogPost extends BaseEntity { ... }

// ✅ 권장: 종속 엔티티 (Physical Delete)
@Entity
public class TechBlogPostTag extends BaseEntity { ... } // 어노테이션 생략
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
- [ ] 모든 엔티티는 `BaseEntity`를 상속받았는가?
- [ ] 데이터 삭제 시 엔티티의 `delete()`를 호출하여 Soft Delete 처리했는가?
- [ ] 엔티티를 직접 노출하지 않고 DTO로 변환하여 응답했는가?
- [ ] 의존성 주입은 생성자 방식을 사용했는가?
- [ ] 모든 필드는 `final`로 선언했는가?
- [ ] API 응답은 `BaseResponse`로 통일했는가?
- [ ] 예외 처리는 `GlobalExceptionHandler`에서 하는가?
- [ ] 로깅은 `@Slf4j`를 사용했는가?
- [ ] 네이밍 컨벤션을 준수했는가?
- [ ] 코드 내에서 전체 패키지 경로(FQN) 대신 올바른 `import` 문을 사용했는가?
- [ ] 신규 서비스인 경우, monitoring-expert.md의 필수 메트릭 4종이 구현되었는가?
DTO로 변환하여 응답했는가?
- [ ] 의존성 주입은 생성자 방식을 사용했는가?
