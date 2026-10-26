package com.example.quapp;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;
import org.junit.Test;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.util.LinkedHashMap;
import java.util.Map;
import static org.junit.Assert.*;

/** Presentation only; Cancel is tapped, but no production submit callback is installed. */
public class WalkInCancelVisualReviewTest extends ConsoleSheetsVisualReviewTest {
    @Override @Test @SuppressWarnings("unchecked") public void reviewConsole() throws Exception {
        SharedPreferences[] prefs={ins.getTargetContext().getSharedPreferences("quapp_session",0),ins.getTargetContext().getSharedPreferences("quapp_device",0)};
        Map<String,?>[] saved=new Map[]{new LinkedHashMap<>(prefs[0].getAll()),new LinkedHashMap<>(prefs[1].getAll())};
        try {
            String queueId=FakeData.queues().get(0).getId();
            activity=ins.startActivitySync(ShareQueueActivity.intent(ins.getTargetContext(),queueId).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));idle();
            boolean landscape="landscape".equals(InstrumentationRegistry.getArguments().getString("displayOrientation"));
            ins.runOnMainSync(()->activity.setRequestedOrientation(landscape?ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE:ActivityInfo.SCREEN_ORIENTATION_PORTRAIT));
            SystemClock.sleep(1000);idle();
            ins.runOnMainSync(()->{for(android.app.Activity candidate:ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED))if(candidate instanceof ShareQueueActivity)activity=candidate;});
            assertEquals(landscape?android.content.res.Configuration.ORIENTATION_LANDSCAPE:android.content.res.Configuration.ORIENTATION_PORTRAIT,activity.getResources().getConfiguration().orientation);
            String[] names={"",worst+" — "+worst,"0",""};
            String[] states={"empty","unicode-long","zero","missing-error"};
            for(int i=0;i<names.length;i++){
                final String name=names[i];final boolean error=i==3;
                ins.runOnMainSync(()->{
                    root=activity.getLayoutInflater().inflate(R.layout.dialog_walk_in,null);
                    ((EditText)root.findViewById(R.id.walk_in_name_input)).setText(name);
                    if(error)((TextInputLayout)root.findViewById(R.id.walk_in_name_layout)).setError(activity.getString(R.string.console_walk_in_error));
                    walk=new MaterialAlertDialogBuilder(activity,R.style.ThemeOverlay_Quapp_Dialog_WalkIn)
                        .setTitle(R.string.console_walk_in_title).setView(root)
                        .setNegativeButton(R.string.console_cancel,null).setPositiveButton(R.string.console_walk_in_confirm,(d,w)->fail("submit operation")).show();
                });
                capture(states[i]+"-top");
                reveal(R.id.walk_in_name_input);capture(states[i]+"-field");
                if(error){reveal(R.id.walk_in_name_layout);capture(states[i]+"-helper");}
                final int[] point=new int[2];
                ins.runOnMainSync(()->{Button cancel=walk.getButton(AlertDialog.BUTTON_NEGATIVE);int[] xy=new int[2];cancel.getLocationOnScreen(xy);point[0]=xy[0]+cancel.getWidth()/2;point[1]=xy[1]+cancel.getHeight()/2;});
                long now=SystemClock.uptimeMillis();
                MotionEvent down=MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,point[0],point[1],0);
                MotionEvent up=MotionEvent.obtain(now,now+50,MotionEvent.ACTION_UP,point[0],point[1],0);
                try{ins.sendPointerSync(down);ins.sendPointerSync(up);}finally{down.recycle();up.recycle();}
                idle();ins.runOnMainSync(()->assertFalse("Cancel tap dismisses dialog",walk.isShowing()));
            }
        } finally {
            if(activity!=null){ins.runOnMainSync(()->{if(walk!=null)walk.dismiss();activity.finish();});idle();}
            for(int i=0;i<prefs.length;i++){if(!saved[i].equals(prefs[i].getAll()))restore(prefs[i],saved[i]);assertEquals(saved[i],prefs[i].getAll());}
        }
    }
    @Override void capture(String state)throws Exception {
        idle();SystemClock.sleep(250);
        File dir=new File(ins.getTargetContext().getExternalFilesDir(null),"phase3d-repair-walkin");assertTrue(dir.isDirectory()||dir.mkdirs());
        android.content.res.Configuration c=activity.getResources().getConfiguration();String config=c.screenWidthDp+"x"+c.screenHeightDp+"-"+c.fontScale;
        StringBuilder geometry=new StringBuilder(state+" "+config+"\n");
        ins.runOnMainSync(()->{
            for(int which:new int[]{AlertDialog.BUTTON_NEGATIVE,AlertDialog.BUTTON_POSITIVE}){
                Button b=walk.getButton(which);Rect r=new Rect();assertTrue(b.getGlobalVisibleRect(r));
                geometry.append(b.getText()).append(" full=").append(b.getWidth()).append('x').append(b.getHeight()).append(" visible=").append(r).append(" lines=").append(b.getLineCount()).append('\n');
                assertEquals("full action width",b.getWidth(),r.width());assertEquals("full action height",b.getHeight(),r.height());
                assertTrue("touch height",b.getHeight()>=activity.getResources().getDimensionPixelSize(R.dimen.touch_target_min));
                assertTrue("touch width",b.getWidth()>=activity.getResources().getDimensionPixelSize(R.dimen.touch_target_min));
                assertTrue(b.isClickable());assertTrue(b.isEnabled());assertNotNull(b.getLayout());
                assertTrue("label height fits",b.getLayout().getHeight()<=b.getHeight()-b.getCompoundPaddingTop()-b.getCompoundPaddingBottom());
                for(int line=0;line<b.getLineCount();line++)assertTrue("label width fits",b.getLayout().getLineWidth(line)<=b.getWidth()-b.getCompoundPaddingLeft()-b.getCompoundPaddingRight()+1);
            }
        });
        try(FileWriter out=new FileWriter(new File(dir,"geometry-"+config+".log"),true)){out.write(geometry.toString());}
        Bitmap bitmap=ins.getUiAutomation().takeScreenshot();assertNotNull(bitmap);
        try(FileOutputStream out=new FileOutputStream(new File(dir,state+"-"+config+".png"))){assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,out));}finally{bitmap.recycle();}
    }
}
