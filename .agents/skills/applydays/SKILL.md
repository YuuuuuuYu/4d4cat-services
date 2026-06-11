# Skill: ApplyDays (Business Logic & Expert Guidelines)

이 스킬은 채용 전형별 소요 시간 및 무통보(Ghosting) 데이터를 시각화하여 제공하는 `ApplyDays` 서비스 관련 작업을 수행할 때 탑재하는 가이드라인입니다.

---

## 1. 도메인 개요 및 비즈니스 핵심
- **목표:** 기존 테크블로그 기업 데이터를 기반으로 채용 전형별 소요 시간 및 무통보(Ghosting) 데이터를 수집/통계화하여 시각적으로 제공하고, 불투명한 채용 시장을 투명하게 만드는 것을 목표로 합니다.

---

## 2. 주요 관련 파일 (JPA, Batch, API, FE)

### 2.1 data 모듈 (통계 및 배치 작업)
- `com.services.data.applydays.batch.ApplyDaysBatchConfiguration`: 매일 새벽 04:00에 실행되는 Spring Batch 로직. PostgreSQL 17.9 `JSON_TABLE` 기반 통계 집계. 계층형 통계(기업/L1/L2)를 산출합니다.
- `com.services.data.config.QuerydslConfig`: `core` 모듈의 QueryDSL 리포지토리 지원을 위한 설정.
- `com.services.data.applydays.worker.AggregatedNotificationScheduler`: DB 알림 큐를 조회하여 사용자별 통합 메일 발송 수행.

### 2.2 api 모듈 (API 및 보안 로직)
- `com.services.api.common.config.SecurityConfiguration`: Spring Security 및 Google OAuth2 설정. 백오피스(`/api/v1/admin/**`) 권한 제어(`ROLE_ADMIN`) 포함.
- `com.services.api.common.security.service.CustomOAuth2UserService`: Google OAuth2 로그인 및 사용자 자동 가입/업데이트 로직.
- `com.services.api.applydays.controller.ApplyDaysController`: 지원서 등록 및 권한별 통계 조회 API 담당.
- `com.services.api.applydays.controller.VerificationController`: Cloudflare R2 연동 및 이미지 프록시 서빙 담당.
- `com.services.api.applydays.controller.AdminApplyDaysController`: 백오피스용 증빙 요청 관리 담당.
- `com.services.api.applydays.service.R2Service`: S3 호환 API를 이용한 파일 관리 및 presigned URL 생성 담당.
- `com.services.api.applydays.service.AdminApplyDaysCommandService`: 백오피스용 증빙 요청 관리 및 **DB 기반 알림 큐(NotificationQueue)** 저장 담당.
- `com.services.api.applydays.scheduler.VerificationImageCleanupScheduler`: soft delete된 증빙 이미지를 R2 스토리지에서 삭제하고 DB에서 정리하는 스케줄러.

### 2.3 core 모듈 (데이터 명세 및 영속성)
- `com.services.core.common.persistence.entity.Company`: 기존 엔티티에 `brn`(사업자번호) 및 `status` 컬럼 추가.
- `com.services.core.member.entity.Member` & `Role`: Google OAuth2 연동을 위한 사용자 엔티티 및 Role 관리.
- `com.services.core.applydays.entity.Application`: 비정형 지원 데이터 보관. `hiringProcess`는 `HiringStepDetail` 객체 리스트로 타입 안전하게 관리됩니다.
- `com.services.core.applydays.dto.HiringStepDetail`: 채용 전형 단계별 상세 정보 DTO.
- `com.services.core.applydays.entity.VerificationRequest`: 증빙 승인을 위한 요청 엔티티. `rejection_reason`은 이 엔티티에서 전담 관리합니다.

