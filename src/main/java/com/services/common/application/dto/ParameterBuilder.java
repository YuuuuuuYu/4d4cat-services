package com.services.common.application.dto;

import org.springframework.web.util.UriComponentsBuilder;

public interface ParameterBuilder {
    UriComponentsBuilder appendToBuilder(UriComponentsBuilder builder);
}
