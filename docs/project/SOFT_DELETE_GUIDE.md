# Persistence deletion policy

Deletion behavior is explicit per entity. Do not infer it only from a superclass name.

## Building blocks

- `BaseEntity` supplies `createdAt` and `updatedAt` auditing fields.
- `BaseSoftDeleteEntity` adds `deleted` plus `delete()` and `restore()` state methods.
- Hibernate-managed soft delete requires both entity-level annotations:

```java
@SQLRestriction("deleted = false")
@SQLDelete(sql = "UPDATE <table> SET deleted = true WHERE id = ?")
```

The annotations stay on each entity because filtering and delete SQL are table-specific.

## Current strategies

### Hibernate-managed soft delete

`Application`, `Company`, `Member`, and `TechBlogPost` use `BaseSoftDeleteEntity` with `@SQLRestriction` and `@SQLDelete`. Normal repository reads hide deleted rows and `repository.delete(entity)` issues the configured update.

Do not repeat `deleted = false` in ordinary JPA/QueryDSL queries for these entities. Use a deliberate native query only when a workflow must include deleted rows.

### Explicit deleted flag

Some subscription entities extend `BaseSoftDeleteEntity` without Hibernate delete annotations. For example, payment-method removal calls the entity's `delete()` method and repositories query `...AndDeletedFalse`.

For these entities, every read/write path must preserve the explicit filtering convention. Adding `@SQLRestriction` is a behavior change and requires reviewing administrative/history queries and existing rows.

### Physical delete

Ephemeral or externally stored data such as `VerificationImage` extends `BaseEntity` and is physically deleted. Its cleanup flow deletes the R2 object before deleting the DB row.

`TechBlogPostTag` currently extends `BaseSoftDeleteEntity` but has no Hibernate soft-delete annotations. It is removed through the post association/cascade and physical cleanup flow; do not assume the inherited flag filters it automatically.

## Bulk operations and transactions

- `deleteAllInBatch` and explicit JPQL/native `DELETE` bypass `@SQLDelete`; use them only when physical deletion is intended.
- JPQL/native bulk update bypasses the managed persistence context. Flush pending entity changes first when needed, and do not use `clearAutomatically = true` in the middle of a transaction without accounting for detached entities.
- For parent/child cleanup, verify both database rows and external resources. A successful DB deletion must not leave R2 verification images orphaned.

When adding an entity, choose one of the three strategies from its retention, recovery, privacy, and query requirements, then test delete and visibility behavior explicitly.
