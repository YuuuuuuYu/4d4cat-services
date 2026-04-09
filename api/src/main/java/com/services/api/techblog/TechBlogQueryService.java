package com.services.api.techblog;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.services.api.techblog.dto.TechBlogListResponse;
import com.services.api.techblog.dto.TechBlogResponse;
import com.services.core.infrastructure.RedisDataStorage;
import com.services.core.techblog.entity.QTechBlogPost;
import com.services.core.techblog.entity.QTechBlogPostTag;
import com.services.core.techblog.entity.TechBlogPost;
import com.services.core.techblog.repository.TechBlogPostStatRepository;
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
  private final RedisDataStorage redisDataStorage;

  private static final String CACHE_KEY_PREFIX = "techblog:list:";
  private static final int CACHE_EXPIRATION_HOURS = 12;

  public TechBlogListResponse getTechBlogs(Long cursorId, String companySlug, String tag) {
    String cacheKey = generateCacheKey(cursorId, companySlug, tag);
    Optional<TechBlogListResponse> cachedResponse = redisDataStorage.getCache(cacheKey);

    if (cachedResponse.isPresent()) {
      return cachedResponse.get();
    }

    QTechBlogPost post = QTechBlogPost.techBlogPost;
    QTechBlogPostTag postTag = QTechBlogPostTag.techBlogPostTag;

    int limit = 5;

    List<TechBlogPost> result =
        queryFactory
            .selectFrom(post)
            .leftJoin(post.company)
            .fetchJoin()
            .leftJoin(post.tags, postTag)
            .where(
                cursorCondition(cursorId, post),
                companyEq(companySlug, post),
                tagEq(tag, postTag),
                post.deleted.eq(false))
            .orderBy(post.id.desc())
            .limit(limit + 1)
            .distinct()
            .fetch();

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
  }

  private String generateCacheKey(Long cursorId, String companySlug, String tag) {
    return CACHE_KEY_PREFIX
        + "cursor:"
        + (cursorId != null ? cursorId : "none")
        + ":company:"
        + (companySlug != null ? companySlug : "all")
        + ":tag:"
        + (tag != null ? tag : "all");
  }

  private BooleanExpression cursorCondition(Long cursorId, QTechBlogPost post) {
    if (cursorId == null) {
      return null;
    }
    return post.id.lt(cursorId);
  }

  private BooleanExpression companyEq(String companySlug, QTechBlogPost post) {
    if (companySlug == null || companySlug.isBlank()) {
      return null;
    }
    return post.company.slug.eq(companySlug);
  }

  private BooleanExpression tagEq(String tag, QTechBlogPostTag postTag) {
    if (tag == null || tag.isBlank()) {
      return null;
    }
    return postTag.tagName.eq(tag);
  }
}
