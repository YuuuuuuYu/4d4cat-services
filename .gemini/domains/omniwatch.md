# OmniWatch Domain Guide

시계(Watch) 관련 메타데이터를 관리하고 제공하는 도메인입니다. 다양한 속성(브랜드, 소재, 태그 등)을 JPA를 통해 영속화합니다.

## 📁 주요 관련 파일 (Service Files)

### 1. 엔티티 (Entities) - api 모듈
- `com.services.api.omniwatch.Watch`: 메인 시계 엔티티 (slug, 이름, 설명, 브랜드 등)
- `com.services.api.omniwatch.brand.Brand`: 시계 브랜드 정보
- `com.services.api.omniwatch.WatchImage`: 시계 관련 이미지 정보 (One-to-One)
- `com.services.api.omniwatch.WatchTag`, `WatchMaterial`, `WatchWork`: 시계의 다대다 속성들을 위한 중간 엔티티

### 2. 속성 (Attributes) - api 모듈
- `com.services.api.omniwatch.attribute.WatchType`: 시계 종류 (Enum)
- `com.services.api.omniwatch.attribute.Material`, `Tag`, `Work`: 시계의 세부 속성 정의

### 3. 공통 기반 (Base)
- `com.services.api.omniwatch.dto.BaseEntity`: 생성/수정 시간 자동 관리를 위한 추상 클래스

## ⚙️ 핵심 워크플로우
1. **데이터 모델링**: 시계는 브랜드와 N:1 관계를 가지며, 태그/소재/작업 등과는 중간 테이블을 통해 다대다 관계를 형성합니다.
2. **조회 및 필터링**: 슬러그(slug)를 기반으로 고유한 시계 정보를 조회하거나, 각종 속성을 필터로 사용합니다.
3. **영속성 관리**: JPA Auditing을 통해 모든 엔티티의 변경 이력을 자동으로 추적합니다.

## ⚠️ 주의 사항
- `Watch` 엔티티는 `slug` 컬럼에 인덱스가 설정되어 있으므로, 조회 시 가급적 slug를 활용하십시오.
- 연관 관계가 복잡하므로 지연 로딩(Lazy Loading) 설정을 확인하고 N+1 문제를 방지해야 합니다.
