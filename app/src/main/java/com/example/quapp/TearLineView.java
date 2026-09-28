package com.example.quapp;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

/**
 * The crema tear line (DESIGN.md section 5): 1.5dp dashes, 4dp on, 4dp off, horizontal or
 * vertical. It runs above every bottom button bar, along the top of the bottom nav and down
 * the ticket stub.
 *
 * A small View instead of a shape drawable because a dashed "line" shape can't go vertical,
 * and this draws the same way on every Android version.
 *
 * In XML: {@code <com.example.quapp.TearLineView app:tearOrientation="vertical" .../>}.
 * Give it 1.5dp across (height for horizontal, width for vertical) and match the other side.
 */
public class TearLineView extends View {

    private static final int HORIZONTAL = 0;
    private static final int DASHED = 0;
    private static final int DOTTED = 1;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int orientation;

    public TearLineView(Context context) {
        this(context, null);
    }

    public TearLineView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TearLineView);
        orientation = a.getInt(R.styleable.TearLineView_tearOrientation, HORIZONTAL);
        int color = a.getColor(R.styleable.TearLineView_tearColor,
                ContextCompat.getColor(context, R.color.crema));
        boolean dotted = a.getInt(R.styleable.TearLineView_tearStyle, DASHED) == DOTTED;
        a.recycle();

        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(getResources().getDimension(R.dimen.tear_line_width));
        if (dotted) {
            // A near-zero dash with round caps draws a dot: the leader in a receipt slip row.
            float gap = getResources().getDimension(R.dimen.dot_leader_gap);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setPathEffect(new DashPathEffect(new float[]{0.01f, gap}, 0f));
        } else {
            float dash = getResources().getDimension(R.dimen.tear_line_dash);
            paint.setPathEffect(new DashPathEffect(new float[]{dash, dash}, 0f));
        }
        // Decorative: screen readers skip it
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (orientation == HORIZONTAL) {
            float y = getHeight() / 2f;
            canvas.drawLine(getPaddingLeft(), y, getWidth() - getPaddingRight(), y, paint);
        } else {
            float x = getWidth() / 2f;
            canvas.drawLine(x, getPaddingTop(), x, getHeight() - getPaddingBottom(), paint);
        }
    }
}
