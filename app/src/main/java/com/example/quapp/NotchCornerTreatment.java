package com.example.quapp;

import androidx.annotation.NonNull;

import com.google.android.material.shape.CornerTreatment;
import com.google.android.material.shape.ShapePath;

/**
 * A corner bitten out instead of rounded: a quarter circle cut into the shape, centred on the
 * corner itself. Used for the tear-off stub dock's top corners (DESIGN.md section 5), so it
 * looks torn from a roll.
 *
 * Material draws every corner as if it were the top-left one and rotates it into place, so
 * this only describes that one corner. A rounded corner's arc is centred inside the shape;
 * this one is centred on the corner point, which makes it curve inwards.
 */
public class NotchCornerTreatment extends CornerTreatment {

    @Override
    public void getCornerPath(@NonNull ShapePath shapePath, float angle, float interpolation,
                              float radius) {
        float r = radius * interpolation;
        // Start on the left edge, r below the corner, and sweep round to r along the top edge.
        shapePath.reset(0, r, 180, 180 - angle);
        shapePath.addArc(-r, -r, r, r, 90, -angle);
    }
}
