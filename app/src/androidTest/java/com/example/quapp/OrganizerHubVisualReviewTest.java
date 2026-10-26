package com.example.quapp;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.view.View;
import android.view.MotionEvent;
import android.os.SystemClock;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.core.widget.NestedScrollView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.android.material.bottomnavigation.BottomNavigationView;
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

/** Non-shipped organizer presentation fixtures; no queue actions or portrait changes. */
@RunWith(AndroidJUnit4.class)
public class OrganizerHubVisualReviewTest {
    private final Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
    private final String worst="👩🏽‍💻 王秀英 — Aleksandra Wiśniewska-Kowalczyk";

    @Test @SuppressWarnings("unchecked") public void reviewHubScreens()throws Exception {
        FakeData.queues();
        Map<Field,List<?>> lists=new LinkedHashMap<>();
        for(String name:new String[]{"queues","ownedIds"}){Field f=FakeData.class.getDeclaredField(name);f.setAccessible(true);lists.put(f,new ArrayList<>((List<?>)f.get(null)));}
        Field held=ActiveTicketStore.class.getDeclaredField("held");held.setAccessible(true);lists.put(held,new ArrayList<>((List<?>)held.get(null)));
        Map<Field,Object> debug=new LinkedHashMap<>();
        for(String name:new String[]{"debugMode","debugDelayMs","debugFailOnce","debugEmptyOnce"}){Field f=FakeData.class.getDeclaredField(name);f.setAccessible(true);debug.put(f,f.get(null));}
        SharedPreferences[] prefs={instrumentation.getTargetContext().getSharedPreferences("quapp_session",Context.MODE_PRIVATE),instrumentation.getTargetContext().getSharedPreferences("quapp_device",Context.MODE_PRIVATE)};
        Map<String,?>[] saved=new Map[]{new LinkedHashMap<>(prefs[0].getAll()),new LinkedHashMap<>(prefs[1].getAll())};
        try {
            for(int screen=0;screen<2;screen++) {
                Activity activity=launch(screen,0);
                try {
                    capture(activity,screen,"default");
                    int heading=screen==0?R.id.today_subtitle:R.id.owner_greeting;
                    instrumentation.runOnMainSync(()->((TextView)activity.findViewById(heading)).setText(worst));
                    capture(activity,screen,"organizer-unicode");
                    instrumentation.runOnMainSync(()->{
                        if(screenFor(activity)==0){
                            ((TextView)activity.findViewById(R.id.today_served)).setText("999999999");
                            ((TextView)activity.findViewById(R.id.today_waiting)).setText("0");
                            ((TextView)activity.findViewById(R.id.today_no_show_rate)).setText("0%");
                            TextView row=activity.findViewById(R.id.today_row_name);assertNotNull(row);row.setText(worst);
                            ((TextView)activity.findViewById(R.id.today_row_served)).setText("999999999");
                        }else{
                            TextView name=activity.findViewById(R.id.owner_live_name);name.setText(worst);
                            ((TextView)activity.findViewById(R.id.owner_live_number)).setText("#999999999");
                            ((TextView)activity.findViewById(R.id.owner_live_detail)).setText("0 waiting · 999999999 served");
                        }
                    });
                    capture(activity,screen,"large-zero-counts");
                    if(screen==1){
                        swipeUp(activity);swipeUp(activity);swipeUp(activity);
                        instrumentation.runOnMainSync(()->{TextView card=activity.findViewById(R.id.card_name);assertNotNull(card);card.setText(worst);});
                    }else scroll(activity,true);
                    capture(activity,screen,"queue-unicode-bottom");
                    if(screen==0){
                        instrumentation.runOnMainSync(()->{View name=activity.findViewById(R.id.today_row_name);name.requestRectangleOnScreen(new android.graphics.Rect(0,0,name.getWidth(),name.getHeight()),true);});
                        capture(activity,screen,"queue-unicode-row");
                    }
                    instrumentation.runOnMainSync(()->{
                        TextView missing=activity.findViewById(screenFor(activity)==0?R.id.today_row_detail:R.id.owner_live_detail);assertNotNull(missing);missing.setText("");
                    });
                    if(screen==0)instrumentation.runOnMainSync(()->{View name=activity.findViewById(R.id.today_row_name);name.requestRectangleOnScreen(new android.graphics.Rect(0,0,name.getWidth(),name.getHeight()),true);});
                    if(screen==1)instrumentation.runOnMainSync(()->((com.google.android.material.appbar.AppBarLayout)activity.findViewById(R.id.owner_app_bar)).setExpanded(true,false));
                    capture(activity,screen,"missing-secondary");
                }finally{instrumentation.runOnMainSync(activity::finish);instrumentation.waitForIdleSync();}
                for(int mode=1;mode<=2;mode++) {
                    Activity state=launch(screen,mode);
                    try {
                        assertTrue(state.findViewById(screen==0?R.id.today_loading:R.id.owner_loading).isShown());capture(state,screen,"loading-"+mode);
                        Thread.sleep(1450);instrumentation.waitForIdleSync();
                        assertTrue(state.findViewById(screen==0?(mode==1?R.id.today_empty:R.id.today_error):(mode==1?R.id.owner_first_visit:R.id.owner_error)).isShown());
                        if(screen==1) {
                            if(mode==2) assertEquals(View.GONE,state.findViewById(R.id.owner_filters).getVisibility());
                            instrumentation.runOnMainSync(()->((com.google.android.material.appbar.AppBarLayout)state.findViewById(R.id.owner_app_bar)).setExpanded(false,false));
                        }
                        scroll(state,true);capture(state,screen,mode==1?"empty-bottom":"error-bottom");
                    }finally{instrumentation.runOnMainSync(state::finish);instrumentation.waitForIdleSync();}
                }
            }
        }finally{
            instrumentation.runOnMainSync(()->{
                try{for(Map.Entry<Field,List<?>>e:lists.entrySet()){List list=(List)e.getKey().get(null);list.clear();list.addAll(e.getValue());}}catch(IllegalAccessException e){throw new AssertionError(e);}
            });
            for(Map.Entry<Field,Object>e:debug.entrySet())e.getKey().set(null,e.getValue());
            for(int i=0;i<prefs.length;i++){if(!saved[i].equals(prefs[i].getAll()))restore(prefs[i],saved[i]);assertEquals(saved[i],prefs[i].getAll());}
            for(Map.Entry<Field,List<?>>e:lists.entrySet())assertEquals(e.getValue(),e.getKey().get(null));
            for(Map.Entry<Field,Object>e:debug.entrySet())assertEquals(e.getValue(),e.getKey().get(null));
        }
    }
    private int screenFor(Activity a){return a.findViewById(R.id.today_served)!=null&&a.findViewById(R.id.today_served).isShown()?0:1;}
    private Activity launch(int screen,int mode)throws Exception {
        FakeData.setDebugMode(false);FakeData.setDebugFailOnce(false);FakeData.setDebugEmptyOnce(false);
        if(screen==1&&mode>0)arm(mode);
        Intent intent=new Intent(instrumentation.getTargetContext(),OwnerHomeActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Activity a=instrumentation.startActivitySync(intent);
        if(screen==0){instrumentation.waitForIdleSync();Thread.sleep(200);if(mode>0)arm(mode);instrumentation.runOnMainSync(()->((BottomNavigationView)a.findViewById(R.id.owner_home_nav)).setSelectedItemId(R.id.nav_today));}
        return a;
    }
    private void arm(int mode){FakeData.setDebugMode(true);FakeData.setDebugDelayMs(1200);FakeData.setDebugEmptyOnce(mode==1);FakeData.setDebugFailOnce(mode==2);}
    private NestedScrollView findScroll(View v){if(v instanceof NestedScrollView&&v.isShown())return(NestedScrollView)v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){NestedScrollView n=findScroll(((ViewGroup)v).getChildAt(i));if(n!=null)return n;}return null;}
    private void scroll(Activity a,boolean down){NestedScrollView n=findScroll(a.findViewById(android.R.id.content));if(n!=null)instrumentation.runOnMainSync(()->n.fullScroll(down?View.FOCUS_DOWN:View.FOCUS_UP));instrumentation.waitForIdleSync();}
    @SuppressWarnings("unchecked") private void restore(SharedPreferences prefs,Map<String,?> saved){SharedPreferences.Editor e=prefs.edit().clear();for(Map.Entry<String,?>x:saved.entrySet()){Object v=x.getValue();String k=x.getKey();if(v instanceof String)e.putString(k,(String)v);else if(v instanceof Boolean)e.putBoolean(k,(Boolean)v);else if(v instanceof Integer)e.putInt(k,(Integer)v);else if(v instanceof Long)e.putLong(k,(Long)v);else if(v instanceof Float)e.putFloat(k,(Float)v);else if(v instanceof Set)e.putStringSet(k,(Set<String>)v);else throw new AssertionError("Unsupported saved preference type");}assertTrue(e.commit());}
    private void swipeUp(Activity a){
        int w=a.getResources().getDisplayMetrics().widthPixels,h=a.getResources().getDisplayMetrics().heightPixels;
        android.graphics.Rect nav=new android.graphics.Rect();
        a.findViewById(R.id.owner_home_nav).getGlobalVisibleRect(nav);
        float from=Math.min(h*.68f,nav.top-24f),to=h*.12f;
        long start=SystemClock.uptimeMillis();
        for(int i=0;i<=12;i++){
            MotionEvent e=MotionEvent.obtain(start,SystemClock.uptimeMillis(),i==0?MotionEvent.ACTION_DOWN:i==12?MotionEvent.ACTION_UP:MotionEvent.ACTION_MOVE,w*.15f,from+(to-from)*i/12,0);
            instrumentation.sendPointerSync(e);e.recycle();SystemClock.sleep(20);
        }
        instrumentation.waitForIdleSync();
    }
    private void capture(Activity a,int screen,String state)throws Exception{Thread.sleep(180);File folder=new File(a.getExternalFilesDir(null),"phase3a-review");assertTrue(folder.isDirectory()||folder.mkdirs());Bitmap image=instrumentation.getUiAutomation().takeScreenshot();assertNotNull(image);android.content.res.Configuration c=a.getResources().getConfiguration();try(FileOutputStream out=new FileOutputStream(new File(folder,(screen==0?"organizer-today":"organizer-queues")+"-"+state+"-"+c.screenWidthDp+"x"+c.screenHeightDp+"-"+c.fontScale+".png"))){assertTrue(image.compress(Bitmap.CompressFormat.PNG,100,out));}finally{image.recycle();}}
}
