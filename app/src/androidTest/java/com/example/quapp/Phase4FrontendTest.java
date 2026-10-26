package com.example.quapp;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Controller/state checks; pointer, screenshot and TalkBack acceptance are separate. */
@RunWith(AndroidJUnit4.class)
public class Phase4FrontendTest {
    @Test public void familyDraftRestoresAndNeverCreatesIndividualTicket() {
        int ticketCount = FakeData.history().size();
        try (ActivityScenario<Phase4Activity> scenario = ActivityScenario.launch(Phase4Activity.intent(ApplicationProvider.getApplicationContext(), Phase4Activity.FAMILY))) {
            scenario.onActivity(a -> {
                click(a, R.string.p4_family_intro);
                click(a, R.string.p4_review); // Empty validation cannot advance.
                assertNotNull(button(a, R.string.p4_review));
                List<TextInputLayout> fields = fields(a.findViewById(R.id.p4_content));
                assertEquals(2, fields.size());
                fields.get(0).getEditText().setText("家族 🌿");
                fields.get(1).getEditText().setText("A\nB\nC\nD\nE");
                click(a, R.string.p4_review);
                assertFalse(button(a, R.string.p4_create_group).isEnabled());
                click(a, R.string.p4_edit);
            });
            scenario.recreate();
            scenario.onActivity(a -> {
                List<TextInputLayout> fields = fields(a.findViewById(R.id.p4_content));
                assertEquals("家族 🌿", fields.get(0).getEditText().getText().toString());
                assertEquals("A\nB\nC\nD\nE", fields.get(1).getEditText().getText().toString());
                click(a, R.string.p4_review);
                click(a, R.string.p4_group_manage);
                assertFalse(button(a, R.string.p4_leave_group).isEnabled());
                click(a, R.string.p4_clear);
                assertEquals("", fields(a.findViewById(R.id.p4_content)).get(0).getEditText().getText().toString());
            });
        }
        assertEquals(ticketCount, FakeData.history().size());
    }
    @Test public void appointmentManagementCannotClaimCancellationOrReschedule() {
        try (ActivityScenario<Phase4Activity> scenario = ActivityScenario.launch(Phase4Activity.intent(ApplicationProvider.getApplicationContext(), Phase4Activity.APPOINTMENTS))) {
            scenario.onActivity(a -> {
                click(a, R.string.p4_upcoming); click(a, R.string.p4_details);
                assertFalse(button(a, R.string.p4_cancel_booking).isEnabled());
                assertFalse(button(a, R.string.p4_reschedule).isEnabled());
            });
            scenario.recreate();
            scenario.onActivity(a -> assertFalse(button(a, R.string.p4_cancel_booking).isEnabled()));
        }
    }
    @Test public void staffRequiresOwnedQueueAndNeverEnablesInvite() {
        try (ActivityScenario<Phase4Activity> denied = ActivityScenario.launch(Phase4Activity.intent(ApplicationProvider.getApplicationContext(), Phase4Activity.STAFF))) {
            assertEquals(Lifecycle.State.DESTROYED, denied.getState());
        }
        Queue queue = FakeData.ownedQueues().get(0);
        try (ActivityScenario<Phase4Activity> scenario = ActivityScenario.launch(Phase4Activity.ownerIntent(ApplicationProvider.getApplicationContext(), Phase4Activity.STAFF, queue.getId()))) {
            scenario.onActivity(a -> {
                click(a, R.string.p4_invite_draft); click(a, R.string.p4_review);
                assertFalse(button(a, R.string.p4_invite).isEnabled());
                click(a, R.string.p4_invitation_status); click(a, R.string.p4_invitation_status);
                assertFalse(button(a, R.string.p4_change_role).isEnabled());
                assertFalse(button(a, R.string.p4_revoke).isEnabled());
            });
        }
    }
    private static void click(Phase4Activity activity, int label) { assertTrue(button(activity, label).performClick()); }
    private static MaterialButton button(Phase4Activity activity, int label) {
        return findButton(activity.findViewById(R.id.p4_content), activity.getString(label));
    }
    private static MaterialButton findButton(View view, String text) {
        if (view instanceof MaterialButton && text.contentEquals(((TextView)view).getText())) return (MaterialButton)view;
        if (view instanceof ViewGroup) for (int i=0;i<((ViewGroup)view).getChildCount();i++) {
            MaterialButton found=findButton(((ViewGroup)view).getChildAt(i),text); if(found!=null)return found;
        }
        return null;
    }
    private static List<TextInputLayout> fields(View view) {
        List<TextInputLayout> result=new ArrayList<>();
        if(view instanceof TextInputLayout) result.add((TextInputLayout)view);
        else if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)result.addAll(fields(((ViewGroup)view).getChildAt(i)));
        return result;
    }
}
