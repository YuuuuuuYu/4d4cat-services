# OmniWatch Expert Skill Guide

복잡한 시계 데이터 모델과 JPA 연관 관계를 다루는 전문 지침입니다.

## 🧠 핵심 처리 지침 (Core Instructions)

### 1. 엔티티 관계 및 지연 로딩
- `Watch` 엔티티와 연관된 컬렉션(`watchTags`, `watchMaterials` 등) 접근 시 **N+1 문제**를 항상 경계하십시오.
- 필요 시 `EntityGraph`나 `Fetch Join`을 사용하여 쿼리 최적화를 수행하십시오.

### 2. 데이터 무결성 및 Auditing
- 엔티티 생성/수정 시 `BaseEntity`를 상속받아 `createdAt`, `updatedAt`이 자동으로 기록되도록 하십시오.
- `slug`와 같은 고유 식별값 생성 시 중복 체크 로직을 반드시 포함하십시오.

### 3. 도메인 주도 설계 (DDD) 지향
- 엔티티 내부에 비즈니스 로직(예: `updateBasicInfo`)을 캡슐화하여 상태 변경을 제어하십시오.
- 단순한 Setter 사용은 지양하고 의미 있는 메서드명을 사용하십시오.

### 4. 확장 가능한 속성 관리
- 신규 속성(예: 방수 기능, 무브먼트 타입 등) 추가 시 기존 `attribute` 패키지의 패턴을 참고하여 Enum 또는 별도 엔티티로 분리하십시오.

## 🔍 작업 전 체크리스트
- [ ] JPA 연관 관계 설정이 `FetchType.LAZY`로 되어 있는가?
- [ ] 고유 식별자인 `slug`에 대한 인덱스 및 유니크 제약 조건이 설정되었는가?
- [ ] 복잡한 조회 쿼리 실행 시 실행 계획을 확인했는가?
- [ ] `BaseEntity` 상속을 통해 감사(Auditing) 기능이 활성화되었는가?

## 🧪 테스트 전략 (Testing Strategy)
- **공유 픽스처 활용**: `Watch` 관련 객체 생성 시 공통 픽스처를 우선 사용하십시오.
- **JPA Auditing 검증**: `@DataJpaTest` 등을 활용하여 `BaseEntity`의 `createdAt`, `updatedAt`이 영속화 시점에 정상적으로 기록되는지 확인합니다.
- **연관 관계 성능**: Fetch Join이 의도대로 동작하여 N+1 문제가 발생하지 않는지 쿼리 로그를 통해 검증하십시오.
- **Cascade 전이**: 부모 엔티티(`Watch`) 저장 시 자식 엔티티(`WatchTag` 등)가 함께 저장되는지 테스트합니다.
