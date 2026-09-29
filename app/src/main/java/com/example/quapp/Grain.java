package com.example.quapp;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.view.View;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.shape.ShapeAppearanceModel;
import com.google.android.material.shape.ShapeAppearancePathProvider;

/**
 * Texture on components, so they look like they're cut from the same paper as the grained
 * screen ground (DESIGN.md section 7). The tiles come from tools/make_grain.py.
 *
 * The texture follows the component's fill, read fresh every time it draws, so a chip that
 * becomes selected (paper to ink) switches texture by itself:
 * <ul>
 *   <li>no fill (text and outlined buttons): nothing; the grained paper already shows through</li>
 *   <li>the espresso spotlight: {@code grain_ticket}, pale fibres and a mottle</li>
 *   <li>any other dark fill (filled buttons, the selected chip, the FAB): {@code grain_ink},
 *       pale specks where the ink didn't take</li>
 *   <li>a light fill (cards, fields, chips, tonal buttons, the nav bar, sheets):
 *       {@code grain_stock}, dark fibres like card stock</li>
 * </ul>
 *
 * The texture is painted inside the component's own Material shape, so rounded corners, pills
 * and the ticket's punched holes stay clean.
 */
public final class Grain {

    /** Fills darker than this (relative luminance, 0 = black) get the pale textures. */
    private static final double DARK_FILL = 0.3;
    /** Fills more transparent than this get no texture. */
    private static final int MIN_FILL_ALPHA = 16;

    private static Bitmap stock;
    private static Bitmap ink;
    private static Bitmap ticket;

    private Grain() {
    }

