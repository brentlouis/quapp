package com.example.quapp;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

/**
 * The eight queue categories (DECISIONS.md "Categories: eight"). An enum, not strings, so a
 * typo can't create a ninth category. Each carries its label, the picker's hint line and its
 * icon; the enum name is what travels in the API ("RELIEF").
 */
public enum Category {
    RELIEF(R.string.category_relief, R.string.category_relief_hint, R.drawable.ic_category_relief),
    MEDICAL(R.string.category_medical, R.string.category_medical_hint, R.drawable.ic_category_medical),
    GOVERNMENT(R.string.category_government, R.string.category_government_hint,
            R.drawable.ic_category_government),
    EDUCATION(R.string.category_education, R.string.category_education_hint,
            R.drawable.ic_category_education),
    BILLS(R.string.category_bills, R.string.category_bills_hint, R.drawable.ic_category_bills),
    IDS(R.string.category_ids, R.string.category_ids_hint, R.drawable.ic_category_ids),
    JOBS(R.string.category_jobs, R.string.category_jobs_hint, R.drawable.ic_category_jobs),
    OTHER(R.string.category_other, R.string.category_other_hint, R.drawable.ic_category_other);

    @StringRes
    public final int label;
    @StringRes
    public final int hint;
    @DrawableRes
    public final int icon;

    Category(@StringRes int label, @StringRes int hint, @DrawableRes int icon) {
        this.label = label;
        this.hint = hint;
        this.icon = icon;
    }
}
