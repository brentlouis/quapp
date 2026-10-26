package com.example.quapp;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.SystemClock;
import android.widget.TextView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;
import com.google.android.material.materialswitch.MaterialSwitch;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Configuration recreation must not merge the reused toggle rows' independent choices. */
@RunWith(AndroidJUnit4.class)
public class CreateFormStateTest {
    private final Instrumentation ins = InstrumentationRegistry.getInstrumentation();
    private Activity form;

    @Test public void keepsIndependentVerificationOptionsAcrossRecreation() {
        try {
            form = ins.startActivitySync(new Intent(ins.getTargetContext(), CreateQueueActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            for (boolean proximity : new boolean[]{false, true}) {
                ins.runOnMainSync(() -> {
                    ((TextView) form.findViewById(R.id.create_name_input)).setText("Unsaved form 王秀英");
                    toggle(R.id.create_toggle_grace).setChecked(!proximity);
                    toggle(R.id.create_toggle_penalty).setChecked(proximity);
                    toggle(R.id.create_toggle_proximity).setChecked(proximity);
                });
                Activity previous = form;
                ins.runOnMainSync(previous::recreate);
                long deadline = SystemClock.uptimeMillis() + 5000;
                do {
                    ins.waitForIdleSync();
                    ins.runOnMainSync(() -> {
                        for (Activity current : ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED))
                            if (current instanceof CreateQueueActivity && current != previous) form = current;
                    });
                    if (form != previous) break;
                    SystemClock.sleep(50);
                } while (SystemClock.uptimeMillis() < deadline);
                assertNotSame("Form was recreated", previous, form);
                ins.runOnMainSync(() -> {
                    assertEquals("Grace choice survives", !proximity, toggle(R.id.create_toggle_grace).isChecked());
                    assertEquals("Cooldown choice survives", proximity, toggle(R.id.create_toggle_penalty).isChecked());
                    assertEquals("Proximity choice survives", proximity, toggle(R.id.create_toggle_proximity).isChecked());
                    assertFalse("Planned SMS stays off", toggle(R.id.create_toggle_otp).isChecked());
                    assertFalse(toggle(R.id.create_toggle_otp).isEnabled());
                    assertEquals("Unsaved form 王秀英", ((TextView) form.findViewById(R.id.create_name_input)).getText().toString());
                });
            }
        } finally {
            if (form != null) ins.runOnMainSync(() -> form.finish());
            ins.waitForIdleSync();
        }
    }

    private MaterialSwitch toggle(int row) { return form.findViewById(row).findViewById(R.id.toggle_switch); }
}
