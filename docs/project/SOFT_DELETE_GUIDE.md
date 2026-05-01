# 소프트 삭제(Soft Delete) 가이드

## 1. 개요
본 프로젝트는 데이터의 물리적 삭제 대신 논리적 삭제 상태를 관리하는 **소프트 삭제(Soft Delete)** 방식을 채택합니다. Hibernate 6.4+의 `@SoftDelete` 기능의 불안정성(연관 관계 매핑 버그)을 피하기 위해, 가장 안정적인 **`@SQLRestriction` + `@SQLDelete`** 조합을 사용합니다.

---

## 2. 핵심 구현 전략

### 2.1. BaseEntity (공통 필드)
모든 엔티티는 `BaseEntity`를 상속받아 `deleted` 컬럼을 공통으로 가집니다.

```java
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {
    @CreatedDate
    @Column(updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;
}
```

### 2.2. 개별 엔티티 (명시적 설정)
Hibernate의 매핑 엔진과 Querydsl의 호환성을 위해 **각 엔티티 클래스 레벨**에 아래 어노테이션을 반드시 명시합니다. `@MappedSuperclass`에만 선언할 경우 일부 조회 경로에서 필터가 누락될 수 있습니다.

```java
@Entity
@Table(name = "techblog_post")
@SQLRestriction("deleted = false") // 조회 시 자동으로 WHERE deleted = false 추가
@SQLDelete(sql = "UPDATE techblog_post SET deleted = true WHERE id = ?") // delete() 호출 시 UPDATE 실행
public class TechBlogPost extends BaseEntity {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // ...
}
```

---

## 3. 예외: 물리 삭제(Physical Delete) 적용 기준
모든 엔티티가 `BaseEntity`를 상속받더라도, 다음 조건에 해당하면 `@SQLRestriction`과 `@SQLDelete`를 생략하고 물리 삭제를 수행합니다.

### 3.1. 선정 기준
1.  **독립적 가치 부족**: 해당 엔티티가 부모 엔티티에 완전히 종속되어 있으며, 부모 없이 존재할 수 없는 경우 (예: `TechBlogPostTag`).
2.  **데이터 복구 불필요**: 삭제된 이력을 추적하거나 복구할 비즈니스적 요구사항이 없는 경우.
3.  **저장 공간 및 인덱스 효율**: 매핑 테이블의 경우 데이터가 무의미하게 쌓이는 것을 방지하여 쿼리 성능을 유지합니다.

### 3.2. 구현 예시 (`TechBlogPostTag`)
`BaseEntity`를 상속받아 감사(Auditing) 필드는 유지하되, Hibernate 어노테이션은 제외합니다.
```java
@Entity
@Table(name = "techblog_post_tag")
public class TechBlogPostTag extends BaseEntity {
    // @SQLRestriction, @SQLDelete 생략 -> 물리 삭제 수행
}
```

---

## 4. 개발 규칙 및 주의 사항

### 3.1. 조회 (Select)
*   **자동 필터링**: JPA Repository, Querydsl, Fetch Join 등 모든 조회 시 `deleted = false` 조건이 자동으로 붙습니다.
*   **수동 조건 금지**: 쿼리 작성 시 `where(post.deleted.eq(false))`를 중복해서 작성하지 마십시오.

### 3.2. 삭제 (Delete)
*   **표준 메서드 사용**: `repository.delete(entity)` 또는 `repository.deleteById(id)`를 호출하면 `@SQLDelete`에 정의된 SQL이 실행되어 논리 삭제됩니다.
*   **주의**: `deleteAllInBatch()`는 `@SQLDelete`를 타지 않고 물리 삭제를 시도하므로, 대량 삭제 시에는 별도의 `@Modifying` 쿼리를 작성하거나 주의가 필요합니다.

### 3.3. 삭제된 데이터 접근 (Native Query)
소프트 삭제된 데이터를 포함하여 조회해야 하는 경우(예: 중복 URL 체크)에는 **Native SQL**을 사용해야 필터를 우회할 수 있습니다.

```java
@Query(value = "SELECT * FROM techblog_post WHERE url = :url", nativeQuery = true)
Optional<TechBlogPost> findByUrlIncludeDeleted(@Param("url") String url);
```

---

## 4. 왜 @SoftDelete를 사용하지 않나요?
Hibernate 6.4에 도입된 `@SoftDelete`는 편리하지만, 현재 버전(6.6 기준)에서 다음과 같은 치명적인 한계가 있습니다:
1. **연관 관계 오류**: `ManyToOne` 관계에서 대상 엔티티가 `String` ID를 사용하거나 복잡한 매핑을 가질 때 `UnsupportedMappingException`을 발생시킵니다.
2. **전역 설정의 불안정성**: `BaseEntity`에 적용 시 모든 하위 엔티티의 매핑 메타데이터를 오염시키는 사례가 보고되었습니다.

따라서 명시적이고 안정적인 `@SQLRestriction` 방식을 권장합니다.
