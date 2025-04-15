package com.services.pixabay.application.dto;

import com.services.common.domain.ParameterBuilder;
import lombok.Builder;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Objects;
import java.util.function.Predicate;

@Builder
public class PixabayVideoParameter implements ParameterBuilder {

    private final String key;
    private final String q;
    private final String lang;
    private final String id;
    private final String videoType;
    private final String category;
    private final int minWidth;
    private final int minHeight;
    private final boolean editorsChoice;
    private final boolean safesearch;
    private final String order;
    private final int page;
    private final int perPage;
    private final String callback;
    private final boolean pretty;

    @Override
    public UriComponentsBuilder appendToBuilder(UriComponentsBuilder builder) {
        addParamIfPresent(builder, "key", key, Objects::nonNull);
        addParamIfPresent(builder, "q", q, Objects::nonNull);
        addParamIfPresent(builder, "lang", lang, Objects::nonNull);
        addParamIfPresent(builder, "id", id, Objects::nonNull);
        addParamIfPresent(builder, "video_type", videoType, Objects::nonNull);
        addParamIfPresent(builder, "category", category, Objects::nonNull);
        addParamIfPresent(builder, "min_width", minWidth, val -> val > 0);
        addParamIfPresent(builder, "min_height", minHeight, val -> val > 0);
        addParamIfPresent(builder, "editors_choice", editorsChoice, val -> val);
        addParamIfPresent(builder, "safesearch", safesearch, val -> val);
        addParamIfPresent(builder, "order", order, Objects::nonNull);
        addParamIfPresent(builder, "page", page, val -> val > 0);
        addParamIfPresent(builder, "per_page", perPage, val -> val > 0);
        addParamIfPresent(builder, "callback", callback, Objects::nonNull);
        addParamIfPresent(builder, "pretty", pretty, val -> val);

        return builder;
    }

    private <T> void addParamIfPresent(UriComponentsBuilder builder, String name, T value, Predicate<T> condition) {
        if (condition.test(value)) {
            builder.queryParam(name, value);
        }
    }
}
