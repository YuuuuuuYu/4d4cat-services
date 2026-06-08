package com.services.core.common.persistence.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.services.core.applydays.dto.CompanyListResponse;
import com.services.core.applydays.entity.QApplyDaysStatistics;
import com.services.core.common.dto.CompanyResponse;
import com.services.core.common.persistence.entity.CompanyStatus;
import com.services.core.common.persistence.entity.QCompany;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.util.StringUtils;

@RequiredArgsConstructor
public class CompanyRepositoryImpl implements CompanyRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  @Override
  public List<CompanyResponse> findAllActiveCompanies() {
    QCompany company = QCompany.company;
    return queryFactory
        .select(Projections.constructor(CompanyResponse.class, company.slug, company.name))
        .from(company)
        .where(company.feedUrl.isNotNull())
        .fetch();
  }

  @Override
  public List<CompanyResponse> searchByNameOrChosung(String query) {
    QCompany company = QCompany.company;
    return queryFactory
        .select(Projections.constructor(CompanyResponse.class, company.slug, company.name))
        .from(company)
        .where(
            company
                .name
                .startsWithIgnoreCase(query)
                .or(company.nameChosung.startsWithIgnoreCase(query)))
        .fetch();
  }

  @Override
  public Slice<CompanyListResponse> findAllVerifiedWithStats(String query, Pageable pageable) {
    QCompany company = QCompany.company;
    QApplyDaysStatistics statistics = QApplyDaysStatistics.applyDaysStatistics;

    BooleanBuilder builder = new BooleanBuilder();
    builder.and(company.status.eq(CompanyStatus.VERIFIED));
    builder.and(statistics.statType.eq("COMPANY"));
    builder.and(statistics.categoryId.isNull());
    builder.and(statistics.reviewCount.gt(0));

    if (StringUtils.hasText(query)) {
      builder.and(
          company.name.containsIgnoreCase(query).or(company.nameChosung.containsIgnoreCase(query)));
    }

    NumberExpression<Double> ghostingRate =
        new CaseBuilder()
            .when(statistics.reviewCount.gt(0))
            .then(statistics.ghostingCount.doubleValue().divide(statistics.reviewCount))
            .otherwise(0.0);

    List<CompanyListResponse> content =
        queryFactory
            .select(
                Projections.constructor(
                    CompanyListResponse.class,
                    company.slug,
                    company.name,
                    statistics.reviewCount,
                    statistics.ghostingCount,
                    statistics.stepStatistics,
                    ghostingRate))
            .from(company)
            .leftJoin(statistics)
            .on(company.slug.eq(statistics.companySlug))
            .where(builder)
            .orderBy(statistics.reviewCount.desc())
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
