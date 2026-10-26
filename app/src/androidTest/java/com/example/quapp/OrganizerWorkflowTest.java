package com.example.quapp;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.test.filters.SdkSuppress;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;
import com.google.android.material.textfield.TextInputLayout;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import org.junit.Test;
import java.lang.reflect.*;
import java.util.*;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.*;

/** Real production callbacks against isolated FakeData; no backend or external share recipient. */
@SdkSuppress(minSdkVersion = 29)
public class OrganizerWorkflowTest extends LiveConsoleVisualReviewTest {
    @Override @Test @SuppressWarnings({"rawtypes", "unchecked"}) public void reviewConsole() throws Exception {
        FakeData.queues();
        Map<Field,Object> flags = new LinkedHashMap<>();
        for (String n : new String[]{"debugMode","debugDelayMs","debugFailOnce","debugEmptyOnce","nextQueueNumber","counterListener","myVerification","myRequest"}) {
            Field f=field(FakeData.class,n); flags.put(f,f.get(null));
        }
        Map<Field,List> lists=new LinkedHashMap<>();
        for(String n:new String[]{"queues","ownedIds","history"}) { Field f=field(FakeData.class,n);lists.put(f,new ArrayList((List)f.get(null))); }
        Field held=field(ActiveTicketStore.class,"held");lists.put(held,new ArrayList((List)held.get(null)));
        Map<Field,Map> maps=new LinkedHashMap<>();
        for(String n:new String[]{"live","finished"}) { Field f=field(FakeData.class,n);maps.put(f,new LinkedHashMap((Map)f.get(null))); }
        android.content.SharedPreferences[] prefs={ins.getTargetContext().getSharedPreferences("quapp_session",0),ins.getTargetContext().getSharedPreferences("quapp_device",0)};
        Map<String,?>[] saved=new Map[]{new LinkedHashMap<>(prefs[0].getAll()),new LinkedHashMap<>(prefs[1].getAll())};
        List<Activity> opened=new ArrayList<>();
        Instrumentation.ActivityMonitor monitor=null;
        try {
            queue=FakeData.queues().get(0).toBuilder().setId(id).setName("Workflow demo").setStatus(Queue.Status.OPEN).setGracePeriodEnabled(false).build();
            Constructor<?> constructor=Class.forName("com.example.quapp.FakeData$LiveQueue").getDeclaredConstructor();constructor.setAccessible(true);fixture=constructor.newInstance();
            waiting=(List<Ticket>)field(fixture.getClass(),"waiting").get(fixture);
            ins.runOnMainSync(()->{try {
                for(String n:new String[]{"debugMode","debugFailOnce","debugEmptyOnce"})field(FakeData.class,n).set(null,false);
                field(FakeData.class,"debugDelayMs").set(null,0L);
                field(FakeData.class,"counterListener").set(null,null);
                List qs=(List)field(FakeData.class,"queues").get(null);qs.clear();qs.add(queue);
                List owned=(List)field(FakeData.class,"ownedIds").get(null);owned.clear();owned.add(id);
                Map live=(Map)field(FakeData.class,"live").get(null);live.clear();live.put(id,fixture);
            }catch(Exception e){throw new AssertionError(e);}});
            activity=ins.startActivitySync(new Intent(ins.getTargetContext(),LiveConsoleActivity.class).putExtra(LiveConsoleActivity.EXTRA_QUEUE_ID,id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));opened.add(activity);idle();
            clickConsole(R.id.console_walk_in);
            onView(withId(android.R.id.button1)).perform(click());idle();
            assertWalkInError();assertTrue(FakeData.waitingTickets(id).isEmpty());
            onView(withId(R.id.walk_in_name_input)).perform(replaceText("   "),closeSoftKeyboard());
            onView(withId(android.R.id.button1)).perform(click());idle();assertWalkInError();assertTrue(FakeData.waitingTickets(id).isEmpty());
            onView(withId(R.id.walk_in_name_input)).perform(replaceText("  王秀英 👩🏽‍💻  "),closeSoftKeyboard());
            ins.runOnMainSync(()->{try{field(FakeData.class,"debugFailOnce").set(null,true);}catch(Exception e){throw new AssertionError(e);}});
            onView(withId(android.R.id.button1)).perform(click());idle();
            onView(withId(android.R.id.button1)).check((v,e)->{if(e!=null)throw e;assertTrue("Retry enabled after FakeData error",v.isEnabled());});
            assertTrue("Failure must not add ticket",FakeData.waitingTickets(id).isEmpty());
            onView(withId(android.R.id.button1)).perform(click());idle();
            assertEquals(1,FakeData.waitingTickets(id).size());Ticket first=FakeData.waitingTickets(id).get(0);
            assertEquals("王秀英 👩🏽‍💻",first.getHolderName());assertTrue(first.isWalkIn());assertNull(first.getHolderPhone());
            capture("walk-in-submitted-console");
            clickConsole(R.id.console_call_next);assertEquals(first.getId(),FakeData.nowServing(id).getId());
            clickConsole(R.id.console_call_next);assertEquals(Ticket.Status.SERVED,FakeData.ticket(id,first.getId()).getStatus());
            ins.runOnMainSync(()->FakeData.addWalkIn(id,"No show fixture"));
            ins.runOnMainSync(this::render);clickConsole(R.id.console_call_next);Ticket missed=FakeData.nowServing(id);
            clickConsole(R.id.console_no_show);assertEquals(Ticket.Status.NO_SHOW,FakeData.ticket(id,missed.getId()).getStatus());
            final Ticket[] rows=new Ticket[3];
            ins.runOnMainSync(()->{rows[0]=FakeData.addWalkIn(id,"Serve row");rows[1]=FakeData.addWalkIn(id,"No show row");rows[2]=FakeData.addWalkIn(id,"Remove row");render();
                ((LiveConsoleActivity)activity).onServeTicket(rows[0]);((LiveConsoleActivity)activity).onNoShowTicket(rows[1]);});idle();
            assertEquals(Ticket.Status.WAITING,FakeData.ticket(id,rows[0].getId()).getStatus());
            assertEquals(Ticket.Status.WAITING,FakeData.ticket(id,rows[1].getId()).getStatus());
            ins.runOnMainSync(()->((LiveConsoleActivity)activity).onRemoveTicket(rows[2]));idle();
            onView(withId(R.id.choices_danger)).perform(scrollTo(),click());idle();
            Ticket removed=FakeData.ticket(id,rows[2].getId());assertEquals(Ticket.Status.REMOVED,removed.getStatus());assertEquals(Ticket.RemovalReason.PRANK,removed.getRemovalReason());

            Activity share=ins.startActivitySync(ShareQueueActivity.intent(ins.getTargetContext(),id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));opened.add(share);idle();
            assertEquals(QueueLink.display(queue),((TextView)share.findViewById(R.id.share_link)).getText().toString());
            Bitmap qr=((BitmapDrawable)((ImageView)share.findViewById(R.id.share_qr)).getDrawable()).getBitmap();
            int[] pixels=new int[qr.getWidth()*qr.getHeight()];qr.getPixels(pixels,0,qr.getWidth(),0,0,qr.getWidth(),qr.getHeight());
            String decoded=new QRCodeReader().decode(new BinaryBitmap(new HybridBinarizer(new RGBLuminanceSource(qr.getWidth(),qr.getHeight(),pixels))),Collections.singletonMap(com.google.zxing.DecodeHintType.PURE_BARCODE,true)).getText();
            assertEquals(QueueLink.url(queue),decoded);
            capture("sharing-decoded-qr");
            final Intent[] chooser={null};
            monitor=new Instrumentation.ActivityMonitor(){@Override public Instrumentation.ActivityResult onStartActivity(Intent intent){
                if(Intent.ACTION_CHOOSER.equals(intent.getAction())){chooser[0]=intent;return new Instrumentation.ActivityResult(Activity.RESULT_CANCELED,null);}return null;
            }};ins.addMonitor(monitor);
            ins.runOnMainSync(()->assertTrue(share.findViewById(R.id.share_send).performClick()));idle();assertNotNull(chooser[0]);
            Intent send=chooser[0].getParcelableExtra(Intent.EXTRA_INTENT);assertNotNull(send);assertEquals(Intent.ACTION_SEND,send.getAction());assertEquals("text/plain",send.getType());
            assertEquals(queue.getName(),send.getStringExtra(Intent.EXTRA_SUBJECT));assertEquals(share.getString(R.string.share_message,queue.getName(),queue.getVenue(),QueueLink.url(queue)),send.getStringExtra(Intent.EXTRA_TEXT));
            ins.removeMonitor(monitor);monitor=null;
            ins.runOnMainSync(()->assertTrue(share.findViewById(R.id.share_counter).performClick()));idle();
            final Activity[] display={null};
            ins.runOnMainSync(()->{for(Activity a:ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED))if(a instanceof CounterDisplayActivity)display[0]=a;assertNotNull(display[0]);opened.add(display[0]);});
            assertEquals(display[0].getString(R.string.counter_none),((TextView)display[0].findViewById(R.id.counter_number)).getText().toString());
            final Ticket[] displayTicket={null};
            ins.runOnMainSync(()->{FakeData.addWalkIn(id,"Display refresh fixture");displayTicket[0]=FakeData.callNext(id);});
            String expectedNumber=display[0].getString(R.string.ticket_number_format,displayTicket[0].getTicketNumber());
            final String[] observed={null};long deadline=android.os.SystemClock.uptimeMillis()+6000;
            do{ins.runOnMainSync(()->observed[0]=((TextView)display[0].findViewById(R.id.counter_number)).getText().toString());if(expectedNumber.equals(observed[0]))break;android.os.SystemClock.sleep(200);}while(android.os.SystemClock.uptimeMillis()<deadline);
            assertEquals("Production Display refresh observes local serving ticket",expectedNumber,observed[0]);
            capture("display-refreshed-serving");
            ins.runOnMainSync(()->display[0].finish());idle();

            Activity home=ins.startActivitySync(new Intent(ins.getTargetContext(),OwnerHomeActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));opened.add(home);idle();
            ins.runOnMainSync(()->assertTrue(home.findViewById(R.id.owner_live_console).performClick()));
            Activity routedConsole=resumed(LiveConsoleActivity.class);opened.add(routedConsole);
            assertEquals(id,routedConsole.getIntent().getStringExtra(LiveConsoleActivity.EXTRA_QUEUE_ID));
            ins.runOnMainSync(routedConsole::finish);idle();
            ins.runOnMainSync(()->assertTrue(home.findViewById(R.id.nav_today).performClick()));idle();
            capture("today-owned-queue");
            ins.runOnMainSync(()->{
                androidx.recyclerview.widget.RecyclerView today=home.findViewById(R.id.today_list);
                androidx.recyclerview.widget.RecyclerView.ViewHolder row=today.findViewHolderForAdapterPosition(0);assertNotNull(row);assertTrue(row.itemView.performClick());
            });
            Activity analytics=resumed(QueueAnalyticsActivity.class);opened.add(analytics);
            assertEquals(id,analytics.getIntent().getStringExtra(QueueAnalyticsActivity.EXTRA_QUEUE_ID));
            ins.runOnMainSync(analytics::finish);idle();
            ins.runOnMainSync(()->assertTrue(home.findViewById(R.id.nav_queues).performClick()));idle();
            capture("queues-owned-live");
            ins.runOnMainSync(()->assertTrue(home.findViewById(R.id.owner_create_fab).performClick()));
            Activity create=resumed(CreateQueueActivity.class);opened.add(create);idle();
            ins.runOnMainSync(()->create.findViewById(R.id.create_submit_button).performClick());idle();
            assertEquals(View.GONE,create.findViewById(R.id.create_name_layout).getVisibility());
            assertEquals(View.VISIBLE,create.findViewById(R.id.create_pick_error).getVisibility());assertEquals(1,FakeData.queues().size());
            capture("create-required-validation");
            ins.runOnMainSync(()->{try{
                ((TextView)create.findViewById(R.id.create_details_input)).setText("Isolated functional test");
                field(CreateQueueActivity.class,"municipality").set(create,"Tagbilaran City");
                field(CreateQueueActivity.class,"category").set(create,Category.values()[0]);
                java.time.LocalDate tomorrow=Format.today().plusDays(1);
                field(CreateQueueActivity.class,"startDate").set(create,tomorrow);field(CreateQueueActivity.class,"endDate").set(create,tomorrow);
                Method showChoices=CreateQueueActivity.class.getDeclaredMethod("showChoices");showChoices.setAccessible(true);showChoices.invoke(create);
                field(FakeData.class,"debugFailOnce").set(null,true);
            }catch(Exception e){throw new AssertionError(e);}});idle();
            capture("create-filled-form-before-save");
            ins.runOnMainSync(()->create.findViewById(R.id.create_submit_button).performClick());idle();
            assertTrue(create.findViewById(R.id.create_submit_button).isEnabled());assertEquals(1,FakeData.queues().size());
            ins.runOnMainSync(()->create.findViewById(R.id.create_submit_button).performClick());idle();
            assertTrue("Successful create closes form",create.isFinishing());assertEquals(2,FakeData.queues().size());
            Queue created=null;for(Queue candidate:FakeData.queues())if(create.getString(R.string.create_generated_name, create.getString(Category.values()[0].label), "Tagbilaran City").equals(candidate.getName()))created=candidate;
            assertNotNull(created);assertEquals(Queue.Status.UPCOMING,created.getStatus());assertEquals("Tagbilaran City",created.getVenue());
            assertEquals("",created.getShortDescription());
            assertEquals("Isolated functional test",created.getDetails());
            Activity detail=ins.startActivitySync(new Intent(ins.getTargetContext(),QueueDetailActivity.class)
                    .putExtra(QueueDetailActivity.EXTRA_QUEUE_ID,created.getId()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            opened.add(detail);idle();
            assertEquals("Tagbilaran City",((TextView)detail.findViewById(R.id.detail_venue)).getText().toString());
            assertTrue(((List)field(FakeData.class,"ownedIds").get(null)).contains(created.getId()));
        } finally {
            if(monitor!=null)ins.removeMonitor(monitor);
            ins.runOnMainSync(()->{for(int i=opened.size()-1;i>=0;i--)opened.get(i).finish();});idle();
            ins.runOnMainSync(()->{try{for(Map.Entry<Field,List>e:lists.entrySet()){List l=(List)e.getKey().get(null);l.clear();l.addAll(e.getValue());}for(Map.Entry<Field,Map>e:maps.entrySet()){Map m=(Map)e.getKey().get(null);m.clear();m.putAll(e.getValue());}for(Map.Entry<Field,Object>e:flags.entrySet())e.getKey().set(null,e.getValue());}catch(Exception e){throw new AssertionError(e);}});
            for(int i=0;i<prefs.length;i++){if(!saved[i].equals(prefs[i].getAll()))restore(prefs[i],saved[i]);assertEquals(saved[i],prefs[i].getAll());}
            for(Map.Entry<Field,List>e:lists.entrySet())assertEquals(e.getValue(),e.getKey().get(null));for(Map.Entry<Field,Map>e:maps.entrySet())assertEquals(e.getValue(),e.getKey().get(null));for(Map.Entry<Field,Object>e:flags.entrySet())assertEquals(e.getValue(),e.getKey().get(null));
        }
    }
    private void clickConsole(int id){ins.runOnMainSync(()->assertTrue(activity.findViewById(id).performClick()));idle();}
    private void assertWalkInError(){onView(withId(R.id.walk_in_name_layout)).check((v,e)->{if(e!=null)throw e;assertEquals(activity.getString(R.string.console_walk_in_error),((TextInputLayout)v).getError().toString());});}
    private Activity resumed(Class<? extends Activity> type){
        final Activity[] found={null};long deadline=android.os.SystemClock.uptimeMillis()+4000;
        do{idle();ins.runOnMainSync(()->{for(Activity a:ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED))if(type.isInstance(a))found[0]=a;});}while(found[0]==null&&android.os.SystemClock.uptimeMillis()<deadline);
        assertNotNull("Expected destination "+type.getSimpleName(),found[0]);return found[0];
    }
    @Override void capture(String state)throws Exception{
        idle();Bitmap bitmap=ins.getUiAutomation().takeScreenshot();assertNotNull(bitmap);
        java.io.File dir=new java.io.File(ins.getTargetContext().getExternalFilesDir(null),"phase3-final-workflows");assertTrue(dir.isDirectory()||dir.mkdirs());
        android.content.res.Configuration c=ins.getTargetContext().getResources().getConfiguration();
        try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(dir,state+"-"+c.screenWidthDp+"x"+c.screenHeightDp+"-"+c.fontScale+".png"))){assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,out));}finally{bitmap.recycle();}
    }
}
