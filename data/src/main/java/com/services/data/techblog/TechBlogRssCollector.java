package com.services.data.techblog;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import com.services.core.techblog.entity.TechBlogCompany;
import com.services.core.techblog.entity.TechBlogPost;
import com.services.core.techblog.entity.TechBlogPostStat;
import com.services.core.techblog.entity.TechBlogPostTag;
import com.services.core.techblog.repository.TechBlogCompanyRepository;
import com.services.core.techblog.repository.TechBlogPostRepository;
import com.services.core.techblog.repository.TechBlogPostStatRepository;
import java.net.URL;
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

  public void collectFeeds() {
    List<TechBlogCompany> companies = companyRepository.findAll();
    if (companies.isEmpty()) {
      log.info("No companies found to collect RSS feeds.");
      return;
    }

    try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
      List<Callable<Void>> tasks = new ArrayList<>();
      for (TechBlogCompany company : companies) {
        tasks.add(
            () -> {
              transactionTemplate.executeWithoutResult(status -> processCompanyFeed(company));
              return null;
            });
      }
      List<Future<Void>> futures = executor.invokeAll(tasks);
      for (Future<Void> future : futures) {
        try {
          future.get();
        } catch (Exception e) {
          log.error("Error during feed collection", e);
        }
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.error("Feed collection interrupted", e);
    }
  }

  private void processCompanyFeed(TechBlogCompany company) {
    log.info("Processing feed for company: {}", company.getName());
    try {
      URL feedUrl = new URL(company.getFeedUrl());
      SyndFeedInput input = new SyndFeedInput();
      SyndFeed feed = input.build(new XmlReader(feedUrl));

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
        } else {
          if (post.isDeleted()) {
            post.restore();
            postRepository.save(post);
          }
        }
        activePostIds.add(post.getId());
      }

      if (!activePostIds.isEmpty()) {
        postRepository.deleteMissingPosts(activePostIds, company.getSlug());
      }

    } catch (Exception e) {
      log.error("Failed to parse feed for company: {}", company.getSlug(), e);
      throw new RuntimeException("Feed parsing failed", e); // trigger rollback
    }
  }
}
