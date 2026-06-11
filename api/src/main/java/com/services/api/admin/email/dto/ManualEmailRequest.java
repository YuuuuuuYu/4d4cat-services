package com.services.api.admin.email.dto;

import java.util.List;

public record ManualEmailRequest(List<String> toEmails, String subject, String body) {}
