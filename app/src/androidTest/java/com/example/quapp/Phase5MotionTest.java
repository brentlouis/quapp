package com.example.quapp;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.view.Choreographer;
import android.view.View;
import android.widget.EditText;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;

/** Real resources and frame samples; no persistent preferences or backend mutations. */
@RunWith(AndroidJUnit4.class)
public class Phase5MotionTest {
    private final android.app.Instrumentation ins = InstrumentationRegistry.getInstrumentation();

    @Test public void pressFramesAndImmediateActions() throws Exception {
        try (ActivityScenario<LoginActivity> s = ActivityScenario.launch(LoginActivity.class)) {
            MaterialButton[] button = new MaterialButton[1];
            int[] clicks = {0};
            s.onActivity(a -> {
                button[0] = a.findViewById(R.id.login_submit_button);
                assertEquals(InstrumentationRegistry.getArguments().getString("motion", "true"),
                        Boolean.toString(QuappMotion.enabled(a)));
                button[0].setOnClickListener(v -> clicks[0]++);
                button[0].setEnabled(true);
                assertNull("Native ripple needs no extra scale animator", button[0].getStateListAnimator());
                button[0].performClick(); button[0].performClick();
                assertEquals("Actions are never queued behind motion", 2, clicks[0]);
                button[0].setPressed(true);
            });
            sample(button[0], "press", true);
            s.onActivity(a -> button[0].setPressed(false));
            sample(button[0], "release", true);
            s.onActivity(a -> assertEquals(1f, button[0].getScaleX(), .001f));
            capture("login");
        }
    }

    @Test public void statusInterruptionPreservesInputAndContent() throws Exception {
        try (ActivityScenario<LoginActivity> s = ActivityScenario.launch(LoginActivity.class)) {
            View[] root = new View[1]; LoadingStateHelper[] helper = new LoadingStateHelper[1];
            s.onActivity(a -> {
                root[0] = a.getLayoutInflater().inflate(R.layout.fragment_browse, null);
                a.setContentView(root[0]); SystemBars.applyPadding(root[0]);
                EditText input = root[0].findViewById(R.id.browse_search_input);
                input.setText("Wiśniewska 👩🏽‍💻 日本語"); input.requestFocus();
                helper[0] = new LoadingStateHelper(root[0].findViewById(R.id.browse_list),
                        root[0].findViewById(R.id.browse_empty), root[0].findViewById(R.id.browse_loading),
                        root[0].findViewById(R.id.browse_error));
                helper[0].showContent();
            });
            ins.waitForIdleSync();
            s.onActivity(a -> helper[0].showError("Connection unavailable — retry when ready"));
            sample(root[0].findViewById(R.id.browse_error), "error", false);
            int[] scroll = {0};
            s.onActivity(a -> ((androidx.core.widget.NestedScrollView)root[0].findViewById(R.id.browse_error))
                    .fullScroll(View.FOCUS_DOWN));
            ins.waitForIdleSync();
            s.onActivity(a -> {
                scroll[0]=root[0].findViewById(R.id.browse_error).getScrollY();
                helper[0].showLoading("Checking"); helper[0].showEmpty();
                helper[0].showError("Connection unavailable — retry when ready");
                helper[0].showContent();
                for (int id : new int[]{R.id.browse_loading,R.id.browse_empty,R.id.browse_error}) {
                    assertEquals(View.GONE, root[0].findViewById(id).getVisibility());
                    assertEquals(1f, root[0].findViewById(id).getAlpha(), 0f);
                }
                assertEquals(1f, root[0].findViewById(R.id.browse_list).getAlpha(), 0f);
                EditText input = root[0].findViewById(R.id.browse_search_input);
                assertEquals("Wiśniewska 👩🏽‍💻 日本語", input.getText().toString());
                assertTrue(input.hasFocus());
            });
            ins.waitForIdleSync();
            s.onActivity(a -> assertEquals("State changes preserve the panel scroll offset",scroll[0],
                    root[0].findViewById(R.id.browse_error).getScrollY()));
        }
    }

