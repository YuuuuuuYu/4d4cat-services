package com.services.core.common.persistence.converter;

import com.services.core.common.util.AesUtil;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@Converter
@RequiredArgsConstructor
public class CryptoConverter implements AttributeConverter<String, String> {

  private final AesUtil aesUtil;

  @Override
  public String convertToDatabaseColumn(String attribute) {
    return aesUtil.encrypt(attribute);
  }

  @Override
  public String convertToEntityAttribute(String dbData) {
    return aesUtil.decrypt(dbData);
  }
}