    /**
     * Textures a Material component (button, card, chip, field, nav bar), or a view with an XML
     * {@code <shape>} background. It draws in the view's
     * overlay, on top of its content: the grain is faint enough that it only shows on the fill.
     * Called for every inflated component by {@link QuappViewInflater}.
     */
    public static void attach(@NonNull View view) {
        final Overlay grain = new Overlay(view);
        view.getOverlay().add(grain);
        view.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
                grain.setBounds(0, 0, right - left, bottom - top));
    }

    /** Textures a bottom sheet's paper once it's on screen. */
    public static void attach(@NonNull BottomSheetDialog sheet) {
        sheet.setOnShowListener(dialog -> {
            View paper = sheet.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (paper != null) {
                attach(paper);
                paper.requestLayout();
            }
        });
    }

    /**
     * A background built in code (TicketShapes), with its texture painted on top of the fill
     * and under the content.
     */
    @NonNull
    public static Drawable over(@NonNull Context context, @NonNull MaterialShapeDrawable shape) {
        return new LayerDrawable(new Drawable[]{shape, new Background(context, shape)});
    }

    /** The tile for a fill colour, or null when the fill is (nearly) see-through. */
    @Nullable
    static Bitmap tileFor(@NonNull Context context, int fill) {
        if (Color.alpha(fill) < MIN_FILL_ALPHA) {
            return null;
        }
        if (fill == ContextCompat.getColor(context, R.color.spotlight)) {
            if (ticket == null) ticket = load(context, R.drawable.grain_ticket);
            return ticket;
        }
        if (ColorUtils.calculateLuminance(fill) < DARK_FILL) {
            if (ink == null) ink = load(context, R.drawable.grain_ink);
            return ink;
        }
        if (stock == null) stock = load(context, R.drawable.grain_stock);
        return stock;
    }

    private static Bitmap load(Context context, @DrawableRes int res) {
        // inScaled off: the tiles are drawn at one texture pixel per screen pixel, like paper_grain
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        return BitmapFactory.decodeResource(context.getApplicationContext().getResources(), res, options);
    }

    private static int colorFor(@Nullable ColorStateList list, int[] state) {
        return list == null ? Color.TRANSPARENT : list.getColorForState(state, list.getDefaultColor());
    }

    /** The XML {@code <shape>} in a background, looking inside a {@code <selector>}. */
    @Nullable
    private static GradientDrawable gradientIn(@Nullable Drawable drawable) {
        if (drawable instanceof android.graphics.drawable.DrawableContainer) {
            drawable = drawable.getCurrent();
        }
        return drawable instanceof GradientDrawable ? (GradientDrawable) drawable : null;
    }

    /** Finds the first MaterialShapeDrawable in a background (they're often wrapped in layers). */
    @Nullable
    private static MaterialShapeDrawable shapeIn(@Nullable Drawable drawable) {
        if (drawable instanceof MaterialShapeDrawable) {
            return (MaterialShapeDrawable) drawable;
        }
        if (drawable instanceof LayerDrawable) {
            LayerDrawable layers = (LayerDrawable) drawable;
            for (int i = 0; i < layers.getNumberOfLayers(); i++) {
                MaterialShapeDrawable found = shapeIn(layers.getDrawable(i));
                if (found != null) return found;
            }
        }
        if (drawable instanceof android.graphics.drawable.DrawableWrapper) {
            return shapeIn(((android.graphics.drawable.DrawableWrapper) drawable).getDrawable());
        }
        return null;
    }

    /** The common part: fill a shape with a tiled texture. */
    private abstract static class Textured extends Drawable {
        private final ShapeAppearancePathProvider paths = new ShapeAppearancePathProvider();
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        private final RectF area = new RectF();
        private Bitmap shaderTile;

        /** Paints the texture for {@code fill} inside {@code shape} over {@code area}. */
        void paint(@NonNull Canvas canvas, @NonNull Context context, int fill,
                   @NonNull ShapeAppearanceModel shape) {
            Bitmap tile = tileFor(context, fill);
            if (tile == null || area.isEmpty()) {
                return;
            }
            if (tile != shaderTile) {
                paint.setShader(new BitmapShader(tile, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT));
                shaderTile = tile;
            }
            path.rewind();
            paths.calculatePath(shape, 1f, area, path);
            canvas.drawPath(path, paint);
        }

        RectF area() {
            return area;
        }

        @Override
        public void setAlpha(int alpha) {
            paint.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(@Nullable ColorFilter colorFilter) {
            paint.setColorFilter(colorFilter);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }

    /** Over a view: asks the view for its shape, fill and drawn area every frame. */
    private static final class Overlay extends Textured {
        private final View view;

        Overlay(View view) {
            this.view = view;
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            int[] state = view.getDrawableState();
            RectF area = area();
            area.set(0, 0, view.getWidth(), view.getHeight());
            if (view instanceof Chip) {
                // A chip draws smaller than its 48dp touch area; texture only the pill
                Chip chip = (Chip) view;
                if (chip.getChipDrawable() != null) area.set(chip.getChipDrawable().getBounds());
                int fill = colorFor(chip.getChipBackgroundColor(), state);
                if (Color.alpha(fill) < MIN_FILL_ALPHA) {
                    // A "clear" chip still sits on Material's chip surface colour
                    fill = MaterialColors.getColor(chip, com.google.android.material.R.attr.colorSurface);
                }
                paint(canvas, view.getContext(), fill, chip.getShapeAppearanceModel());
            } else if (view instanceof MaterialButton) {
                MaterialButton button = (MaterialButton) view;
                area.set(button.getInsetLeft(), button.getInsetTop(),
                        view.getWidth() - button.getInsetRight(), view.getHeight() - button.getInsetBottom());
                paint(canvas, view.getContext(), colorFor(button.getBackgroundTintList(), state),
                        button.getShapeAppearanceModel());
            } else if (view instanceof MaterialCardView) {
                MaterialCardView card = (MaterialCardView) view;
                paint(canvas, view.getContext(), colorFor(card.getCardBackgroundColor(), state),
                        card.getShapeAppearanceModel());
            } else if (gradientIn(view.getBackground()) != null) {
                // An XML <shape> background (a queue card, the search bar): a rounded rectangle.
                // A backgroundTint on the view wins over the shape's own colour.
                GradientDrawable shape = gradientIn(view.getBackground());
                ColorStateList fill = view.getBackgroundTintList() != null
                        ? view.getBackgroundTintList() : shape.getColor();
                float radius = Math.min(shape.getCornerRadius(),
                        Math.min(view.getWidth(), view.getHeight()) / 2f);
                paint(canvas, view.getContext(), colorFor(fill, state),
                        ShapeAppearanceModel.builder().setAllCornerSizes(radius).build());
            } else {
                // Fields, the nav bar, sheets: whatever MaterialShapeDrawable their background holds
                MaterialShapeDrawable shape = shapeIn(view.getBackground());
                if (shape == null) return;
                area.set(shape.getBounds());
                paint(canvas, view.getContext(), colorFor(shape.getFillColor(), state),
                        shape.getShapeAppearanceModel());
            }
        }
    }

    /** Under a view's content, as a layer of a background built in code. */
    private static final class Background extends Textured {
        private final Context context;
        private final MaterialShapeDrawable shape;

        Background(Context context, MaterialShapeDrawable shape) {
            this.context = context;
            this.shape = shape;
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            area().set(getBounds());
            paint(canvas, context, colorFor(shape.getFillColor(), getState()),
                    shape.getShapeAppearanceModel());
        }
    }
}
