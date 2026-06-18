package com.services.api.common.config;

import com.services.api.common.security.handler.OAuth2SuccessHandler;
import com.services.api.common.security.jwt.JwtAuthenticationFilter;
import com.services.api.common.security.jwt.JwtProvider;
import com.services.api.common.security.service.CustomOAuth2UserService;
import com.services.core.common.infrastructure.RedisDataStorage;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@Slf4j
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {

  private final CustomOAuth2UserService customOAuth2UserService;
  private final OAuth2SuccessHandler oAuth2SuccessHandler;
  private final JwtProvider jwtProvider;
  private final RedisDataStorage redisDataStorage;

  @Value("${cors.allowed-origins}")
  private String allowedOrigins;

  @Bean
  public RoleHierarchy roleHierarchy() {
    return RoleHierarchyImpl.withDefaultRolePrefix()
        .role("ADMIN")
        .implies("SUBSCRIBER")
        .role("SUBSCRIBER")
        .implies("REVIEWER")
        .role("REVIEWER")
        .implies("USER")
        .role("USER")
        .implies("GUEST")
        .build();
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http, RoleHierarchy roleHierarchy)
      throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .formLogin(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            authorize ->
                authorize
                    .requestMatchers(HttpMethod.OPTIONS, "/**")
                    .permitAll()
                    .requestMatchers(
                        "/",
                        "/favicon.ico",
                        "/error",
                        "/auth/logout",
                        "/auth/refresh",
                        "/techblogs/**",
                        "/pixabay/**",
                        "/video",
                        "/music",
                        "/message/**",
                        "/applydays/statistics/summary",
                        "/applydays/companies",
                        "/applydays/companies/**",
                        "/applydays/companies/search",
                        "/applydays/categories",
                        "/actuator/**",
                        "/swagger-ui/**",
                        "/v3/api-docs/**")
                    .permitAll()
                    .requestMatchers(
                        AntPathRequestMatcher.antMatcher(
                            HttpMethod.POST, "/applydays/applications"),
                        AntPathRequestMatcher.antMatcher(
                            HttpMethod.POST, "/applydays/verification/**"),
                        AntPathRequestMatcher.antMatcher("/api/v1/applydays/subscriptions/**"),
                        AntPathRequestMatcher.antMatcher(
                            HttpMethod.GET, "/applydays/verification/images/**"),
                        AntPathRequestMatcher.antMatcher("/applydays/statistics/category"))
                    .hasRole("USER")
                    .requestMatchers("/applydays/statistics/detail")
                    .hasRole("REVIEWER")
                    .requestMatchers("/applydays/statistics/premium")
                    .hasRole("SUBSCRIBER")
                    .requestMatchers("/admin/**")
                    .hasRole("ADMIN")
                    .anyRequest()
                    .authenticated())
        .oauth2Login(
            oauth2 ->
                oauth2
                    .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                    .successHandler(oAuth2SuccessHandler))
        .exceptionHandling(
            exception ->
                exception
                    .accessDeniedHandler(
                        (request, response, accessDeniedException) -> {
                          log.error(
                              "Access Denied: {} {}, Reason: {}",
                              request.getMethod(),
                              request.getRequestURI(),
                              accessDeniedException.getMessage());
                          response.sendError(
                              HttpServletResponse.SC_FORBIDDEN, accessDeniedException.getMessage());
                        })
                    .authenticationEntryPoint(
                        (request, response, authException) -> {
                          log.error(
                              "Unauthorized: {} {}, Reason: {}",
                              request.getMethod(),
                              request.getRequestURI(),
                              authException.getMessage());
                          response.sendError(
                              HttpServletResponse.SC_UNAUTHORIZED, authException.getMessage());
                        }))
        .addFilterBefore(
            new JwtAuthenticationFilter(jwtProvider, redisDataStorage, roleHierarchy),
            UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    List<String> origins =
        Arrays.stream(allowedOrigins.split(","))
            .map(String::trim)
            .filter(StringUtils::hasText)
            .toList();

    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(origins);
    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(Arrays.asList("*"));
    configuration.setExposedHeaders(Arrays.asList("X-Token-Expired"));
    configuration.setAllowCredentials(true);
    configuration.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}
