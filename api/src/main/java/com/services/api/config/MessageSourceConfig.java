package com.services.api.config;

import java.util.Locale;
import java.util.ResourceBundle;
import net.rakugakibox.util.YamlResourceBundle;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;

@Configuration
public class MessageSourceConfig {

  @Bean
  public MessageSource messageSource(@Value("${spring.messages.basename}") String basename) {
    YamlMessageSource ms = new YamlMessageSource();
    ms.setBasename(basename);
    ms.setDefaultEncoding("UTF-8");
    ms.setAlwaysUseMessageFormat(true);
    ms.setUseCodeAsDefaultMessage(true);
    ms.setFallbackToSystemLocale(true);
    return ms;
  }

  private static class YamlMessageSource extends ResourceBundleMessageSource {
    @Override
    protected ResourceBundle doGetBundle(String basename, Locale locale) {
      return ResourceBundle.getBundle(basename, locale, YamlResourceBundle.Control.INSTANCE);
    }
  }
}
