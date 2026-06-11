package com.services.api.applydays.dto;

import java.util.List;
import java.util.UUID;

public record BulkDeleteRequest(List<UUID> ids) {}
