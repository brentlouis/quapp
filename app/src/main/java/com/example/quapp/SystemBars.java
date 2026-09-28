package com.example.quapp;

import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public final class SystemBars {

    private SystemBars() {
        // Utility class — never instantiated.
    }

    public static void applyPadding(final View root) {
        final int basePaddingLeft = root.getPaddingLeft();
        final int basePaddingTop = root.getPaddingTop();
        final int basePaddingRight = root.getPaddingRight();
        final int basePaddingBottom = root.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(root, new OnApplyWindowInsetsListener() {
            @Override
            public WindowInsetsCompat onApplyWindowInsets(View view, WindowInsetsCompat windowInsets) {
                Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());

                view.setPadding(
                        basePaddingLeft + systemBars.left,
                        basePaddingTop + systemBars.top,
                        basePaddingRight + systemBars.right,
                        basePaddingBottom + systemBars.bottom);

                return windowInsets;
            }
        });
    }

    /**
     * For screens with a bottom navigation bar: pads the top and sides only. The bar handles the
     * bottom inset itself (BottomNavigationView adds it as padding by default), so its paper colour
     * runs under the gesture pill instead of stopping above it. The insets aren't consumed, so they
     * still reach the bar.
     */
    public static void applyPaddingExceptBottom(final View root) {
        final int basePaddingLeft = root.getPaddingLeft();
        final int basePaddingTop = root.getPaddingTop();
        final int basePaddingRight = root.getPaddingRight();
        final int basePaddingBottom = root.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(root, new OnApplyWindowInsetsListener() {
            @Override
            public WindowInsetsCompat onApplyWindowInsets(View view, WindowInsetsCompat windowInsets) {
                Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());

                view.setPadding(
                        basePaddingLeft + systemBars.left,
                        basePaddingTop + systemBars.top,
                        basePaddingRight + systemBars.right,
                        basePaddingBottom);

                return windowInsets;
            }
        });
    }

    public static void applyPaddingWithKeyboard(final View root) {
        final int basePaddingLeft = root.getPaddingLeft();
        final int basePaddingTop = root.getPaddingTop();
        final int basePaddingRight = root.getPaddingRight();
        final int basePaddingBottom = root.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(root, new OnApplyWindowInsetsListener() {
            @Override
            public WindowInsetsCompat onApplyWindowInsets(View view, WindowInsetsCompat windowInsets) {
                Insets insets = windowInsets.getInsets(
                        WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());

                view.setPadding(
                        basePaddingLeft + insets.left,
                        basePaddingTop + insets.top,
                        basePaddingRight + insets.right,
                        basePaddingBottom + insets.bottom);

                return windowInsets;
            }
        });
    }
}