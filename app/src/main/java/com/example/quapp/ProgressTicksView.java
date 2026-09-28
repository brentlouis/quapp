package com.example.quapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

/**
 * The row of ticks on the My ticket spotlight: how far the line has moved towards you.
 * Done ticks are light, the rest are the spotlight's line colour, and the last tick is you,
 * in crema. Decorative; the numbers next to it say the same thing in words.
 */
public class ProgressTicksView extends View {

    private static final int TICK_COUNT = 24;

    private final Paint donePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint todoPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF tick = new RectF();
    private final float gap;
    private final float corner;

    private int done;

    public ProgressTicksView(Context context) {
        this(context, null);
    }

    public ProgressTicksView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        donePaint.setColor(ContextCompat.getColor(context, R.color.on_spotlight_muted));
        todoPaint.setColor(ContextCompat.getColor(context, R.color.spotlight_line));
        mePaint.setColor(ContextCompat.getColor(context, R.color.crema));
        gap = getResources().getDimension(R.dimen.ticks_gap);
        corner = getResources().getDimension(R.dimen.hairline);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    /**
     * @param fraction how much of the line ahead of you has been served, 0 to 1. The last tick
     *                 is always you, so at most TICK_COUNT - 1 ticks are ever done.
     */
    public void setProgress(float fraction) {
        float clamped = Math.max(0f, Math.min(1f, fraction));
        done = Math.round(clamped * (TICK_COUNT - 1));
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width = (getWidth() - gap * (TICK_COUNT - 1)) / TICK_COUNT;
        for (int i = 0; i < TICK_COUNT; i++) {
            float left = i * (width + gap);
            tick.set(left, 0, left + width, getHeight());
            Paint paint = i == TICK_COUNT - 1 ? mePaint : (i < done ? donePaint : todoPaint);
            canvas.drawRoundRect(tick, corner, corner, paint);
        }
    }
}
