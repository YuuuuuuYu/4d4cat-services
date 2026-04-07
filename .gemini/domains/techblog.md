# TechBlog Domain Guide (Business Logic)

기술 블로그들의 RSS/Atom 피드 데이터를 주기적으로 수집하고, 개발자들이 회사, 직군, 태그 등의 조건으로 최신 기술 아티클을 필터링하여 무한 스크롤 형태로 조회할 수 있도록 제공하는 비즈니스 도메인입니다.

## 📁 주요 관련 파일 (Domain Files)

### 1. 데이터 수집 로직 (data 모듈)
- `com.services.data.techblog.TechBlogRssCollector`: RSS/Atom 피드 파싱 및 데이터 수집 로직.
- `com.services.data.techblog.scheduler.TechBlogDataScheduler`: 데이터 수집(08:00, 13:00, 22:00) 및 미노출 포스트 일괄 삭제(00:00) 스케줄러.
- **인프라 상세**: [Data Module 가이드](../modules/data.md) 참조.

### 2. API 제공 로직 (api 모듈)
- `com.services.api.techblog.TechBlogController`: `/techblogs` 엔드포인트 노출 (목록 조회 및 필터링), 클릭 수 통계 집계 API.
- `com.services.api.techblog.TechBlogQueryService`: 복잡한 필터 조건(회사 slug, 직군, 태그) 및 커서 기반 무한 스크롤 처리를 위한 동적 쿼리 로직.
- **인프라 상세**: [API Module 가이드](../modules/api.md) 참조.

### 3. 데이터 명세 및 영속성 (core 모듈)
- 모든 엔티티는 `BaseEntity`를 상속받아 `createdAt`, `updatedAt`, `deleted` 필드를 공통으로 관리합니다.
- `com.services.core.techblog.entity.TechBlogCompany`: 회사 정보 (고유 식별자: 영문 Slug).
- `com.services.core.techblog.entity.TechBlogPost`: 기술 블로그 게시물. 실제 삭제 시에는 `BaseEntity`의 `deleted` 필드를 사용합니다.
- `com.services.core.techblog.entity.TechBlogPostTag`: 게시물 태그 매핑 테이블.
- `com.services.core.techblog.entity.TechBlogPostStat`: 포스트 클릭수 통계 정보.
- **인프라 상세**: [Core Module 가이드](../modules/core.md) 참조.

## ⚙️ 상세 비즈니스 워크플로우

### 1. RSS 피드 수집 및 갱신 프로세스 (08:00, 13:00, 22:00)
1. **타겟 로드**: 사전에 정의된 기술 블로그 피드 URL 목록을 로드합니다.
2. **병렬 수집**: 가상 스레드(Virtual Threads)를 활용하여 각 피드 URL에 대해 병렬로 파싱합니다.
3. **상태 갱신**: 
   - 피드에 존재하는 신규 게시물은 DB에 저장합니다.
   - 기존에 저장된 데이터 중 이번 피드 수집 목록에 없는 게시물은 노출되지 않도록 `deleted = true` 로 전환합니다.
4. **결과 보고**: 총 수집된 글 및 실패 건수를 알림으로 전송합니다.

### 2. 포스트 정리 프로세스 (00:00)
1. **일괄 삭제**: 데이터베이스에 `deleted = true` 로 전환되어 있는 미노출 게시물들을 물리적으로 완전히 삭제(Delete)합니다.

### 3. 피드 조회 및 무한 스크롤 프로세스
1. **요청 수신**: 클라이언트로부터 커서 값 및 필터 조건(slug 등)을 수신합니다. 초기 로드 사이즈는 5개로 제한합니다.
2. **동적 쿼리 수행**: Cursor 기반 페이징 방식으로 쿼리를 구성하여 효율적으로 DB를 조회합니다.

### 4. 조회수(클릭) 통계 집계
1. **클릭 이벤트**: 사용자가 포스트를 클릭할 때 통계 API를 호출합니다.
2. **통계 갱신**: `TechBlogPostStat` 테이블의 해당 포스트 `click_count`를 1 증가시킵니다.

## ⚠️ 비즈니스 규칙 및 주의 사항
- 블로그 식별 시 반드시 영문 Slug를 기반으로 조회 및 참조해야 합니다.
- 수집 스케줄러 동작 시 피드에서 누락된 글을 미노출로 전환하는 로직이 정상 작동해야 합니다.
- 통계 업데이트 시 발생할 수 있는 동시성 이슈(Lost Update)를 방지하기 위해 원자적 Update 쿼리를 사용합니다.
