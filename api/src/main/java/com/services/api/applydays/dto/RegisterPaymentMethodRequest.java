package com.services.api.applydays.dto;

public record RegisterPaymentMethodRequest(
    String billingKey, String cardCompany, String cardNumberMasked, Boolean isDefault) {}
