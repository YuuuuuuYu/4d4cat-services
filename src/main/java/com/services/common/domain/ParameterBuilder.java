package com.services.common.domain;

import org.springframework.web.util.UriComponentsBuilder;

public interface ParameterBuilder {
    UriComponentsBuilder appendToBuilder(UriComponentsBuilder builder);
}
