package com.services.core.common.util;

import com.services.core.common.exception.ErrorCode;
import com.services.core.common.exception.InternalServerException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AesUtil {

  private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
  private final SecretKeySpec keySpec;
  private final IvParameterSpec ivSpec;

  public AesUtil(@Value("${app.crypto.key}") String key, @Value("${app.crypto.iv") String iv) {
    this.keySpec = new SecretKeySpec(prepareKey(key, 16), "AES");
    this.ivSpec = new IvParameterSpec(prepareKey(iv, 16));
  }

  private byte[] prepareKey(String key, int length) {
    byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
    return Arrays.copyOf(keyBytes, length);
  }

  public String encrypt(String plainText) {
    if (plainText == null || plainText.isEmpty()) return plainText;
    try {
      Cipher cipher = Cipher.getInstance(ALGORITHM);
      cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
      byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
      return Base64.getEncoder().encodeToString(encrypted);
    } catch (Exception e) {
      log.error("Encryption failed: {}", e.getMessage(), e);
      throw new InternalServerException(ErrorCode.INTERNAL_SERVER_ERROR);
    }
  }

  public String decrypt(String encryptedText) {
    if (encryptedText == null || encryptedText.isEmpty()) return encryptedText;
    try {
      Cipher cipher = Cipher.getInstance(ALGORITHM);
      cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
      byte[] decoded = Base64.getDecoder().decode(encryptedText);
      byte[] decrypted = cipher.doFinal(decoded);
      return new String(decrypted, StandardCharsets.UTF_8);
    } catch (Exception e) {
      // If decryption fails, it might be plain text or wrong key
      log.warn("Decryption failed. Returning original text. Error: {}", e.getMessage());
      return encryptedText;
    }
  }
}
