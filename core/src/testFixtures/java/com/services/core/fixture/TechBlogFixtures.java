package com.services.core.fixture;

import com.services.core.common.persistence.BaseEntity;
import com.services.core.common.persistence.entity.Company;
import com.services.core.techblog.entity.TechBlogPost;
import com.services.core.techblog.entity.TechBlogPostTag;
import java.time.LocalDateTime;
import org.springframework.test.util.ReflectionTestUtils;

public class TechBlogFixtures {

  public static final String DEFAULT_COMPANY_SLUG = "woowahan";
  public static final String DEFAULT_COMPANY_NAME = "woowahan";
  public static final String DEFAULT_FEED_URL = "https://techblog.woowahan.com/feed";
  public static final String DEFAULT_POST_URL_PREFIX = "https://techblog.woowahan.com/";
  public static final String DEFAULT_POST_TITLE = "develop";

  public static Company createDefaultCompany() {
    return Company.builder()
        .slug(DEFAULT_COMPANY_SLUG)
        .name(DEFAULT_COMPANY_NAME)
        .feedUrl(DEFAULT_FEED_URL)
        .build();
  }

  public static TechBlogPost createPost(Company company, String title, String url) {
    return new TechBlogPost(company, title, url, LocalDateTime.now());
  }

  public static TechBlogPost createDefaultPost(Company company, int idSuffix) {
    return new TechBlogPost(
        company,
        DEFAULT_POST_TITLE + " " + idSuffix,
        DEFAULT_POST_URL_PREFIX + idSuffix,
        LocalDateTime.now());
  }

  public static TechBlogPostTag createTag(TechBlogPost post, String tagName) {
    return new TechBlogPostTag(post, tagName);
  }

  public static void setAuditingFields(BaseEntity entity) {
    LocalDateTime now = LocalDateTime.now();
    ReflectionTestUtils.setField(entity, "createdAt", now);
    ReflectionTestUtils.setField(entity, "updatedAt", now);
  }
}
