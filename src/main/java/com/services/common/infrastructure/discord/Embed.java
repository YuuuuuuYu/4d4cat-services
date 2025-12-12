package com.services.common.infrastructure.discord;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
@JsonDeserialize(builder = Embed.EmbedBuilder.class)
public class Embed {

    private String title;
    private String description;
    private int color;
    private String timestamp;
    private Footer footer;

    @JsonPOJOBuilder(withPrefix = "")
    public static class EmbedBuilder {

    }
}
