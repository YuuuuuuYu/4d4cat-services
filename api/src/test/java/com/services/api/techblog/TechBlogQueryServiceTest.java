package com.services.api.techblog;

import static org.assertj.core.api.Assertions.assertThat;

import com.services.api.config.TestRedisConfig;
import com.services.api.techblog.dto.TechBlogCompanyResponse;
import com.services.api.techblog.dto.TechBlogListResponse;
import com.services.api.techblog.dto.TechBlogResponse;
import com.services.core.common.persistence.entity.Company;
import com.services.core.fixture.TechBlogFixtures;
import com.services.core.techblog.entity.TechBlogPost;
import com.services.core.common.persistence.repository.CompanyRepository;
import com.services.core.techblog.repository.TechBlogPostRepository;
import com.services.core.techblog.repository.TechBlogPostStatRepository;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.persistence.EntityManager;
import java.util.List;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
@Transactional
class TechBlogQueryServiceTest {

  @Autowired private TechBlogQueryService techBlogQueryService;

  @Autowired private TechBlogPostRepository postRepository;

  @Autowired private CompanyRepository companyRepository;

  @Autowired private TechBlogPostStatRepository statRepository;

  @Autowired private MeterRegistry meterRegistry;

  @Autowired private EntityManager entityManager;

  @BeforeEach
  void setUp() {
    cleanup();
    meterRegistry.forEachMeter(meterRegistry::remove);

    Company company = companyRepository.save(TechBlogFixtures.createDefaultCompany());

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
    entityManager.flush();
    entityManager.clear();
  }

  @AfterEach
  void tearDown() {
    cleanup();
  }

