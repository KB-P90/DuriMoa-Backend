package com.project.common.logging;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SensitiveDataMasker {

    private static final String REDACTED = "[REDACTED]";
    private static final String REDACTED_JWT = "[REDACTED_JWT]";

    private static final Pattern JWT_PATTERN = Pattern.compile(
            "(?i)(?:Bearer\\s+)?eyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+");
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "\\b(01[016789])[- ]?\\d{3,4}[- ]?(\\d{4})\\b");
    private static final Pattern SENSITIVE_KEY_VALUE_PATTERN = Pattern.compile(
            "(?i)\\b(password(?:Confirm)?|currentPassword|authorization|accessToken|"
                    + "refreshToken|signupToken|fcmToken|token|connectedId)"
                    + "\\s*([:=])\\s*([^,\\s}\\]]+)");
    private static final Pattern ACCOUNT_KEY_VALUE_PATTERN = Pattern.compile(
            "(?i)\\b(account(?:Number)?|number)\\s*([:=])\\s*[\\\"']?"
                    + "([0-9][0-9-]{6,20}[0-9])[\\\"']?");

    private SensitiveDataMasker() {
    }

    public static String mask(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }

        String masked = JWT_PATTERN.matcher(message).replaceAll(REDACTED_JWT);
        masked = maskSensitiveKeyValues(masked);
        masked = maskAccountKeyValues(masked);
        return PHONE_PATTERN.matcher(masked).replaceAll("$1-****-$2");
    }

    private static String maskSensitiveKeyValues(String message) {
        Matcher matcher = SENSITIVE_KEY_VALUE_PATTERN.matcher(message);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String replacement = matcher.group(1) + matcher.group(2) + REDACTED;
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static String maskAccountKeyValues(String message) {
        Matcher matcher = ACCOUNT_KEY_VALUE_PATTERN.matcher(message);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String replacement = matcher.group(1) + matcher.group(2) + maskAccountNumber(matcher.group(3));
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static String maskAccountNumber(String accountNumber) {
        String digits = accountNumber.replace("-", "");
        if (digits.length() <= 8) {
            return REDACTED;
        }
        return digits.substring(0, 4) + "****" + digits.substring(digits.length() - 4);
    }
}
