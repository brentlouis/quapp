package com.example.quapp;

import java.util.Locale;

/** Small text formatting shared by Profile and Join: initials and a readable phone number. */
public final class Format {

    private Format() {
        // Utility class.
    }

    /** "Maria Santos" → "MS"; one word gives one letter. */
    public static String initials(String name) {
        String[] words = name.trim().split("\\s+");
        if (words.length == 0 || words[0].isEmpty()) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        result.append(words[0].charAt(0));
        if (words.length > 1) {
            result.append(words[words.length - 1].charAt(0));
        }
        return result.toString().toUpperCase(Locale.ROOT);
    }

    /** "09171234567" → "0917 123 4567", the way people read a PH mobile number aloud. */
    public static String spacedPhone(String phone) {
        if (phone.length() != 11) {
            return phone;
        }
        return phone.substring(0, 4) + " " + phone.substring(4, 7) + " " + phone.substring(7);
    }
}
