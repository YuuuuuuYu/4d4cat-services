package com.services.common.application.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

  // common errors
  INVALID_REQUEST("CO1000", "error.common.CO1000"),
  DATA_NOT_FOUND("CO1001", "error.common.CO1001"),
  INTERNAL_SERVER_ERROR("CO1002", "error.common.CO1002"),
  BAD_GATEWAY("CO1003", "error.common.CO1003"),

  // pixabay video
  PIXABAY_VIDEO_NOT_FOUND("PV1000", "error.pixabay.PV1000"),

  // pixabay music
  PIXABAY_MUSIC_NOT_FOUND("PM1000", "error.pixabay.PM1000"),

  // message
  MESSAGE_NO_CONTENT("MS0204", "error.message.MS0204"),
  MESSAGE_INVALID_REQUEST("MS0400", "error.message.MS0400"),

  // discord
  DISCORD_WEBHOOK_FAILED("DC1000", "error.discord.DC1000"),
  ;

  private final String code;
  private final String messageKey;
}