### 2.4 프론트엔드 (ApplyDays/ 디렉토리)
- **Tech Stack:** React 18 (TypeScript), Vite 5, Tailwind CSS v4, Recharts, wouter, TanStack Query v5, React Hook Form, Zod.
- **핵심 파일:** `home.tsx` (대시보드), `company-detail.tsx` (차트 시각화), `my-applications.tsx` (내 지원 내역 관리, 무한 스크롤 및 다중 동시 페칭 적용).

---

## 3. 핵심 처리 지침 & 전문가 규칙

### 3.1 탈결합 아키텍처 및 CQRS
- **Command & Query 분리:** 서비스 레이어는 상태를 변경하는 `CommandService`와 데이터를 조회하는 `QueryService`로 분리하십시오.
- **ID/Slug 기반 참조:** 엔티티 간 객체 연관관계(`@ManyToOne` 등)를 지양하고, 대신 `company_slug`, `category_id`와 같은 **기본 타입 필드**를 사용하십시오.
- **Dedicated DTO/Record:** `Map<String, Object>`나 `Map.of()`를 이용한 하드코딩 데이터 처리를 금지합니다. 반드시 전용 DTO 또는 Record 클래스를 정의하여 사용하십시오.

### 3.2 Spring Security 및 권한 관리
- **접근 제어:** 권한(`USER`, `REVIEWER`, `SUBSCRIBER`)에 따라 보안 설정을 적용하며, `ROLE_SUBSCRIBER`는 `ROLE_REVIEWER`를 포함하는 계층 구조를 가집니다.
- **성능 최적화:** 불필요한 `member` 조회를 피하기 위해 `Authentication` 객체의 `GrantedAuthority`를 우선 활용하십시오.

### 3.3 비정형 데이터(JSONB) 및 통계 배치
- **HiringStepDetail:** `Application` 엔티티의 `hiring_process` 필드는 `List<HiringStepDetail>` 타입을 사용하여 타입 안전하게 관리하십시오.
- **PostgreSQL 17.9 JSON_TABLE:** 통계 집계 시 Native Query와 `JSON_TABLE`을 활용하여 효율적으로 데이터를 평탄화하십시오.
- **무통보(Ghosting) 구분:** `GHOSTED` 상태의 데이터는 시간 평균 계산에서 제외하고 전용 카운트에 합산하십시오.

### 3.4 이미지 처리 및 보안
- **클라이언트 측 최적화:** 이미지 리사이징(1024px) 및 WebP 변환은 프론트엔드에서 수행하십시오.
- **이미지 프록시 API:** 외부 경로 은닉을 위해 `/applydays/verification/images/{imageId}` 프록시 엔드포인트를 통해 이미지를 서빙하십시오.
- **이미지 수명 주기 관리:** 지원 내역 삭제 시 연관 이미지를 반드시 soft delete 처리하고, `VerificationImageCleanupScheduler`를 통해 스토리지와 DB를 주기적으로 정리하십시오 (새벽 3시).
- **비동기 후처리:** 승인 시 알림 등 무거운 작업은 `notification_queue` 테이블을 통해 Transactional Outbox 패턴으로 처리하십시오.
- **Bulk Actions:** 다량의 요청을 처리할 때는 일괄 처리 메서드를 사용하십시오.

### 3.5 성능 최적화 및 캐싱 전략
- **N+1 및 메모리 부하 방지**:
  - **대량 데이터 인메모리 필터링 금지**: 전체 데이터를 로드(예: `findAll()`)한 뒤 Java Stream 등으로 필터링하는 방식은 OOM(Out of Memory)의 원인이 되므로, 반드시 DB 레벨에서 조건 필터링을 수행하는 쿼리 메서드(예: `findByNameChosungIsNull()`)를 정의하여 최적화하십시오.
  - **벌크 쓰기/삭제 최적화**: 복수의 데이터 삭제 시 단건 루프를 돌며 조회 및 삭제 쿼리를 발생시키지 말고, IN 조건 조회(`findByApplicationIdIn`)와 일괄 삭제 메서드(`deleteAllByIdInBatch`)를 사용하여 DB 왕복 비용을 최소화하십시오.
