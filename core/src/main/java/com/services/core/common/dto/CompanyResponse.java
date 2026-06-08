package com.services.core.common.dto;

import java.io.Serializable;

public record CompanyResponse(String slug, String name) implements Serializable {}
