package com.example.quapp;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;

import androidx.annotation.ColorInt;
import androidx.annotation.DimenRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.android.material.shape.CornerFamily;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.shape.ShapeAppearanceModel;

/**
 * The ticket shapes from DESIGN.md section 5, built from PunchEdgeTreatment.
 *
 * Shapes are {@link ShapeAppearanceModel}s, so they work anywhere Material takes one:
 * {@code card.setShapeAppearanceModel(...)}, {@code chip.setShapeAppearanceModel(...)}, or as a
 * plain background via {@link #background}.
 */
public final class TicketShapes {

    private TicketShapes() {
    }

    /**
     * Punches in the left and right edges at the same height.
     *
     * @param fromTop how far down the punches sit, as a fraction of the height (0.5 = middle)
     */
    public static ShapeAppearanceModel sidePunched(@NonNull Context context, @DimenRes int cornerRes,
                                                   @DimenRes int punchRes, float fromTop) {
        float corner = px(context, cornerRes);
        float punch = px(context, punchRes);
        return ShapeAppearanceModel.builder()
                .setAllCorners(CornerFamily.ROUNDED, corner)
                // The right edge is drawn top→bottom, the left edge bottom→top, so the left one mirrors.
                .setRightEdge(new PunchEdgeTreatment(punch, fromTop, false))
                .setLeftEdge(new PunchEdgeTreatment(punch, 1f - fromTop, false))
                .build();
    }

    /**
     * Punches in the top and bottom edges at the same distance from the left: the My ticket stub,
     * where the tear line runs top to bottom.
     *
     * @param fromLeftPx distance of the punches (and the tear line) from the left edge, in px
     * @param widthPx    the view's width in px; the bottom edge is drawn right→left, so it needs it
     */
    public static ShapeAppearanceModel stub(@NonNull Context context, @DimenRes int cornerRes,
                                            @DimenRes int punchRes, float fromLeftPx, float widthPx) {
        float corner = px(context, cornerRes);
        float punch = px(context, punchRes);
        return ShapeAppearanceModel.builder()
                .setAllCorners(CornerFamily.ROUNDED, corner)
                .setTopEdge(new PunchEdgeTreatment(punch, fromLeftPx, true))
                .setBottomEdge(new PunchEdgeTreatment(punch, widthPx - fromLeftPx, true))
                .build();
    }

    /** The espresso spotlight: 10dp corners, 9dp punches halfway down both sides. */
    public static ShapeAppearanceModel spotlight(@NonNull Context context) {
        return sidePunched(context, R.dimen.radius_md, R.dimen.punch_radius, 0.5f);
    }

    /** The punched selection on a nav tab or a selected chip: 5dp punches. */
    public static ShapeAppearanceModel selection(@NonNull Context context, @DimenRes int cornerRes) {
        return sidePunched(context, cornerRes, R.dimen.punch_radius_small, 0.5f);
    }

    /** A filled drawable of any ticket shape, for {@code view.setBackground(...)}. */
    public static Drawable background(@NonNull ShapeAppearanceModel shape, @ColorInt int fill) {
        MaterialShapeDrawable drawable = new MaterialShapeDrawable(shape);
        drawable.setFillColor(ColorStateList.valueOf(fill));
        return drawable;
    }

    /** Shortcut: the spotlight shape filled espresso. */
    public static Drawable spotlightBackground(@NonNull Context context) {
        return background(spotlight(context), ContextCompat.getColor(context, R.color.spotlight));
    }

    private static float px(Context context, @DimenRes int res) {
        return context.getResources().getDimension(res);
    }
}