- **역직렬화(Serialization)와 캐싱**:
  - **Spring Redis Cache와 Record 타입 주의**: Jackson `DefaultTyping.NON_FINAL` 환경에서 Java `record`는 `final` 클래스이므로 `@class` 속성이 JSON에 직렬화되지 않습니다. 이를 캐시 조회 시 다형성(`java.lang.Object`)으로 캐스팅하려 할 때 `SerializationException`이 발생하므로, 캐시 대상 DTO는 final이 아닌 **일반 Serializable 클래스**(`public class`)로 구현하십시오.
  - **권한별 캐시 구분**: 사용자 권한에 따라 응답 정보가 제어되는 경우(예: `getCompanySummary`), 캐시 키에 권한 식별자도 조합(예: `key = "#companySlug + '_' + (#authentication != null ? #authentication.authorities.toString() : 'ANONYMOUS')"`)하여 데이터 오염을 방지하십시오.
  - **무효화(Eviction) 일치**: 신규 캐싱(예: `publicSummary`) 적용 시, 이를 무효화하는 리스너([StatisticsCacheEvictListener](file:///Users/yyh/IdeaProjects/4d4cat-services/data/src/main/java/com/services/data/applydays/batch/StatisticsCacheEvictListener.java)) 및 컨트롤러([ApplyDaysInternalController](file:///Users/yyh/IdeaProjects/4d4cat-services/data/src/main/java/com/services/data/applydays/ApplyDaysInternalController.java))의 무효화 배열에 동일한 캐시 이름이 관리되고 있는지 확인하십시오.

---

## 4. 자가 검증 체크리스트
- [ ] 서비스가 Command와 Query 서비스로 완전히 독립적으로 나뉘었는가?
- [ ] JPA Entity 간 직접적인 연관관계를 지양하고 ID/Slug 필드 참조를 하였는가?
- [ ] `JSON_TABLE` 통계 쿼리에서 `GHOSTED` 데이터가 제외 처리되었는가?
- [ ] 이미지 업로드 시 WebP 리사이징이 고려되었는가?
- [ ] soft delete된 이미지를 정리할 스케줄러가 정상 작동하는가?
- [ ] 승인 후처리 작업이 알림 큐(notification_queue)를 통해 비동기로 수행되는가?
- [ ] 목록형 UI에서 `useInfiniteQuery`와 무한 스크롤이 적용되었는가?
- [ ] 부모 컴포넌트 리렌더링 시 scroll jump를 막기 위해 리스트 렌더링 영역을 독립 컴포넌트로 분리했는가?
- [ ] 대량 데이터 조회 시 `findAll()` 후 스트림 필터링 대신 DB 조건 조회를 적용하였는가?
- [ ] 복수 데이터 삭제 시 N+1 쿼리를 예방하기 위해 `deleteAllByIdInBatch` 등으로 일괄 삭제 처리를 했는가?
- [ ] 캐시용 DTO가 Java `record` 타입으로 지정되어 Redis 역직렬화 오류(`missing type id property`)가 유발될 가능성이 없는가?
- [ ] 권한별로 조회 범위가 바뀌는 캐싱 대상에 대해 키 조합 처리가 완료되었는가?

---

## 5. 테스트 검증 전략 (Test Strategy)
- **CQRS 단위 테스트:** Command와 Query 서비스 각각의 비즈니스 로직을 검증하십시오.
- **QueryDSL 검증:** Custom Repository 구현체가 복잡한 조건을 올바르게 필터링하는지 확인하십시오.
- **소유권 검증:** `VerificationRequest`를 통해 사용자별 지원서 접근 권한이 올바르게 제어되는지 검증하십시오.
- **캐시 동작 테스트:** Redis 캐시 연동을 필요로 하는 테스트 시, 전후의 캐시 오염을 제어할 수 있도록 `BeforeEach` 등에서 `CacheManager`로 캐시를 비워주고 시작하는지 검증하십시오.
