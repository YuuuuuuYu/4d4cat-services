# Skill: TechBlog (RSS Harvesting & Article Search)

이 스킬은 외부 기술 블로그 RSS/Atom 피드를 수집하여 저장하고, 다양한 필터 조건과 커서 기반 무한 스크롤 기능을 제공하는 `TechBlog` 서비스 관련 작업을 수행할 때 탑재하는 가이드라인입니다.

---

## 1. 도메인 개요 및 비즈니스 핵심
- **목표:** 여러 기술 블로그들의 RSS/Atom 피드를 하루 3회 병렬로 안전하게 수집하여 저장하고, 피드에서 사라진 글은 소프트 딜리트 처리 후 자정에 물리 삭제합니다. 사용자는 회사 slug, 직군, 태그 등의 조건으로 최신 기술 아티클을 커서 기반 페이징(5개씩)으로 중복 없이 무한 스크롤로 조회할 수 있습니다.

---

## 2. 주요 관련 파일

### 2.1 data 모듈 (데이터 수집 및 스케줄링)
- `com.services.data.techblog.TechBlogRssCollector`: RSS/Atom 피드 파싱 및 데이터 수집 로직.
- `com.services.data.techblog.scheduler.TechBlogDataScheduler`: 데이터 수집(08:00, 13:00, 22:00 KST) 및 미노출 포스트 일괄 물리 삭제(00:00 KST) 스케줄러.

### 2.2 api 모듈 (API 및 서비스)
- `com.services.api.techblog.TechBlogController`: `/techblogs` 엔드포인트 노출.
- `com.services.api.techblog.TechBlogQueryService`: 복잡한 필터 조건(회사 slug, 직군, 태그) 및 커서 기반 무한 스크롤 처리를 위한 동적 쿼리 로직(QueryDSL 필수).

### 2.3 core 모듈 (데이터 명세 및 영속성)
- `com.services.core.common.entity.Company`: 회사 정보 (고유 식별자: 영문 Slug).
- `com.services.core.techblog.entity.TechBlogPost`: 기술 블로그 게시물 (Soft Delete 적용).
- `com.services.core.techblog.entity.TechBlogPostTag`: 게시물 태그 매핑 테이블 (Physical Delete 적용).

---

## 3. 핵심 처리 지침 & 전문가 규칙

### 3.1 스케줄링 및 데이터 라이프사이클 관리
- **수집 주기:** 매일 08:00, 13:00, 22:00 (KST)에 동작하도록 크론식을 설정합니다. `@Scheduled(cron = "0 0 8,13,22 * * *")`
- **미노출 상태 전환 (Soft Delete):** 피드 수집 시 최신 피드 목록에 없는 기존 DB 포스트들은 사용자에게 노출되지 않도록 `deleted = true`로 소프트 처리합니다.
- **포스트 정리 (Physical Delete):** 매일 00:00 (KST)에 `deleted = true`인 포스트들을 일괄 삭제(DELETE)하여 데이터베이스 크기를 관리합니다.

### 3.2 가상 스레드 기반 병렬 수집
- 피드 데이터 수집 로직 구현 시 반드시 **Java 21 가상 스레드(Virtual Threads)**를 활용하여 블로킹 I/O 대기 시간을 최소화해야 합니다.

### 3.3 커서 기반 무한 스크롤 및 FQCN 금지
- 데이터 정합성(중복/밀림 방지)을 위해 **커서(Cursor)** 기반으로 페이징을 구현하고, 초기 로딩/페이지당 사이즈는 **5개**로 제한합니다.
- 동적 쿼리는 반드시 QueryDSL로 구현하며, FQCN 사용 금지 규칙을 엄격히 적용합니다.

---

## 4. 자가 검증 체크리스트
- [ ] 수집 스케줄러(8, 13, 22시)와 삭제 스케줄러(0시)가 타임존(KST) 기반으로 명확하게 분리되었는가?
- [ ] 파싱된 피드에 없는 포스트를 미노출 처리(`deleted = true`)하는 갱신 로직이 존재하는가?
- [ ] 회사 조회 조건은 URL Slug를 기반으로 동작하는가?
- [ ] 피드 목록 조회 API가 오프셋 기반이 아닌 커서 기반으로 작동하며 기본 크기가 5인가?
- [ ] 모든 DTO가 `record`로 구현되었는가?
- [ ] 모든 엔티티가 `BaseEntity`를 상속받고 있는가?
- [ ] 핵심 엔티티 삭제 시 `repository.delete()`를 통해 논리 삭제를 수행하며, 종속 엔티티는 물리 삭제를 수행하는가?

---

## 5. 테스트 검증 전략 (Testing Strategy)
- **공유 픽스처:** `TechBlogFixtures`를 사용하여 테스트 객체를 생성하십시오.
- **수동 ID 엔티티 검증:** `TechBlogCompany`는 수동으로 ID를 할당하므로 `Persistable` 인터페이스를 통해 `isNew()`를 처리합니다. 테스트에서 `save()` 호출 후 `createdAt`이 정상적으로 채워지는지 반드시 검증하십시오.
- **라이프사이클 통합 테스트:** 미노출 상태 전환 및 0시 물리적 삭제 로직이 의도대로 동작하는지 검증합니다.
- **무한 스크롤 페이징 테스트:** 커서 기반 조회 시 중복 및 누락 없이 5개씩 반환되는지 확인합니다.
