# 4d4cat-services Project Unified Instructions

이 파일은 `4d4cat-services` 프로젝트에서 코드를 생성, 수정 및 검증할 때 Antigravity 에이전트가 준수해야 하는 **최우선순위 전역 지침(Single Source of Truth)**입니다. 모든 작업 전에 이 규약을 완벽히 숙지하고 준수하십시오.

---

## 1. 프로젝트 개요 및 기술 스택 (Overview & Tech Stack)

### 1.1 기본 정보
- **프로젝트명:** 4d4cat-services (외부 API 연동 및 데이터 관리를 위한 백엔드 서비스)
- **구조:** Gradle 멀티모듈 프로젝트

### 1.2 기술 스택
- **언어:** Java 21 (가상 스레드 활성화: `spring.threads.virtual.enabled=true`)
- **프레임워크:** Spring Boot 3.4.12
- **빌드 도구:** Gradle (Multi-Module)
- **저장소:** Redis (데이터 동기화, 캐싱 및 메시지 큐), PostgreSQL (JPA 영속성)
- **컨테이너 & 인프라:** Docker, Docker Compose, Cloudflare R2
- **모니터링:** Prometheus, Grafana, Actuator, Micrometer

---

## 2. 아키텍처 및 디자인 패턴 (Architecture & Design Patterns)

### 2.1 멀티모듈 아키텍처
```
api-server (api) ────▶ core
data-server (data) ──▶ core
monitoring-server (monitoring) ──▶ (metrics from all servers)
```
- **core (공통 라이브러리):** 모든 모듈의 공통 기반 (Exception, AOP, Redis Storage & Queue, Cloudflare R2 Config 등). 타 모듈에 대한 의존성 없음.
- **data (데이터 수집/비동기 워커):** 외부 API 수집, Redis Result Listener, 알림 메일 발송 스케줄러, 지원서 이미지 삭제 스케줄러.
- **api (API 제공/발행):** REST API 제공, Security/OAuth2, 비즈니스 요청 발행.
- **monitoring (관측성):** Actuator, Prometheus 연동 및 지표 노출.

### 2.2 핵심 설계 원칙
- **CQRS 패턴:** 서비스 계층은 상태 변경을 처리하는 `CommandService`와 조회를 처리하는 `QueryService`로 분리합니다.
- **ID 기반 참조 (Decoupling):** 엔티티 간 직접적인 객체 연관관계(`@ManyToOne` 등)를 지양하고, `company_slug`, `category_id` 등 기본 타입 필드를 사용합니다.
- **비동기 후처리 (Transactional Outbox):** 이메일 전송, 대량 알림 등 트랜잭션 안전성이 중요한 작업은 DB 내 `notification_queue` 테이블에 적재 후 `data` 모듈의 스케줄러가 일괄 발송합니다.
- **Lombok & Record:** 의존성 주입은 `@RequiredArgsConstructor`를 통한 생성자 주입을 원칙으로 하며, 모든 DTO는 불변성을 보장하는 Java `record`로 작성합니다.

---

## 3. 코딩 스타일 및 표준 규약 (Coding Standards)

### 3.1 클래스 참조 및 Import 규약 (강력 준수)
- **FQCN(Fully Qualified Class Name) 사용 절대 금지:** 코드 본문, 어노테이션, JPQL 쿼리 내에서 전체 패키지 경로(예: `java.util.Date`) 사용을 금지합니다.
- **Import 선언 의무화:** 반드시 파일 상단에 `import` 문을 추가한 뒤, 본문에서는 클래스명만 사용하십시오. 단 한 번만 사용하는 클래스라도 예외는 없습니다.

### 3.2 JPA 삭제 전략 (Soft vs Physical Delete)
- **Soft Delete (보존 데이터):** 게시글(`TechBlogPost`), 회사(`Company`), 지원서(`Application`) 등은 `BaseSoftDeleteEntity`를 상속받고 클래스 레벨에 `@SQLRestriction("deleted = false")`와 `@SQLDelete`를 선언합니다.
- **Physical Delete (임시 데이터 및 완전 삭제 대상):** 통계 데이터, 알림 큐 등은 `BaseEntity`를 상속받습니다. **특히 지원서 증빙 이미지(`VerificationImage`)는 개인정보 보호 및 스토리지 공간 절약을 위해 Soft Delete를 수행하지 않고 `BaseEntity`를 상속받아 Physical Delete를 사용합니다.**
- **삭제 처리 & 비동기 정리:** 
  - 서비스 계층에서 Soft Delete 엔티티를 삭제할 때는 `repository.delete()`를 호출하여 자동으로 상태를 변경(`deleted = true`)합니다.
  - **지원서 삭제 후속 조치:** 지원서(`Application`)가 삭제되면, `data` 모듈의 `VerificationImageCleanupScheduler` 스케줄러(매일 새벽 3시 실행)가 연관된 `VerificationImage`를 데이터베이스에서 조회하여, Cloudflare R2에서 실제 이미지 파일을 삭제한 후 데이터베이스에서도 해당 이미지를 완전 삭제(Physical Delete)합니다.
