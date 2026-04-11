# RSS 피드 수집 403 에러 및 테스트 안정성 개선

## 문제 상황
- **증상 1**: 특정 기술 블로그(예: 우아한형제들) RSS 피드 수집 시 `403 Forbidden` 에러 발생.
- **증상 2**: GitHub Actions CI 환경에서 실제 외부 URL을 호출하여 테스트가 간헐적으로 실패하거나 데이터가 수집되지 않아 `AssertionError` 발생.
- **발생 시점**: `TechBlogRssCollector` 실행 및 CI 테스트 단계.

## 원인 분석
1. **헤더 부재에 따른 차단**: 많은 기술 블로그 서버가 `User-Agent` 등 적절한 HTTP 헤더가 없는 자동화된 요청을 봇으로 간주하여 `403 Forbidden`으로 차단함.
2. **테스트 불가능한 구조**: 기존 `TechBlogRssCollector` 내부에서 `HttpClient`를 `new` 키워드로 직접 생성(Hard-coded)하여 사용하고 있었음.
    - 이로 인해 테스트 코드에서 `HttpClient`를 Mocking할 수 없었음.
    - 테스트 실행 시 항상 실제 외부 네트워크를 호출하게 되어 환경(CI 등)에 따라 결과가 불안정해짐.

## 해결 방법

### 1. HTTP 요청 헤더 최적화
`HttpClient`를 사용할 때 브라우저와 유사한 환경을 흉내 내기 위해 필수 헤더를 추가했습니다.
- `User-Agent`: 실제 브라우저 문자열 설정.
- `Accept`: XML 및 RSS 관련 MIME 타입 명시.
- `Cache-Control`: 캐시 우회 설정.

### 2. HttpClient 생성자 주입 및 Bean 등록
테스트 시 Mocking이 가능하도록 의존성 주입(DI) 구조로 리팩토링했습니다.
- **설정 클래스 추가**: `HttpClientConfig`를 생성하여 `HttpClient`를 Spring Bean으로 등록.
- **생성자 주입**: `TechBlogRssCollector`에서 `@RequiredArgsConstructor`를 통해 `HttpClient`를 주입받도록 수정.

### 3. 테스트 코드 안정화 (Mocking)
- `@MockitoBean`을 사용하여 주입된 `HttpClient`를 가짜 객체로 대체.
- 실제 네트워크 호출 없이 샘플 RSS XML 데이터를 반환하도록 Mocking 설정.
- CI 환경에서도 외부 요인 없이 비즈니스 로직(파싱, 저장)을 검증할 수 있게 됨.

## 예상 개선 효과
- **수집 성공률 향상**: 적절한 헤더 설정으로 서버 측 차단 회피.
- **테스트 속도 및 안정성**: 외부 네트워크 의존성을 제거하여 테스트 실행 속도가 빨라지고 CI 통과율이 100%로 보장됨.
- **유지보수 용이성**: `HttpClient` 설정을 한곳(`HttpClientConfig`)에서 관리 가능.

## 검증 방법
```bash
# 로컬에서 리팩토링된 테스트 실행
./gradlew :data:test --tests com.services.data.techblog.TechBlogRssCollectorTest
```
