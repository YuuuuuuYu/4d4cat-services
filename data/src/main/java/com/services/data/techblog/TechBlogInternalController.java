package com.services.data.techblog;

import com.services.core.aop.NotifyDiscord;
import com.services.core.infrastructure.RedisDataStorage;
import com.services.core.notification.DataCollectionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/techblogs")
@RequiredArgsConstructor
public class TechBlogInternalController {

  private final TechBlogRssCollector rssCollector;
  private final RedisDataStorage redisDataStorage;

  @PostMapping("/collect")
  @NotifyDiscord(taskName = "기술 블로그 RSS 수집 (수동)")
  public DataCollectionResult triggerCollection() {
    log.info("Starting manual RSS feed collection.");
    DataCollectionResult result = rssCollector.collectFeeds();

    redisDataStorage.deleteKeysByPattern("techblog:list:*");

    log.info("Finished manual RSS feed collection.");
    return result;
  }
}
