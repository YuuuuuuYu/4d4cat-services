package com.services.pixabay.application.dto.request;

import org.springframework.web.util.UriComponentsBuilder;

import com.services.common.application.dto.ParameterBuilder;

import lombok.Getter;

@Getter
public class PixabayMusicRequest implements ParameterBuilder {
    
    private String genre;

    public PixabayMusicRequest(String genre) {
        this.genre = genre;
    }

    @Override
    public UriComponentsBuilder appendToBuilder(UriComponentsBuilder builder) {
        builder.queryParam("genre", genre);
        return builder;
    }
}
