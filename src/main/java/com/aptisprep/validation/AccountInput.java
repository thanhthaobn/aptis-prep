package com.aptisprep.validation;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class AccountInput {
    private AccountInput() {}

    public static final String NAME_PATTERN = "\\p{Lu}\\p{M}*(?: *\\p{L}\\p{M}*)*";
    public static final String NAME_HINT =
            "Họ tên phải bắt đầu bằng chữ in hoa và chỉ được chứa chữ cái, dấu tiếng Việt và khoảng trắng.";
    public static final String PASSWORD_HINT =
            "Dùng 8–72 ký tự, tối đa 72 byte UTF-8, có ít nhất một chữ in hoa, một chữ số và một ký tự đặc biệt.";
    private static final Pattern SPECIAL_CHARACTER = Pattern.compile("[\\p{P}\\p{S}]");
    private static final Set<String> COMMON =
            Set.of(
                    "password",
                    "password1",
                    "password123",
                    "12345678",
                    "123456789",
                    "1234567890",
                    "qwerty123",
                    "qwertyuiop",
                    "abcdefgh",
                    "87654321",
                    "11111111",
                    "admin123",
                    "aptis123",
                    "letmein123");

    public static String name(String value) {
        if (value == null || value.codePoints().anyMatch(Character::isISOControl)) return value;
        return Normalizer.normalize(value, Normalizer.Form.NFC).replaceAll("\\p{Zs}+", " ").strip();
    }

    public static String email(String value) {
        return value == null ? null : value.strip().toLowerCase(Locale.ROOT);
    }

    public static boolean strongPassword(String value) {
        if (value == null || value.isBlank()) return true; // @NotBlank supplies the error
        return value.length() >= 8
                && value.length() <= 72
                && value.getBytes(StandardCharsets.UTF_8).length <= 72
                && !COMMON.contains(value.toLowerCase(Locale.ROOT))
                && value.codePoints().anyMatch(Character::isUpperCase)
                && value.codePoints().anyMatch(Character::isDigit)
                && SPECIAL_CHARACTER.matcher(value).find()
                && value.codePoints().noneMatch(Character::isISOControl);
    }
}
