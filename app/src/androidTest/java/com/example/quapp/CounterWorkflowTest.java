package com.example.quapp;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import android.view.inspector.WindowInspector;
import android.widget.TextView;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;
import com.google.android.material.textfield.TextInputLayout;
import org.junit.Test;
import java.io.*;
import java.lang.reflect.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

/** Production owner callbacks and shared-line semantics against disposable process-local state. */
@SdkSuppress(minSdkVersion=29)
public class CounterWorkflowTest extends LiveConsoleVisualReviewTest {
    final List<Activity> opened=new ArrayList<>();
    @Override @Test @SuppressWarnings({"rawtypes","unchecked"}) public void reviewConsole() throws Exception {
        FakeData.queues();
        Map<Field,Object> flags=new LinkedHashMap<>();for(String n:new String[]{"debugMode","debugDelayMs","debugFailOnce","debugEmptyOnce","nextQueueNumber","counterListener","myVerification","myRequest"}){Field f=field(FakeData.class,n);flags.put(f,f.get(null));}
        Map<Field,List> lists=new LinkedHashMap<>();for(String n:new String[]{"queues","ownedIds","history"}){Field f=field(FakeData.class,n);lists.put(f,new ArrayList((List)f.get(null)));}
        Field held=field(ActiveTicketStore.class,"held");lists.put(held,new ArrayList((List)held.get(null)));
        Map<Field,Map> maps=new LinkedHashMap<>();for(String n:new String[]{"live","finished"}){Field f=field(FakeData.class,n);maps.put(f,new LinkedHashMap((Map)f.get(null)));}
        android.content.SharedPreferences[] prefs={ins.getTargetContext().getSharedPreferences("quapp_session",0),ins.getTargetContext().getSharedPreferences("quapp_device",0)};
        Map<String,?>[] saved=new Map[]{new LinkedHashMap<>(prefs[0].getAll()),new LinkedHashMap<>(prefs[1].getAll())};
        try {
            queue=FakeData.queues().get(0).toBuilder().setId(id).setName("Shared line demo").setOrganizer(FakeData.MY_ORGANIZER_ID,"Demo owner",true).setStatus(Queue.Status.OPEN).setGracePeriodEnabled(false).build();
            ins.runOnMainSync(()->{try{
                for(String n:new String[]{"debugMode","debugFailOnce","debugEmptyOnce"})field(FakeData.class,n).set(null,false);
                field(FakeData.class,"debugDelayMs").set(null,0L);field(FakeData.class,"counterListener").set(null,null);
                List qs=(List)field(FakeData.class,"queues").get(null);qs.clear();qs.add(queue);
                List owned=(List)field(FakeData.class,"ownedIds").get(null);owned.clear();owned.add(id);((List)field(ActiveTicketStore.class,"held").get(null)).clear();
                freshLine();
            }catch(Exception e){throw new AssertionError(e);}});
            modelChecks();
            ins.runOnMainSync(()->{try{FakeData.saveQueue(queue);freshLine();}catch(Exception e){throw new AssertionError(e);}});
            activity=launch(new Intent(ins.getTargetContext(),LiveConsoleActivity.class).putExtra(LiveConsoleActivity.EXTRA_QUEUE_ID,id));
            click(activity,R.id.console_menu);sheetText(R.string.counters_title);
            activity=resumed(CounterListActivity.class);orient(activity);capture("list-default");
            click(activity,R.id.counters_add);dialogClick(android.R.id.button1);
            assertNotNull(((TextInputLayout)window(R.id.counter_name_layout)).getError());
            for(int id:new int[]{android.R.id.button1,android.R.id.button2}){View button=window(id);ins.runOnMainSync(()->{Rect visible=new Rect();assertTrue(button.getGlobalVisibleRect(visible));assertEquals(button.getHeight(),visible.height());assertTrue(visible.height()>=activity.getResources().getDimensionPixelSize(R.dimen.touch_target_min));});}
            capture("add-validation-actions");
            setDialogName(worst);dialogClick(android.R.id.button1);assertEquals(2,FakeData.counters(id).size());capture("list-unicode");
            final Activity list=activity;ins.runOnMainSync(()->{androidx.recyclerview.widget.RecyclerView view=list.findViewById(R.id.counters_list);view.scrollToPosition(1);});idle();
            ins.runOnMainSync(()->{androidx.recyclerview.widget.RecyclerView view=list.findViewById(R.id.counters_list);View row=view.findViewHolderForAdapterPosition(1).itemView;View label=row.findViewById(R.id.managed_counter_row_state);label.requestRectangleOnScreen(new Rect(0,0,label.getWidth(),label.getHeight()),true);});idle();
            ins.runOnMainSync(()->{androidx.recyclerview.widget.RecyclerView view=list.findViewById(R.id.counters_list);View row=view.findViewHolderForAdapterPosition(1).itemView;View label=row.findViewById(R.id.managed_counter_row_state);Rect visible=new Rect();assertTrue(label.getGlobalVisibleRect(visible));assertEquals("Long counter row end is reachable",label.getHeight(),visible.height());assertTrue(row.performClick());});
            activity=resumed(CounterActivity.class);orient(activity);String counterId=activity.getIntent().getStringExtra(CounterActivity.EXTRA_COUNTER_ID);
            assertEquals(id,activity.getIntent().getStringExtra(CounterActivity.EXTRA_QUEUE_ID));
            click(activity,R.id.managed_counter_rename);setDialogName("  ");dialogClick(android.R.id.button1);assertNotNull(((TextInputLayout)window(R.id.counter_name_layout)).getError());
            setDialogName("Window B · 王秀英");dialogClick(android.R.id.button1);assertEquals("Window B · 王秀英",FakeData.counters(id).get(1).getName());capture("counter-empty");
            ins.runOnMainSync(()->{try{field(fixture.getClass(),"nextTicketNumber").set(fixture,999999999);}catch(Exception e){throw new AssertionError(e);}FakeData.addWalkIn(id,worst);});refreshCounter();
            click(activity,R.id.managed_counter_call);assertEquals(999999999,FakeData.counterServing(id,counterId).getTicketNumber());capture("counter-nine-digit");
            assertFalse(activity.findViewById(R.id.managed_counter_call).isEnabled());
            click(activity,R.id.managed_counter_done);assertNull(FakeData.counterServing(id,counterId));assertEquals(1,FakeData.stats(id).getServedToday());
            ins.runOnMainSync(()->{try{field(fixture.getClass(),"nextTicketNumber").set(fixture,0);}catch(Exception e){throw new AssertionError(e);}FakeData.addWalkIn(id,"");});refreshCounter();
            click(activity,R.id.managed_counter_call);assertEquals(0,FakeData.counterServing(id,counterId).getTicketNumber());capture("counter-zero-missing");
            click(activity,R.id.managed_counter_no_show);assertEquals(1,FakeData.stats(id).getNoShowsToday());
            ins.runOnMainSync(()->FakeData.addWalkIn(id,"Selected display"));refreshCounter();click(activity,R.id.managed_counter_call);
            click(activity,R.id.managed_counter_display);activity=resumed(CounterDisplayActivity.class);idle();
            assertEquals(counterId,activity.getIntent().getStringExtra(CounterDisplayActivity.EXTRA_COUNTER_ID));
            assertTrue(((TextView)activity.findViewById(R.id.counter_queue)).getText().toString().contains("Window B"));capture("selected-display");
            extraWorkflows();
        } finally {
            ins.runOnMainSync(()->{for(int i=opened.size()-1;i>=0;i--)opened.get(i).finish();});idle();
            ins.runOnMainSync(()->{try{for(Map.Entry<Field,List>e:lists.entrySet()){List l=(List)e.getKey().get(null);l.clear();l.addAll(e.getValue());}for(Map.Entry<Field,Map>e:maps.entrySet()){Map m=(Map)e.getKey().get(null);m.clear();m.putAll(e.getValue());}for(Map.Entry<Field,Object>e:flags.entrySet())e.getKey().set(null,e.getValue());}catch(Exception e){throw new AssertionError(e);}});
            for(int i=0;i<prefs.length;i++){if(!saved[i].equals(prefs[i].getAll()))restore(prefs[i],saved[i]);assertEquals(saved[i],prefs[i].getAll());}
            for(Map.Entry<Field,List>e:lists.entrySet())assertEquals(e.getValue(),e.getKey().get(null));for(Map.Entry<Field,Map>e:maps.entrySet())assertEquals(e.getValue(),e.getKey().get(null));for(Map.Entry<Field,Object>e:flags.entrySet())assertEquals(e.getValue(),e.getKey().get(null));
        }
    }
    void extraWorkflows()throws Exception { }
    @SuppressWarnings({"rawtypes","unchecked"}) void freshLine()throws Exception{
        Constructor<?> c=Class.forName("com.example.quapp.FakeData$LiveQueue").getDeclaredConstructor();c.setAccessible(true);fixture=c.newInstance();
        Map live=(Map)field(FakeData.class,"live").get(null);live.clear();live.put(id,fixture);
    }
    void modelChecks()throws Exception{
        QueueCounter b=FakeData.createCounter(id,"B"),c=FakeData.createCounter(id,"C");
        denied(()->FakeData.createCounter(id," "),IllegalArgumentException.class);denied(()->FakeData.counterServing(id,"missing"),IllegalArgumentException.class);
        denied(()->FakeData.counters("foreign"),SecurityException.class);denied(()->FakeData.counterServing(id,"foreign-counter-2"),IllegalArgumentException.class);
        Queue foreign=queue.toBuilder().setId("foreign").setOrganizer("other-owner","Other owner",true).build();FakeData.saveQueue(foreign);
        denied(()->FakeData.counters("foreign"),SecurityException.class);denied(()->FakeData.createCounter("foreign","Not permitted"),SecurityException.class);
        denied(()->FakeData.renameCounter("foreign",FakeData.MAIN_COUNTER_ID,"Not permitted"),SecurityException.class);denied(()->FakeData.callNextAtCounter("foreign",FakeData.MAIN_COUNTER_ID),SecurityException.class);denied(()->FakeData.finishCounter("foreign",FakeData.MAIN_COUNTER_ID,false),SecurityException.class);
        for(int i=0;i<10;i++)FakeData.addWalkIn(id,"Fixture "+i);
        AtomicReference<Ticket> main=new AtomicReference<>(),extra=new AtomicReference<>();AtomicReference<Throwable> error=new AtomicReference<>();CountDownLatch start=new CountDownLatch(1);
        Thread first=new Thread(()->{try{start.await();main.set(FakeData.callNext(id));}catch(Throwable e){error.set(e);}});
        Thread second=new Thread(()->{try{start.await();extra.set(FakeData.callNextAtCounter(id,b.getId()));}catch(Throwable e){error.set(e);}});
        first.start();second.start();start.countDown();first.join(3000);second.join(3000);assertFalse(first.isAlive());assertFalse(second.isAlive());assertNull(error.get());assertNotEquals(main.get().getId(),extra.get().getId());assertEquals(8,FakeData.waitingTickets(id).size());
        @SuppressWarnings("unchecked") List<Ticket> held=(List<Ticket>)field(ActiveTicketStore.class,"held").get(null);held.add(FakeData.waitingTickets(id).get(0));
        Activity summary=launch(new Intent(ins.getTargetContext(),ActiveTicketActivity.class).putExtra(ActiveTicketActivity.EXTRA_TICKET_ID,held.get(0).getId()));idle();
        String servingText=((TextView)summary.findViewById(R.id.ticket_now_serving)).getText().toString();
        assertTrue(servingText.contains(summary.getString(R.string.ticket_number_format,main.get().getTicketNumber())));assertTrue(servingText.contains(summary.getString(R.string.ticket_number_format,extra.get().getTicketNumber())));
        ins.runOnMainSync(summary::finish);idle();held.clear();
        denied(()->FakeData.callNextAtCounter(id,b.getId()),IllegalStateException.class);assertEquals(8,FakeData.waitingTickets(id).size());
        FakeData.finishCounter(id,FakeData.MAIN_COUNTER_ID,false);assertEquals(Integer.valueOf(extra.get().getTicketNumber()),FakeData.queueById(id).getNowServing());
        assertEquals(extra.get().getId(),FakeData.ticket(id,extra.get().getId()).getId());FakeData.confirmArrival(id,extra.get().getId());
        FakeData.moveBack(id,extra.get().getId(),1);assertNull(FakeData.counterServing(id,b.getId()));assertEquals(Ticket.Status.WAITING,FakeData.ticket(id,extra.get().getId()).getStatus());
        Ticket leave=FakeData.callNextAtCounter(id,b.getId());FakeData.leave(id,leave.getId());assertNull(FakeData.ticket(id,leave.getId()));
        FakeData.setQueueStatus(id,Queue.Status.PAUSED);Ticket confirmed=FakeData.callNextAtCounter(id,b.getId()),expired=FakeData.callNextAtCounter(id,c.getId());
        FakeData.saveQueue(FakeData.queueById(id).toBuilder().setGracePeriodEnabled(true).build());
        Object bSlot=((Map<?,?>)field(fixture.getClass(),"counters").get(fixture)).get(b.getId());Object cSlot=((Map<?,?>)field(fixture.getClass(),"counters").get(fixture)).get(c.getId());
        field(bSlot.getClass(),"nowServing").set(bSlot,confirmed.toBuilder().setCalledAt(Instant.now().minusSeconds(600)).build());
        field(cSlot.getClass(),"nowServing").set(cSlot,expired.toBuilder().setCalledAt(Instant.now().minusSeconds(600)).build());
        FakeData.confirmArrival(id,confirmed.getId());int before=FakeData.stats(id).getNoShowsToday();FakeData.checkGraceExpired(id);FakeData.checkGraceExpired(id);
        assertEquals(before+1,FakeData.stats(id).getNoShowsToday());assertEquals(Ticket.Status.NO_SHOW,FakeData.ticket(id,expired.getId()).getStatus());assertEquals(Ticket.Status.CALLED,FakeData.ticket(id,confirmed.getId()).getStatus());
        FakeData.releaseCalled(id,confirmed.getId());FakeData.releaseCalled(id,confirmed.getId());assertEquals(before+2,FakeData.stats(id).getNoShowsToday());
        FakeData.callNextAtCounter(id,b.getId());FakeData.callNextAtCounter(id,c.getId());List<Ticket> active=FakeData.servingTickets(id);assertEquals(2,active.size());
        FakeData.setQueueStatus(id,Queue.Status.CLOSED);assertTrue(FakeData.servingTickets(id).isEmpty());assertTrue(FakeData.waitingTickets(id).isEmpty());for(Ticket t:active)assertEquals(Ticket.Status.QUEUE_CLOSED,FakeData.ticket(id,t.getId()).getStatus());
        denied(()->FakeData.callNextAtCounter(id,b.getId()),IllegalStateException.class);
    }
    interface Action{void run();}void denied(Action action,Class<? extends Throwable> expected){try{action.run();fail("Expected "+expected.getSimpleName());}catch(Throwable error){assertTrue(error.toString(),expected.isInstance(error));}}
    Activity launch(Intent intent){Activity a=ins.startActivitySync(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));opened.add(a);orient(a);return activity;}
    Activity resumed(Class<? extends Activity> type){final Activity[] result={null};long until=android.os.SystemClock.uptimeMillis()+4000;do{idle();ins.runOnMainSync(()->{for(Activity a:ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED))if(type.isInstance(a))result[0]=a;});}while(result[0]==null&&android.os.SystemClock.uptimeMillis()<until);assertNotNull(result[0]);opened.add(result[0]);return result[0];}
    void orient(Activity a){boolean landscape="landscape".equals(InstrumentationRegistry.getArguments().getString("displayOrientation"));ins.runOnMainSync(()->a.setRequestedOrientation(landscape?ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE:ActivityInfo.SCREEN_ORIENTATION_PORTRAIT));android.os.SystemClock.sleep(500);idle();ins.runOnMainSync(()->{for(Activity current:ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED))if(current.getClass()==a.getClass()){activity=current;if(!opened.contains(current))opened.add(current);}});}
    void click(Activity a,int id){ins.runOnMainSync(()->{View v=a.findViewById(id);assertNotNull(v);v.requestRectangleOnScreen(new Rect(0,0,v.getWidth(),v.getHeight()),true);});idle();ins.runOnMainSync(()->assertTrue(a.findViewById(id).performClick()));idle();}
    View window(int id){final View[] found={null};ins.runOnMainSync(()->{for(View root:WindowInspector.getGlobalWindowViews())if(root.findViewById(id)!=null)found[0]=root.findViewById(id);});assertNotNull(found[0]);return found[0];}
    void dialogClick(int id){View button=window(id);ins.runOnMainSync(()->assertTrue(button.performClick()));idle();}
    void setDialogName(String value){TextView input=(TextView)window(R.id.counter_name_input);ins.runOnMainSync(()->input.setText(value));idle();}
    void sheetText(int string){String text=activity.getString(string);final View[] found={null};ins.runOnMainSync(()->{for(View root:WindowInspector.getGlobalWindowViews()){View match=findText(root,text);if(match!=null)found[0]=match;}assertNotNull(found[0]);View v=found[0];v.requestRectangleOnScreen(new Rect(0,0,v.getWidth(),v.getHeight()),true);while(!v.isClickable()&&v.getParent() instanceof View)v=(View)v.getParent();assertTrue(v.performClick());});idle();}
    View findText(View view,String text){if(view instanceof TextView&&text.contentEquals(((TextView)view).getText()))return view;if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++){View found=findText(group.getChildAt(i),text);if(found!=null)return found;}}return null;}
    void refreshCounter(){ins.runOnMainSync(()->{try{Method m=CounterActivity.class.getDeclaredMethod("render");m.setAccessible(true);m.invoke(activity);}catch(Exception e){throw new AssertionError(e);}});idle();}
    @Override void capture(String state)throws Exception{
        idle();if(activity instanceof CounterDisplayActivity)dismissFullscreenEducation();if(activity instanceof CounterActivity){for(int id:new int[]{R.id.managed_counter_name,R.id.managed_counter_ticket,R.id.managed_counter_call,R.id.managed_counter_done,R.id.managed_counter_no_show}){View v=activity.findViewById(id);ins.runOnMainSync(()->v.requestRectangleOnScreen(new Rect(0,0,v.getWidth(),v.getHeight()),true));idle();ins.runOnMainSync(()->{Rect r=new Rect();assertTrue(v.getGlobalVisibleRect(r));assertEquals(v.getHeight(),r.height());if(v instanceof TextView){TextView t=(TextView)v;for(int n=0;n<t.getLineCount();n++)assertEquals(0,t.getLayout().getEllipsisCount(n));}if(v.isClickable())assertTrue(v.getHeight()>=activity.getResources().getDimensionPixelSize(R.dimen.touch_target_min));});}ins.runOnMainSync(()->((androidx.core.widget.NestedScrollView)activity.findViewById(R.id.managed_counter_scroll)).scrollTo(0,0));idle();}
        Bitmap b=ins.getUiAutomation().takeScreenshot();assertNotNull(b);File dir=new File(ins.getTargetContext().getExternalFilesDir(null),captureFolder());assertTrue(dir.isDirectory()||dir.mkdirs());android.content.res.Configuration c=activity.getResources().getConfiguration();try(FileOutputStream out=new FileOutputStream(new File(dir,state+"-"+c.screenWidthDp+"x"+c.screenHeightDp+"-"+c.fontScale+".png"))){assertTrue(b.compress(Bitmap.CompressFormat.PNG,100,out));}finally{b.recycle();}
    }
    String captureFolder(){return "phase3-counter-shared-line";}
    void dismissFullscreenEducation()throws Exception{
        for(int attempt=0;attempt<3;attempt++){
            android.view.accessibility.AccessibilityNodeInfo root=ins.getUiAutomation().getRootInActiveWindow();if(root==null)return;
            java.util.List<android.view.accessibility.AccessibilityNodeInfo> buttons=root.findAccessibilityNodeInfosByText("Got it");boolean found=false;
            for(android.view.accessibility.AccessibilityNodeInfo button:buttons){CharSequence owner=button.getPackageName();if(owner!=null&&(owner.toString().equals("com.android.systemui")||owner.toString().equals("android"))){found=true;button.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK);}}
            if(!found)return;ins.getUiAutomation().waitForIdle(100,1500);idle();
        }
        fail("Fullscreen education overlay did not dismiss");
    }
}
