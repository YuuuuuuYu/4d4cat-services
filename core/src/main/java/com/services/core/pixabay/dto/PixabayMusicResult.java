package com.services.core.pixabay.dto;

import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PixabayMusicResult implements Serializable {

  private long id;
  private String title;
  private int duration;
  private String author;
  private List<String> tags;
  private String download_url;
  private String thumbnail_url;
  private String url;
}
