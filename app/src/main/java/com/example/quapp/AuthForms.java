package com.example.quapp;

import android.app.Activity;
import android.graphics.Rect;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import com.google.android.material.textfield.TextInputLayout;

/** Authentication-only keyboard and secret-state policy. Other forms keep trimmed text. */
final class AuthForms {
    static String password(EditText input) {
        return input.getText() == null ? "" : input.getText().toString();
    }

    static void bind(Activity activity, int[] layouts, int[] secrets, Runnable submit) {
        for (int id : secrets) activity.findViewById(id).setSaveEnabled(false);
        for (int i = 0; i < layouts.length; i++) {
            TextInputLayout layout = activity.findViewById(layouts[i]);
            EditText input = layout.getEditText();
            if (!input.isSaveEnabled()) layout.setSaveEnabled(false);
            boolean last = i == layouts.length - 1;
            android.text.method.TransformationMethod transformation = input.getTransformationMethod();
            input.setSingleLine(true);
            input.setTransformationMethod(transformation);
            input.setImeOptions(last ? EditorInfo.IME_ACTION_DONE : EditorInfo.IME_ACTION_NEXT);
            if (!last) input.setNextFocusForwardId(((TextInputLayout) activity.findViewById(layouts[i + 1])).getEditText().getId());
            input.addTextChangedListener(new TextWatcher() {
                public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
                public void onTextChanged(CharSequence s, int start, int before, int count) { layout.setError(null); }
                public void afterTextChanged(Editable text) { }
            });
            if (last) input.setOnEditorActionListener((v, action, event) -> {
                if (action != EditorInfo.IME_ACTION_DONE) return false;
                submit.run();
                return true;
            });
        }
    }

    static void revealError(Activity activity, int... layouts) {
        for (int id : layouts) {
            TextInputLayout layout = activity.findViewById(id);
            if (layout.getError() == null) continue;
            EditText input = layout.getEditText();
            input.requestFocus();
            layout.post(() -> layout.requestRectangleOnScreen(new Rect(0, 0, layout.getWidth(), layout.getHeight()), true));
            input.announceForAccessibility(layout.getError());
            return;
        }
    }

    static void clearPasswords(Activity activity, int... inputs) {
        for (int id : inputs) {
            EditText input = activity.findViewById(id);
            input.setText("");
            input.setTransformationMethod(android.text.method.PasswordTransformationMethod.getInstance());
        }
    }
}
