package com.services.pixabay.application.dto.request;

import com.services.common.application.dto.ParameterBuilder;
import java.util.Objects;
import java.util.function.Predicate;
import lombok.Getter;
import org.springframework.web.util.UriComponentsBuilder;

public class PixabayVideoRequest implements ParameterBuilder {

  @Getter private String key;
  private String q = null;
  private String lang = "en";
  private String id = null;
  private String videoType = "all";

  @Getter private String category;
  private int minWidth = 0;
  private int minHeight = 0;
  private boolean editorsChoice = false;
  private boolean safesearch = false;
  private String order = "popular";
  private int page = 1;
  private int perPage = 200;
  private String callback = null;
  private boolean pretty = false;

  public PixabayVideoRequest(String key, String category) {
    this.key = key;
    this.category = category;
  }

  @Override
  public UriComponentsBuilder appendToBuilder(UriComponentsBuilder builder) {
    addParamIfPresent(builder, "key", key, Objects::nonNull);
    addParamIfPresent(builder, "category", category, Objects::nonNull);

    return builder;
  }

  private <T> void addParamIfPresent(
      UriComponentsBuilder builder, String name, T value, Predicate<T> condition) {
    if (condition.test(value)) {
      builder.queryParam(name, value);
    }
  }
}
