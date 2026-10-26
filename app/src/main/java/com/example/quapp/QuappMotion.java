package com.example.quapp;

import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.view.ContextThemeWrapper;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

/** Presentation motion; both the app guard and Android's remove-animations setting win. */
final class QuappMotion {
    private QuappMotion() { }

    static boolean enabled(Context context) {
        if (context.getResources().getBoolean(R.bool.config_prefersReducedMotion)
                || ProfilePreferences.get(context).getBoolean("reduced", false)) return false;
        return Build.VERSION.SDK_INT >= 26 ? ValueAnimator.areAnimatorsEnabled()
                : Settings.Global.getFloat(context.getContentResolver(),
                        Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f;
    }

    static void buttons(View view) {
        if (view instanceof MaterialButton) {
            // Material's ripple already confirms presses without shrinking 48dp hit targets.
            view.setStateListAnimator(null);
            view.setScaleX(1f);
            view.setScaleY(1f);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) buttons(group.getChildAt(i));
        }
    }

    static void list(RecyclerView list) {
        if (!enabled(list.getContext())) {
            list.setItemAnimator(null);
            return;
        }
        DefaultItemAnimator animator = new DefaultItemAnimator();
        animator.setMoveDuration(list.getResources().getInteger(R.integer.motion_duration_long));
        animator.setAddDuration(0);
        animator.setRemoveDuration(0);
        animator.setChangeDuration(0);
        animator.setSupportsChangeAnimations(false);
        list.setItemAnimator(animator);
    }

    static void sheet(BottomSheetDialog sheet) {
        if (sheet.getWindow() != null) {
            sheet.getWindow().setWindowAnimations(enabled(sheet.getContext())
                    ? R.style.Animation_Quapp_Sheet : R.style.Animation_Quapp_None);
        }
    }

    static Snackbar snackbar(View anchor, int text, int duration) {
        return snackbar(anchor, anchor.getResources().getText(text), duration);
    }

    static Snackbar snackbar(View anchor, CharSequence text, int duration) {
        Context context = new ContextThemeWrapper(anchor.getContext(), enabled(anchor.getContext())
                ? R.style.ThemeOverlay_Quapp_Motion : R.style.ThemeOverlay_Quapp_ReducedMotion);
        return Snackbar.make(context, anchor, text, duration)
                .setAnimationMode(Snackbar.ANIMATION_MODE_FADE);
    }
}
