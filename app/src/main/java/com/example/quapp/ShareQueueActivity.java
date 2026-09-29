package com.example.quapp;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;

/**
 * Share queue (canvas 31): the queue's QR code and link, to post on a barangay page or print for
 * the venue. "Show on a counter screen" opens the counter display (32) for a TV or tablet.
 */
public class ShareQueueActivity extends AppCompatActivity {

    public static final String EXTRA_QUEUE_ID = "com.example.quapp.EXTRA_SHARE_QUEUE_ID";

    private String queueId;

    public static Intent intent(Context context, String queueId) {
        Intent intent = new Intent(context, ShareQueueActivity.class);
        intent.putExtra(EXTRA_QUEUE_ID, queueId);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_queue);
        SystemBars.applyPadding(findViewById(R.id.share_root));

        queueId = getIntent().getStringExtra(EXTRA_QUEUE_ID);
        final Queue queue = Queues.get(queueId);  // the screen that opened Share had loaded it
        if (queue == null) {
            finish();
            return;
        }

        ((TextView) findViewById(R.id.share_queue_name)).setText(queue.getName());
        ((TextView) findViewById(R.id.share_link)).setText(QueueLink.display(queue));

        int size = getResources().getDimensionPixelSize(R.dimen.share_qr_size);
        ((ImageView) findViewById(R.id.share_qr)).setImageBitmap(QueueLink.qr(QueueLink.url(queue),
                size, ContextCompat.getColor(this, R.color.ink),
                ContextCompat.getColor(this, R.color.paper_raised)));

        findViewById(R.id.share_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        findViewById(R.id.share_copy).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                copyLink(queue);
            }
        });

        findViewById(R.id.share_send).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                shareLink(queue);
            }
        });

        findViewById(R.id.share_counter).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(CounterDisplayActivity.intent(ShareQueueActivity.this, queueId));
            }
        });
    }

    private void copyLink(Queue queue) {
        ClipboardManager clipboard = ContextCompat.getSystemService(this, ClipboardManager.class);
        if (clipboard == null) {
            return;
        }
        clipboard.setPrimaryClip(ClipData.newPlainText(queue.getName(), QueueLink.url(queue)));
        // Android 13+ shows its own "Copied" preview, so only older versions need a message.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Snackbar.make(findViewById(R.id.share_root), R.string.share_copied, Snackbar.LENGTH_SHORT)
                    .setAnchorView(R.id.share_dock)
                    .show();
        }
    }

    /** The system share sheet: Messenger, Facebook, SMS, whatever the organizer uses. */
    private void shareLink(Queue queue) {
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_SUBJECT, queue.getName());
        send.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_message,
                queue.getName(), queue.getVenue(), QueueLink.url(queue)));
        startActivity(Intent.createChooser(send, getString(R.string.share_chooser_title)));
    }
}
