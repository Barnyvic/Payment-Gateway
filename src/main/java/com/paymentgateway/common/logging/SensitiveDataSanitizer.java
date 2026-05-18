package com.paymentgateway.common.logging;

import java.util.regex.Pattern;

public final class SensitiveDataSanitizer {

    private static final Pattern PAN_PATTERN = Pattern.compile("\\b\\d{13,19}\\b");
    private static final Pattern CVV_PATTERN = Pattern.compile("\"cvv\"\\s*:\\s*\"\\d{3,4}\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern CVV_XML_PATTERN =
            Pattern.compile("(<cvv>)[^<]+(</cvv>)", Pattern.CASE_INSENSITIVE);

    private SensitiveDataSanitizer() {}

    public static String sanitize(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        String sanitized = PAN_PATTERN.matcher(value).replaceAll(match -> maskPan(match.group()));
        sanitized = CVV_PATTERN.matcher(sanitized).replaceAll("\"cvv\":\"***\"");
        sanitized = CVV_XML_PATTERN.matcher(sanitized).replaceAll("$1***$2");
        return sanitized;
    }

    private static String maskPan(String pan) {
        if (pan.length() <= 4) {
            return "****";
        }
        return "*".repeat(pan.length() - 4) + pan.substring(pan.length() - 4);
    }
}
