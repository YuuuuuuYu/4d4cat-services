package com.services.api.techblog;

import static org.assertj.core.api.Assertions.assertThat;

import com.services.api.config.TestRedisConfig;
import com.services.api.techblog.dto.TechBlogCompanyResponse;
import com.services.api.techblog.dto.TechBlogListResponse;
import com.services.api.techblog.dto.TechBlogResponse;
import com.services.core.fixture.TechBlogFixtures;
import com.services.core.techblog.entity.TechBlogCompany;
import com.services.core.techblog.entity.TechBlogPost;
import com.services.core.techblog.repository.TechBlogCompanyRepository;
import com.services.core.techblog.repository.TechBlogPostRepository;
import com.services.core.techblog.repository.TechBlogPostStatRepository;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

  @Autowired private TechBlogPostStatRepository statRepository;

  @Autowired private MeterRegistry meterRegistry;

  @BeforeEach
  void setUp() {
    statRepository.deleteAll();
    postRepository.deleteAll();
    companyRepository.deleteAll();
    meterRegistry.forEachMeter(meterRegistry::remove);

    TechBlogCompany company = companyRepository.save(TechBlogFixtures.createDefaultCompany());

    for (int i = 1; i <= 10; i++) {
      TechBlogPost post = TechBlogFixtures.createDefaultPost(company, i);
      if (i % 2 == 0) {
        post.addTag(TechBlogFixtures.createTag(post, "backend"));
      } else {
        post.addTag(TechBlogFixtures.createTag(post, "frontend"));
      }
      postRepository.save(post);
      statRepository.save(TechBlogFixtures.createStat(post.getId(), post.getTitle()));
    }
  }

  @AfterEach
  void tearDown() {
    statRepository.deleteAll();
    postRepository.deleteAll();
    companyRepository.deleteAll();
  }

  @Test
  @DisplayName("게시글 목록 조회 - 첫 페이지 성공")
  void getTechBlogs_whenNoCursor_shouldReturnFirstPage() {
    // When
    TechBlogListResponse response = techBlogQueryService.getTechBlogs(null, null, null);

    // Then
    assertThat(response.items()).hasSize(5);
    assertThat(response.hasNext()).isTrue();
    assertThat(response.nextCursor()).isNotNull();
  }

  @Test
  @DisplayName("게시글 목록 조회 - 커서 페이징 성공")
  void getTechBlogs_whenCursorProvided_shouldReturnNextPage() {
    // Given
    TechBlogListResponse firstPage = techBlogQueryService.getTechBlogs(null, null, null);
    Long nextCursor = firstPage.nextCursor();

    // When
    TechBlogListResponse secondPage = techBlogQueryService.getTechBlogs(nextCursor, null, null);

    // Then
    assertThat(secondPage.items()).hasSize(5);
    assertThat(secondPage.hasNext()).isFalse();
    assertThat(firstPage.items().get(4).id()).isGreaterThan(secondPage.items().get(0).id());
  }

  @Test
  @DisplayName("게시글 목록 조회 - 단일 회사 필터링")
  void getTechBlogs_whenCompanyFilterProvided_shouldReturnFilteredPosts() {
    // When
    TechBlogListResponse response = techBlogQueryService.getTechBlogs(null, List.of("woowahan"), null);

    // Then
    assertThat(response.items()).hasSize(5);
  }

  @Test
  @DisplayName("게시글 목록 조회 - 다중 회사 필터링")
  void getTechBlogs_whenMultiCompanyFilterProvided_shouldReturnFilteredPosts() {
    // Given
    TechBlogCompany kakao = companyRepository.save(new TechBlogCompany("kakao", "kakao", "url"));
    TechBlogPost kakaoPost = postRepository.save(TechBlogFixtures.createDefaultPost(kakao, 100));
    statRepository.save(TechBlogFixtures.createStat(kakaoPost.getId(), kakaoPost.getTitle()));

    // When
    TechBlogListResponse response = techBlogQueryService.getTechBlogs(null, List.of("woowahan", "kakao"), null);

    // Then
    assertThat(response.items()).hasSize(5);
    assertThat(response.hasNext()).isTrue();
    assertThat(response.items()).extracting(TechBlogResponse::companySlug).contains("woowahan", "kakao");
  }

  @Test
  @DisplayName("게시글 목록 조회 - 태그 필터링")
  void getTechBlogs_whenTagFilterProvided_shouldReturnFilteredPosts() {
    // When
    TechBlogListResponse response = techBlogQueryService.getTechBlogs(null, null, "backend");

    // Then
    assertThat(response.items()).hasSize(5);
    assertThat(response.items()).allMatch(item -> item.tags().contains("backend"));
  }

  @Test
  @DisplayName("활성 회사 목록 조회 - 성공")
  void getActiveCompanies_shouldReturnOnlyActiveCompanies() {
    // Given
    TechBlogCompany active = new TechBlogCompany("active-test", "Active Test", "url");
    companyRepository.save(active);

    TechBlogCompany deleted = new TechBlogCompany("deleted-test", "Deleted Test", "url");
    deleted.delete();
    companyRepository.save(deleted);

    // When
    List<TechBlogCompanyResponse> companies = techBlogQueryService.getActiveCompanies();

    // Then
    assertThat(companies).extracting(TechBlogCompanyResponse::slug)
        .contains("woowahan", "active-test")
        .doesNotContain("deleted-test");
    assertThat(companies).extracting(TechBlogCompanyResponse::name)
        .contains("woowahan", "Active Test");
  }

  @Test
  @DisplayName("조회 요청 및 클릭 발생 시 - 메트릭 기록 검증")
  void getTechBlogsAndIncrementClickCount_shouldRecordMetrics() {
    // Given
    Long postId = postRepository.findAll().get(0).getId();

    // When
    techBlogQueryService.getTechBlogs(null, List.of("woowahan"), "backend");
    techBlogQueryService.incrementClickCount(postId);

    // Then
    assertThat(meterRegistry.find("techblog.api.request").counter()).isNotNull();
    assertThat(meterRegistry.find("techblog.cache.access").counter()).isNotNull();
    assertThat(meterRegistry.find("techblog.query.duration").timer()).isNotNull();
    assertThat(meterRegistry.find("techblog.post.click").counter()).isNotNull();

    assertThat(meterRegistry.find("techblog.api.request").tag("has_company_filter", "true").counter().count())
        .isEqualTo(1);
    assertThat(meterRegistry.find("techblog.post.click").counter().count())
        .isEqualTo(1);
  }
}
