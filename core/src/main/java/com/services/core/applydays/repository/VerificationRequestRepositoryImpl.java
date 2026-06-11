package com.services.core.applydays.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.services.core.applydays.dto.AdminPendingRequestResponse;
import com.services.core.applydays.entity.QApplication;
import com.services.core.applydays.entity.QCategory;
import com.services.core.applydays.entity.QVerificationRequest;
import com.services.core.applydays.entity.VerificationStatus;
import com.services.core.common.persistence.entity.QCompany;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

@RequiredArgsConstructor
public class VerificationRequestRepositoryImpl implements VerificationRequestRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  @Override
  public Slice<AdminPendingRequestResponse> findAllWithDetailsByStatus(
      VerificationStatus status, Pageable pageable) {
    QVerificationRequest vr = QVerificationRequest.verificationRequest;
    QApplication application = QApplication.application;
    QCompany company = QCompany.company;
    QCategory cat = new QCategory("cat");
    QCategory parentCat = new QCategory("parentCat");

    List<AdminPendingRequestResponse> content =
        queryFactory
            .select(
                Projections.constructor(
                    AdminPendingRequestResponse.class,
                    vr.id,
                    vr.applicationId,
                    application.appliedAt,
                    company.name.coalesce(application.companySlug),
                    application.positionDetail,
                    vr.status,
                    application.categoryId,
                    parentCat.name,
                    cat.name))
            .from(vr)
            .join(application)
            .on(vr.applicationId.eq(application.id))
            .leftJoin(company)
            .on(application.companySlug.eq(company.slug))
            .leftJoin(cat)
            .on(application.categoryId.eq(cat.id))
            .leftJoin(parentCat)
            .on(cat.parentId.eq(parentCat.id))
            .where(vr.status.eq(status))
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
}
