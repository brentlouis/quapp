package com.example.quapp;

import android.widget.EditText;

import com.google.android.material.textfield.TextInputLayout;

/** Small helpers for reading and flagging form fields. */
public final class Forms {

    private Forms() {
        // Utility class.
    }

    /** getText() can return null before the field is ever touched, so guard it once here. */
    public static String text(EditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }

    /**
     * Shows the error when {@code valid} is false, clears it otherwise.
     * Returns {@code valid} so checks can be chained: ok &= Forms.check(...).
     */
    public static boolean check(TextInputLayout layout, boolean valid, String error) {
        layout.setError(valid ? null : error);
        return valid;
    }
}
