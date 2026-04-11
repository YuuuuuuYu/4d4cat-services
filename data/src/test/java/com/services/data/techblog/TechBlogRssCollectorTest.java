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
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    @MockitoBean
    private HttpClient httpClient;

    @Test
    @DisplayName("RSS 피드 수집 - 성공 (포스트 및 통계 저장 확인)")
    void collectFeeds_shouldSavePostsAndStats() throws Exception {
        // Given
        TechBlogCompany company = TechBlogFixtures.createDefaultCompany();
        companyRepository.save(company);

        String mockRss = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<rss version=\"2.0\">" +
                "  <channel>" +
                "    <title>Woowahan Tech Blog</title>" +
                "    <link>https://techblog.woowahan.com</link>" +
                "    <item>" +
                "      <title>Test Post 1</title>" +
                "      <link>https://techblog.woowahan.com/1</link>" +
                "      <pubDate>Sat, 11 Apr 2026 10:00:00 +0900</pubDate>" +
                "      <category>Java</category>" +
                "    </item>" +
                "  </channel>" +
                "</rss>";

        HttpResponse<InputStream> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(new ByteArrayInputStream(mockRss.getBytes(StandardCharsets.UTF_8)));

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(response);

        // When
        DataCollectionResult result = dataScheduler.collectTechBlogFeeds();

        // Then
        long postCount = postRepository.count();
        long statCount = statRepository.count();

        assertThat(result).isNotNull();
        assertThat(result.totalItems()).isEqualTo(1);
        assertThat(postCount).isEqualTo(1);
        assertThat(statCount).isEqualTo(1);

        // Virtual Thread로 발송되는 디스코드 메시지가 완료될 때까지 대기 (데몬 스레드 종료 방지)
        Thread.sleep(2000);
    }
}
