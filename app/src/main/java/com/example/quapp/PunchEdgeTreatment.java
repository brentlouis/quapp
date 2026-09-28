package com.example.quapp;

import androidx.annotation.NonNull;

import com.google.android.material.shape.EdgeTreatment;
import com.google.android.material.shape.ShapePath;

/**
 * Cuts one half-circle "punch" into an edge of a Material shape, like the holes where a paper
 * ticket tore off the roll (DESIGN.md section 5). Same idea as Material's TriangleEdgeTreatment.
 *
 * How edges are drawn: MaterialShapeDrawable walks each edge in its own coordinate space. x runs
 * along the edge from 0 to length (top edge left→right, right edge top→bottom, bottom edge
 * right→left, left edge bottom→top), and positive y points into the shape. So a punch is a
 * straight line to the hole, a half-circle dipping into +y, then a straight line to the end.
 *
 * The part of the view outside the path isn't drawn, so the holes really are holes: the paper
 * and its grain show through.
 */
public class PunchEdgeTreatment extends EdgeTreatment {

    private final float radius;
    private final float position;
    private final boolean absolute;

    /**
     * @param radius   punch radius in px
     * @param position where the punch's centre sits, measured from the edge's start in drawing
     *                 direction: a fraction of the edge (0..1) or, if {@code absolute}, px
     * @param absolute true when {@code position} is in px rather than a fraction
     */
    public PunchEdgeTreatment(float radius, float position, boolean absolute) {
        this.radius = radius;
        this.position = position;
        this.absolute = absolute;
    }

    @Override
    public void getEdgePath(float length, float center, float interpolation, @NonNull ShapePath shapePath) {
        float r = radius * interpolation;
        float c = absolute ? position : position * length;
        shapePath.lineTo(c - r, 0f);
        // Bounding box of the circle centred on the edge; start at its left point (180°) and sweep
        // back through 90° (+y, into the shape) to its right point.
        shapePath.addArc(c - r, -r, c + r, r, 180f, -180f);
        shapePath.lineTo(length, 0f);
    }
}
