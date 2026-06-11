# Skill: Security (Secret Management & Protection)

이 스킬은 프로젝트의 보안 무결성을 유지하고 민감한 정보 유출 및 취약점을 방지하기 위한 작업을 수행할 때 탑재하는 가이드라인입니다.

---

## 1. 도메인 개요 및 비즈니스 핵심
- **목표:** API 키, 데이터베이스 패스워드 등의 자격 증명을 안전하게 관리하고, SQL Injection, XSS, 무단 CORS 접근을 차단하며, 에러 마스킹과 개인식별정보(PII)의 안전한 처리/익명화를 통해 애플리케이션의 높은 보안 규격을 보장하는 것을 목표로 합니다.

---

## 2. 주요 관련 설정 및 컴포넌트
- `.env` 및 `application.yml`: 외부 민감 키 및 설정값 환경 변수 매핑.
- `com.services.api.common.config.CorsConfig`: 허용된 도메인(`CORS_ALLOWED_ORIGINS`) 외의 웹 요청 제한.
- `com.services.api.common.exception.GlobalExceptionHandler`: 내부 시스템 구조가 응답으로 유출되지 않도록 하는 에러 마스킹 필터.
- `com.services.core.message.MessageValidator`: XSS 방지 필터링 및 입력값 유효성 검사.

---

## 3. 핵심 처리 지침 & 전문가 규칙

### 3.1 민감 정보 관리 및 노출 방지 (Secret Management)
- **Hardcoding 금지:** API 키(Pixabay), DB 비밀번호, Discord 웹훅 URL 등을 코드에 직접 기입하지 마십시오.
- **환경 변수 사용:** 모든 민감 정보는 `.env` 또는 시스템 환경 변수를 통해 주입받고, Spring의 `Environment` 객체나 `@Value`로만 참조하십시오.
- **Git 노출 차단:** `.env`, `api-run.log`, `data-run.log` 등 실 서버 정보가 담긴 파일이 `.gitignore`에 등록되어 있는지 항상 확인하십시오.

### 3.2 에러 마스킹 및 로깅 보안
- **Stack Trace 노출 금지:** API 응답에 내부 코드 구조나 에러 스택 트레이스를 포함하지 마십시오.
- **사용자 친화적 에러:** `GlobalExceptionHandler`를 통해 사용자에게는 사전에 정의된 `ErrorCode`와 친화적인 메시지만 전달하고, 상세 예외는 서버 로그에만 기록하십시오.
- **로깅 보안:** 사용자 비밀번호, API 키, 개인인증번호 등이 로그(`log.info`, `log.debug`)에 평문으로 찍히지 않도록 마스킹 처리(`*` 치환 등)를 하십시오.

### 3.3 입력값 검증 및 필터링
- **SQL Injection 방지:** JPA와 QueryDSL 사용 시 파라미터 바인딩 방식을 준수하여 SQL 인젝션을 차단하십시오. (Native Query 작성 시 하드코딩된 String 결합 금지)
- **XSS 방지:** 사용자가 제출한 텍스트에 HTML 태그, 스크립트가 포함되지 않도록 `MessageValidator` 등에서 특수 문자 필터링을 수행하십시오.
- **CORS 설정:** 허용된 도메인(`CORS_ALLOWED_ORIGINS`)에서만 API 접근이 가능하도록 CORS 정책을 보수적으로 유지하십시오.

---

## 4. 자가 검증 체크리스트
- [ ] 새로운 API 키나 설정값이 환경 변수로 분리 관리되는가?
- [ ] 에러 응답에 내부 시스템 정보나 DB 쿼리 구조가 노출되지 않는가?
- [ ] 외부 입력값에 대한 검증(Validation) 및 XSS 방지가 수행되는가?
- [ ] 로그에 민감한 정보(비밀번호, 토큰 등)가 평문으로 찍히지 않는가?
