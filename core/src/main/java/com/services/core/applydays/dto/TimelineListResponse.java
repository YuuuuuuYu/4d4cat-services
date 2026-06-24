package com.services.core.applydays.dto;

import java.util.List;

public record TimelineListResponse(
    List<? extends TimelineBasicResponse> items, String nextCursor, boolean hasNext) {}
