package com.example.quapp;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Fills a receipt slip (DESIGN.md section 5): a LinearLayout of "LABEL ······ value" rows.
 * The slip's look (raised paper, line edge, rules between rows) is on the LinearLayout in XML;
 * this only adds the rows, so a screen can list whatever numbers it has.
 */
public final class ReceiptSlip {

    private ReceiptSlip() {
        // Utility class.
    }

    /** Removes the old rows. Call before adding, so a refresh doesn't stack duplicates. */
    public static void clear(LinearLayout slip) {
        slip.removeAllViews();
    }

    public static void addRow(LinearLayout slip, CharSequence label, CharSequence value) {
        View row = LayoutInflater.from(slip.getContext())
                .inflate(R.layout.view_slip_row, slip, false);
        ((TextView) row.findViewById(R.id.slip_label)).setText(label);
        ((TextView) row.findViewById(R.id.slip_value)).setText(value);
        slip.addView(row);
    }
}
