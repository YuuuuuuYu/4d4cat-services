package com.services.data.techblog;

import com.services.core.infrastructure.RedisDataStorage;
import com.services.core.notification.DataCollectionResult;
import com.services.core.techblog.entity.TechBlogCompany;
import com.services.core.techblog.repository.TechBlogCompanyRepository;
import com.services.core.techblog.repository.TechBlogPostRepository;
import com.services.core.techblog.repository.TechBlogPostStatRepository;
import com.services.core.fixture.TechBlogFixtures;
import com.services.data.pixabay.PixabayMusicCollector;
import com.services.data.pixabay.PixabayVideoCollector;
import com.services.data.techblog.scheduler.TechBlogDataScheduler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "discord.webhook.url=${test.discord.webhook.url}"
})
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TechBlogRssCollectorTest {

    @Autowired
    private TechBlogDataScheduler dataScheduler;

    @Autowired
    private TechBlogCompanyRepository companyRepository;

    @Autowired
    private TechBlogPostRepository postRepository;

    @Autowired
    private TechBlogPostStatRepository statRepository;

    @MockitoBean
    private RedisDataStorage redisDataStorage;

    @MockitoBean
    private PixabayVideoCollector pixabayVideoCollector;

    @MockitoBean
    private PixabayMusicCollector pixabayMusicCollector;

    @Test
    @DisplayName("RSS 피드 수집 - 성공 (포스트 및 통계 저장 확인)")
    void collectFeeds_shouldSavePostsAndStats() throws InterruptedException {
        // Given
        TechBlogCompany company = TechBlogFixtures.createDefaultCompany();
        assertThat(company.getCreatedAt()).isNull(); // Should be null before save

        companyRepository.save(company);
        assertThat(company.getCreatedAt()).isNotNull(); // Should be populated by JPA Auditing

        // When
        DataCollectionResult result = dataScheduler.collectTechBlogFeeds();

        // Then
        long postCount = postRepository.count();
        long statCount = statRepository.count();

        assertThat(result).isNotNull();
        assertThat(result.totalItems()).isGreaterThan(0);
        assertThat(postCount).isEqualTo(result.totalItems());
        assertThat(statCount).isEqualTo(postCount);

        // Virtual Thread로 발송되는 디스코드 메시지가 완료될 때까지 대기 (데몬 스레드 종료 방지)
        Thread.sleep(2000);
    }
}
