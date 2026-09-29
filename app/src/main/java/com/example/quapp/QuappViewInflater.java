package com.example.quapp;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.theme.MaterialComponentsViewInflater;

/**
 * Builds the views for every layout the app inflates, so each Material component gets its
 * texture ({@link Grain}) in one place instead of in every Activity and adapter.
 *
 * How it's wired: AppCompat turns each XML tag into a view through a "view inflater", and the
 * theme names which class that is ({@code viewInflaterClass} in themes.xml). Material's own
 * inflater swaps {@code <Button>} for MaterialButton and so on; this one extends it and, for
 * the component tags below, builds the component and attaches the texture. Plain layouts,
 * TextViews and ImageViews get it too when their background is one of the paper shapes (a queue
 * card, a note). Every other tag returns null, which tells AppCompat to inflate it the normal way.
 */
public class QuappViewInflater extends MaterialComponentsViewInflater {

    private static final String ANDROID = "http://schemas.android.com/apk/res/android";

    /** XML backgrounds that are a piece of paper: views on them get the texture too. */
    private static final int[] PAPER_SHAPES = {
            R.drawable.bg_card, R.drawable.bg_index_card, R.drawable.bg_search, R.drawable.bg_field,
            R.drawable.bg_note, R.drawable.bg_note_warn, R.drawable.bg_tile, R.drawable.bg_avatar,
            R.drawable.bg_step_number,
    };

    @Nullable
    @Override
    protected View createView(Context context, String name, AttributeSet attrs) {
        View view;
        switch (name) {
            case "com.google.android.material.button.MaterialButton":
                view = new MaterialButton(context, attrs);
                break;
            case "com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton":
                view = new ExtendedFloatingActionButton(context, attrs);
                break;
            case "com.google.android.material.card.MaterialCardView":
                view = new MaterialCardView(context, attrs);
                break;
            case "com.google.android.material.chip.Chip":
                view = new Chip(context, attrs);
                break;
            case "com.google.android.material.textfield.TextInputEditText":
                view = new TextInputEditText(context, attrs);
                break;
            case "com.google.android.material.bottomnavigation.BottomNavigationView":
                view = new BottomNavigationView(context, attrs);
                break;
            // Plain containers are only built here when they sit on a paper background
            case "LinearLayout":
                if (!onPaperShape(attrs)) return null;
                view = new LinearLayout(context, attrs);
                break;
            case "FrameLayout":
                if (!onPaperShape(attrs)) return null;
                view = new FrameLayout(context, attrs);
                break;
            case "androidx.constraintlayout.widget.ConstraintLayout":
                if (!onPaperShape(attrs)) return null;
                view = new ConstraintLayout(context, attrs);
                break;
            default:
                return null;
        }
        Grain.attach(view);
        return view;
    }

    /** A TextView on a paper background (a note, a step number): texture it. */
    @NonNull
    @Override
    protected AppCompatTextView createTextView(Context context, AttributeSet attrs) {
        AppCompatTextView text = super.createTextView(context, attrs);
        if (onPaperShape(attrs)) Grain.attach(text);
        return text;
    }

    /** An ImageView on a paper background (a category tile, an avatar): texture it. */
    @NonNull
    @Override
    protected AppCompatImageView createImageView(Context context, AttributeSet attrs) {
        AppCompatImageView image = super.createImageView(context, attrs);
        if (onPaperShape(attrs)) Grain.attach(image);
        return image;
    }

    /**
     * True when the tag's android:background is one of the paper shapes. Status pills and the QR
     * plate aren't listed: pills are too small to show it, and the QR has to scan cleanly.
     */
    private static boolean onPaperShape(AttributeSet attrs) {
        int background = attrs.getAttributeResourceValue(ANDROID, "background", 0);
        for (int paper : PAPER_SHAPES) {
            if (background == paper) return true;
        }
        return false;
    }

    /** A plain {@code <Button>} tag (Material makes it a MaterialButton): texture it too. */
    @NonNull
    @Override
    protected AppCompatButton createButton(@NonNull Context context, @NonNull AttributeSet attrs) {
        AppCompatButton button = super.createButton(context, attrs);
        Grain.attach(button);
        return button;
    }
}
