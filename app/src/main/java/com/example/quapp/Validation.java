package com.example.quapp;

import java.util.regex.Pattern;

/** Form checks shared by Login, Register, Join and Create Queue. */
public final class Validation {

    public static final int MIN_PASSWORD_LENGTH = 6;

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

    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= MIN_PASSWORD_LENGTH;
    }
}
