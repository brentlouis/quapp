package com.example.quapp;

import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.SystemClock;
import android.widget.TextView;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;
import org.junit.Test;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import static org.junit.Assert.*;

/** Isolated display fixtures. No expired ticket or operational callback is used. */
public class DisplayVisualReviewTest extends LiveConsoleVisualReviewTest {
    private Object displaySlot;
    @Override @Test @SuppressWarnings({"rawtypes","unchecked"}) public void reviewConsole() throws Exception {
        FakeData.queues();
        Map<Field,Object> flags=new LinkedHashMap<>();
        for(String n:new String[]{"debugMode","debugDelayMs","debugFailOnce","debugEmptyOnce","nextQueueNumber"}){Field f=field(FakeData.class,n);flags.put(f,f.get(null));}
        Map<Field,List> lists=new LinkedHashMap<>();
        for(String n:new String[]{"queues","ownedIds","history"}){Field f=field(FakeData.class,n);lists.put(f,new ArrayList((List)f.get(null)));}
        Field held=field(ActiveTicketStore.class,"held");lists.put(held,new ArrayList((List)held.get(null)));
        Map<Field,Map> maps=new LinkedHashMap<>();
        for(String n:new String[]{"live","finished"}){Field f=field(FakeData.class,n);maps.put(f,new LinkedHashMap((Map)f.get(null)));}
        android.content.SharedPreferences[] prefs={ins.getTargetContext().getSharedPreferences("quapp_session",0),ins.getTargetContext().getSharedPreferences("quapp_device",0)};
        Map<String,?>[] saved=new Map[]{new LinkedHashMap<>(prefs[0].getAll()),new LinkedHashMap<>(prefs[1].getAll())};


        try {
            queue=FakeData.queues().get(0).toBuilder().setId(id).setName("Demo community service").setOrganizer(FakeData.MY_ORGANIZER_ID,"Demo owner",true).setGracePeriodEnabled(false).build();
            Constructor<?> constructor=Class.forName("com.example.quapp.FakeData$LiveQueue").getDeclaredConstructor();constructor.setAccessible(true);fixture=constructor.newInstance();
            waiting=(List<Ticket>)field(fixture.getClass(),"waiting").get(fixture);
            ins.runOnMainSync(()->{try{List qs=(List)field(FakeData.class,"queues").get(null);qs.clear();qs.add(queue);Map live=(Map)field(FakeData.class,"live").get(null);live.clear();live.put(id,fixture);}catch(Exception e){throw new AssertionError(e);}});
            boolean selected=Boolean.parseBoolean(InstrumentationRegistry.getArguments().getString("selectedDisplay","false"));
            displaySlot=fixture;
            android.content.Intent displayIntent=CounterDisplayActivity.intent(ins.getTargetContext(),id);
            if(selected){
                ((List)field(FakeData.class,"ownedIds").get(null)).add(id);
                QueueCounter counter=FakeData.createCounter(id,"Counter 2");
                displaySlot=((Map)field(fixture.getClass(),"counters").get(fixture)).get(counter.getId());
                displayIntent=CounterDisplayActivity.intent(ins.getTargetContext(),id,counter.getId());
            }
            activity=ins.startActivitySync(displayIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK));idle();
            boolean portrait=!"landscape".equals(InstrumentationRegistry.getArguments().getString("displayOrientation"));
            // Selected-display checks exercise the manifest's natural orientation, without an override.
            if(!selected)ins.runOnMainSync(()->activity.setRequestedOrientation(portrait?ActivityInfo.SCREEN_ORIENTATION_PORTRAIT:ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));
            SystemClock.sleep(1000);idle();
            ins.runOnMainSync(()->{for(android.app.Activity a:ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED))if(a instanceof CounterDisplayActivity)activity=a;stopRefresh();});
            assertEquals(portrait?android.content.res.Configuration.ORIENTATION_PORTRAIT:android.content.res.Configuration.ORIENTATION_LANDSCAPE,activity.getResources().getConfiguration().orientation);
            show("Demo community service",23,new int[]{24,25,26});shots("demo");
            show(worst+" — "+worst,999999999,new int[]{999999997,999999998,999999999});shots("worst-unicode-large");
            show("",0,new int[]{0});shots("missing-name-zero");
            show("Demo community service",null,new int[]{});shots("empty-no-data");
        } finally {
            if(activity!=null)ins.runOnMainSync(()->{stopRefresh();activity.finish();});idle();
            ins.runOnMainSync(()->{try{for(Map.Entry<Field,List>e:lists.entrySet()){List l=(List)e.getKey().get(null);l.clear();l.addAll(e.getValue());}for(Map.Entry<Field,Map>e:maps.entrySet()){Map m=(Map)e.getKey().get(null);m.clear();m.putAll(e.getValue());}for(Map.Entry<Field,Object>e:flags.entrySet())e.getKey().set(null,e.getValue());}catch(Exception e){throw new AssertionError(e);}});
            for(int i=0;i<prefs.length;i++){if(!saved[i].equals(prefs[i].getAll()))restore(prefs[i],saved[i]);assertEquals(saved[i],prefs[i].getAll());}
            for(Map.Entry<Field,List>e:lists.entrySet())assertEquals(e.getValue(),e.getKey().get(null));
            for(Map.Entry<Field,Map>e:maps.entrySet())assertEquals(e.getValue(),e.getKey().get(null));
            for(Map.Entry<Field,Object>e:flags.entrySet())assertEquals(e.getValue(),e.getKey().get(null));
        }
    }


    void stopRefresh(){try{((Handler)field(CounterDisplayActivity.class,"handler").get(activity)).removeCallbacks((Runnable)field(CounterDisplayActivity.class,"refresh").get(activity));}catch(Exception e){throw new AssertionError(e);}}
    void show(String name,Integer number,int[] next){ins.runOnMainSync(()->{try{
        queue=queue.toBuilder().setName(name).build();((List)field(FakeData.class,"queues").get(null)).set(0,queue);
        waiting.clear();for(int n:next)waiting.add(ticket(n,"",null,false));
        field(displaySlot.getClass(),"nowServing").set(displaySlot,number==null?null:ticket(number,"",null,false));
        render();
        ((TextView)activity.findViewById(R.id.counter_scan)).setText(activity.getString(R.string.counter_scan,name));
        assertEquals(number==null?activity.getString(R.string.counter_none):activity.getString(R.string.ticket_number_format,number),((TextView)activity.findViewById(R.id.counter_number)).getText().toString());
        assertFalse((Boolean)field(fixture.getClass(),"nowServingTimedOut").get(fixture));
    }catch(Exception e){throw new AssertionError(e);}});idle();}
    @Override void render(){try{Method m=CounterDisplayActivity.class.getDeclaredMethod("render");m.setAccessible(true);m.invoke(activity);}catch(Exception e){throw new AssertionError(e);}}
    void shots(String name)throws Exception{
        reveal(R.id.counter_number);
        ins.runOnMainSync(()->{TextView number=activity.findViewById(R.id.counter_number);assertFull(number,"serving number");assertEquals(1,number.getLineCount());assertTrue("Number glyphs fit width",number.getLayout().getLineMax(0)<=number.getWidth()-number.getCompoundPaddingLeft()-number.getCompoundPaddingRight());assertTrue("Number glyphs fit height",number.getLayout().getHeight()<=number.getHeight()-number.getCompoundPaddingTop()-number.getCompoundPaddingBottom());});
        boolean landscape=activity.getResources().getConfiguration().orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE;
        if(landscape){
            ins.runOnMainSync(()->{for(int id:new int[]{R.id.counter_queue,R.id.counter_scan})scrollFor(id).scrollTo(0,0);});idle();
            if(!name.startsWith("worst"))ins.runOnMainSync(()->{for(int id:new int[]{R.id.counter_queue,R.id.counter_next_label,R.id.counter_next,R.id.counter_scan})assertFull(activity.findViewById(id),"initial "+name);});
            capture(name+"-initial");
        }
        for(int target:new int[]{R.id.counter_number,R.id.counter_queue,R.id.counter_next,R.id.counter_qr,R.id.counter_scan}){reveal(target);capture(name+"-"+activity.getResources().getResourceEntryName(target));}
        if(landscape&&name.startsWith("worst"))for(int id:new int[]{R.id.counter_next,R.id.counter_scan}){
            ins.runOnMainSync(()->scrollFor(id).fullScroll(android.view.View.FOCUS_DOWN));idle();
            ins.runOnMainSync(()->{android.view.View v=activity.findViewById(id);android.graphics.Rect visible=new android.graphics.Rect();assertTrue(v.getGlobalVisibleRect(visible));int[] xy=new int[2];v.getLocationOnScreen(xy);assertEquals("long text end reachable",xy[1]+v.getHeight(),visible.bottom);});
            capture(name+"-end-"+activity.getResources().getResourceEntryName(id));
        }
    }
    androidx.core.widget.NestedScrollView scrollFor(int id){android.view.ViewParent p=activity.findViewById(id).getParent();while(!(p instanceof androidx.core.widget.NestedScrollView))p=p.getParent();return (androidx.core.widget.NestedScrollView)p;}
    void assertFull(android.view.View v,String state){android.graphics.Rect r=new android.graphics.Rect();assertTrue(state+" visible",v.getGlobalVisibleRect(r));assertEquals(state+" full width",v.getWidth(),r.width());assertEquals(state+" full height",v.getHeight(),r.height());}
    @Override void capture(String state)throws Exception{
        idle();File dir=new File(ins.getTargetContext().getExternalFilesDir(null),InstrumentationRegistry.getArguments().getString("captureFolder","phase3d-repair-display"));assertTrue(dir.isDirectory()||dir.mkdirs());
        android.content.res.Configuration c=activity.getResources().getConfiguration();String config=c.screenWidthDp+"x"+c.screenHeightDp+"-"+c.fontScale;
        ins.runOnMainSync(()->{try(FileWriter out=new FileWriter(new File(dir,"geometry-"+config+".log"),true)){
            out.write(state+"\n");android.graphics.Rect number=new android.graphics.Rect(),qr=new android.graphics.Rect();
            for(int id:new int[]{R.id.counter_number,R.id.counter_qr,R.id.counter_qr_plate,R.id.counter_instruction,R.id.counter_queue,R.id.counter_next_label,R.id.counter_next,R.id.counter_scan}){
                android.view.View v=activity.findViewById(id);android.graphics.Rect rect=new android.graphics.Rect();boolean visible=v.getGlobalVisibleRect(rect);
                out.write(activity.getResources().getResourceEntryName(id)+" size="+v.getWidth()+"x"+v.getHeight()+" visible="+visible+":"+rect);
                if(v instanceof TextView){TextView text=(TextView)v;out.write(" textPx="+text.getTextSize()+" lines="+text.getLineCount());}
                out.write("\n");
                if(c.orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE&&(id==R.id.counter_number||id==R.id.counter_qr||id==R.id.counter_qr_plate)){
                    assertTrue(state+" critical view visible",visible);assertEquals(state+" full height",v.getHeight(),rect.height());assertEquals(state+" full width",v.getWidth(),rect.width());
                    if(id==R.id.counter_number){number.set(rect);TextView text=(TextView)v;assertEquals("Number stays on one line",1,text.getLineCount());assertTrue("Number text height fits",text.getLayout().getHeight()<=text.getHeight()-text.getCompoundPaddingTop()-text.getCompoundPaddingBottom());}
                    if(id==R.id.counter_qr){qr.set(rect);int expected=activity.getResources().getDimensionPixelSize(R.dimen.counter_qr_size);assertEquals("QR width retained",expected,v.getWidth());assertEquals("QR height retained",expected,v.getHeight());}
                }
            }
            if(c.orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE)assertFalse(state+" number/QR overlap",android.graphics.Rect.intersects(number,qr));
        }catch(Exception e){throw new AssertionError(e);}});
        Bitmap b=ins.getUiAutomation().takeScreenshot();assertNotNull(b);try(FileOutputStream out=new FileOutputStream(new File(dir,state+"-"+config+".png"))){assertTrue(b.compress(Bitmap.CompressFormat.PNG,100,out));}finally{b.recycle();}
    }
}
