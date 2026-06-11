package com.services.api.applydays.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.services.api.common.security.jwt.JwtProvider;
import com.services.core.common.infrastructure.RedisDataStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ApplyDaysSecurityTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private CacheManager cacheManager;

  @MockitoBean private RedisDataStorage redisDataStorage;

  @MockitoBean private JwtProvider jwtProvider;

  @BeforeEach
  void clearCaches() {
    if (cacheManager != null) {
      String[] caches = {
        "companyList", "companySearch", "companySummary", "publicSummary", "categoryList"
      };
      for (String name : caches) {
        var cache = cacheManager.getCache(name);
        if (cache != null) {
          cache.clear();
        }
      }
    }
  }

  @Test
  @DisplayName("비회원은 요약 통계에 접근 가능하다")
  void publicAccess_shouldBeAllowed() throws Exception {
    mockMvc.perform(get("/applydays/statistics/summary")).andExpect(status().isOk());
  }

  @Test
  @DisplayName("비회원은 카테고리 통계에 접근할 수 없다")
  void guestAccess_shouldBeDenied() throws Exception {
    mockMvc
        .perform(get("/applydays/statistics/category"))
        .andDo(print())
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(roles = "USER")
  @DisplayName("USER 권한은 카테고리 통계에 접근 가능하다")
  void userAccess_shouldBeAllowed() throws Exception {
    mockMvc.perform(get("/applydays/statistics/category")).andExpect(status().isOk());
  }

  @Test
  @WithMockUser(roles = "USER")
  @DisplayName("USER 권한은 상세 통계에 접근할 수 없다")
  void userDetailAccess_shouldBeDenied() throws Exception {
    mockMvc
        .perform(get("/applydays/statistics/detail"))
        .andDo(print())
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "REVIEWER")
  @DisplayName("REVIEWER 권한은 상세 통계에 접근 가능하다 (계층적 권한 확인)")
  void reviewerAccess_shouldBeAllowed() throws Exception {
    mockMvc.perform(get("/applydays/statistics/detail")).andExpect(status().isOk());
    mockMvc.perform(get("/applydays/statistics/category")).andExpect(status().isOk());
  }

  @Test
  @WithMockUser(roles = "SUBSCRIBER")
  @DisplayName("SUBSCRIBER 권한은 프리미엄 통계 및 하위 모든 통계에 접근 가능하다")
  void subscriberAccess_shouldBeAllowed() throws Exception {
    mockMvc.perform(get("/applydays/statistics/premium")).andExpect(status().isOk());
    mockMvc.perform(get("/applydays/statistics/detail")).andExpect(status().isOk());
    mockMvc.perform(get("/applydays/statistics/category")).andExpect(status().isOk());
  }

  @Test
  @DisplayName("기존 테크블로그 API는 여전히 무인증 접근 가능하다")
  void techblogPublicAccess_shouldBeAllowed() throws Exception {
    mockMvc.perform(get("/techblogs")).andExpect(status().isOk());
  }
}
