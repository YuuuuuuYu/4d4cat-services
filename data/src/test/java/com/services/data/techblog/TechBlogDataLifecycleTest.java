package com.services.data.techblog;

import static org.assertj.core.api.Assertions.assertThat;

import com.services.core.fixture.TechBlogFixtures;
import com.services.core.techblog.entity.TechBlogCompany;
import com.services.core.techblog.entity.TechBlogPost;
import com.services.core.techblog.repository.TechBlogCompanyRepository;
import com.services.core.techblog.repository.TechBlogPostRepository;
import com.services.data.config.TestRedisConfig;
import com.services.data.techblog.scheduler.TechBlogDataScheduler;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(properties = {
    "discord.webhook.url=${test.discord.webhook.url}"
})
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
class TechBlogDataLifecycleTest {

  @Autowired private TechBlogDataScheduler scheduler;

  @Autowired private TechBlogPostRepository postRepository;

  @Autowired private TechBlogCompanyRepository companyRepository;

  @Autowired private TransactionTemplate transactionTemplate;

  @Autowired private EntityManager entityManager;

  @BeforeEach
  void setUp() {
    cleanup();
  }

  @AfterEach
  void tearDown() {
    cleanup();
  }

  private void cleanup() {
    transactionTemplate.executeWithoutResult(status -> {
      entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE").executeUpdate();
      entityManager.createNativeQuery("DELETE FROM techblog_post_stat").executeUpdate();
      entityManager.createNativeQuery("DELETE FROM techblog_post_tag").executeUpdate();
      entityManager.createNativeQuery("DELETE FROM techblog_post").executeUpdate();
      entityManager.createNativeQuery("DELETE FROM techblog_company").executeUpdate();
      entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY TRUE").executeUpdate();
    });
  }

  @Test
  @DisplayName("논리 삭제된 게시글 물리 삭제 - 성공")
  void cleanupUnexposedPosts_shouldDeleteUnexposedPosts() {
    // Given
    TechBlogCompany company = companyRepository.save(TechBlogFixtures.createDefaultCompany());

    TechBlogPost exposedPost =
        TechBlogFixtures.createPost(
            company, "Exposed", TechBlogFixtures.DEFAULT_POST_URL_PREFIX + "1");
    postRepository.save(exposedPost);

    TechBlogPost unexposedPost =
        TechBlogFixtures.createPost(
            company, "Unexposed", TechBlogFixtures.DEFAULT_POST_URL_PREFIX + "2");
    postRepository.save(unexposedPost);

    postRepository.delete(unexposedPost);

    // When
    scheduler.cleanupUnexposedPosts();

    // Then
    assertThat(postRepository.findByUrl(TechBlogFixtures.DEFAULT_POST_URL_PREFIX + "1"))
        .isPresent();
    assertThat(postRepository.findByUrl(TechBlogFixtures.DEFAULT_POST_URL_PREFIX + "2"))
        .isEmpty();
  }
}
