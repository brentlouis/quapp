package com.example.quapp;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.time.Instant;
import static org.junit.Assert.assertEquals;

/** Exercises the real adapter binding without queue operations or persisted fixtures. */
@RunWith(AndroidJUnit4.class)
public class WaitingTicketPresentationTest {
    @Test public void optionalPhoneKeepsJoinedTimeAndExistingTags() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context context = new ContextThemeWrapper(
                    InstrumentationRegistry.getInstrumentation().getTargetContext(), R.style.Theme_Quapp);
            WaitingTicketAdapter adapter = new WaitingTicketAdapter(null);
            WaitingTicketAdapter.WaitingViewHolder holder = adapter.onCreateViewHolder(new FrameLayout(context), 0);
            Ticket base = new Ticket.Builder().setId("presentation-only").setQueue("presentation-only", "Demo", "")
                    .setHolder("Demo", null).setTicketNumber(1).setJoinedAt(Instant.now()).build();
            TextView phone = holder.itemView.findViewById(R.id.waiting_phone);
            TextView tag = holder.itemView.findViewById(R.id.waiting_tag);
            for (String value : new String[]{null, "", "09171234567"}) {
                Ticket ticket = base.toBuilder().setHolder("Demo", value).build();
                holder.bind(ticket, null);
                String joined = WaitingTicketAdapter.joinedAgo(context, ticket);
                assertEquals(value == null || value.isEmpty() ? joined
                        : context.getString(R.string.console_joined_phone_format,
                                WaitingTicketAdapter.maskPhone(context, value), joined), phone.getText().toString());
                assertEquals(View.GONE, tag.getVisibility());
            }
            Ticket walkIn = base.toBuilder().setWalkIn(true).build();
            holder.bind(walkIn, null);
            assertEquals(WaitingTicketAdapter.joinedAgo(context, walkIn), phone.getText().toString());
            assertEquals(context.getString(R.string.console_walk_in_label), tag.getText().toString());
            assertEquals(View.VISIBLE, tag.getVisibility());
            Ticket moved = walkIn.toBuilder().setMovedBack(Instant.now()).build();
            holder.bind(moved, null);
            assertEquals(context.getString(R.string.console_moved_at_format,
                    Format.time(context, moved.getMovedBackAt())), phone.getText().toString());
            assertEquals(context.getString(R.string.console_moved_back_tag), tag.getText().toString());
            assertEquals(View.VISIBLE, tag.getVisibility());
        });
    }
}
