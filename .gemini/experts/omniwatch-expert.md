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
- **Mocking 대상**: 불필요 (가급적 DataJpaTest를 활용한 통합 테스트 권장)
- **검증 포인트**:
  - JPA Repository 테스트 시 `@DataJpaTest`를 사용하여 쿼리가 예상대로 실행되는지(특히 Fetch Join) 확인합니다.
  - `BaseEntity`의 `createdAt`, `updatedAt`이 실제 영속화 시점에 자동으로 채워지는지 검증합니다.
  - N:1, 다대다 관계 엔티티 저장 시 Cascade 옵션이 정상 작동하여 연관 엔티티도 함께 저장/삭제되는지 테스트합니다.
