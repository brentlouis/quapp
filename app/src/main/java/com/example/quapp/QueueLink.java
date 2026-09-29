package com.example.quapp;

import android.graphics.Bitmap;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.text.Normalizer;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * A queue's public link and its QR code, for Share queue (canvas 31) and the counter display
 * (32). Organizers post the link on their page or print the QR at the venue, which is how most
 * people find a queue (DECISIONS.md "Design leans on how people already find queues").
 *
 * quapp.app is a placeholder domain until the project has a real one (see DECISIONS.md).
 */
final class QueueLink {

    private static final String BASE = "quapp.app/q/";

    private QueueLink() {
        // Utility class.
    }

    /** "quapp.app/q/free-medical-mission": short enough to read out or print. */
    static String display(Queue queue) {
        return BASE + slug(queue.getName());
    }

    /** The same link with https://, for sharing and inside the QR code. */
    static String url(Queue queue) {
        return "https://" + display(queue);
    }

    /**
     * Draws a QR code as a square bitmap. Null if the text is too long to encode (a queue link
     * never is). Dark modules on a light ground, since phone cameras expect that way round.
     */
    @Nullable
    static Bitmap qr(String text, int sizePx, @ColorInt int dark, @ColorInt int light) {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        // The quiet zone is the plate the code sits on in the layout, so none is added here.
        hints.put(EncodeHintType.MARGIN, 0);
        BitMatrix matrix;
        try {
            matrix = new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, sizePx, sizePx, hints);
        } catch (WriterException e) {
            return null;
        }

        int width = matrix.getWidth();
        int height = matrix.getHeight();
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixels[y * width + x] = matrix.get(x, y) ? dark : light;
            }
        }
        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888);
    }

    /** "Barangay Relief Distribution" → "barangay-relief-distribution"; accents dropped. */
    static String slug(String name) {
        String plain = Normalizer.normalize(name, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String slug = plain.toLowerCase(Locale.US).replaceAll("[^a-z0-9]+", "-");
        return slug.replaceAll("^-|-$", "");
    }
}
