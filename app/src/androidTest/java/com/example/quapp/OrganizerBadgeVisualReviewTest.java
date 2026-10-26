package com.example.quapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.view.View;
import android.view.inspector.WindowInspector;
import android.widget.TextView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import org.junit.Test;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import static org.junit.Assert.*;

/** Shared notice presentation fixtures only; never activates report or verification actions. */
public class OrganizerBadgeVisualReviewTest extends ConsoleSheetsVisualReviewTest {
    @Override @Test @SuppressWarnings({"rawtypes","unchecked"}) public void reviewConsole() throws Exception {
        FakeData.queues();
        Map<Field,Object> flags=new LinkedHashMap<>();
        for(String n:new String[]{"debugMode","debugDelayMs","debugFailOnce","debugEmptyOnce","nextQueueNumber","myVerification","myRequest","counterListener"}){Field f=field(FakeData.class,n);flags.put(f,f.get(null));}
        Map<Field,List> lists=new LinkedHashMap<>();
        for(String n:new String[]{"queues","ownedIds","history"}){Field f=field(FakeData.class,n);lists.put(f,new ArrayList((List)f.get(null)));}
        Field held=field(ActiveTicketStore.class,"held");lists.put(held,new ArrayList((List)held.get(null)));
        Map<Field,Map> maps=new LinkedHashMap<>();
        for(String n:new String[]{"live","finished"}){Field f=field(FakeData.class,n);maps.put(f,new LinkedHashMap((Map)f.get(null)));}
        android.content.SharedPreferences[] prefs={ins.getTargetContext().getSharedPreferences("quapp_session",0),ins.getTargetContext().getSharedPreferences("quapp_device",0)};
        Map<String,?>[] saved=new Map[]{new LinkedHashMap<>(prefs[0].getAll()),new LinkedHashMap<>(prefs[1].getAll())};

        try {
            queue=FakeData.queues().get(0).toBuilder().setId(id).setName("Demo community service").setGracePeriodEnabled(false).build();
            Constructor<?> constructor=Class.forName("com.example.quapp.FakeData$LiveQueue").getDeclaredConstructor();constructor.setAccessible(true);fixture=constructor.newInstance();
            ins.runOnMainSync(()->{try{List qs=(List)field(FakeData.class,"queues").get(null);qs.clear();qs.add(queue);Map live=(Map)field(FakeData.class,"live").get(null);live.clear();live.put(id,fixture);}catch(Exception e){throw new AssertionError(e);}});
            activity=ins.startActivitySync(new Intent(ins.getTargetContext(),QueueDetailActivity.class).putExtra(QueueDetailActivity.EXTRA_QUEUE_ID,id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));idle();
            for(boolean verified:new boolean[]{true,false})for(String name:new String[]{"Demo organizer",worst,"",null," \t "}){
                final Queue q=queue.toBuilder().setOrganizer("visual-owner",name,verified).build();
                ins.runOnMainSync(()->TrustSheets.showOrganizer(activity,q));notice();
                String expectedName=name==null||name.trim().isEmpty()?activity.getString(R.string.badge_organizer_fallback):name;
                assertTrue(((TextView)root.findViewById(R.id.notice_body)).getText().toString().contains(expectedName));
                String label=(verified?"verified":"unverified")+(name==null?"-null":name.isEmpty()?"-missing":name.trim().isEmpty()?"-blank":name.equals(worst)?"-unicode-long":"-default");
                capture(label);dismissNotice();
            }
            ins.runOnMainSync(()->TrustSheets.showOrganizer(activity,queue.toBuilder().setOrganizer("visual-owner",worst,false).build()));notice();
            ins.runOnMainSync(()->((TextView)root.findViewById(R.id.notice_body)).setText((worst+". "+activity.getString(R.string.badge_unverified_note)+" "+activity.getString(R.string.badge_verified_body,worst)+" ").repeat(4)));
            capture("long-explanation-fixture");dismissNotice();
            for(boolean console:new boolean[]{false,true}){
                ins.runOnMainSync(()->activity.finish());idle();
                activity=ins.startActivitySync(console?new Intent(ins.getTargetContext(),LiveConsoleActivity.class).putExtra(LiveConsoleActivity.EXTRA_QUEUE_ID,id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK):new Intent(ins.getTargetContext(),CreateQueueActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));idle();
                for(Queue.Status status:new Queue.Status[]{Queue.Status.OPEN,Queue.Status.PAUSED})for(boolean pending:new boolean[]{false,true}){
                    final Queue q=queue.toBuilder().setStatus(status).setName(worst).build();
                    ins.runOnMainSync(()->{try{field(FakeData.class,"myVerification").set(null,pending?VerificationStatus.PENDING:VerificationStatus.NONE);}catch(Exception e){throw new AssertionError(e);}TrustSheets.showOneLiveQueue(activity,q);});notice();
                    assertEquals(!pending,root.findViewById(R.id.notice_primary).isEnabled());
                    assertEquals(View.GONE,root.findViewById(R.id.notice_note).getVisibility());
                    capture((console?"console":"create")+"-limit-"+status+"-"+(pending?"pending":"unverified"));dismissNotice();
                }
            }
        } finally {
            if(activity!=null)ins.runOnMainSync(()->{for(View v:WindowInspector.getGlobalWindowViews()){View sheet=v.findViewById(com.google.android.material.R.id.design_bottom_sheet);if(sheet!=null)BottomSheetBehavior.from(sheet).setState(BottomSheetBehavior.STATE_HIDDEN);}activity.finish();});idle();
            ins.runOnMainSync(()->{try{for(Map.Entry<Field,List>e:lists.entrySet()){List l=(List)e.getKey().get(null);l.clear();l.addAll(e.getValue());}for(Map.Entry<Field,Map>e:maps.entrySet()){Map m=(Map)e.getKey().get(null);m.clear();m.putAll(e.getValue());}for(Map.Entry<Field,Object>e:flags.entrySet())e.getKey().set(null,e.getValue());}catch(Exception e){throw new AssertionError(e);}});
            for(int i=0;i<prefs.length;i++){if(!saved[i].equals(prefs[i].getAll()))restore(prefs[i],saved[i]);assertEquals(saved[i],prefs[i].getAll());}
            for(Map.Entry<Field,List>e:lists.entrySet())assertEquals(e.getValue(),e.getKey().get(null));
            for(Map.Entry<Field,Map>e:maps.entrySet())assertEquals(e.getValue(),e.getKey().get(null));
            for(Map.Entry<Field,Object>e:flags.entrySet())assertEquals(e.getValue(),e.getKey().get(null));
        }
    }

