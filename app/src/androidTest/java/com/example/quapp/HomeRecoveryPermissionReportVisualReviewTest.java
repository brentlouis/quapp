package com.example.quapp;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.widget.NestedScrollView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
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
import static org.junit.Assert.*;

/** Non-shipped fixtures. Never invoke email, real permissions, notifications or report submission. */
@RunWith(AndroidJUnit4.class)
public class HomeRecoveryPermissionReportVisualReviewTest {
    private final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
    private final String worst = "👩🏽‍💻 王秀英 — Aleksandra Wiśniewska-Kowalczyk";

    @Test @SuppressWarnings("unchecked") public void reviewScreens() throws Exception {
        Field heldField = ActiveTicketStore.class.getDeclaredField("held"); heldField.setAccessible(true);
        List<Ticket> held = (List<Ticket>) heldField.get(null); List<Ticket> original = new ArrayList<>(held);
        Map<Field,Object> debug = new LinkedHashMap<>();
        for (String name : new String[]{"debugMode","debugDelayMs","debugFailOnce","debugEmptyOnce"}) {
            Field f = FakeData.class.getDeclaredField(name); f.setAccessible(true); debug.put(f,f.get(null));
        }
        SharedPreferences prefs = instrumentation.getTargetContext().getSharedPreferences("quapp_session", Context.MODE_PRIVATE);
        boolean hadTown = prefs.contains("town"), hadAsked = prefs.contains("notifications_asked");
        String town = prefs.getString("town", null); boolean asked = prefs.getBoolean("notifications_asked", false);
        Queue queue = FakeData.queues().get(0);
        try {
            instrumentation.runOnMainSync(held::clear); prefs.edit().remove("town").commit();
            FakeData.setDebugMode(false); FakeData.setDebugFailOnce(false); FakeData.setDebugEmptyOnce(false);
            for (int screen=0;screen<4;screen++) {
                if(screen==2) instrumentation.runOnMainSync(()->held.add(new Ticket.Builder().setId("phase2e-notify-fixture")
                    .setQueue(queue.getId(),"Fictional demo service","Demo entrance").setHolder("Fictional participant",null)
                    .setTicketNumber(64).setStatus(Ticket.Status.WAITING).build()));
                Activity activity = launch(screen,queue.getId());
                try {
                    assertEquals(screen==2?1:0,held.size());
                    capture(activity,screen,"default");
                    TextView target = screen==0 ? activity.findViewById(R.id.browse_greeting)
                        : screen==1 ? text(activity.findViewById(android.R.id.content),activity.getString(R.string.forgot_title))
                        : screen==2 ? activity.findViewById(R.id.notify_preview_title) : activity.findViewById(R.id.report_queue);
                    assertNotNull(target); instrumentation.runOnMainSync(()->target.setText(worst));
                    capture(activity,screen,"unicode-long");
                    if(screen==0) { swipeUp(activity); capture(activity,screen,"collapsed-list"); }
                    else scroll(activity,true);
                    capture(activity,screen,"bottom");
                    TextView missing = screen==0 ? activity.findViewById(R.id.browse_greeting)
                        : screen==1 ? text(activity.findViewById(android.R.id.content),activity.getString(R.string.forgot_body))
                        : screen==2 ? activity.findViewById(R.id.notify_body) : activity.findViewById(R.id.report_details_input);
                    assertNotNull(missing); instrumentation.runOnMainSync(()->missing.setText(""));
                    if(screen!=0) scroll(activity,false);
                    else instrumentation.runOnMainSync(()->((com.google.android.material.appbar.AppBarLayout)activity.findViewById(R.id.browse_app_bar)).setExpanded(true,false));
                    capture(activity,screen,"missing-secondary");
                    if(screen==3) {
                        instrumentation.runOnMainSync(()->{
                            LinearLayout reasons=activity.findViewById(R.id.report_reasons);
                            reasons.getChildAt(Report.Reason.OTHER.ordinal()).performClick();
                            ((EditText)activity.findViewById(R.id.report_details_input)).setText("");
                            activity.findViewById(R.id.report_send).performClick();
                            TextInputLayout field=activity.findViewById(R.id.report_details_layout);
                            assertNotNull("OTHER needs details; cannot submit",field.getError());
                            field.requestRectangleOnScreen(new Rect(0,0,field.getWidth(),field.getHeight()),true);
                        });
                        capture(activity,screen,"error-helper-manual-reveal");
                        scroll(activity,true);capture(activity,screen,"error-helper-bottom");
                        instrumentation.runOnMainSync(()->{
                            EditText field=activity.findViewById(R.id.report_details_input); field.setText(worst);field.requestFocus();
                            ((InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(field,InputMethodManager.SHOW_IMPLICIT);
                        });
                        Thread.sleep(600); capture(activity,screen,"keyboard");
                        instrumentation.runOnMainSync(()->((InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE))
                            .hideSoftInputFromWindow(activity.getWindow().getDecorView().getWindowToken(),0));
                    }
                } finally { instrumentation.runOnMainSync(()->{activity.finish();held.clear();}); }
            }
            for(int state=0;state<2;state++) {
                FakeData.setDebugMode(true);FakeData.setDebugDelayMs(1200);FakeData.setDebugFailOnce(state==1);FakeData.setDebugEmptyOnce(state==0);
                Activity home=launch(0,queue.getId());
                try {
                    assertTrue(home.findViewById(R.id.browse_loading).isShown());capture(home,0,"loading-"+state);
                    Thread.sleep(1400);instrumentation.waitForIdleSync();
                    if(state==0) {
                        androidx.recyclerview.widget.RecyclerView list=home.findViewById(R.id.browse_list);
                        assertEquals("Actual first-visit zero-results adapter",0,list.getAdapter().getItemCount());
                    } else assertTrue(home.findViewById(R.id.browse_error).isShown());
                    swipeUp(home);
                    if(state==1) {
                        NestedScrollView errorScroll=findScroll(home.findViewById(R.id.browse_error));
                        if(errorScroll!=null)instrumentation.runOnMainSync(()->errorScroll.fullScroll(View.FOCUS_DOWN));
                    }
                    capture(home,0,state==0?"empty-first-visit":"error");
                } finally { instrumentation.runOnMainSync(home::finish); }
            }
        } finally {
            instrumentation.runOnMainSync(()->{held.clear();held.addAll(original);});
            for(Map.Entry<Field,Object> e:debug.entrySet())e.getKey().set(null,e.getValue());
            SharedPreferences.Editor editor=prefs.edit();if(hadTown)editor.putString("town",town);else editor.remove("town");
            if(hadAsked)editor.putBoolean("notifications_asked",asked);else editor.remove("notifications_asked");assertTrue(editor.commit());
            assertEquals(original,held);for(Map.Entry<Field,Object>e:debug.entrySet())assertEquals(e.getValue(),e.getKey().get(null));
            assertEquals(hadTown,prefs.contains("town"));assertEquals(town,prefs.getString("town",null));
            assertEquals(hadAsked,prefs.contains("notifications_asked"));assertEquals(asked,prefs.getBoolean("notifications_asked",false));
        }
    }
    @Test public void reviewReportKeyboardOnly() throws Exception {
        Activity activity=launch(3,FakeData.queues().get(0).getId());
        try {
            instrumentation.runOnMainSync(()->{
                EditText field=activity.findViewById(R.id.report_details_input);field.setText(worst);field.requestFocus();
                ((InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(field,InputMethodManager.SHOW_IMPLICIT);
            });
            Thread.sleep(800);
            android.accessibilityservice.AccessibilityServiceInfo info=instrumentation.getUiAutomation().getServiceInfo();
            int originalFlags=info.flags;
            try {
                info.flags |= android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
                instrumentation.getUiAutomation().setServiceInfo(info);
                for(android.view.accessibility.AccessibilityWindowInfo window:instrumentation.getUiAutomation().getWindows()) {
                    if(window.getType()!=android.view.accessibility.AccessibilityWindowInfo.TYPE_INPUT_METHOD)continue;
                    android.view.accessibility.AccessibilityNodeInfo root=window.getRoot();
                    if(root==null)continue;
                    for(android.view.accessibility.AccessibilityNodeInfo node:root.findAccessibilityNodeInfosByText("OK")) {
                        if("OK".contentEquals(node.getText()==null?"":node.getText()))node.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK);
                    }
                }
            } finally { info.flags=originalFlags;instrumentation.getUiAutomation().setServiceInfo(info); }
            Thread.sleep(500);capture(activity,3,"keyboard-banner-dismissed");
        } finally {
            instrumentation.runOnMainSync(()->{
                ((InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(activity.getWindow().getDecorView().getWindowToken(),0);
                activity.finish();
            });
        }
    }
    private Activity launch(int screen,String queueId) {
        Intent intent=screen==0?QueuerHomeActivity.intent(instrumentation.getTargetContext(),QueuerHomeActivity.TAB_BROWSE)
            :screen==1?new Intent(instrumentation.getTargetContext(),ForgotPasswordActivity.class)
            :screen==2?NotificationPermissionActivity.intent(instrumentation.getTargetContext(),"phase2e-notify-fixture")
            :ReportQueueActivity.intent(instrumentation.getTargetContext(),queueId);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);return instrumentation.startActivitySync(intent);
    }
    private TextView text(View view,String value) {
        if(view instanceof TextView && value.contentEquals(((TextView)view).getText()))return(TextView)view;
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){
            TextView found=text(((ViewGroup)view).getChildAt(i),value);if(found!=null)return found;
        }return null;
    }
    private NestedScrollView findScroll(View view) {
        if(view instanceof NestedScrollView)return(NestedScrollView)view;
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){
            NestedScrollView found=findScroll(((ViewGroup)view).getChildAt(i));if(found!=null)return found;
        }return null;
    }
    private void scroll(Activity activity,boolean down) {
        NestedScrollView view=findScroll(activity.findViewById(android.R.id.content));
        if(view!=null)instrumentation.runOnMainSync(()->view.fullScroll(down?View.FOCUS_DOWN:View.FOCUS_UP));
        instrumentation.waitForIdleSync();
    }
    private void swipeUp(Activity activity) {
        int w=activity.getResources().getDisplayMetrics().widthPixels,h=activity.getResources().getDisplayMetrics().heightPixels;
        long start=SystemClock.uptimeMillis();
        for(int i=0;i<=12;i++){
            MotionEvent e=MotionEvent.obtain(start,SystemClock.uptimeMillis(),i==0?MotionEvent.ACTION_DOWN:i==12?MotionEvent.ACTION_UP:MotionEvent.ACTION_MOVE,w*.65f,h*(.68f-.4f*i/12),0);
            instrumentation.sendPointerSync(e);e.recycle();SystemClock.sleep(20);
        }instrumentation.waitForIdleSync();
    }
    private void capture(Activity activity,int screen,String state)throws Exception {
        Thread.sleep(180);File folder=new File(activity.getExternalFilesDir(null),"phase2e-review");assertTrue(folder.isDirectory()||folder.mkdirs());
        Bitmap bitmap=instrumentation.getUiAutomation().takeScreenshot();assertNotNull(bitmap);
        android.content.res.Configuration c=activity.getResources().getConfiguration();String name=new String[]{"home","password-recovery","notification-permission","report"}[screen];
        try(FileOutputStream out=new FileOutputStream(new File(folder,name+"-"+state+"-"+c.screenWidthDp+"x"+c.screenHeightDp+"-"+c.fontScale+".png"))){assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,out));}
        finally{bitmap.recycle();}
    }
}
