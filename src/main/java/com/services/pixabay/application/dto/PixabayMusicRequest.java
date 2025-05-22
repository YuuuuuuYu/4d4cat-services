package com.services.pixabay.application.dto;

import org.springframework.web.util.UriComponentsBuilder;

import com.services.common.domain.ParameterBuilder;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class PixabayMusicRequest implements ParameterBuilder {
    
    private final String genre;

    @Override
    public UriComponentsBuilder appendToBuilder(UriComponentsBuilder builder) {
        builder.queryParam("genre", genre);
        return builder;
    }
}
