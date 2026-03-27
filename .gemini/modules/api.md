# API Module Guide (REST Services)

`api` 모듈은 클라이언트의 요청을 처리하고 데이터를 제공하는 백엔드 서버입니다.

## 🏗️ 서비스 인프라 (Service Infrastructure)

### 1. 전역 예외 처리 (GlobalExceptionHandler)
도메인 전체에서 발생하는 모든 예외를 중앙에서 일관되게 처리합니다.
- **ErrorCode 매핑**: `core` 모듈의 `ErrorCode`를 사용하여 HTTP 상태 코드 및 응답 메시지를 결정합니다.
- **MessageSource 연동**: 다국어 또는 메시지 중앙 관리를 위해 YAML 기반의 메시지 소스를 활용합니다.
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<BaseResponse<Void>> handleCustomException(CustomException e) { ... }
}
```

### 2. JPA 및 데이터베이스 연동 (Persistence)
OmniWatch 등 도메인의 영속성 관리를 위해 Spring Data JPA를 사용합니다.
- **Auditing**: `BaseEntity`를 상속받아 `createdAt`, `updatedAt`이 자동 관리되도록 설정합니다.
- **연관 관계**: 지연 로딩(Lazy Loading)을 기본으로 하며 N+1 문제 방지를 위해 Fetch Join 및 EntityGraph를 활용합니다.

### 3. API 보안 및 설정 (Security & Config)
- **CORS**: 허용된 도메인(`CORS_ALLOWED_ORIGINS`)에서만 API 접근이 가능하도록 설정합니다.
- **OpenAPI/Swagger**: API 문서 자동화를 위해 SpringDoc을 사용하여 문서화 환경을 제공합니다.

## ⚙️ 주요 라이브러리 및 설정
- **Spring Web**: RESTful 컨트롤러 개발.
- **Spring Data JPA**: MySQL 등 RDB 연동 및 데이터 처리.
- **SpringDoc OpenAPI (2.8.x)**: `/swagger-ui/index.html`을 통한 문서화.
- **Lombok**: 보일러플레이트 코드 제거 및 가독성 확보.

## 📈 요청 처리 흐름 (Standard Flow)
1. **Request**: 클라이언트가 엔드포인트 호출.
2. **Validate**: 컨트롤러 진입 전 또는 서비스 계층에서 유효성 검사 수행.
3. **Logic**: `core` 모듈의 저장소 또는 JPA 레포지토리를 통해 비즈니스 로직 처리.
4. **Respond**: `BaseResponse`로 감싸서 표준 JSON 응답 반환.

## 🔗 의존성 및 영향도 (Dependency Map)
- **의존하는 모듈**: `core` (RedisStorage, Exception, AOP 사용)
- **영향을 받는 모듈**: `monitoring` (Prometheus 스크랩 대상이 됨)