    void notice(){idle();ins.runOnMainSync(()->{root=null;for(View v:WindowInspector.getGlobalWindowViews())if(v.findViewById(R.id.notice_title)!=null)root=v;assertNotNull(root);BottomSheetBehavior.from(root.findViewById(com.google.android.material.R.id.design_bottom_sheet)).setState(BottomSheetBehavior.STATE_EXPANDED);for(int id:new int[]{R.id.notice_icon,R.id.notice_title,R.id.notice_body,R.id.notice_note,R.id.notice_primary,R.id.notice_secondary})assertNotNull(root.findViewById(id));});idle();}
    void dismissNotice(){ins.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK);idle();}
    @Override void capture(String state)throws Exception{
        idle();androidx.core.widget.NestedScrollView scroll=root.findViewById(R.id.notice_scroll);
        assertNotNull(scroll);ins.runOnMainSync(()->scroll.scrollTo(0,0));idle();
        final boolean[] overflow={false};
        ins.runOnMainSync(()->overflow[0]=scroll.canScrollVertically(1));
        if(state.equals("long-explanation-fixture"))assertTrue("Long explanation must overflow bounded viewport",overflow[0]);
        capturePosition(state+"-top");
        if(overflow[0]){
            ins.runOnMainSync(()->scroll.fullScroll(View.FOCUS_DOWN));idle();
            ins.runOnMainSync(()->{assertTrue("Content must move",scroll.getScrollY()>0);assertFalse("End of content reachable",scroll.canScrollVertically(1));});
            capturePosition(state+"-bottom");
        }
    }
    void capturePosition(String state)throws Exception{
        idle();File dir=new File(ins.getTargetContext().getExternalFilesDir(null),"phase3e-scroll-review");assertTrue(dir.isDirectory()||dir.mkdirs());
        android.content.res.Configuration c=activity.getResources().getConfiguration();String config=c.screenWidthDp+"x"+c.screenHeightDp+"-"+c.fontScale;
        ins.runOnMainSync(()->{try(FileWriter out=new FileWriter(new File(dir,"geometry-"+config+".log"),true)){
            androidx.core.widget.NestedScrollView scroll=root.findViewById(R.id.notice_scroll);
            out.write(state+" scrollY="+scroll.getScrollY()+" viewport="+scroll.getHeight()+"\n");
            int minimum=activity.getResources().getDimensionPixelSize(R.dimen.touch_target_min);
            for(int id:new int[]{R.id.notice_title,R.id.notice_body,R.id.notice_note,R.id.notice_primary,R.id.notice_secondary}){
                TextView v=root.findViewById(id);Rect visible=new Rect();boolean shown=v.getGlobalVisibleRect(visible);
                int ellipses=0;if(v.getLayout()!=null)for(int line=0;line<v.getLayout().getLineCount();line++)ellipses+=v.getLayout().getEllipsisCount(line);
                out.write(activity.getResources().getResourceEntryName(id)+" size="+v.getWidth()+"x"+v.getHeight()+" visible="+shown+":"+visible+" lines="+v.getLineCount()+" ellipses="+ellipses+" enabled="+v.isEnabled()+"\n");
                if((id==R.id.notice_primary||id==R.id.notice_secondary)&&v.getVisibility()==View.VISIBLE){
                    assertTrue(state+" action visible",shown);assertTrue(state+" action touch height",v.getHeight()>=minimum);
                    assertEquals(state+" full action height visible",v.getHeight(),visible.height());
                    assertTrue(state+" action touch width",visible.width()>=minimum);assertEquals(state+" no action ellipsis",0,ellipses);
                }
            }
        }catch(Exception e){throw new AssertionError(e);}});
        Bitmap b=ins.getUiAutomation().takeScreenshot();assertNotNull(b);try(FileOutputStream out=new FileOutputStream(new File(dir,state+"-"+config+".png"))){assertTrue(b.compress(Bitmap.CompressFormat.PNG,100,out));}finally{b.recycle();}
    }
}
