package com.example.quapp;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.core.widget.NestedScrollView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import static org.junit.Assert.*;

/** Existing participant Activities only; fixtures never enter the shipped APK. */
@RunWith(AndroidJUnit4.class)
public class ParticipantVisualReviewTest {
    private final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();

    @Test public void reviewThreeVisualScreens() throws Exception {
        Class<?>[] screens = {RoleSelectActivity.class, LoginActivity.class, RegisterActivity.class};
        int[] headings = {R.id.role_heading, R.id.login_heading, R.id.register_heading};
        String[] names = {"welcome", "sign-in", "create-account"};
        for (int i = 0; i < screens.length; i++) {
            Intent intent = new Intent(instrumentation.getTargetContext(), screens[i]);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            Activity activity = instrumentation.startActivitySync(intent);
            try {
                instrumentation.waitForIdleSync();
                capture(activity, names[i] + "-default");
                final int heading = headings[i];
                instrumentation.runOnMainSync(() -> ((TextView) activity.findViewById(heading))
                    .setText("👩🏽‍💻 王秀英 — Aleksandra Wiśniewska-Kowalczyk"));
                instrumentation.waitForIdleSync();
                capture(activity, names[i] + "-unicode-long");
                NestedScrollView scroll = findScroll(activity.findViewById(android.R.id.content));
                if (scroll != null) {
                    instrumentation.runOnMainSync(() -> scroll.fullScroll(View.FOCUS_DOWN));
                    instrumentation.waitForIdleSync();
                    capture(activity, names[i] + "-bottom");
                }
                if (i > 0) {
                    int submit = i == 1 ? R.id.login_submit_button : R.id.register_submit_button;
                    int field = i == 1 ? R.id.login_phone_layout : R.id.register_name_layout;
                    instrumentation.runOnMainSync(() -> {
                        MaterialButton button = activity.findViewById(submit);
                        float density = activity.getResources().getDisplayMetrics().density;
                        assertTrue("Primary touch height", button.getHeight() >= 48 * density - 1);
                        assertTrue("Primary touch width", button.getWidth() >= 48 * density - 1);
                        button.performClick();
                        assertNotNull("Existing empty-field validation", ((TextInputLayout) activity.findViewById(field)).getError());
                    });
                    if (scroll != null) instrumentation.runOnMainSync(() -> scroll.fullScroll(View.FOCUS_UP));
                    instrumentation.waitForIdleSync();
                    capture(activity, names[i] + "-empty-error");
                }
            } finally {
                instrumentation.runOnMainSync(activity::finish);
            }
        }
    }

    @Test public void verificationActionKeepsItsFullLabel() throws Exception {
        Activity activity = instrumentation.startActivitySync(new Intent(
                instrumentation.getTargetContext(), GetVerifiedActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        try {
            instrumentation.waitForIdleSync();
            capture(activity, "organizer-verification");
            instrumentation.runOnMainSync(() -> {
                MaterialButton submit = activity.findViewById(R.id.verify_submit);
                android.graphics.Rect bounds = new android.graphics.Rect();
                assertTrue(submit.getGlobalVisibleRect(bounds));
                assertTrue(bounds.height() == submit.getHeight());
                assertTrue(submit.getHeight() >= activity.getResources().getDimensionPixelSize(R.dimen.touch_target_min));
                assertNotNull(submit.getLayout());
                assertTrue(submit.getLayout().getHeight() <= submit.getHeight()
                        - submit.getCompoundPaddingTop() - submit.getCompoundPaddingBottom());
            });
        } finally {
            instrumentation.runOnMainSync(activity::finish);
        }
    }

    @Test public void dialogActionsRemainReadableOnLightSurfaces() throws Exception {
        Activity activity = instrumentation.startActivitySync(new Intent(
                instrumentation.getTargetContext(), LoginActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        try {
            for (int overlay : new int[]{0, R.style.ThemeOverlay_Quapp_Dialog_WalkIn}) {
                final androidx.appcompat.app.AlertDialog[] dialog = new androidx.appcompat.app.AlertDialog[1];
                instrumentation.runOnMainSync(() -> {
                    dialog[0] = new com.google.android.material.dialog.MaterialAlertDialogBuilder(activity, overlay)
                            .setTitle(R.string.saved_clear_title).setMessage(R.string.saved_clear_body)
                            .setNegativeButton(android.R.string.cancel, null)
                            .setPositiveButton(R.string.saved_clear_confirm, null).show();
                    for (int which : new int[]{android.content.DialogInterface.BUTTON_POSITIVE, android.content.DialogInterface.BUTTON_NEGATIVE}) {
                        int ink = dialog[0].getButton(which).getCurrentTextColor();
                        assertTrue("Dialog action contrast must reach 4.5:1",
                                androidx.core.graphics.ColorUtils.calculateContrast(ink, activity.getColor(R.color.paper_sunk)) >= 4.5);
                    }
                });
                capture(activity, "dialog-actions-" + overlay);
                instrumentation.runOnMainSync(dialog[0]::dismiss);
            }
        } finally {
            instrumentation.runOnMainSync(activity::finish);
        }
    }

    private NestedScrollView findScroll(View view) {
        if (view instanceof NestedScrollView) return (NestedScrollView) view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                NestedScrollView found = findScroll(group.getChildAt(i));
                if (found != null) return found;
            }
        }
        return null;
    }

    private void capture(Activity activity, String name) throws Exception {
        Thread.sleep(180);
        File folder = new File(activity.getExternalFilesDir(null), "phase2a-review");
        assertTrue(folder.isDirectory() || folder.mkdirs());
        Bitmap bitmap = instrumentation.getUiAutomation().takeScreenshot();
        assertNotNull(bitmap);
        String scale = String.valueOf(activity.getResources().getConfiguration().fontScale);
        File file = new File(folder, name + "-" + scale + ".png");
        try (FileOutputStream stream = new FileOutputStream(file)) {
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream));
        } finally {
            bitmap.recycle();
        }
    }
}
