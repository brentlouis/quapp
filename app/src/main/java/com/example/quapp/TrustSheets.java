package com.example.quapp;

import android.app.Activity;
import android.content.Intent;
import android.text.Html;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;

/**
 * The trust and safety sheets, both on sheet_notice.xml:
 * who runs a queue (canvas 52, from the organizer line on Queue detail) and the unverified
 * organizer's one-live-queue limit (58, when opening a second queue).
 */
final class TrustSheets {

    private TrustSheets() {
        // Utility class.
    }

    /**
     * Canvas 52. Verified: what the badge promises, and that it doesn't vouch for each queue.
     * Unverified: that nobody has checked, so compare it with the organizer's own announcement.
     * Both offer Report this queue.
     */
    static void showOrganizer(final Activity activity, final Queue queue) {
        final Notice notice = new Notice(activity);
        boolean verified = queue.isOrganizerVerified();
        String name = "<b>" + TextUtils.htmlEncode(queue.getOrganizerName()) + "</b>";

        notice.icon(verified ? R.drawable.ic_badge_check : R.drawable.ic_info,
                verified ? R.color.ok : R.color.warn);
        notice.title.setText(verified ? R.string.badge_verified_title : R.string.badge_unverified_title);
        notice.body.setText(Html.fromHtml(activity.getString(verified
                ? R.string.badge_verified_body : R.string.badge_unverified_body, name),
                Html.FROM_HTML_MODE_LEGACY));
        notice.note.setText(verified ? R.string.badge_verified_note : R.string.badge_unverified_note);

        notice.primary.setText(R.string.badge_got_it);
        notice.primary.setOnClickListener(notice.dismiss());
        notice.secondary.setText(R.string.badge_report);
        notice.secondary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                notice.dialog.dismiss();
                activity.startActivity(ReportQueueActivity.intent(activity, queue.getId()));
            }
        });
        notice.dialog.show();
    }

    /**
     * Canvas 58: an unverified organizer already has a queue running. They can still set up an
     * upcoming one, or get verified to lift the limit.
     */
    static void showOneLiveQueue(final Activity activity, Queue running) {
        final Notice notice = new Notice(activity);
        notice.icon(R.drawable.ic_info, R.color.warn);
        notice.title.setText(R.string.limit_title);
        notice.body.setText(activity.getString(running.getStatus() == Queue.Status.PAUSED
                ? R.string.limit_body_paused : R.string.limit_body, running.getName()));
        notice.note.setVisibility(View.GONE);

        // Already asked: nothing to do but wait for the call.
        boolean pending = FakeData.myVerification() == VerificationStatus.PENDING;
        notice.primary.setText(pending ? R.string.limit_pending : R.string.limit_get_verified);
        notice.primary.setEnabled(!pending);
        notice.primary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                notice.dialog.dismiss();
                activity.startActivity(new Intent(activity, GetVerifiedActivity.class));
            }
        });
        notice.secondary.setText(R.string.limit_not_now);
        notice.secondary.setOnClickListener(notice.dismiss());
        notice.dialog.show();
    }

    /** One sheet_notice.xml in a BottomSheetDialog. */
    private static final class Notice {
        final Activity activity;
        final BottomSheetDialog dialog;
        final View root;
        final TextView title;
        final TextView body;
        final TextView note;
        final MaterialButton primary;
        final MaterialButton secondary;

        Notice(Activity activity) {
            this.activity = activity;
            dialog = new BottomSheetDialog(activity);
            root = activity.getLayoutInflater().inflate(R.layout.sheet_notice, null);
            title = root.findViewById(R.id.notice_title);
            body = root.findViewById(R.id.notice_body);
            note = root.findViewById(R.id.notice_note);
            primary = root.findViewById(R.id.notice_primary);
            secondary = root.findViewById(R.id.notice_secondary);
            dialog.setContentView(root);
        }

        void icon(int drawable, int color) {
            ImageView icon = root.findViewById(R.id.notice_icon);
            icon.setImageResource(drawable);
            icon.setColorFilter(ContextCompat.getColor(activity, color));
        }

        View.OnClickListener dismiss() {
            return new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            };
        }
    }
}