    @Test public void appReducedGuardAndSheetDismissalPreserveDraft() throws Exception {
        try (ActivityScenario<LoginActivity> s = ActivityScenario.launch(LoginActivity.class)) {
            BottomSheetDialog[] sheet = new BottomSheetDialog[1];
            s.onActivity(a -> {
                Context reduced = reduced(a);
                assertFalse(QuappMotion.enabled(reduced));
                MaterialButton b = new MaterialButton(reduced);
                QuappMotion.buttons(b); assertNull(b.getStateListAnimator());
                androidx.recyclerview.widget.RecyclerView list = new androidx.recyclerview.widget.RecyclerView(reduced);
                QuappMotion.list(list); assertNull(list.getItemAnimator());
                android.widget.TextView panel=new android.widget.TextView(reduced);
                android.view.ViewGroup host=a.findViewById(android.R.id.content);
                host.addView(panel);
                LoadingStateHelper reducedStates=new LoadingStateHelper(null,null,panel,null);
                reducedStates.showContent(); reducedStates.showLoading("Checking");
                assertEquals(1f,panel.getAlpha(),0f); host.removeView(panel);
                com.google.android.material.snackbar.Snackbar bar = QuappMotion.snackbar(a.findViewById(R.id.login_root), "Offline", -1);
                assertEquals(QuappMotion.enabled(a) ? 150 : 0,
                        com.google.android.material.motion.MotionUtils.resolveThemeDuration(bar.getContext(),
                                com.google.android.material.R.attr.motionDurationLong2,-1));
                EditText input = a.findViewById(R.id.login_phone_input);
                input.setText("09123456789"); input.requestFocus();
                sheet[0] = new BottomSheetDialog(a);
                Grain.attach(sheet[0]);
                View help=a.getLayoutInflater().inflate(R.layout.sheet_help,null);
                ((android.widget.TextView)help.findViewById(R.id.help_sheet_title)).setText(R.string.help_queues_title);
                ((android.widget.TextView)help.findViewById(R.id.help_sheet_body)).setText(R.string.help_queues_body);
                sheet[0].setContentView(help);
                sheet[0].show();
                assertFalse(sheet[0].getDismissWithAnimation());
                assertEquals(QuappMotion.enabled(a) ? R.style.Animation_Quapp_Sheet : R.style.Animation_Quapp_None,
                        sheet[0].getWindow().getAttributes().windowAnimations);
            });
            ins.waitForIdleSync();
            // Capture compositor output across the real window transition, including timing.
            long start=SystemClock.uptimeMillis();
            StringBuilder captureTimes=new StringBuilder("frame,elapsed_ms\n");
            for(int frame=0;frame<8;frame++) {
                captureTimes.append(frame).append(',').append(SystemClock.uptimeMillis()-start).append('\n');
                capture("sheet-frame-"+frame);
            }
            try(FileWriter out=new FileWriter(new File(folder(),"sheet-capture-times.csv"))){out.write(captureTimes.toString());}
            capture("sheet");
            s.onActivity(a -> sheet[0].onBackPressed());
            ins.waitForIdleSync();
            s.onActivity(a -> {
                assertFalse(sheet[0].isShowing());
                EditText input = a.findViewById(R.id.login_phone_input);
                assertEquals("09123456789",input.getText().toString()); assertTrue(input.hasFocus());
                BottomSheetDialog reducedSheet = new BottomSheetDialog(reduced(a));
                QuappMotion.sheet(reducedSheet);
                reducedSheet.setContentView(new android.widget.TextView(reduced(a)));
                reducedSheet.show();
                assertEquals(R.style.Animation_Quapp_None,reducedSheet.getWindow().getAttributes().windowAnimations);
                reducedSheet.dismiss();
                android.content.res.TypedArray none=a.obtainStyledAttributes(R.style.Animation_Quapp_None,
                        new int[]{android.R.attr.windowEnterAnimation,android.R.attr.windowExitAnimation});
                assertEquals(0,none.getResourceId(0,0)); assertEquals(0,none.getResourceId(1,0)); none.recycle();
            });
        }
    }

    private Context reduced(Context context) {
        Resources original=context.getResources();
        Resources resources=new Resources(original.getAssets(),original.getDisplayMetrics(),original.getConfiguration()) {
            @Override public boolean getBoolean(int id) {
                return id == R.bool.config_prefersReducedMotion || super.getBoolean(id);
            }
        };
        return new ContextWrapper(context) { @Override public Resources getResources(){ return resources; } };
    }

    private File folder() {
        File f=new File(ins.getTargetContext().getExternalFilesDir(null), "phase5-motion/"
                + InstrumentationRegistry.getArguments().getString("config","normal"));
        assertTrue(f.isDirectory() || f.mkdirs()); return f;
    }

    private void sample(View view,String name,boolean scale) throws Exception {
        CountDownLatch done=new CountDownLatch(1); StringBuilder frames=new StringBuilder("elapsed_ms,value\n");
        ins.runOnMainSync(() -> {
            long start=SystemClock.uptimeMillis();
            Choreographer.getInstance().postFrameCallback(new Choreographer.FrameCallback() {
                @Override public void doFrame(long time) {
                    long elapsed=SystemClock.uptimeMillis()-start;
                    frames.append(elapsed).append(',').append(scale?view.getScaleX():view.getAlpha()).append('\n');
                    if(elapsed<350) Choreographer.getInstance().postFrameCallback(this); else done.countDown();
                }
            });
        });
        assertTrue("Frame sampler completed",done.await(3,TimeUnit.SECONDS));
        try(FileWriter out=new FileWriter(new File(folder(),name+"-frames.csv"))){out.write(frames.toString());}
        assertTrue("Frame evidence contains samples",frames.toString().split("\n").length>3);
    }

    private void capture(String name) throws Exception {
        Bitmap b=ins.getUiAutomation().takeScreenshot(); assertNotNull(b);
        try(FileOutputStream out=new FileOutputStream(new File(folder(),name+".png"))){b.compress(Bitmap.CompressFormat.PNG,100,out);}
        b.recycle();
    }
}