- **회원 탈퇴 및 익명화:** 탈퇴 처리 시 `email`, `name` 등 PII 데이터를 파기(Anonymize)하되, 지원 내역과 통계 데이터는 무결성을 위해 익명화 상태로 유지합니다.

### 3.3 RESTful API 네이밍 및 응답 형식
- `/{도메인}/{행동}` 과 같은 형식을 사용하며, `/api/v1/` 같은 버전형을 사용하지 않는다. (예: `GET /pixabay/video`)
- 모든 API 응답은 `BaseResponse<T>(code, message, data)` 규격을 준수합니다.

---

## 4. 테스트 가이드 (Testing Guide)

### 4.1 기본 규칙
- **파일명/메소드명:** `[테스트 대상]Test.java` / `[시나리오]_[예상결과]` (스네이크 케이스)
- **구조:** `Given-When-Then` 구조 및 `@DisplayName` 명시 필수.
- **Test Fixtures:** 멀티모듈 환경에서 테스트 객체 생성 로직의 중복 방지를 위해 `core` 모듈의 Test Fixtures를 활용합니다 (`core/src/testFixtures/java/com/services/core/fixture/`).

### 4.2 Redis Mocking 설정 (TestRedisConfig)
로컬이나 CI 환경(특히 Redis가 없는 headless CI 환경)에서도 테스트가 가능하도록 `api` 및 `data` 테스트 패키지에 `TestRedisConfig`를 로드하여 `RedisConnectionFactory`와 `RedisDataStorage` 등을 Mock 빈으로 등록합니다.
```java
@TestConfiguration
public class TestRedisConfig {
    @Bean @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        return Mockito.mock(RedisConnectionFactory.class);
    }

    @Bean @Primary
    public RedisDataStorage redisDataStorage() {
        return Mockito.mock(RedisDataStorage.class);
    }
}
```

---

## 5. 빌드 및 배포 가이드 (Build & Deployment)

### 5.1 빌드 및 실행 명령어
- **전체 빌드:** `./gradlew build` (Spotless 코드 포맷 정렬: `./gradlew spotlessApply`)
- **로컬 실행:** `docker-compose up -d` (Redis + Data Server + API Server)
- **개별 모듈 실행:** `./gradlew :data:bootRun` / `./gradlew :api:bootRun`

### 5.2 CI/CD 및 환경 변수
- **Secrets:** GitHub Actions 비밀값(`PIXABAY_KEY`, `DATA_SERVER_IP`, `MONITORING_SERVER_IP` 등)을 통해 이미지 빌드 및 SSH 배포가 제어됩니다.
- **환경 변수 (.env):** API Key 및 DB 커넥션 정보는 하드코딩하지 않고 `.env` 또는 시스템 환경 변수로 참조합니다.

### 5.3 로컬 CI/CD 테스트 (act)
로컬 환경에서 GitHub Actions 워크플로우를 실행하고 디버깅하기 위해 `act` 도구를 활용할 수 있습니다.
- **필수 조건:** Docker 데몬이 실행 중이어야 합니다.
- **주요 명령어:**
  - 전체 워크플로우 로컬 실행: `act`
  - 특정 Job만 로컬 실행 (예: test): `act -j test`
  - 이벤트 데이터를 JSON 파일로 전달하여 실행: `act -e event.json`
  - 환경변수 및 Secrets 전달: `act --secret-file .env`

---

## 6. AI 어시스턴트 필수 가드레일 (Guardrails)

1. **테스트 우선주의 (Test-First & 묶음 작업):** 기능 구현 완료 후 테스트를 미루지 마십시오. 핵심 로직 구현 시 반드시 테스트 코드를 함께 작성하고 검증 결과를 포함해야 합니다.
2. **최종 체크리스트 강제 확인 (Self-Reflection):** 작업을 완료하고 보고하기 전에, 관련된 `SKILL.md`에 정의된 **<작업 전 체크리스트>** 및 **<테스트 전략>** 항목을 모두 완수했는지 스스로 점검하고 점검 결과를 보고하십시오.
3. **빠른 피드백 및 토큰 최적화 (Fail-Fast Thinking):** 문제 해결 방향이 불확실하여 4분 이상 생각이 길어질 경우, 생각을 멈추고 현재 상태를 명시하여 사용자에게 질문하십시오.
4. **수술적 수정 (Surgical Update):** 단순히 한 줄 수정에 그치지 않고, 관련된 import 추가 및 아키텍처 규칙을 모두 준수하는 '완성된 단위'의 수정을 의미합니다.
5. **컨텍스트 동기화 규칙 (Doc Sync Rule):** 코드 수정, 새로운 도메인 추가 등의 작업이 완료되면 관련 스킬(`SKILL.md`)들이 최신 상태인지 검토하고 업데이트를 제안하십시오.
