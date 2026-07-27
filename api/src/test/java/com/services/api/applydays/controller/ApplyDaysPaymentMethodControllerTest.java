package com.services.api.applydays.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.services.api.applydays.dto.RegisterPaymentMethodRequest;
import com.services.api.common.security.handler.OAuth2SuccessHandler;
import com.services.api.common.security.jwt.JwtProvider;
import com.services.api.common.security.service.CustomOAuth2UserService;
import com.services.core.applydays.entity.subscription.ApplyDaysPaymentMethod;
import com.services.core.applydays.service.ApplyDaysPaymentMethodCommandService;
import com.services.core.applydays.service.ApplyDaysPaymentMethodQueryService;
import com.services.core.common.infrastructure.RedisDataStorage;
import com.services.core.common.persistence.entity.member.Member;
import com.services.core.common.persistence.entity.member.Role;
import com.services.core.common.persistence.repository.member.MemberRepository;
import com.services.core.fixture.ApplyDaysFixtures;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ApplyDaysPaymentMethodController.class)
class ApplyDaysPaymentMethodControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private ClientRegistrationRepository clientRegistrationRepository;
  @MockitoBean private ApplyDaysPaymentMethodQueryService paymentMethodQueryService;
  @MockitoBean private ApplyDaysPaymentMethodCommandService paymentMethodCommandService;
  @MockitoBean private MemberRepository memberRepository;

  @MockitoBean private CustomOAuth2UserService customOAuth2UserService;
  @MockitoBean private OAuth2SuccessHandler oAuth2SuccessHandler;
  @MockitoBean private JwtProvider jwtProvider;
  @MockitoBean private RedisDataStorage redisDataStorage;
  @MockitoBean private MeterRegistry meterRegistry;

  @Test
  @WithMockUser(username = "user@example.com")
  @DisplayName("GET /applydays/payment-methods/me - 내 결제 수단 조회 성공")
  void getMyPaymentMethod_success() throws Exception {
    // given
    UUID memberId = UUID.randomUUID();
    Member member = ApplyDaysFixtures.createMember("user@example.com", Role.USER);
    ApplyDaysFixtures.setId(member, memberId);

    ApplyDaysPaymentMethod pm =
        ApplyDaysPaymentMethod.builder()
            .memberId(memberId)
            .billingKey("billing_key_123")
            .cardCompany("신한카드")
            .cardNumberMasked("1234-****")
            .isDefault(true)
            .build();
    ApplyDaysFixtures.setId(pm, UUID.randomUUID());

    given(memberRepository.findByEmail("user@example.com")).willReturn(Optional.of(member));
    given(paymentMethodQueryService.findByMemberId(memberId)).willReturn(Optional.of(pm));

    // when & then
    mockMvc
        .perform(get("/applydays/payment-methods/me"))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(200))
        .andExpect(jsonPath("$.data.billingKey").value("billing_key_123"))
        .andExpect(jsonPath("$.data.cardCompany").value("신한카드"));
  }

  @Test
  @WithMockUser(username = "user@example.com")
  @DisplayName("POST /applydays/payment-methods/register - 결제 수단 등록 성공")
  void registerPaymentMethod_success() throws Exception {
    // given
    UUID memberId = UUID.randomUUID();
    Member member = ApplyDaysFixtures.createMember("user@example.com", Role.USER);
    ApplyDaysFixtures.setId(member, memberId);

    RegisterPaymentMethodRequest request =
        new RegisterPaymentMethodRequest("billing_new", "국민카드", "5520-****", true);

    ApplyDaysPaymentMethod pm =
        ApplyDaysPaymentMethod.builder()
            .memberId(memberId)
            .billingKey("billing_new")
            .cardCompany("국민카드")
            .cardNumberMasked("5520-****")
            .isDefault(true)
            .build();
    ApplyDaysFixtures.setId(pm, UUID.randomUUID());

    given(memberRepository.findByEmail("user@example.com")).willReturn(Optional.of(member));
    given(
            paymentMethodCommandService.registerPaymentMethod(
                eq(memberId), eq("billing_new"), eq("국민카드"), eq("5520-****"), eq(true)))
        .willReturn(pm);

    // when & then
    mockMvc
        .perform(
            post("/applydays/payment-methods/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(201))
        .andExpect(jsonPath("$.data.billingKey").value("billing_new"));
  }

  @Test
  @WithMockUser(username = "user@example.com")
  @DisplayName("DELETE /applydays/payment-methods - 결제 수단 삭제 성공")
  void deletePaymentMethod_success() throws Exception {
    // given
    UUID memberId = UUID.randomUUID();
    Member member = ApplyDaysFixtures.createMember("user@example.com", Role.USER);
    ApplyDaysFixtures.setId(member, memberId);

    given(memberRepository.findByEmail("user@example.com")).willReturn(Optional.of(member));

    // when & then
    mockMvc
        .perform(delete("/applydays/payment-methods").with(csrf()))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(200));

    verify(paymentMethodCommandService).deletePaymentMethod(memberId);
  }
}
