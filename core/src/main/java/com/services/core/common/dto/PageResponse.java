package com.services.core.common.dto;

import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Slice;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> implements Serializable {
  private List<T> content;
  private boolean hasNext;

  public static <T> PageResponse<T> of(Slice<T> slice) {
    return new PageResponse<>(slice.getContent(), slice.hasNext());
  }
}
