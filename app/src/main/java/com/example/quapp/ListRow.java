package com.example.quapp;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;

/** Fills in an included view_list_row: icon, title and an optional subtitle. */
public final class ListRow {

    private ListRow() {
        // Utility class.
    }

    public static void bind(View row, @DrawableRes int icon, CharSequence title,
                            @Nullable CharSequence subtitle) {
        ((ImageView) row.findViewById(R.id.row_icon)).setImageResource(icon);
        ((TextView) row.findViewById(R.id.row_title)).setText(title);

        TextView subtitleText = row.findViewById(R.id.row_subtitle);
        subtitleText.setText(subtitle);
        subtitleText.setVisibility(subtitle == null ? View.GONE : View.VISIBLE);
    }
}