  private void cleanup() {
    entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE").executeUpdate();
    entityManager.createNativeQuery("DELETE FROM techblog_post_stat").executeUpdate();
    entityManager.createNativeQuery("DELETE FROM techblog_post_tag").executeUpdate();
    entityManager.createNativeQuery("DELETE FROM techblog_post").executeUpdate();
    entityManager.createNativeQuery("DELETE FROM techblog_company").executeUpdate();
    entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY TRUE").executeUpdate();
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
  @DisplayName("게시글 삭제 시 - 조회 결과에서 제외됨 (Soft Delete 검증)")
  void deletePost_shouldExcludeFromList() {
    // Given
    List<TechBlogPost> allPosts = postRepository.findAll();
    TechBlogPost postToDelete = allPosts.stream()
        .max((p1, p2) -> p1.getId().compareTo(p2.getId()))
        .orElseThrow();
    Long targetId = postToDelete.getId();

    // When
    postRepository.delete(postToDelete);
    entityManager.flush();

    // Then
    TechBlogListResponse response = techBlogQueryService.getTechBlogs(null, null, null);
    assertThat(response.items()).extracting(TechBlogResponse::id).doesNotContain(targetId);

    var deletedFlag =
        entityManager
            .createNativeQuery("SELECT deleted FROM techblog_post WHERE id = :id")
            .setParameter("id", targetId)
            .getSingleResult();
    assertThat(deletedFlag).isEqualTo(true);
  }

  @Test
  @DisplayName("게시글 태그 삭제 시 - 해당 태그는 결과에서 제외됨 (물리 삭제 검증)")
  void deleteTag_shouldExcludeFromList() {
    // Given
    List<TechBlogPost> allPosts = postRepository.findAll();
    TechBlogPost post = allPosts.stream()
        .max((p1, p2) -> p1.getId().compareTo(p2.getId()))
        .orElseThrow();
    
    String targetTagName = post.getTags().get(0).getTagName();
    Long tagId = post.getTags().get(0).getId();

    // When
    post.getTags().remove(0);
    postRepository.save(post);
    entityManager.flush();
    entityManager.clear();

    // Then
    // 1. 서비스 조회 결과에서 해당 태그가 제외되었는지 확인
    TechBlogListResponse response = techBlogQueryService.getTechBlogs(null, null, null);
    TechBlogResponse postResponse =
        response.items().stream()
            .filter(item -> item.id().equals(post.getId()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("첫 페이지에서 게시글을 찾을 수 없습니다."));
    assertThat(postResponse.tags()).doesNotContain(targetTagName);

    // 2. DB에서 태그 데이터가 물리적으로 삭제되었는지 확인
    Long count = ((Number) entityManager
            .createNativeQuery("SELECT count(*) FROM techblog_post_tag WHERE id = :id")
            .setParameter("id", tagId)
            .getSingleResult()).longValue();
    assertThat(count).isZero();
  }

  @Test
  @DisplayName("게시글 목록 조회 - 커서 페이징 성공")
  void getTechBlogs_whenCursorProvided_shouldReturnNextPage() {
    // Given
    TechBlogListResponse firstPage = techBlogQueryService.getTechBlogs(null, null, null);
    String nextCursor = firstPage.nextCursor();

    // When
    TechBlogListResponse secondPage = techBlogQueryService.getTechBlogs(nextCursor, null, null);

    // Then
    assertThat(secondPage.items()).hasSize(5);
    assertThat(secondPage.hasNext()).isFalse();
    assertThat(firstPage.items().get(4).publishedAt())
        .isAfterOrEqualTo(secondPage.items().get(0).publishedAt());
  }

  @Test
  @DisplayName("게시글 목록 조회 - 단일 회사 필터링")
  void getTechBlogs_whenCompanyFilterProvided_shouldReturnFilteredPosts() {
    // When
    TechBlogListResponse response =
        techBlogQueryService.getTechBlogs(null, List.of("woowahan"), null);

    // Then
    assertThat(response.items()).hasSize(5);
  }

  @Test
  @DisplayName("게시글 목록 조회 - 다중 회사 필터링")
  void getTechBlogs_whenMultiCompanyFilterProvided_shouldReturnFilteredPosts() {
    // Given
    Company kakao = companyRepository.save(new Company("kakao", "kakao", "url"));
    TechBlogPost kakaoPost = postRepository.save(TechBlogFixtures.createDefaultPost(kakao, 100));
    statRepository.save(TechBlogFixtures.createStat(kakaoPost.getId(), kakaoPost.getTitle()));

    // When
    TechBlogListResponse response =
        techBlogQueryService.getTechBlogs(null, List.of("woowahan", "kakao"), null);

    // Then
    assertThat(response.items()).hasSize(5);
    assertThat(response.hasNext()).isTrue();
    assertThat(response.items())
        .extracting(TechBlogResponse::companySlug)
        .contains("woowahan", "kakao");
  }

  @Test
  @DisplayName("게시글 목록 조회 - 잘못된 형식의 커서인 경우 첫 페이지 반환")
  void getTechBlogs_whenInvalidFormatCursor_shouldReturnFirstPage() {
    // Given
    String invalidCursor = "invalid_format_cursor";

    // When
    TechBlogListResponse response = techBlogQueryService.getTechBlogs(invalidCursor, null, null);

    // Then
    assertThat(response.items()).hasSize(5);
    assertThat(response.hasNext()).isTrue();
  }

  @Test
  @DisplayName("게시글 목록 조회 - 날짜 형식이 잘못된 커서인 경우 첫 페이지 반환")
  void getTechBlogs_whenInvalidDateCursor_shouldReturnFirstPage() {
    // Given
    String invalidDateCursor = "invalid-date_1";

    // When
    TechBlogListResponse response = techBlogQueryService.getTechBlogs(invalidDateCursor, null, null);

    // Then
    assertThat(response.items()).hasSize(5);
    assertThat(response.hasNext()).isTrue();
  }

  @Test
  @DisplayName("게시글 목록 조회 - ID 형식이 잘못된 커서인 경우 첫 페이지 반환")
  void getTechBlogs_whenInvalidIdCursor_shouldReturnFirstPage() {
    // Given
    String invalidIdCursor = "2024-04-13T10:00:00_notanid";

    // When
    TechBlogListResponse response = techBlogQueryService.getTechBlogs(invalidIdCursor, null, null);

    // Then
    assertThat(response.items()).hasSize(5);
    assertThat(response.hasNext()).isTrue();
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
    Company active = new Company("active-test", "Active Test", "url");
    companyRepository.save(active);

    Company deleted = new Company("deleted-test", "Deleted Test", "url");
    companyRepository.save(deleted);
    companyRepository.delete(deleted);

    // When
    List<TechBlogCompanyResponse> companies = techBlogQueryService.getActiveCompanies();

    // Then
    assertThat(companies)
        .extracting(TechBlogCompanyResponse::slug)
        .contains("woowahan", "active-test")
        .doesNotContain("deleted-test");
    assertThat(companies).extracting(TechBlogCompanyResponse::name).contains("woowahan", "Active Test");
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

    assertThat(
            meterRegistry
                .find("techblog.api.request")
                .tag("has_company_filter", "true")
                .counter()
                .count())
        .isEqualTo(1);
    assertThat(meterRegistry.find("techblog.post.click").counter().count()).isEqualTo(1);
  }
}
