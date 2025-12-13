package com.services.pixabay.application.dto.result;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PixabayMusicResult {

  private long id;
  private String title;
  private int duration;
  private String author;
  private List<String> tags;
  private String download_url;
  private String thumbnail_url;
  private String url;
}
