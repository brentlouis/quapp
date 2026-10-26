package com.example.quapp;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.SystemClock;
import android.view.View;
import android.widget.TextView;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;
import org.junit.Test;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import static org.junit.Assert.*;

/** Read-only native analytics binders with isolated presentation fixtures; no operations. */
public class InsightsVisualReviewTest extends LiveConsoleVisualReviewTest {
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
            queue=FakeData.queues().get(0);
            activity=ins.startActivitySync(new Intent(ins.getTargetContext(),QueueAnalyticsActivity.class)
                .putExtra(QueueAnalyticsActivity.EXTRA_QUEUE_ID,queue.getId()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            idle();
            boolean portrait=!"landscape".equals(InstrumentationRegistry.getArguments().getString("displayOrientation"));
            ins.runOnMainSync(()->activity.setRequestedOrientation(portrait?ActivityInfo.SCREEN_ORIENTATION_PORTRAIT:ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));
            SystemClock.sleep(1000);idle();
            ins.runOnMainSync(()->{for(android.app.Activity a:ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED))if(a instanceof QueueAnalyticsActivity)activity=a;});
            assertEquals(portrait?android.content.res.Configuration.ORIENTATION_PORTRAIT:android.content.res.Configuration.ORIENTATION_LANDSCAPE,activity.getResources().getConfiguration().orientation);
            bind("Demo service",new QueueStats(20,2,8,1.3,5,11,QueueStats.EstimateSource.MODEL,142));
            capture("demo-top");reveal(R.id.analytics_slip);capture("demo-today");
            bind(worst+" — "+worst,new QueueStats(999999999,999999999,999999999,12345678.9,999999999,999999999,QueueStats.EstimateSource.MODEL,999999999));
            reveal(R.id.analytics_queue_name);capture("worst-title");reveal(R.id.analytics_projected_value);capture("worst-forecast");
            reveal(R.id.analytics_average_basis);capture("worst-basis");reveal(R.id.analytics_source);capture("worst-source");
            reveal(R.id.analytics_slip);capture("worst-today");
            countShots("worst");
            bind("",new QueueStats(0,0,0,0,0,0,QueueStats.EstimateSource.ROLLING_AVERAGE,0));
            reveal(R.id.analytics_queue_name);capture("missing-zero-top");reveal(R.id.analytics_slip);capture("empty-no-data");
            bind(null,new QueueStats(0,0,0,0,0,0,QueueStats.EstimateSource.ROLLING_AVERAGE,0));capture("null-name");
            bind(" \t ",new QueueStats(0,0,0,0,0,0,QueueStats.EstimateSource.ROLLING_AVERAGE,0));capture("blank-name");
            ins.runOnMainSync(()->((LoadingStateHelper)get("loadingStateHelper")).showLoading(activity.getString(R.string.loading_analytics)));
            reveal(R.id.analytics_loading);capture("loading");
            assertState(R.id.analytics_loading);
            ins.runOnMainSync(()->((LoadingStateHelper)get("loadingStateHelper")).showError(activity.getString(R.string.error_default_message)));
            reveal(R.id.analytics_error);capture("error");
            assertState(R.id.analytics_error);
            ins.runOnMainSync(()->((LoadingStateHelper)get("loadingStateHelper")).showContent());idle();
            assertState(R.id.analytics_content);
            if("true".equals(InstrumentationRegistry.getArguments().getString("leaveScreen"))){
                bind("Demo service",new QueueStats(20,2,8,1.3,5,11,QueueStats.EstimateSource.MODEL,142));
                reveal(R.id.analytics_queue_name);capture("preview-normal");
            }
        } finally {
            if(activity!=null&&!"true".equals(InstrumentationRegistry.getArguments().getString("leaveScreen")))ins.runOnMainSync(()->activity.finish());
            idle();
            ins.runOnMainSync(()->{try{for(Map.Entry<Field,List>e:lists.entrySet()){List l=(List)e.getKey().get(null);l.clear();l.addAll(e.getValue());}for(Map.Entry<Field,Map>e:maps.entrySet()){Map m=(Map)e.getKey().get(null);m.clear();m.putAll(e.getValue());}for(Map.Entry<Field,Object>e:flags.entrySet())e.getKey().set(null,e.getValue());}catch(Exception e){throw new AssertionError(e);}});
            for(int i=0;i<prefs.length;i++){if(!saved[i].equals(prefs[i].getAll()))restore(prefs[i],saved[i]);assertEquals(saved[i],prefs[i].getAll());}
            for(Map.Entry<Field,List>e:lists.entrySet())assertEquals(e.getValue(),e.getKey().get(null));
            for(Map.Entry<Field,Map>e:maps.entrySet())assertEquals(e.getValue(),e.getKey().get(null));
            for(Map.Entry<Field,Object>e:flags.entrySet())assertEquals(e.getValue(),e.getKey().get(null));
        }
    }

    Object get(String name){try{return field(QueueAnalyticsActivity.class,name).get(activity);}catch(Exception e){throw new AssertionError(e);}}
    void bind(String name,QueueStats stats){ins.runOnMainSync(()->{try{
        ((LoadingStateHelper)get("loadingStateHelper")).showContent();
        Method queueNameBinder=QueueAnalyticsActivity.class.getDeclaredMethod("bindQueueName",String.class);queueNameBinder.setAccessible(true);queueNameBinder.invoke(activity,name);
        assertEquals(name==null||name.trim().isEmpty()?activity.getString(R.string.analytics_today_label):activity.getString(R.string.analytics_subtitle_format,name),((TextView)activity.findViewById(R.id.analytics_queue_name)).getText().toString());
        for(String method:new String[]{"bindForecast","bindToday"}){Method m=QueueAnalyticsActivity.class.getDeclaredMethod(method,QueueStats.class);m.setAccessible(true);m.invoke(activity,stats);}
        assertEquals(activity.getString(R.string.analytics_projected_format,stats.getProjectedWaitMinutes()),((TextView)activity.findViewById(R.id.analytics_projected_value)).getText().toString());
        assertEquals(4,((android.widget.LinearLayout)activity.findViewById(R.id.analytics_slip)).getChildCount());
    }catch(Exception e){throw new AssertionError(e);}});idle();
        assertState(R.id.analytics_content);
        reveal(R.id.analytics_queue_name);
        ins.runOnMainSync(()->{
            TextView subtitle=activity.findViewById(R.id.analytics_queue_name);assertTextFits(subtitle);
            View content=activity.findViewById(R.id.analytics_content);assertTrue("Scrollable metrics retain a viewport",content.getHeight()>0);
            Rect subtitleVisible=new Rect();assertTrue(subtitle.getGlobalVisibleRect(subtitleVisible));
            assertEquals("Complete subtitle width",subtitle.getWidth(),subtitleVisible.width());assertEquals("Complete fixture subtitle height",subtitle.getHeight(),subtitleVisible.height());
            TextView forecast=activity.findViewById(R.id.analytics_projected_value);assertTextFits(forecast);assertEquals("Forecast must remain a complete number",1,forecast.getLineCount());
            android.widget.LinearLayout slip=activity.findViewById(R.id.analytics_slip);
            for(int i=0;i<slip.getChildCount();i++)for(int id:new int[]{R.id.slip_label,R.id.slip_value})assertTextFits(slip.getChildAt(i).findViewById(id));
        });
    }
    void assertState(int visible){ins.runOnMainSync(()->{for(int id:new int[]{R.id.analytics_content,R.id.analytics_loading,R.id.analytics_error})assertEquals("Exclusive analytics state",id==visible?View.VISIBLE:View.GONE,activity.findViewById(id).getVisibility());});}
    void assertTextFits(TextView text){
        android.text.Layout layout=text.getLayout();assertNotNull(layout);
        assertTrue("Text height must fit: "+text.getText(),layout.getHeight()<=text.getHeight()-text.getCompoundPaddingTop()-text.getCompoundPaddingBottom());
        for(int line=0;line<layout.getLineCount();line++){assertEquals("No ellipsis",0,layout.getEllipsisCount(line));assertTrue("Text width must fit: "+text.getText(),layout.getLineWidth(line)<=layout.getWidth()+1);}
    }
    void countShots(String state)throws Exception{
        android.widget.LinearLayout slip=activity.findViewById(R.id.analytics_slip);
        for(int i=0;i<slip.getChildCount();i++){
            final View row=slip.getChildAt(i);ins.runOnMainSync(()->row.requestRectangleOnScreen(new Rect(0,0,row.getWidth(),row.getHeight()),true));idle();
            ins.runOnMainSync(()->{Rect visible=new Rect();assertTrue(row.getGlobalVisibleRect(visible));assertEquals("Complete count row width",row.getWidth(),visible.width());assertEquals("Complete count row height",row.getHeight(),visible.height());});
            capture(state+"-count-"+i);
        }
    }
    @Override void capture(String state)throws Exception{idle();Bitmap b=ins.getUiAutomation().takeScreenshot();assertNotNull(b);File dir=new File(ins.getTargetContext().getExternalFilesDir(null),"phase3-insights-repair");assertTrue(dir.isDirectory()||dir.mkdirs());android.content.res.Configuration c=activity.getResources().getConfiguration();try(FileOutputStream out=new FileOutputStream(new File(dir,state+"-"+c.screenWidthDp+"x"+c.screenHeightDp+"-"+c.fontScale+".png"))){assertTrue(b.compress(Bitmap.CompressFormat.PNG,100,out));}finally{b.recycle();}}
}
