package com.services.message.validator;

import java.util.regex.Pattern;

public class MessageValidator {

    private static final Pattern VALID_CONTENT_PATTERN = Pattern.compile("^[ㄱ-ㅎㅏ-ㅣ가-힣a-zA-Z0-9\\s]*$");
    private static final int MAX_CHAR_COUNT = 30;

    public static boolean isValid(String content) {
        if (content == null || content.trim().isEmpty()) {
            return false;
        }

        // 허용된 문자만 포함하는지 검사
        if (!VALID_CONTENT_PATTERN.matcher(content).matches()) {
            return false;
        }

        // 글자 수 제한 검사
        int charCount = calculateCharCount(content);
        return charCount <= MAX_CHAR_COUNT;
    }

    public static int calculateCharCount(String text) {
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (Character.UnicodeBlock.of(ch).equals(Character.UnicodeBlock.HANGUL_SYLLABLES) ||
                    Character.UnicodeBlock.of(ch).equals(Character.UnicodeBlock.HANGUL_COMPATIBILITY_JAMO) ||
                    Character.UnicodeBlock.of(ch).equals(Character.UnicodeBlock.HANGUL_JAMO)) {
                count += 2;
            } else {
                count += 1;
            }
        }
        return count;
    }
}