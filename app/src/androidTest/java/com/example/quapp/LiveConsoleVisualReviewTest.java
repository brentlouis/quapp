package com.example.quapp;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.SystemClock;
import android.view.View;
import android.widget.TextView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.*;
import java.time.Instant;
import java.util.*;
import static org.junit.Assert.*;

/** Isolated rendering only: never calls, serves, removes, announces or saves a queue. */
@RunWith(AndroidJUnit4.class)
public class LiveConsoleVisualReviewTest {
    final Instrumentation ins=InstrumentationRegistry.getInstrumentation();
    final String id="visual-console-only",worst="👩🏽‍💻 王秀英 — Aleksandra Wiśniewska-Kowalczyk Community Services";
    Activity activity; Object fixture; List<Ticket> waiting; Queue queue;
    @Test @SuppressWarnings("unchecked") public void reviewConsole()throws Exception {
        FakeData.queues();
        Map<Field,List<?>> lists=new LinkedHashMap<>();
        for(String n:new String[]{"queues","ownedIds","history"}){Field f=field(FakeData.class,n);lists.put(f,new ArrayList<>((List<?>)f.get(null)));}
        Field held=field(ActiveTicketStore.class,"held");lists.put(held,new ArrayList<>((List<?>)held.get(null)));
        Map<Field,Map<?,?>> maps=new LinkedHashMap<>();
        for(String n:new String[]{"live","finished"}){Field f=field(FakeData.class,n);maps.put(f,new LinkedHashMap<>((Map<?,?>)f.get(null)));}
        Map<Field,Object> flags=new LinkedHashMap<>();
        for(String n:new String[]{"debugMode","debugDelayMs","debugFailOnce","debugEmptyOnce","nextQueueNumber"}){Field f=field(FakeData.class,n);flags.put(f,f.get(null));}
        SharedPreferences[] prefs={ins.getTargetContext().getSharedPreferences("quapp_session",0),ins.getTargetContext().getSharedPreferences("quapp_device",0)};
        Map<String,?>[] saved=new Map[]{new LinkedHashMap<>(prefs[0].getAll()),new LinkedHashMap<>(prefs[1].getAll())};
        try {
            queue=FakeData.queues().get(0).toBuilder().setId(id).setName("Demo service · visual fixture").setStatus(Queue.Status.OPEN).setGracePeriodEnabled(true).build();
            Constructor<?> constructor=Class.forName("com.example.quapp.FakeData$LiveQueue").getDeclaredConstructor();constructor.setAccessible(true);fixture=constructor.newInstance();
            waiting=(List<Ticket>)field(fixture.getClass(),"waiting").get(fixture);
            ins.runOnMainSync(()->{try{List qs=(List)field(FakeData.class,"queues").get(null);qs.clear();qs.add(queue);Map live=(Map)field(FakeData.class,"live").get(null);live.clear();live.put(id,fixture);}catch(Exception e){throw new AssertionError(e);}});
            activity=ins.startActivitySync(new Intent(ins.getTargetContext(),LiveConsoleActivity.class).putExtra(LiveConsoleActivity.EXTRA_QUEUE_ID,id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));idle();
            capture("default-zero");reveal(R.id.console_empty_body);capture("empty");
            ins.runOnMainSync(()->{waiting.add(ticket(999999999,worst,null,false));waiting.add(ticket(1,"Jo",null,true));render();((TextView)activity.findViewById(R.id.console_queue_name)).setText(worst);});
            reveal(R.id.console_queue_name);capture("queue-long");reveal(R.id.waiting_name);capture("waiting-unicode");
            ins.runOnMainSync(()->((TextView)activity.findViewById(R.id.console_waiting_header)).setText("Up next · 999999999"));reveal(R.id.console_waiting_header);capture("large-count");
            for(int state=0;state<3;state++){
                final int mode=state;
                ins.runOnMainSync(()->{try{field(fixture.getClass(),"nowServing").set(fixture,ticket(999999999,worst,null,false).toBuilder().setStatus(Ticket.Status.CALLED).setCalledAt(Instant.now()).build());field(fixture.getClass(),"nowServingConfirmedAt").set(fixture,mode==1?Instant.now():null);field(fixture.getClass(),"nowServingTimedOut").set(fixture,mode==2);render();}catch(Exception e){throw new AssertionError(e);}});
                String label=state==0?"awaiting":state==1?"confirmed":"timed-out";
                reveal(R.id.console_serving_name);capture(label+"-identity");reveal(R.id.console_serving_wait);capture(label+"-status");
                assertTrue(activity.findViewById(R.id.console_serving_wait).isShown());
                reveal(R.id.console_call_next);assertTrue(activity.findViewById(R.id.console_call_next).getGlobalVisibleRect(new Rect()));capture(label+"-actions");
            }
            // Existing action feedback presented directly; no operational callback is invoked.
            ins.runOnMainSync(()->{android.widget.Button button=activity.findViewById(R.id.console_call_next);button.setText(R.string.calling_next);button.setEnabled(false);});
            reveal(R.id.console_call_next);capture("loading-label-fixture");
            final com.google.android.material.snackbar.Snackbar[] error=new com.google.android.material.snackbar.Snackbar[1];
            try {ins.runOnMainSync(()->{error[0]=com.google.android.material.snackbar.Snackbar.make(activity.findViewById(R.id.console_list),R.string.error_default_message,com.google.android.material.snackbar.Snackbar.LENGTH_INDEFINITE);error[0].show();});capture("error-feedback-fixture");assertTrue(error[0].getView().getGlobalVisibleRect(new Rect()));}
            finally {if(error[0]!=null){ins.runOnMainSync(()->error[0].dismiss());idle();}}
            ins.runOnMainSync(()->{try{field(fixture.getClass(),"nowServing").set(fixture,null);field(fixture.getClass(),"nowServingTimedOut").set(fixture,false);waiting.clear();waiting.add(ticket(0,"",null,true));render();}catch(Exception e){throw new AssertionError(e);}});
            reveal(R.id.waiting_number);capture("missing-name-zero-number");
            for(Queue.Status status:new Queue.Status[]{Queue.Status.PAUSED,Queue.Status.CLOSED}){
                ins.runOnMainSync(()->{try{waiting.clear();queue=queue.toBuilder().setStatus(status).build();List qs=(List)field(FakeData.class,"queues").get(null);qs.set(0,queue);render();}catch(Exception e){throw new AssertionError(e);}});
                reveal(R.id.console_empty_body);capture(status.name().toLowerCase()+"-empty");reveal(R.id.console_call_next);capture(status.name().toLowerCase()+"-actions");
                if(status==Queue.Status.CLOSED)assertFalse(activity.findViewById(R.id.console_call_next).isEnabled());
            }
        } finally {
            if(activity!=null){ins.runOnMainSync(activity::finish);idle();}
            ins.runOnMainSync(()->{try{for(Map.Entry<Field,List<?>> e:lists.entrySet()){List l=(List)e.getKey().get(null);l.clear();l.addAll(e.getValue());}for(Map.Entry<Field,Map<?,?>>e:maps.entrySet()){Map map=(Map)e.getKey().get(null);map.clear();map.putAll(e.getValue());}for(Map.Entry<Field,Object>e:flags.entrySet())e.getKey().set(null,e.getValue());}catch(Exception e){throw new AssertionError(e);}});
            for(int i=0;i<prefs.length;i++){if(!saved[i].equals(prefs[i].getAll()))restore(prefs[i],saved[i]);assertEquals(saved[i],prefs[i].getAll());}
            for(Map.Entry<Field,List<?>>e:lists.entrySet())assertEquals(e.getValue(),e.getKey().get(null));
            for(Map.Entry<Field,Map<?,?>>e:maps.entrySet())assertEquals(e.getValue(),e.getKey().get(null));
            for(Map.Entry<Field,Object>e:flags.entrySet())assertEquals(e.getValue(),e.getKey().get(null));
        }
    }
    Ticket ticket(int number,String name,String phone,boolean walkIn){return new Ticket.Builder().setId(id+number).setQueue(id,queue.getName(),queue.getVenue()).setHolder(name,phone).setWalkIn(walkIn).setTicketNumber(number).setJoinedAt(Instant.now()).build();}
    static Field field(Class<?> c,String n)throws Exception{Field f=c.getDeclaredField(n);f.setAccessible(true);return f;}
    void render(){try{Method m=LiveConsoleActivity.class.getDeclaredMethod("render");m.setAccessible(true);m.invoke(activity);}catch(Exception e){throw new AssertionError(e);}}
    void idle(){ins.waitForIdleSync();SystemClock.sleep(150);}
    void reveal(int id){ins.runOnMainSync(()->{View v=activity.findViewById(id);assertNotNull(v);v.requestRectangleOnScreen(new Rect(0,0,v.getWidth(),v.getHeight()),true);});idle();}
    void capture(String state)throws Exception{idle();SystemClock.sleep(600);Bitmap bitmap=ins.getUiAutomation().takeScreenshot();assertNotNull(bitmap);File dir=new File(ins.getTargetContext().getExternalFilesDir(null),"phase3c-review");assertTrue(dir.isDirectory()||dir.mkdirs());android.content.res.Configuration c=activity.getResources().getConfiguration();try(FileOutputStream out=new FileOutputStream(new File(dir,"console-"+state+"-"+c.screenWidthDp+"x"+c.screenHeightDp+"-"+c.fontScale+".png"))){assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,out));}finally{bitmap.recycle();}}
    void restore(SharedPreferences p,Map<String,?> values){SharedPreferences.Editor e=p.edit().clear();for(Map.Entry<String,?>v:values.entrySet()){Object x=v.getValue();if(x instanceof String)e.putString(v.getKey(),(String)x);else if(x instanceof Boolean)e.putBoolean(v.getKey(),(Boolean)x);else if(x instanceof Integer)e.putInt(v.getKey(),(Integer)x);else if(x instanceof Long)e.putLong(v.getKey(),(Long)x);else if(x instanceof Float)e.putFloat(v.getKey(),(Float)x);else if(x instanceof Set)e.putStringSet(v.getKey(),(Set<String>)x);else throw new AssertionError(x);}assertTrue(e.commit());}
}
