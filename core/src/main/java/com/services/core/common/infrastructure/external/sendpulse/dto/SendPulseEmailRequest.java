package com.services.core.common.infrastructure.external.sendpulse.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Builder;

@Builder
public record SendPulseEmailRequest(@JsonProperty("email") EmailDetail email) {
  @Builder
  public record EmailDetail(
      String subject, String html, String text, Participant from, List<Participant> to) {}

  @Builder
  public record Participant(String name, String email) {}
}
