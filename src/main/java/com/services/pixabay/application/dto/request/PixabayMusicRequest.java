package com.services.pixabay.application.dto.request;

import com.services.common.application.dto.ParameterBuilder;
import lombok.Getter;
import org.springframework.web.util.UriComponentsBuilder;

@Getter
public class PixabayMusicRequest implements ParameterBuilder {

  private String genre;

  public PixabayMusicRequest(String genre) {
    this.genre = genre;
  }

  @Override
  public UriComponentsBuilder appendToBuilder(UriComponentsBuilder builder) {
    builder.queryParam("genre", genre);
    return builder;
  }
}
