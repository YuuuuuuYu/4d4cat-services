package com.services.data.techblog;

import static org.assertj.core.api.Assertions.assertThat;

import com.services.core.techblog.entity.TechBlogCompany;
import com.services.core.techblog.entity.TechBlogPost;
import com.services.core.techblog.repository.TechBlogCompanyRepository;
import com.services.core.techblog.repository.TechBlogPostRepository;
import com.services.data.config.TestRedisConfig;
import com.services.core.fixture.TechBlogFixtures;
import com.services.data.techblog.scheduler.TechBlogDataScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
        "discord.webhook.url=${test.discord.webhook.url}"
})
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
class TechBlogDataLifecycleTest {

  @Autowired private TechBlogDataScheduler scheduler;

  @Autowired private TechBlogPostRepository postRepository;

  @Autowired private TechBlogCompanyRepository companyRepository;

  @BeforeEach
  void setUp() {
    postRepository.deleteAll();
    companyRepository.deleteAll();
  }

  @AfterEach
  void tearDown() {
    postRepository.deleteAll();
    companyRepository.deleteAll();
  }

  @Test
  void cleanupUnexposedPosts_DeletesUnexposedPosts() {
    TechBlogCompany company = companyRepository.save(TechBlogFixtures.createDefaultCompany());

    TechBlogPost exposedPost =
        TechBlogFixtures.createPost(
            company, "Exposed", TechBlogFixtures.DEFAULT_POST_URL_PREFIX + "1");
    postRepository.save(exposedPost);

    TechBlogPost unexposedPost =
        TechBlogFixtures.createPost(
            company, "Unexposed", TechBlogFixtures.DEFAULT_POST_URL_PREFIX + "2");
    unexposedPost.delete();
    postRepository.save(unexposedPost);

    // Act
    scheduler.cleanupUnexposedPosts();

    // Assert
    assertThat(postRepository.findByUrl(TechBlogFixtures.DEFAULT_POST_URL_PREFIX + "1"))
        .isPresent();
    assertThat(postRepository.findByUrl(TechBlogFixtures.DEFAULT_POST_URL_PREFIX + "2"))
        .isEmpty();
  }
}
