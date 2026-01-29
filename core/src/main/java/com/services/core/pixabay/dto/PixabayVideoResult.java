package com.services.core.pixabay.dto;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PixabayVideoResult implements Serializable {

  private Integer id;
  private String pageURL;
  private String type;
  private String tags;
  private Integer duration;
  private Object videos;
  private Integer views;
  private Integer downloads;
  private Integer likes;
  private Integer comments;
  private Integer user_id;
  private String user;
  private String userImageURL;
  private Boolean noAiTraining;
}
