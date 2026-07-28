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
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
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
  private final ClientRegistrationRepository clientRegistrationRepository;

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
  @Order(1)
  public SecurityFilterChain webhookSecurityFilterChain(HttpSecurity http) throws Exception {
    http.securityMatcher(
            AntPathRequestMatcher.antMatcher("/applydays/subscriptions/webhook"))
        .csrf(AbstractHttpConfigurer::disable)
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll());
    return http.build();
  }

  @Bean
  @Order(2)
  public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http, RoleHierarchy roleHierarchy)
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
                    .requestMatchers(AntPathRequestMatcher.antMatcher(HttpMethod.OPTIONS, "/**"))
                    .permitAll()
                    .requestMatchers(
                        antMatchers(
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
                            "/v3/api-docs/**"))
                    .permitAll()
                    .requestMatchers(
                        antMatchers(
                            HttpMethod.POST,
                            "/applydays/applications",
                            "/applydays/verification/**"))
                    .hasRole("USER")
                    .requestMatchers(antMatchers("/applydays/subscriptions/**"))
                    .hasRole("USER")
                    .requestMatchers(
                        antMatchers(HttpMethod.GET, "/applydays/verification/images/**"))
                    .hasRole("USER")
                    .requestMatchers(antMatchers("/applydays/statistics/category"))
                    .hasRole("USER")
                    .requestMatchers(antMatchers("/applydays/statistics/detail"))
                    .hasRole("REVIEWER")
                    .requestMatchers(antMatchers("/applydays/statistics/premium"))
                    .hasRole("SUBSCRIBER")
                    .requestMatchers(antMatchers("/admin/**"))
                    .hasRole("ADMIN")
                    .anyRequest()
                    .authenticated())
        .oauth2Login(
            oauth2 ->
                oauth2
                    .authorizationEndpoint(
                        authorization ->
                            authorization.authorizationRequestResolver(
                                authorizationRequestResolver(clientRegistrationRepository)))
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
    configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "Accept", "Origin"));
    configuration.setExposedHeaders(Arrays.asList("Authorization", "X-Token-Expired"));
    configuration.setAllowCredentials(true);
    configuration.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  private RequestMatcher[] antMatchers(String... patterns) {
    return Arrays.stream(patterns)
        .map(AntPathRequestMatcher::antMatcher)
        .toArray(RequestMatcher[]::new);
  }

  private RequestMatcher[] antMatchers(HttpMethod method, String... patterns) {
    return Arrays.stream(patterns)
        .map(pattern -> AntPathRequestMatcher.antMatcher(method, pattern))
        .toArray(RequestMatcher[]::new);
  }

  private OAuth2AuthorizationRequestResolver authorizationRequestResolver(
      ClientRegistrationRepository clientRegistrationRepository) {
    DefaultOAuth2AuthorizationRequestResolver authorizationRequestResolver =
        new DefaultOAuth2AuthorizationRequestResolver(
            clientRegistrationRepository, "/oauth2/authorization");
    authorizationRequestResolver.setAuthorizationRequestCustomizer(
        customizer ->
            customizer.additionalParameters(params -> params.put("prompt", "select_account")));
    return authorizationRequestResolver;
  }
}
