# Skill: OmniWatch (JPA Metadata & Entity Optimization)

이 스킬은 복잡한 시계 데이터 모델과 JPA 연관 관계를 처리하는 `OmniWatch` 서비스 관련 작업을 수행할 때 탑재하는 가이드라인입니다.

---

## 1. 도메인 개요 및 비즈니스 핵심
- **목표:** 다양한 속성(브랜드, 소재, 태그 등)을 가진 시계 관련 메타데이터를 효율적으로 모델링 및 영속화하고, 다대다 관계 테이블을 최적화하여 O(1)~O(log N) 수준의 데이터 조회 속도를 제공하는 것을 목표로 합니다.

---

## 2. 주요 관련 파일

### 2.1 api/core 모듈 (엔티티 및 속성)
- `com.services.api.omniwatch.Watch`: 메인 시계 엔티티 (slug, 이름, 설명, 브랜드 등. slug 컬럼 인덱스 설정 필수).
- `com.services.api.omniwatch.brand.Brand`: 시계 브랜드 정보 엔티티.
- `com.services.api.omniwatch.WatchImage`: 시계 관련 이미지 정보 엔티티 (One-to-One).
- `com.services.api.omniwatch.WatchTag`, `WatchMaterial`, `WatchWork`: 시계의 다대다 속성들을 위한 중간 엔티티.
- `com.services.api.omniwatch.attribute.WatchType`: 시계 종류 (Enum).
- `com.services.api.omniwatch.attribute.Material`, `Tag`, `Work`: 시계의 세부 속성 정의.

### 2.2 공통 기반
- `com.services.api.persistence.BaseEntity`: 생성/수정 시간 자동 관리 및 소프트 딜리트(soft-delete)를 위한 공통 추상 클래스.

---

## 3. 핵심 처리 지침 & 전문가 규칙

### 3.1 엔티티 관계 및 지연 로딩 (N+1 방지)
- `Watch` 엔티티와 연관된 컬렉션(`watchTags`, `watchMaterials` 등) 접근 시 **N+1 문제**를 항상 경계하십시오.
- 모든 연관 관계는 반드시 `FetchType.LAZY`를 기본으로 설정하며, 대량 조회 시 `EntityGraph` 또는 `Fetch Join`을 사용하여 쿼리 최적화를 수행하십시오.

### 3.2 데이터 무결성, Auditing 및 DDD 지향
- 엔티티 생성/수정 시 `BaseEntity`를 상속받아 `createdAt`, `updatedAt`이 자동으로 기록되도록 하십시오.
- `slug`와 같은 고유 식별값 생성 시 중복 체크 로직을 반드시 포함하십시오.
- **DDD 지향:** 엔티티 내부에 비즈니스 로직(예: `updateBasicInfo`)을 캡슐화하여 상태 변경을 제어하십시오. 단순한 Setter 사용은 지양하고 의미 있는 비즈니스 메서드명을 사용하십시오.

---

## 4. 자가 검증 체크리스트
- [ ] JPA 연관 관계 설정이 `FetchType.LAZY`로 되어 있는가?
- [ ] 고유 식별자인 `slug`에 대한 인덱스 및 유니크 제약 조건이 설정되었는가?
- [ ] 복잡한 조회 쿼리 실행 시 실행 계획을 확인했는가?
- [ ] `BaseEntity` 상속을 통해 감사(Auditing) 기능이 활성화되었는가?

---

## 5. 테스트 검증 전략 (Testing Strategy)
- **공유 픽스처 활용:** `Watch` 관련 객체 생성 시 공통 픽스처를 우선 사용하십시오.
- **JPA Auditing 검증:** `@DataJpaTest` 등을 활용하여 `BaseEntity`의 `createdAt`, `updatedAt`이 영속화 시점에 정상적으로 기록되는지 확인합니다.
- **연관 관계 성능:** Fetch Join이 의도대로 동작하여 N+1 문제가 발생하지 않는지 쿼리 로그를 통해 검증하십시오.
- **Cascade 전이:** 부모 엔티티(`Watch`) 저장 시 자식 엔티티(`WatchTag` 등)가 함께 저장되는지 테스트합니다.
