package com.example.quapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.SystemClock;
import android.view.View;
import android.view.inspector.WindowInspector;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;
import org.junit.Test;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.*;
import static org.junit.Assert.*;

/** Presentation fixtures only. No operation listener may run. Reuses capture/isolation helpers. */
public class ConsoleSheetsVisualReviewTest extends LiveConsoleVisualReviewTest {
    View root;
    AlertDialog walk;
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
            queue=FakeData.queues().get(0).toBuilder().setName(worst).setStatus(Queue.Status.OPEN).build();
            activity=ins.startActivitySync(ShareQueueActivity.intent(ins.getTargetContext(),queue.getId()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));idle();
            ins.runOnMainSync(()->{
                root=activity.getLayoutInflater().inflate(R.layout.dialog_walk_in,null);
                walk=new MaterialAlertDialogBuilder(activity,R.style.ThemeOverlay_Quapp_Dialog_WalkIn).setTitle(R.string.console_walk_in_title).setView(root)
                    .setNegativeButton(R.string.console_cancel,null).setPositiveButton(R.string.console_walk_in_confirm,null).show();
            });
            capture("walk-default");
            ins.runOnMainSync(()->((EditText)root.findViewById(R.id.walk_in_name_input)).setText(worst));
            reveal(R.id.walk_in_name_input);capture("walk-unicode-long");
            ins.runOnMainSync(()->{((EditText)root.findViewById(R.id.walk_in_name_input)).setText("");((TextInputLayout)root.findViewById(R.id.walk_in_name_layout)).setError(activity.getString(R.string.console_walk_in_error));});
            reveal(R.id.walk_in_name_input);capture("walk-missing-error");
            ins.runOnMainSync(()->{EditText input=root.findViewById(R.id.walk_in_name_input);input.requestFocus();((InputMethodManager)activity.getSystemService(android.content.Context.INPUT_METHOD_SERVICE)).showSoftInput(input,InputMethodManager.SHOW_IMPLICIT);});
            SystemClock.sleep(500);reveal(R.id.walk_in_name_layout);capture("walk-error-keyboard");
            ins.runOnMainSync(()->{((InputMethodManager)activity.getSystemService(android.content.Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(root.getWindowToken(),0);walk.dismiss();walk=null;});idle();
            ConsoleSheets.OptionsListener blocked=new ConsoleSheets.OptionsListener(){public void onExtend(){fail("operation");}public void onEdit(){fail("operation");}public void onInsights(){fail("operation");}public void onShare(){fail("operation");}public void onStatus(Queue.Status s){fail("operation");}public void onClose(){fail("operation");}};
            for(Queue.Status status:Queue.Status.values()){
                final Queue q=queue.toBuilder().setStatus(status).build();
                ins.runOnMainSync(()->ConsoleSheets.showOptions(activity,q,blocked));sheet();capture("options-"+status+"-top");
                reveal(R.id.choices_rows);capture("options-"+status+"-rows");dismissSheet();
            }
            for(int count:new int[]{0,999999999}){
                ins.runOnMainSync(()->ConsoleSheets.showExtend((androidx.fragment.app.FragmentActivity)activity,queue,count,time->fail("extend operation")));
                sheet();capture("extend-"+count+"-top");reveal(R.id.choices_after);capture("extend-"+count+"-times");reveal(R.id.choices_note);capture("extend-"+count+"-note");reveal(R.id.choices_primary);capture("extend-"+count+"-action");dismissSheet();
            }
            for(int number:new int[]{0,999999999}){
                final Ticket t=new Ticket.Builder().setId("visual-only").setQueue(queue.getId(),queue.getName(),queue.getVenue()).setHolder(number==0?"":worst,null).setTicketNumber(number).setJoinedAt(Instant.now()).build();
                ins.runOnMainSync(()->ConsoleSheets.showRemove(activity,t,(ticket,reason)->fail("remove operation")));
                sheet();capture("remove-"+number+"-top");reveal(R.id.choices_rows);capture("remove-"+number+"-reasons");reveal(R.id.choices_danger);capture("remove-"+number+"-action");dismissSheet();
            }
            // Shared caller regression: render its existing radio rows, never submit a report.
            ins.runOnMainSync(()->{activity.finish();});idle();
            activity=ins.startActivitySync(new Intent(ins.getTargetContext(),ReportQueueActivity.class).putExtra("com.example.quapp.EXTRA_REPORT_QUEUE_ID",queue.getId()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));idle();
            root=activity.getWindow().getDecorView();reveal(R.id.report_reasons);capture("report-shared-rows");
        } finally {
            if(activity!=null)ins.runOnMainSync(()->{if(walk!=null)walk.dismiss();for(View v:WindowInspector.getGlobalWindowViews()){View parent=v.findViewById(com.google.android.material.R.id.design_bottom_sheet);if(parent!=null)BottomSheetBehavior.from(parent).setState(BottomSheetBehavior.STATE_HIDDEN);}((InputMethodManager)activity.getSystemService(android.content.Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(activity.getWindow().getDecorView().getWindowToken(),0);activity.finish();});
            idle();
            ins.runOnMainSync(()->{try{for(Map.Entry<Field,List>e:lists.entrySet()){List l=(List)e.getKey().get(null);l.clear();l.addAll(e.getValue());}for(Map.Entry<Field,Map>e:maps.entrySet()){Map m=(Map)e.getKey().get(null);m.clear();m.putAll(e.getValue());}for(Map.Entry<Field,Object>e:flags.entrySet())e.getKey().set(null,e.getValue());}catch(Exception e){throw new AssertionError(e);}});
            for(int i=0;i<prefs.length;i++){if(!saved[i].equals(prefs[i].getAll()))restore(prefs[i],saved[i]);assertEquals(saved[i],prefs[i].getAll());}
            for(Map.Entry<Field,List>e:lists.entrySet())assertEquals(e.getValue(),e.getKey().get(null));
            for(Map.Entry<Field,Map>e:maps.entrySet())assertEquals(e.getValue(),e.getKey().get(null));
            for(Map.Entry<Field,Object>e:flags.entrySet())assertEquals(e.getValue(),e.getKey().get(null));
        }
    }
    void sheet(){idle();ins.runOnMainSync(()->{root=null;for(View v:WindowInspector.getGlobalWindowViews())if(v.findViewById(R.id.choices_title)!=null)root=v;assertNotNull(root);View sheet=root.findViewById(com.google.android.material.R.id.design_bottom_sheet);BottomSheetBehavior.from(sheet).setState(BottomSheetBehavior.STATE_EXPANDED);});idle();}
    void dismissSheet(){ins.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK);idle();}
    @Override void reveal(int id){ins.runOnMainSync(()->{View v=root.findViewById(id);assertNotNull(v);v.requestRectangleOnScreen(new Rect(0,0,v.getWidth(),v.getHeight()),true);});idle();}
    @Override void capture(String state)throws Exception{idle();SystemClock.sleep(350);Bitmap b=ins.getUiAutomation().takeScreenshot();assertNotNull(b);File dir=new File(ins.getTargetContext().getExternalFilesDir(null),"phase3d-review");assertTrue(dir.isDirectory()||dir.mkdirs());android.content.res.Configuration c=activity.getResources().getConfiguration();try(FileOutputStream out=new FileOutputStream(new File(dir,state+"-"+c.screenWidthDp+"x"+c.screenHeightDp+"-"+c.fontScale+".png"))){assertTrue(b.compress(Bitmap.CompressFormat.PNG,100,out));}finally{b.recycle();}
        if(walk!=null&&walk.isShowing()&&!state.endsWith("keyboard"))ins.runOnMainSync(()->{
            for(int which:new int[]{AlertDialog.BUTTON_POSITIVE,AlertDialog.BUTTON_NEGATIVE}) {
                View button=walk.getButton(which);Rect visible=new Rect();assertTrue("Walk-in action visible",button.getGlobalVisibleRect(visible));
                assertEquals("Walk-in action full height",button.getHeight(),visible.height());assertEquals("Walk-in action full width",button.getWidth(),visible.width());
                int minimum=activity.getResources().getDimensionPixelSize(R.dimen.touch_target_min);assertTrue(button.getHeight()>=minimum);assertTrue(button.getWidth()>=minimum);
            }
        });
    }
}
