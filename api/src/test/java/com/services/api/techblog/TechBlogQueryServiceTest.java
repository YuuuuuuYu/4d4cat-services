package com.services.api.techblog;

import static org.assertj.core.api.Assertions.assertThat;

import com.services.api.config.TestRedisConfig;
import com.services.core.fixture.TechBlogFixtures;
import com.services.api.techblog.dto.TechBlogListResponse;
import com.services.core.techblog.entity.TechBlogCompany;
import com.services.core.techblog.entity.TechBlogPost;
import com.services.core.techblog.repository.TechBlogCompanyRepository;
import com.services.core.techblog.repository.TechBlogPostRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
class TechBlogQueryServiceTest {

  @Autowired private TechBlogQueryService techBlogQueryService;

  @Autowired private TechBlogPostRepository postRepository;

  @Autowired private TechBlogCompanyRepository companyRepository;

  @BeforeEach
  void setUp() {
    postRepository.deleteAll();
    companyRepository.deleteAll();

    TechBlogCompany company = companyRepository.save(TechBlogFixtures.createDefaultCompany());

    for (int i = 1; i <= 10; i++) {
      TechBlogPost post = TechBlogFixtures.createDefaultPost(company, i);
      if (i % 2 == 0) {
        post.addTag(TechBlogFixtures.createTag(post, "backend"));
      } else {
        post.addTag(TechBlogFixtures.createTag(post, "frontend"));
      }
      postRepository.save(post);
    }
  }

  @AfterEach
  void tearDown() {
    postRepository.deleteAll();
    companyRepository.deleteAll();
  }

  @Test
  void getTechBlogs_FirstPage() {
    TechBlogListResponse response = techBlogQueryService.getTechBlogs(null, null, null);

    assertThat(response.items()).hasSize(5);
    assertThat(response.hasNext()).isTrue();
    assertThat(response.nextCursor()).isNotNull();
  }

  @Test
  void getTechBlogs_CursorPagination() {
    TechBlogListResponse firstPage = techBlogQueryService.getTechBlogs(null, null, null);
    Long nextCursor = firstPage.nextCursor();

    TechBlogListResponse secondPage = techBlogQueryService.getTechBlogs(nextCursor, null, null);

    assertThat(secondPage.items()).hasSize(5);
    assertThat(secondPage.hasNext()).isFalse();
    assertThat(firstPage.items().get(4).id()).isGreaterThan(secondPage.items().get(0).id());
  }

  @Test
  void getTechBlogs_FilterByCompany() {
    TechBlogListResponse response = techBlogQueryService.getTechBlogs(null, "woowahan", null);
    assertThat(response.items()).hasSize(5);
  }

  @Test
  void getTechBlogs_FilterByTag() {
    TechBlogListResponse response = techBlogQueryService.getTechBlogs(null, null, "backend");
    assertThat(response.items()).hasSize(5);
    assertThat(response.items()).allMatch(item -> item.tags().contains("backend"));
  }
}
