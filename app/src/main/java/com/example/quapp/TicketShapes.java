package com.example.quapp;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.view.View;

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
     * Punches in the top and bottom edges at the same distance from the left (the kept ticket).
     *
     * @param fromLeftPx distance of the punches from the left edge, in px
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

    /**
     * The espresso ticket on My tickets and Active ticket: the stub is on top, the tear line runs
     * across, and the punches sit at its two ends. Call it after layout (the tear line's height
     * in the ticket is only known then); {@code tear} must be a direct child of {@code ticket}.
     */
    public static Drawable tearLineBackground(@NonNull Context context, @NonNull View ticket,
                                              @NonNull View tear) {
        float fromTop = (tear.getTop() + tear.getHeight() / 2f) / ticket.getHeight();
        return background(context,
                sidePunched(context, R.dimen.radius_card, R.dimen.punch_radius, fromTop),
                ContextCompat.getColor(context, R.color.spotlight));
    }

    /** The espresso spotlight: 10dp corners, 9dp punches halfway down both sides. */
    public static ShapeAppearanceModel spotlight(@NonNull Context context) {
        return sidePunched(context, R.dimen.radius_card, R.dimen.punch_radius, 0.5f);
    }

    /** The punched selection on a nav tab or a selected chip: 5dp punches. */
    public static ShapeAppearanceModel selection(@NonNull Context context, @DimenRes int cornerRes) {
        return sidePunched(context, cornerRes, R.dimen.punch_radius_small, 0.5f);
    }

    /** A filled, textured drawable of any ticket shape, for {@code view.setBackground(...)}. */
    public static Drawable background(@NonNull Context context, @NonNull ShapeAppearanceModel shape,
                                      @ColorInt int fill) {
        MaterialShapeDrawable drawable = new MaterialShapeDrawable(shape);
        drawable.setFillColor(ColorStateList.valueOf(fill));
        return Grain.over(context, drawable);
    }

    /** Shortcut: the spotlight shape filled espresso. */
    public static Drawable spotlightBackground(@NonNull Context context) {
        return background(context, spotlight(context), ContextCompat.getColor(context, R.color.spotlight));
    }

    /**
     * A quiet dock for Queue detail and Join; the ticket itself carries the punched identity.
     */
    public static Drawable stubDockBackground(@NonNull Context context) {
        return new android.graphics.drawable.ColorDrawable(
                ContextCompat.getColor(context, R.color.paper_raised));
    }

    /**
     * The kept ticket on an outcome (Served, Slot released): raised paper with a line edge,
     * punched top and bottom halfway across, where its tear line runs.
     */
    public static Drawable keptTicketBackground(@NonNull Context context, float widthPx) {
        MaterialShapeDrawable drawable = new MaterialShapeDrawable(
                stub(context, R.dimen.radius_card, R.dimen.punch_radius, widthPx / 2f, widthPx));
        drawable.setFillColor(ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.paper_raised)));
        drawable.setStroke(px(context, R.dimen.hairline),
                ContextCompat.getColor(context, R.color.line));
        return Grain.over(context, drawable);
    }

    /**
     * The rubber stamp's outline: a thin rule, a gap, a heavy rule, in the status colour.
     * Built in code rather than tinted, because tinting a stroke-only shape can fill it solid.
     */
    public static Drawable stampBackground(@NonNull Context context, @ColorInt int color) {
        GradientDrawable outer = new GradientDrawable();
        outer.setCornerRadius(px(context, R.dimen.stamp_radius));
        outer.setStroke(Math.round(px(context, R.dimen.hairline)), color);

        GradientDrawable inner = new GradientDrawable();
        inner.setCornerRadius(px(context, R.dimen.stamp_inner_radius));
        inner.setStroke(Math.round(px(context, R.dimen.stamp_rule)), color);

        LayerDrawable stamp = new LayerDrawable(new Drawable[]{outer, inner});
        int gap = Math.round(px(context, R.dimen.stamp_gap));
        stamp.setLayerInset(1, gap, gap, gap, gap);
        return stamp;
    }

    private static float px(Context context, @DimenRes int res) {
        return context.getResources().getDimension(res);
    }
}
