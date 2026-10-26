package com.example.quapp;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static org.junit.Assert.*;

/** Isolated presentation checks. Never invokes sharing, clipboard or a successful save. */
@RunWith(AndroidJUnit4.class)
public class OrganizerFormVisualReviewTest {
    private final Instrumentation ins=InstrumentationRegistry.getInstrumentation();
    private final String worst="👩🏽‍💻 王秀英 — Aleksandra Wiśniewska-Kowalczyk, Community Service Centre";
    private Activity active;

    @Test @SuppressWarnings("unchecked") public void reviewScreens() throws Exception {
        FakeData.queues();
        Map<Field,List<?>> lists=new LinkedHashMap<>();
        for(String n:new String[]{"queues","ownedIds"}){Field f=field(FakeData.class,n);lists.put(f,new ArrayList<>((List<?>)f.get(null)));}
        Field held=field(ActiveTicketStore.class,"held");lists.put(held,new ArrayList<>((List<?>)held.get(null)));
        Map<Field,Object> flags=new LinkedHashMap<>();
        for(String n:new String[]{"debugMode","debugDelayMs","debugFailOnce","debugEmptyOnce","nextQueueNumber"}){Field f=field(FakeData.class,n);flags.put(f,f.get(null));}
        SharedPreferences[] prefs={ins.getTargetContext().getSharedPreferences("quapp_session",0),ins.getTargetContext().getSharedPreferences("quapp_device",0)};
        Map<String,?>[] saved=new Map[]{new LinkedHashMap<>(prefs[0].getAll()),new LinkedHashMap<>(prefs[1].getAll())};
        try {
            FakeData.setDebugMode(false);FakeData.setDebugFailOnce(false);FakeData.setDebugEmptyOnce(false);
            String only=InstrumentationRegistry.getArguments().getString("screen","");
            if(!"share".equals(only))reviewCreate();
            if(!"create".equals(only))reviewShare();
        } finally {
            if(active!=null)close();
            boolean untouched=true; for(Map.Entry<Field,List<?>> e:lists.entrySet())untouched &= e.getValue().equals(e.getKey().get(null));
            ins.runOnMainSync(()->{try{for(Map.Entry<Field,List<?>>e:lists.entrySet()){List l=(List)e.getKey().get(null);l.clear();l.addAll(e.getValue());}}catch(Exception e){throw new AssertionError(e);}});
            for(Map.Entry<Field,Object>e:flags.entrySet())e.getKey().set(null,e.getValue());
            for(int i=0;i<prefs.length;i++){if(!saved[i].equals(prefs[i].getAll()))restore(prefs[i],saved[i]);assertEquals(saved[i],prefs[i].getAll());}
            for(Map.Entry<Field,Object>e:flags.entrySet())assertEquals(e.getValue(),e.getKey().get(null));
            for(Map.Entry<Field,List<?>>e:lists.entrySet())assertEquals(e.getValue(),e.getKey().get(null)); assertTrue("No queue/ticket mutations",untouched);
        }
    }
    @Test public void reviewExpandedCategory() throws Exception {
        SharedPreferences session=ins.getTargetContext().getSharedPreferences("quapp_session",0),device=ins.getTargetContext().getSharedPreferences("quapp_device",0);
        Map<String,?> sessionSaved=new LinkedHashMap<>(session.getAll()),deviceSaved=new LinkedHashMap<>(device.getAll());
        try {
            launch(new Intent(ins.getTargetContext(),CreateQueueActivity.class));
            ins.runOnMainSync(()->active.findViewById(R.id.create_category).performClick());idle();
            androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withId(R.id.category_list)).check((view,error)->{if(error!=null)throw error;View sheet=view.getRootView().findViewById(com.google.android.material.R.id.design_bottom_sheet);com.google.android.material.bottomsheet.BottomSheetBehavior.from(sheet).setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);});
            SystemClock.sleep(450);
            androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withId(R.id.category_list)).check((view,error)->{if(error!=null)throw error;View panel=view;while(panel.getParent() instanceof View){if(panel instanceof NestedScrollView){((NestedScrollView)panel).setSmoothScrollingEnabled(false);((NestedScrollView)panel).fullScroll(View.FOCUS_DOWN);break;}panel=(View)panel.getParent();}});
            idle();capture("create-category-expanded-bottom");
            androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withId(R.id.category_list)).check((view,error)->{if(error!=null)throw error;androidx.recyclerview.widget.RecyclerView list=(androidx.recyclerview.widget.RecyclerView)view;androidx.recyclerview.widget.RecyclerView.ViewHolder last=list.findViewHolderForAdapterPosition(list.getAdapter().getItemCount()-1);assertNotNull(last);View hint=last.itemView.findViewById(R.id.category_hint);Rect visible=new Rect();assertTrue(hint.getGlobalVisibleRect(visible));assertEquals("last category hint fully visible",hint.getHeight(),visible.height());});
        } finally {if(active!=null)close();if(!sessionSaved.equals(session.getAll()))restore(session,sessionSaved);if(!deviceSaved.equals(device.getAll()))restore(device,deviceSaved);assertEquals(sessionSaved,session.getAll());assertEquals(deviceSaved,device.getAll());}
    }
    private void reviewCreate() throws Exception {
        launch(new Intent(ins.getTargetContext(),CreateQueueActivity.class));
        capture("create-default");
        ins.runOnMainSync(()->active.findViewById(R.id.create_category).performClick()); idle(); capture("create-category-sheet");
        androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withId(R.id.category_list)).check((view,error)->{if(error!=null)throw error; androidx.recyclerview.widget.RecyclerView list=(androidx.recyclerview.widget.RecyclerView)view;assertTrue(list.getAdapter().getItemCount()>0);View panel=view;while(panel.getParent() instanceof View){if(panel instanceof NestedScrollView){((NestedScrollView)panel).fullScroll(View.FOCUS_DOWN);break;}panel=(View)panel.getParent();}});
        SystemClock.sleep(350);capture("create-category-sheet-bottom");ins.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK);idle();
        ins.runOnMainSync(()->active.findViewById(R.id.create_submit_button).performClick());idle();
        for(int id:new int[]{R.id.create_name_layout,R.id.create_venue_layout,R.id.create_short_layout}) assertEquals(View.GONE,active.findViewById(id).getVisibility());
        reveal(active.findViewById(R.id.create_pick_error));assertTrue(active.findViewById(R.id.create_pick_error).isShown());capture("create-picker-error");
        int[] fields={R.id.create_details_input,R.id.create_bring_input};
        for(int id:fields){
            EditText input=active.findViewById(id);
            ins.runOnMainSync(()->{input.setText(worst);input.requestFocus();((InputMethodManager)active.getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(input,InputMethodManager.SHOW_IMPLICIT);});
            SystemClock.sleep(650);dismissImeTip();reveal(input);
            assertTrue("docked keyboard",ViewCompat.getRootWindowInsets(active.findViewById(R.id.create_root)).isVisible(WindowInsetsCompat.Type.ime()));
            capture("create-keyboard-before-assert-"+active.getResources().getResourceEntryName(id)); Rect r=new Rect();assertTrue(input.getGlobalVisibleRect(r));
            View submit=active.findViewById(R.id.create_submit_button);assertTrue(submit.getGlobalVisibleRect(new Rect()));
            capture("create-keyboard-"+active.getResources().getResourceEntryName(id));
            hideKeyboard();
        }
        ins.runOnMainSync(()->active.findViewById(R.id.create_multiple_days).performClick());
        for(int id:new int[]{R.id.create_municipality,R.id.create_category,R.id.create_start_date,R.id.create_end_date,R.id.create_opens,R.id.create_closes}){
            TextView v=active.findViewById(id);ins.runOnMainSync(()->v.setText(worst));reveal(v);capture("create-picker-long-"+active.getResources().getResourceEntryName(id));
        }
        ins.runOnMainSync(()->((com.google.android.material.materialswitch.MaterialSwitch)active.findViewById(R.id.create_toggle_proximity).findViewById(R.id.toggle_switch)).setChecked(true));
        reveal(active.findViewById(R.id.create_toggle_otp));capture("create-planned-toggle");
        reveal(active.findViewById(R.id.create_radius_group));capture("create-radius");
        // Real schedule validator with deliberately reversed in-memory choices, never saved.
        ins.runOnMainSync(()->{try{field(CreateQueueActivity.class,"startDate").set(active,java.time.LocalDate.of(2030,1,3));field(CreateQueueActivity.class,"endDate").set(active,java.time.LocalDate.of(2030,1,2));active.findViewById(R.id.create_submit_button).performClick();}catch(Exception e){throw new AssertionError(e);}});
        reveal(active.findViewById(R.id.create_schedule_error));assertTrue(active.findViewById(R.id.create_schedule_error).isShown());capture("create-schedule-error");
        ins.runOnMainSync(()->{try{field(CreateQueueActivity.class,"endDate").set(active,java.time.LocalDate.of(2030,1,3));field(CreateQueueActivity.class,"opensAt").set(active,java.time.LocalTime.of(17,0));field(CreateQueueActivity.class,"closesAt").set(active,java.time.LocalTime.of(8,0));active.findViewById(R.id.create_submit_button).performClick();}catch(Exception e){throw new AssertionError(e);}});reveal(active.findViewById(R.id.create_schedule_error));capture("create-time-error");
        close();
        // Edit an existing fixture; fail BEFORE supplier execution. No queue is created or edited.
        Queue q=FakeData.queues().get(0);
        launch(new Intent(ins.getTargetContext(),CreateQueueActivity.class).putExtra(CreateQueueActivity.EXTRA_QUEUE_ID,q.getId()));
        FakeData.setDebugMode(true);FakeData.setDebugDelayMs(1400);FakeData.setDebugFailOnce(true);
        ins.runOnMainSync(()->active.findViewById(R.id.create_submit_button).performClick());idle();
        assertFalse(active.findViewById(R.id.create_submit_button).isEnabled());capture("create-save-loading");
        SystemClock.sleep(1550);idle();assertTrue(active.findViewById(R.id.create_submit_button).isEnabled());
        capture("create-save-error");close();
        FakeData.setDebugMode(false);FakeData.setDebugFailOnce(false);
    }
    private void reviewShare() throws Exception {
        Queue q=FakeData.queues().get(0);
        launch(ShareQueueActivity.intent(ins.getTargetContext(),q.getId()));
        capture("share-default");
        TextView name=active.findViewById(R.id.share_queue_name), link=active.findViewById(R.id.share_link);
        ins.runOnMainSync(()->{name.setText(worst);link.setText("https://quapp.app/q/community-health-screening-and-local-services-registration-aleksandra-wisniewska-kowalczyk");});
        reveal(name);capture("share-unicode-name");reveal(link);capture("share-long-link");
        for(int id:new int[]{R.id.share_copy,R.id.share_send,R.id.share_counter}){View action=active.findViewById(id);reveal(action);assertTrue(action.getGlobalVisibleRect(new Rect()));capture("share-action-"+active.getResources().getResourceEntryName(id));}
        // Exercise the older-API feedback layout without invoking clipboard or sharing.
        reveal(active.findViewById(R.id.share_copy));
        final com.google.android.material.snackbar.Snackbar[] feedback=new com.google.android.material.snackbar.Snackbar[1];
        try {
            ins.runOnMainSync(()->{feedback[0]=com.google.android.material.snackbar.Snackbar.make(active.findViewById(R.id.share_root),R.string.share_copied,com.google.android.material.snackbar.Snackbar.LENGTH_INDEFINITE).setAnchorView(R.id.share_dock);feedback[0].show();});SystemClock.sleep(400);idle();
            capture("share-snackbar-anchor");assertTrue("anchored Snackbar visible",feedback[0].getView().getGlobalVisibleRect(new Rect()));
        } finally {if(feedback[0]!=null){ins.runOnMainSync(()->feedback[0].dismiss());idle();}}
        ins.runOnMainSync(()->{name.setText("");link.setText("");});reveal(link);capture("share-missing-fields");close();
        // Native missing queue state finishes. No fabricated loading/error UI.
        launch(ShareQueueActivity.intent(ins.getTargetContext(),"missing-review-fixture"));
        assertTrue(active.isFinishing()||active.isDestroyed());close();
    }
    private static Field field(Class<?> c,String n)throws Exception{Field f=c.getDeclaredField(n);f.setAccessible(true);return f;}
    private void launch(Intent intent){active=ins.startActivitySync(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));idle();}
    private void close(){Activity a=active;ins.runOnMainSync(a::finish);idle();active=null;}
    private void idle(){ins.waitForIdleSync();SystemClock.sleep(150);}
    private void reveal(View v){ins.runOnMainSync(()->v.requestRectangleOnScreen(new Rect(0,0,v.getWidth(),v.getHeight()),true));idle();}
    private void dismissImeTip(){
        android.app.UiAutomation ui=ins.getUiAutomation();android.accessibilityservice.AccessibilityServiceInfo info=ui.getServiceInfo();int flags=info.flags;
        try {
            info.flags|=android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;ui.setServiceInfo(info);
            for(android.view.accessibility.AccessibilityWindowInfo window:ui.getWindows()){
                android.view.accessibility.AccessibilityNodeInfo root=window.getRoot();if(root==null)continue;
                for(android.view.accessibility.AccessibilityNodeInfo node:root.findAccessibilityNodeInfosByText("OK"))
                    if(String.valueOf(node.getPackageName()).contains("inputmethod")&&node.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK)){SystemClock.sleep(200);return;}
            }
        } finally {info.flags=flags;ui.setServiceInfo(info);}
    }
    private void hideKeyboard(){ins.runOnMainSync(()->((InputMethodManager)active.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(active.getWindow().getDecorView().getWindowToken(),0));SystemClock.sleep(250);idle();}
    private void capture(String state)throws Exception{
        idle();Bitmap image=ins.getUiAutomation().takeScreenshot();assertNotNull(image);
        File folder=new File(ins.getTargetContext().getExternalFilesDir(null),"phase3b-review");assertTrue(folder.isDirectory()||folder.mkdirs());
        android.content.res.Configuration c=active.getResources().getConfiguration();
        try(FileOutputStream out=new FileOutputStream(new File(folder,state+"-"+c.screenWidthDp+"x"+c.screenHeightDp+"-"+c.fontScale+".png"))){assertTrue(image.compress(Bitmap.CompressFormat.PNG,100,out));}finally{image.recycle();}
    }
    private void restore(SharedPreferences p,Map<String,?>values){SharedPreferences.Editor e=p.edit().clear();for(Map.Entry<String,?>v:values.entrySet()){Object x=v.getValue();if(x instanceof String)e.putString(v.getKey(),(String)x);else if(x instanceof Boolean)e.putBoolean(v.getKey(),(Boolean)x);else if(x instanceof Integer)e.putInt(v.getKey(),(Integer)x);else if(x instanceof Long)e.putLong(v.getKey(),(Long)x);else if(x instanceof Float)e.putFloat(v.getKey(),(Float)x);else if(x instanceof Set)e.putStringSet(v.getKey(),(Set<String>)x);else throw new AssertionError(x);}assertTrue(e.commit());}
}
