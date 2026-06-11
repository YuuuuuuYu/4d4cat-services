package com.services.core.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

  // common errors
  INVALID_REQUEST("CO1000", "error.common.CO1000"),
  DATA_NOT_FOUND("CO1001", "error.common.CO1001"),
  INTERNAL_SERVER_ERROR("CO1002", "error.common.CO1002"),
  BAD_GATEWAY("CO1400", "error.common.CO1400"),
  UNAUTHORIZED("CO1401", "error.common.CO1401"),
  FORBIDDEN("CO1403", "error.common.CO1403"),

  // pixabay video
  PIXABAY_VIDEO_NOT_FOUND("PV1000", "error.pixabay.PV1000"),

  // pixabay music
  PIXABAY_MUSIC_NOT_FOUND("PM1000", "error.pixabay.PM1000"),

  // message
  MESSAGE_NO_CONTENT("MS0204", "error.message.MS0204"),
  MESSAGE_INVALID_REQUEST("MS0400", "error.message.MS0400"),

  // discord
  DISCORD_WEBHOOK_FAILED("DC1000", "error.discord.DC1000"),

  // applydays
  COMPANY_NOT_FOUND("AD1000", "error.applydays.AD1000"),
  CATEGORY_NOT_FOUND("AD1001", "error.applydays.AD1001"),
  APPLICATION_NOT_FOUND("AD1002", "error.applydays.AD1002"),
  UNAUTHORIZED_APPLICATION_ACCESS("AD1003", "error.applydays.AD1003"),
  VERIFICATION_IMAGE_LIMIT_EXCEEDED("AD1004", "error.applydays.AD1004"),
  R2_UPLOAD_FAILED("AD1005", "error.applydays.AD1005"),
  R2_DELETE_FAILED("AD1012", "error.applydays.AD1012"),
  USER_NOT_FOUND("AD1007", "error.applydays.AD1007"),
  DUPLICATE_COMPANY_SLUG("AD1008", "error.applydays.AD1008"),
  SUBSCRIPTION_PLAN_NOT_FOUND("AD1009", "error.applydays.AD1009"),
  ALREADY_SUBSCRIBED("AD1010", "error.applydays.ALREADY_SUBSCRIBED"),

  // techblog
  RSS_FETCH_FAILED("TB1000", "error.techblog.TB1000"),
  RSS_PARSE_FAILED("TB1001", "error.techblog.TB1001"),
  ;

  private final String code;
  private final String messageKey;
}
