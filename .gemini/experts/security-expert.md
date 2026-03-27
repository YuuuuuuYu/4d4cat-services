# Security Expert Skill Guide

프로젝트의 보안 무결성을 유지하고 민감한 정보 유출을 방지하기 위한 핵심 지침입니다.

## 🧠 핵심 보안 지침 (Core Security Instructions)

### 1. 민감 정보 관리 (Secret Management)
- **Hardcoding 금지**: API 키(Pixabay), DB 비밀번호, Discord 웹훅 URL 등을 코드에 직접 기입하지 마십시오.
- **환경 변수 활용**: 모든 민감 정보는 `.env` 또는 시스템 환경 변수를 통해 주입받고, `Environment` 객체나 `@Value`로 참조하십시오.
- **Git 노출 방지**: `.env`, `api-run.log`, `data-run.log` 등 민감 정보가 포함될 수 있는 파일이 `.gitignore`에 등록되어 있는지 항상 확인하십시오.

### 2. 에러 마스킹 및 정보 노출 방지
- **Stack Trace 노출 금지**: API 응답에 내부 코드 구조나 스택 트레이스를 포함하지 마십시오.
- **사용자 친화적 에러**: `GlobalExceptionHandler`를 통해 사용자에게는 정해진 `ErrorCode`와 메시지만 전달하고, 상세 에러는 서버 로그에만 남기십시오.

### 3. 입력값 검증 및 필터링
- **SQL Injection 방지**: JPA와 QueryDSL 사용 시 파라미터 바인딩 방식을 준수하여 SQL 인젝션을 차단하십시오.
- **XSS 방지**: 메시지 저장 시 HTML 태그 등이 포함되지 않도록 `MessageValidator`에서 적절한 필터링을 수행하십시오.
- **CORS 설정**: `api` 모듈의 `CorsConfig`에서 허용된 도메인(`CORS_ALLOWED_ORIGINS`) 외의 접근을 엄격히 차단하십시오.

### 4. 로깅 보안
- **로그 내 비밀번호 제외**: 사용자 비밀번호나 API 키가 로그(`log.info`, `log.debug`)에 출력되지 않도록 주의하십시오.
- **민감 데이터 마스킹**: 로그에 출력해야 하는 민감 데이터가 있다면 일부를 별표(`*`) 처리하십시오.

## 🔍 작업 전 체크리스트
- [ ] 새로운 API 키나 설정값이 환경 변수로 관리되는가?
- [ ] 에러 응답에 내부 시스템 정보가 노출되지 않는가?
- [ ] 외부 입력값에 대한 검증(Validation)이 수행되는가?
- [ ] 로그에 민감한 정보가 평문으로 찍히지 않는가?
