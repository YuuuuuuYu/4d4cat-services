package com.services.core.applydays.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.services.core.applydays.dto.AdminApplicationDetailResponse;
import com.services.core.applydays.dto.AdminApplicationResponse;
import com.services.core.applydays.dto.TimelineBasicResponse;
import com.services.core.applydays.dto.TimelineDetailResponse;
import com.services.core.applydays.entity.QApplication;
import com.services.core.applydays.entity.QCategory;
import com.services.core.applydays.entity.QVerificationRequest;
import com.services.core.applydays.entity.VerificationStatus;
import com.services.core.common.persistence.entity.QCompany;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.util.StringUtils;

@RequiredArgsConstructor
public class ApplicationRepositoryImpl implements ApplicationRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  @Override
  public Slice<AdminApplicationResponse> findAllWithDetails(
      String companyName, LocalDateTime appliedAt, Pageable pageable) {
    QApplication application = QApplication.application;
    QCompany company = QCompany.company;
    QCategory cat = new QCategory("cat");
    QCategory parentCat = new QCategory("parentCat");

    BooleanBuilder builder = new BooleanBuilder();
    if (StringUtils.hasText(companyName)) {
      builder.and(
          company
              .name
              .containsIgnoreCase(companyName)
              .or(application.companySlug.containsIgnoreCase(companyName)));
    }
    if (appliedAt != null) {
      builder.and(application.appliedAt.goe(appliedAt));
    }

    List<AdminApplicationResponse> content =
        queryFactory
            .select(
                Projections.constructor(
                    AdminApplicationResponse.class,
                    application.id,
                    company.name.coalesce(application.companySlug),
                    application.appliedAt,
                    application.positionDetail,
                    application.categoryId,
                    parentCat.name,
                    cat.name))
            .from(application)
            .leftJoin(company)
            .on(application.companySlug.eq(company.slug))
            .leftJoin(cat)
            .on(application.categoryId.eq(cat.id))
            .leftJoin(parentCat)
            .on(cat.parentId.eq(parentCat.id))
            .where(builder)
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize() + 1)
            .fetch();

    boolean hasNext = false;
    if (content.size() > pageable.getPageSize()) {
      content.remove(pageable.getPageSize());
      hasNext = true;
    }

    return new SliceImpl<>(content, pageable, hasNext);
  }

  @Override
  public Optional<AdminApplicationDetailResponse> findApplicationDetailById(UUID id) {
    QApplication application = QApplication.application;
    QCompany company = QCompany.company;
    QCategory cat = new QCategory("cat");
    QCategory parentCat = new QCategory("parentCat");
    QVerificationRequest vr = QVerificationRequest.verificationRequest;

    return Optional.ofNullable(
        queryFactory
            .select(
                Projections.constructor(
                    AdminApplicationDetailResponse.class,
                    application.id,
                    application.companySlug,
                    company.name.coalesce(application.companySlug),
                    application.categoryId,
                    parentCat.name,
                    cat.name,
                    application.positionDetail,
                    application.appliedAt,
                    application.hiringProcess,
                    application.verificationStatus,
                    vr.rejectionReason,
                    application.channel))
            .from(application)
            .leftJoin(vr)
            .on(application.id.eq(vr.applicationId))
            .leftJoin(company)
            .on(application.companySlug.eq(company.slug))
            .leftJoin(cat)
            .on(application.categoryId.eq(cat.id))
            .leftJoin(parentCat)
            .on(cat.parentId.eq(parentCat.id))
            .where(application.id.eq(id))
            .fetchOne());
  }

  @Override
  public List<TimelineBasicResponse> findTimelineBasicByCompanySlug(
      String companySlug, String cursor, int limit) {
    QApplication application = QApplication.application;
    QCategory cat = new QCategory("cat");
    QCategory parentCat = new QCategory("parentCat");

    LocalDateTime cursorCreatedAt = null;
    UUID cursorId = null;

    if (cursor != null && !cursor.isBlank()) {
      try {
        String[] parts = cursor.split("_");
        if (parts.length == 2) {
          cursorCreatedAt = LocalDateTime.parse(parts[0]).truncatedTo(ChronoUnit.MICROS);
          cursorId = UUID.fromString(parts[1]);
        }
      } catch (Exception e) {

      }
    }

    return queryFactory
        .select(
            Projections.constructor(
                TimelineBasicResponse.class,
                application.id,
                parentCat.name,
                cat.name,
                application.appliedAt,
                application.createdAt))
        .from(application)
        .leftJoin(cat)
        .on(application.categoryId.eq(cat.id))
        .leftJoin(parentCat)
        .on(cat.parentId.eq(parentCat.id))
        .where(
            application
                .companySlug
                .eq(companySlug)
                .and(application.verificationStatus.eq(VerificationStatus.APPROVED))
                .and(cursorCondition(cursorCreatedAt, cursorId, application)))
        .orderBy(application.createdAt.desc(), application.id.desc())
        .limit(limit + 1)
        .fetch();
  }

  @Override
  public List<TimelineDetailResponse> findTimelineDetailByCompanySlug(
      String companySlug, String cursor, int limit) {
    QApplication application = QApplication.application;
    QCategory cat = new QCategory("cat");
    QCategory parentCat = new QCategory("parentCat");

    LocalDateTime cursorCreatedAt = null;
    UUID cursorId = null;

    if (cursor != null && !cursor.isBlank()) {
      try {
        String[] parts = cursor.split("_");
        if (parts.length == 2) {
          cursorCreatedAt = LocalDateTime.parse(parts[0]).truncatedTo(ChronoUnit.MICROS);
          cursorId = UUID.fromString(parts[1]);
        }
      } catch (Exception e) {
        // ignore invalid cursor
      }
    }

    return queryFactory
        .select(
            Projections.constructor(
                TimelineDetailResponse.class,
                application.id,
                parentCat.name,
                cat.name,
                application.appliedAt,
                application.createdAt,
                application.positionDetail,
                application.channel,
                application.hiringProcess))
        .from(application)
        .leftJoin(cat)
        .on(application.categoryId.eq(cat.id))
        .leftJoin(parentCat)
        .on(cat.parentId.eq(parentCat.id))
        .where(
            application
                .companySlug
                .eq(companySlug)
                .and(application.verificationStatus.eq(VerificationStatus.APPROVED))
                .and(cursorCondition(cursorCreatedAt, cursorId, application)))
        .orderBy(application.createdAt.desc(), application.id.desc())
        .limit(limit + 1)
        .fetch();
  }

  private BooleanExpression cursorCondition(
      LocalDateTime cursorCreatedAt, UUID cursorId, QApplication application) {
    if (cursorCreatedAt == null || cursorId == null) {
      return null;
    }

    return application
        .createdAt
        .lt(cursorCreatedAt)
        .or(application.createdAt.eq(cursorCreatedAt).and(application.id.lt(cursorId)));
  }
}
