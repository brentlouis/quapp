package com.example.quapp;

import java.util.regex.Pattern;

/** Form checks shared by Login, Register, Join and Create Queue. */
public final class Validation {

    /** MODELS.md: minimum password length 8 (the canvas's Create account says so too). */
    public static final int MIN_PASSWORD_LENGTH = 8;

    // Philippine mobile numbers: 09 followed by nine digits.
    private static final Pattern PH_MOBILE = Pattern.compile("^09\\d{9}$");

    private Validation() {
        // Utility class.
    }

    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /** Spaces and dashes are allowed while typing ("0917 123 4567"), so strip them first. */
    public static String normalizePhone(String phone) {
        return phone == null ? "" : phone.replaceAll("[\\s-]", "");
    }

    public static boolean isValidPhone(String phone) {
        return PH_MOBILE.matcher(normalizePhone(phone)).matches();
    }

    /**
     * An office's public number (canvas 50): a landline like "(038) 411 2345" or a mobile.
     * Only the digits count, 7 to 11 of them.
     */
    public static boolean isValidOfficePhone(String phone) {
        int digits = phone == null ? 0 : phone.replaceAll("\\D", "").length();
        return digits >= 7 && digits <= 11;
    }

    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= MIN_PASSWORD_LENGTH;
    }
}
