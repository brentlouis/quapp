package com.example.quapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.SystemClock;
import android.widget.TextView;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;
import com.google.android.material.snackbar.Snackbar;
import org.junit.Test;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import static org.junit.Assert.*;

/** Presentation only: invokes the real feedback factory without operational callbacks. */
public class ConsoleSnackbarVisualReviewTest extends LiveConsoleVisualReviewTest {
    private Snackbar snackbar;
    @Override @Test @SuppressWarnings({"unchecked","rawtypes"}) public void reviewConsole() throws Exception {
        FakeData.queues();
        Map<Field,List> lists=new LinkedHashMap<>();
        for(String n:new String[]{"queues","ownedIds","history"}){Field f=field(FakeData.class,n);lists.put(f,new ArrayList((List)f.get(null)));}
        Field held=field(ActiveTicketStore.class,"held");lists.put(held,new ArrayList((List)held.get(null)));
        Map<Field,Map> maps=new LinkedHashMap<>();
        for(String n:new String[]{"live","finished"}){Field f=field(FakeData.class,n);maps.put(f,new LinkedHashMap((Map)f.get(null)));}
        Map<Field,Object> flags=new LinkedHashMap<>();
        for(String n:new String[]{"debugMode","debugDelayMs","debugFailOnce","debugEmptyOnce","nextQueueNumber"}){Field f=field(FakeData.class,n);flags.put(f,f.get(null));}
        SharedPreferences[] prefs={ins.getTargetContext().getSharedPreferences("quapp_session",0),ins.getTargetContext().getSharedPreferences("quapp_device",0)};
        Map<String,?>[] saved=new Map[]{new LinkedHashMap<>(prefs[0].getAll()),new LinkedHashMap<>(prefs[1].getAll())};
        try {
            queue=FakeData.queues().get(0).toBuilder().setId(id).setName("Demo service").setGracePeriodEnabled(false).build();
            Constructor<?> c=Class.forName("com.example.quapp.FakeData$LiveQueue").getDeclaredConstructor();c.setAccessible(true);fixture=c.newInstance();
            ins.runOnMainSync(()->{try{List qs=(List)field(FakeData.class,"queues").get(null);qs.clear();qs.add(queue);Map live=(Map)field(FakeData.class,"live").get(null);live.clear();live.put(id,fixture);}catch(Exception e){throw new AssertionError(e);}});
            activity=ins.startActivitySync(new Intent(ins.getTargetContext(),LiveConsoleActivity.class).putExtra(LiveConsoleActivity.EXTRA_QUEUE_ID,id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));idle();
            boolean landscape="landscape".equals(InstrumentationRegistry.getArguments().getString("displayOrientation"));
            ins.runOnMainSync(()->activity.setRequestedOrientation(landscape?ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE:ActivityInfo.SCREEN_ORIENTATION_PORTRAIT));
            SystemClock.sleep(1000);idle();
            ins.runOnMainSync(()->{for(android.app.Activity a:ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED))if(a instanceof LiveConsoleActivity)activity=a;});
            assertEquals(landscape?android.content.res.Configuration.ORIENTATION_LANDSCAPE:android.content.res.Configuration.ORIENTATION_PORTRAIT,activity.getResources().getConfiguration().orientation);
            if(activity.getResources().getConfiguration().fontScale>=2f){
                ins.runOnMainSync(()->{snackbar=Snackbar.make(activity.findViewById(R.id.console_list),R.string.error_default_message,Snackbar.LENGTH_INDEFINITE);snackbar.show();});
                capture("baseline-default-error",false);dismiss();
            }
            String[] messages={activity.getString(R.string.error_default_message),activity.getString(R.string.console_marked_served,23,"Jo"),activity.getString(R.string.console_walk_in_added,999999999,worst),activity.getString(R.string.console_walk_in_added,0,"")};
            String[] names={"default-error","normal-served","unicode-long","zero-missing"};
            Method factory=LiveConsoleActivity.class.getDeclaredMethod("feedback",CharSequence.class);factory.setAccessible(true);
            for(int i=0;i<messages.length;i++){
                final String message=messages[i];final boolean anchored=i>0;
                if(anchored)reveal(R.id.console_call_next);
                ins.runOnMainSync(()->{try{
                    snackbar=(Snackbar)factory.invoke(activity,message);assertEquals(Snackbar.LENGTH_SHORT,field(com.google.android.material.snackbar.BaseTransientBottomBar.class,"duration").getInt(snackbar));
                    if(anchored)snackbar.setAnchorView(R.id.console_tear);
                    // Hold only the fixture for screenshots; production duration remains unchanged.
                    snackbar.setDuration(Snackbar.LENGTH_INDEFINITE).show();
                }catch(Exception e){throw new AssertionError(e);}});
                capture(names[i],true);dismiss();
            }
        } finally {
            dismiss();
            if(activity!=null){ins.runOnMainSync(activity::finish);idle();}
            ins.runOnMainSync(()->{try{for(Map.Entry<Field,List>e:lists.entrySet()){List l=(List)e.getKey().get(null);l.clear();l.addAll(e.getValue());}for(Map.Entry<Field,Map>e:maps.entrySet()){Map m=(Map)e.getKey().get(null);m.clear();m.putAll(e.getValue());}for(Map.Entry<Field,Object>e:flags.entrySet())e.getKey().set(null,e.getValue());}catch(Exception e){throw new AssertionError(e);}});
            for(int i=0;i<prefs.length;i++){if(!saved[i].equals(prefs[i].getAll()))restore(prefs[i],saved[i]);assertEquals(saved[i],prefs[i].getAll());}
            for(Map.Entry<Field,List>e:lists.entrySet())assertEquals(e.getValue(),e.getKey().get(null));
            for(Map.Entry<Field,Map>e:maps.entrySet())assertEquals(e.getValue(),e.getKey().get(null));
            for(Map.Entry<Field,Object>e:flags.entrySet())assertEquals(e.getValue(),e.getKey().get(null));
        }
    }
    private void dismiss(){if(snackbar!=null){ins.runOnMainSync(()->snackbar.dismiss());SystemClock.sleep(350);idle();snackbar=null;}}
    private void capture(String state,boolean repaired)throws Exception {
        idle();SystemClock.sleep(400);
        File dir=new File(ins.getTargetContext().getExternalFilesDir(null),"phase3-console-repair");assertTrue(dir.isDirectory()||dir.mkdirs());
        android.content.res.Configuration c=activity.getResources().getConfiguration();String config=c.screenWidthDp+"x"+c.screenHeightDp+"-"+c.fontScale;
        StringBuilder geometry=new StringBuilder(state+" "+config+"\n");
        ins.runOnMainSync(()->{
            TextView text=snackbar.getView().findViewById(com.google.android.material.R.id.snackbar_text);Rect visible=new Rect();
            assertNotNull(text.getLayout());assertTrue(text.getGlobalVisibleRect(visible));int lines=text.getLineCount();
            int end=lines==0?0:text.getLayout().getLineEnd(lines-1);boolean ellipsis=false;
            for(int line=0;line<lines;line++)ellipsis|=text.getLayout().getEllipsisCount(line)>0;
            geometry.append("length=").append(text.length()).append(" lastLineEnd=").append(end).append(" lines=").append(lines).append(" ellipsis=").append(ellipsis).append(" full=").append(text.getWidth()).append('x').append(text.getHeight()).append(" visible=").append(visible).append('\n');
            if(repaired){
                assertEquals("all characters laid out",text.length(),end);assertFalse("no ellipsis",ellipsis);
                assertEquals("full text width visible",text.getWidth(),visible.width());assertEquals("full text height visible",text.getHeight(),visible.height());
                assertTrue("text height fits",text.getLayout().getHeight()<=text.getHeight()-text.getCompoundPaddingTop()-text.getCompoundPaddingBottom());
                for(int line=0;line<lines;line++){geometry.append("line=").append(line).append(" width=").append(text.getLayout().getLineWidth(line)).append(" max=").append(text.getLayout().getLineMax(line)).append('\n');assertTrue("drawn line width fits",text.getLayout().getLineMax(line)<=text.getWidth()-text.getCompoundPaddingLeft()-text.getCompoundPaddingRight()+1);}
            }
        });
        try(FileWriter out=new FileWriter(new File(dir,"geometry-"+config+".log"),true)){out.write(geometry.toString());}
        Bitmap bitmap=ins.getUiAutomation().takeScreenshot();assertNotNull(bitmap);
        try(FileOutputStream out=new FileOutputStream(new File(dir,state+"-"+config+".png"))){assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,out));}finally{bitmap.recycle();}
    }
}
