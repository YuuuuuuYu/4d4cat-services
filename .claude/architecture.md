# Architecture & Design Patterns

## 계층형 아키텍처

### Presentation Layer (`presentation`)
- HTTP 요청 수신/응답, JSON 변환, 검증
- Controller 클래스 (예: `PixabayController`, `MessageController`)

### Application Layer (`application`)
- 핵심 비즈니스 로직, 도메인 모델 관리
- Service 클래스 (예: `PixabayVideoService`, `MessageService`)

### Infrastructure Layer (`infrastructure`)
- 외부 API 통신, 저장소, 환경 설정
- Configuration, Storage, External API Client

### Common/Util (`aop`, `util`)
- 횡단 관심사 및 유틸리티
- Aspect, Exception Handler, Config

## 주요 디자인 패턴

### 의존성 주입 (DI)
```java
@RequiredArgsConstructor  // 생성자 주입
public class PixabayController {
    private final PixabayVideoService service;  // final 필드
}
```

### DTO 패턴
```java
// record 타입 사용 (불변)
public record PixabayVideoResult(
    Long id,
    String pageURL,
    String type
) {}
```

### 관점 지향 프로그래밍 (AOP)
```java
@Aspect
@Component
public class DataInitializationAspect {
    @Around("execution(* ..DataInitializationService.initializeData(..))")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) {
        // 횡단 관심사 처리
    }
}
```

### 템플릿 메서드 패턴
- `DataInitializationService`에서 전체 흐름 정의
- 하위 클래스에서 세부 구현

### 전략 패턴
- `ParameterBuilder`를 통해 파라미터 생성 로직 캡슐화

### 싱글턴 패턴
- Spring Bean으로 관리되는 모든 컴포넌트

## 설계 원칙

### 불변성 (Immutability)
- `record` 타입 적극 활용
- `final` 필드 선언
- 불변 컬렉션 반환 (`List.of()`, `Collections.unmodifiableList()`)

### SOLID 원칙
- **SRP:** 각 클래스는 하나의 책임만
- **OCP:** 확장에는 열려있고, 수정에는 닫혀있음
- **LSP:** 상위 타입을 하위 타입으로 대체 가능
- **ISP:** 클라이언트는 사용하지 않는 인터페이스에 의존하지 않음
- **DIP:** 구체적인 것이 아닌 추상화에 의존

### Fail-Fast & Graceful Degradation
- 입력 검증은 빠르게 실패
- 외부 API 호출 실패는 우아하게 처리
- `CompletableFuture.exceptionally()`를 통한 개별 실패 처리
