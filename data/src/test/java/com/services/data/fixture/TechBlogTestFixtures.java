package com.services.data.fixture;

import com.services.core.techblog.entity.TechBlogCompany;
import com.services.core.techblog.entity.TechBlogPost;
import java.time.LocalDateTime;

public class TechBlogTestFixtures {

  public static final String DEFAULT_COMPANY_SLUG = "woowahan";
  public static final String DEFAULT_COMPANY_NAME = "woowahan";
  public static final String DEFAULT_FEED_URL = "https://techblog.woowahan.com/feed";
  public static final String DEFAULT_POST_URL_PREFIX = "https://techblog.woowahan.com/";

  public static TechBlogCompany createDefaultCompany() {
    return new TechBlogCompany(DEFAULT_COMPANY_SLUG, DEFAULT_COMPANY_NAME, DEFAULT_FEED_URL);
  }

  public static TechBlogPost createPost(TechBlogCompany company, String title, String url) {
    return new TechBlogPost(company, title, url, LocalDateTime.now());
  }
}
