package com.services.api.techblog;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.services.api.techblog.dto.TechBlogCompanyResponse;
import com.services.api.techblog.dto.TechBlogListResponse;
import com.services.api.techblog.dto.TechBlogResponse;
import com.services.core.infrastructure.RedisDataStorage;
import com.services.core.techblog.entity.QTechBlogPost;
import com.services.core.techblog.entity.QTechBlogPostTag;
import com.services.core.techblog.entity.TechBlogPost;
import com.services.core.techblog.repository.TechBlogCompanyRepository;
import com.services.core.techblog.repository.TechBlogPostStatRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TechBlogQueryService {

  private final JPAQueryFactory queryFactory;
  private final TechBlogPostStatRepository statRepository;
  private final TechBlogCompanyRepository companyRepository;
  private final RedisDataStorage redisDataStorage;
  private final MeterRegistry registry;

  private static final String CACHE_KEY_PREFIX = "techblog:list:";
  private static final int CACHE_EXPIRATION_HOURS = 12;

  public TechBlogListResponse getTechBlogs(Long cursorId, List<String> companySlugs, String tag) {
    registry
        .counter(
            "techblog.api.request",
            "has_company_filter",
            String.valueOf(companySlugs != null && !companySlugs.isEmpty()),
            "has_tag_filter",
            String.valueOf(tag != null && !tag.isBlank()))
        .increment();

    String cacheKey = generateCacheKey(cursorId, companySlugs, tag);
    Optional<TechBlogListResponse> cachedResponse = redisDataStorage.getCache(cacheKey);

    if (cachedResponse.isPresent()) {
      registry.counter("techblog.cache.access", "status", "hit").increment();
      return cachedResponse.get();
    }

    registry.counter("techblog.cache.access", "status", "miss").increment();

    QTechBlogPost post = QTechBlogPost.techBlogPost;
    QTechBlogPostTag postTag = QTechBlogPostTag.techBlogPostTag;

    int limit = 5;

    List<TechBlogPost> result =
        Timer.builder("techblog.query.duration")
            .register(registry)
            .record(
                () -> {
                  var query = queryFactory
                      .selectFrom(post)
                      .leftJoin(post.company)
                      .fetchJoin();

                  if (tag != null && !tag.isBlank()) {
                    query.leftJoin(post.tags, postTag);
                  }

                  return query
                      .where(
                          cursorCondition(cursorId, post),
                          companyIn(companySlugs, post),
                          tagEq(tag, postTag),
                          post.deleted.eq(false))
                      .orderBy(post.id.desc())
                      .limit(limit + 1)
                      .distinct()
                      .fetch();
                });

    boolean hasNext = result.size() > limit;
    if (hasNext) {
      result.remove(limit);
    }

    Long nextCursor = result.isEmpty() ? null : result.get(result.size() - 1).getId();

    List<TechBlogResponse> items =
        result.stream().map(TechBlogResponse::from).collect(Collectors.toList());

    TechBlogListResponse response = new TechBlogListResponse(items, nextCursor, hasNext);

    redisDataStorage.setCache(cacheKey, response, CACHE_EXPIRATION_HOURS, TimeUnit.HOURS);

    return response;
  }

  @Transactional
  public void incrementClickCount(Long postId) {
    statRepository.incrementClickCount(postId);
    registry.counter("techblog.post.click").increment();
  }

  public List<TechBlogCompanyResponse> getActiveCompanies() {
    return companyRepository.findAllActiveCompanies().stream()
        .map(TechBlogCompanyResponse::from)
        .toList();
  }

  private String generateCacheKey(Long cursorId, List<String> companySlugs, String tag) {
    String companySlugKey = "all";
    if (companySlugs != null && !companySlugs.isEmpty()) {
      companySlugKey = companySlugs.stream().sorted().collect(Collectors.joining(","));
    }
    return CACHE_KEY_PREFIX
        + "cursor:"
        + (cursorId != null ? cursorId : "none")
        + ":company:"
        + companySlugKey
        + ":tag:"
        + (tag != null ? tag : "all");
  }

  private BooleanExpression cursorCondition(Long cursorId, QTechBlogPost post) {
    if (cursorId == null) {
      return null;
    }
    return post.id.lt(cursorId);
  }

  private BooleanExpression companyIn(List<String> companySlugs, QTechBlogPost post) {
    if (companySlugs == null || companySlugs.isEmpty()) {
      return null;
    }
    return post.company.slug.in(companySlugs);
  }

  private BooleanExpression tagEq(String tag, QTechBlogPostTag postTag) {
    if (tag == null || tag.isBlank()) {
      return null;
    }
    return postTag.tagName.eq(tag);
  }
}
