package com.services.data.techblog;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import com.services.core.notification.DataCollectionResult;
import com.services.core.techblog.entity.TechBlogCompany;
import com.services.core.techblog.entity.TechBlogPost;
import com.services.core.techblog.entity.TechBlogPostStat;
import com.services.core.techblog.entity.TechBlogPostTag;
import com.services.core.techblog.repository.TechBlogCompanyRepository;
import com.services.core.techblog.repository.TechBlogPostRepository;
import com.services.core.techblog.repository.TechBlogPostStatRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class TechBlogRssCollector {

  private final TechBlogCompanyRepository companyRepository;
  private final TechBlogPostRepository postRepository;
  private final TechBlogPostStatRepository statRepository;
  private final TransactionTemplate transactionTemplate;
  private final HttpClient httpClient;

  public DataCollectionResult collectFeeds() {
    final String taskName = "기술 블로그 RSS 수집";
    long startTime = System.currentTimeMillis();
    List<TechBlogCompany> companies = companyRepository.findAll();
    if (companies.isEmpty()) {
      log.info("No companies found to collect RSS feeds.");
      return new DataCollectionResult(taskName, 0, 0, 0, 0);
    }

    int totalItems = 0;
    int successCount = 0;
    int failureCount = 0;

    try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
      List<Callable<Integer>> tasks = new ArrayList<>();
      for (TechBlogCompany company : companies) {
        tasks.add(
            () -> {
              try {
                return transactionTemplate.execute(status -> processCompanyFeed(company));
              } catch (Exception e) {
                log.error("Failed to process feed for company: {}", company.getSlug(), e);
                throw e;
              }
            });
      }
      List<Future<Integer>> futures = executor.invokeAll(tasks);
      for (Future<Integer> future : futures) {
        try {
          totalItems += future.get();
          successCount++;
        } catch (Exception e) {
          failureCount++;
          log.error("Error during feed collection task", e);
        }
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.error("Feed collection interrupted", e);
    }

    double duration = (System.currentTimeMillis() - startTime) / 1000.0;
    return new DataCollectionResult(
            taskName, totalItems, successCount, failureCount, duration);
  }

  private int processCompanyFeed(TechBlogCompany company) {
    log.info("Processing feed for company: {}", company.getName());
    int companyPostCount = 0;
    try {
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(company.getFeedUrl()))
              .header(
                  "User-Agent",
                  "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
              .header(
                  "Accept",
                  "application/xml,application/rss+xml,text/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
              .header("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7")
              .header("Cache-Control", "no-cache")
              .GET()
              .build();

      HttpResponse<java.io.InputStream> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

      if (response.statusCode() != 200) {
        throw new RuntimeException("Failed to fetch feed: HTTP " + response.statusCode());
      }

      // 유효하지 않은 XML 문자 제거 (예: Unicode 0x8)
      String rawXml =
          new String(response.body().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
      String filteredXml = rawXml.replaceAll("[\\x00-\\x08\\x0b\\x0c\\x0e-\\x1f]", "");

      SyndFeedInput input = new SyndFeedInput();
      SyndFeed feed =
          input.build(
              new XmlReader(
                  new java.io.ByteArrayInputStream(
                      filteredXml.getBytes(StandardCharsets.UTF_8))));

      List<Long> activePostIds = new ArrayList<>();

      for (SyndEntry entry : feed.getEntries()) {
        String link = entry.getLink();
        String title = entry.getTitle();
        LocalDateTime publishedAt = null;
        if (entry.getPublishedDate() != null) {
          publishedAt =
              entry.getPublishedDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        }

        TechBlogPost post = postRepository.findByUrl(link).orElse(null);
        if (post == null) {
          post = new TechBlogPost(company, title, link, publishedAt);

          if (entry.getCategories() != null) {
            for (var category : entry.getCategories()) {
              post.addTag(new TechBlogPostTag(post, category.getName()));
            }
          }

          post = postRepository.save(post);
          statRepository.save(new TechBlogPostStat(post.getId(), post.getTitle()));
          companyPostCount++;
        } else {
          if (post.isDeleted()) {
            post.restore();
            postRepository.save(post);
            companyPostCount++;
          }
        }
        activePostIds.add(post.getId());
      }

      if (!activePostIds.isEmpty()) {
        postRepository.deleteMissingPosts(activePostIds, company.getSlug());
      }

      return companyPostCount;

    } catch (Exception e) {
      log.error("Failed to parse feed for company: {}", company.getSlug(), e);
      throw new RuntimeException("Feed parsing failed", e);
    }
  }
}
