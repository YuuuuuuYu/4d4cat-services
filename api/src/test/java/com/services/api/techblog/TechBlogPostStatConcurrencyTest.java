package com.services.api.techblog;

import static org.assertj.core.api.Assertions.assertThat;

import com.services.api.fixture.TechBlogTestFixtures;
import com.services.core.techblog.entity.TechBlogCompany;
import com.services.core.techblog.entity.TechBlogPost;
import com.services.core.techblog.entity.TechBlogPostStat;
import com.services.core.techblog.repository.TechBlogCompanyRepository;
import com.services.core.techblog.repository.TechBlogPostRepository;
import com.services.core.techblog.repository.TechBlogPostStatRepository;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import com.services.api.config.TestRedisConfig;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
class TechBlogPostStatConcurrencyTest {

  @Autowired private TechBlogQueryService techBlogQueryService;

  @Autowired private TechBlogPostStatRepository statRepository;

  @Autowired private TechBlogPostRepository postRepository;

  @Autowired private TechBlogCompanyRepository companyRepository;

  private Long testPostId;

  @BeforeEach
  void setUp() {
    statRepository.deleteAll();
    postRepository.deleteAll();
    companyRepository.deleteAll();

    TechBlogCompany company = companyRepository.save(TechBlogTestFixtures.createDefaultCompany());
    TechBlogPost post = postRepository.save(TechBlogTestFixtures.createDefaultPost(company, 1));
    testPostId = post.getId();
    statRepository.save(TechBlogTestFixtures.createStat(testPostId, post.getTitle()));
  }

  @AfterEach
  void tearDown() {
    statRepository.deleteAll();
    postRepository.deleteAll();
    companyRepository.deleteAll();
  }

  @Test
  void incrementClickCount_ConcurrencyTest() throws InterruptedException {
    int threadCount = 100;
    ExecutorService executorService = Executors.newFixedThreadPool(32);
    CountDownLatch latch = new CountDownLatch(threadCount);

    for (int i = 0; i < threadCount; i++) {
      executorService.submit(
          () -> {
            try {
              techBlogQueryService.incrementClickCount(testPostId);
            } finally {
              latch.countDown();
            }
          });
    }

    latch.await();

    TechBlogPostStat stat = statRepository.findById(testPostId).orElseThrow();
    assertThat(stat.getClickCount()).isEqualTo(threadCount);
  }
}
