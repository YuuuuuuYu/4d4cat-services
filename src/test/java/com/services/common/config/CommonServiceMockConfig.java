package com.services.common.config;

import com.services.common.infrastructure.discord.application.DiscordWebhookService;
import com.services.message.application.MessageService;
import com.services.pixabay.application.PixabayMusicService;
import com.services.pixabay.application.PixabayVideoService;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.MessageSource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class CommonServiceMockConfig {

  @MockitoBean protected MessageService messageService;

  @MockitoBean protected PixabayVideoService pixabayVideoService;

  @MockitoBean protected PixabayMusicService pixabayMusicService;

  @MockitoBean protected DiscordWebhookService discordWebhookService;

  @MockitoBean protected MessageSource messageSource;
}
