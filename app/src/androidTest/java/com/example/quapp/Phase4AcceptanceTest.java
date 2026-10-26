package com.example.quapp;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.core.widget.NestedScrollView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;

/** Rendered acceptance fixtures, separate from production drafts and backend state. */
@RunWith(AndroidJUnit4.class)
public class Phase4AcceptanceTest {
    private final Instrumentation ins = InstrumentationRegistry.getInstrumentation();

    @Test public void serviceAndExploratoryRoleSelectionStayLocal() throws Exception {
        Queue q=FakeData.queues().get(0);
        try(ActivityScenario<Phase4Activity> s=ActivityScenario.launch(Phase4Activity.intent(ins.getTargetContext(),Phase4Activity.APPOINTMENTS))) {
            act(s,R.string.p4_services);
            chooseDialog(q.getName());act(s,R.string.p4_review);
            s.onActivity(a->assertFalse(button(a,R.string.p4_reserve).isEnabled()));
        }
        q=FakeData.ownedQueues().get(0);
        try(ActivityScenario<Phase4Activity> s=ActivityScenario.launch(Phase4Activity.ownerIntent(ins.getTargetContext(),Phase4Activity.STAFF,q.getId()))) {
            act(s,R.string.p4_invite_draft);act(s,R.string.p4_role);
            chooseDialog(ins.getTargetContext().getString(R.string.p4_operator));
            act(s,R.string.p4_review);s.onActivity(a->assertFalse(button(a,R.string.p4_invite).isEnabled()));
            inspect(s,"staff-role-selected");
        }
    }
    private void chooseDialog(String label){onView(withText(label)).inRoot(isDialog()).perform(new androidx.test.espresso.ViewAction(){
        public org.hamcrest.Matcher<View> getConstraints(){return isAssignableFrom(View.class);}
        public String getDescription(){return "Choose actual local selector item";}
        public void perform(androidx.test.espresso.UiController c,View v){View item=v;while(!(item.getParent() instanceof android.widget.AdapterView)&&item.getParent() instanceof View)item=(View)item.getParent();
            assertTrue(item.getParent() instanceof android.widget.AdapterView);android.widget.AdapterView<?> list=(android.widget.AdapterView<?>)item.getParent();int position=list.getPositionForView(item);assertTrue(list.performItemClick(item,position,list.getItemIdAtPosition(position)));}
    });ins.waitForIdleSync();}

    @Test public void pickersReopenSavedPreferencesAfterRecreation() throws Exception {
        Queue q=FakeData.ownedQueues().get(0);
        try(ActivityScenario<Phase4Activity> s=ActivityScenario.launch(Phase4Activity.ownerIntent(ins.getTargetContext(),Phase4Activity.APPOINTMENTS,q.getId()))) {
            act(s,R.string.p4_services);
            s.onActivity(a->{try{
                java.lang.reflect.Field date=Phase4Activity.class.getDeclaredField("date"),time=Phase4Activity.class.getDeclaredField("time");
                date.setAccessible(true);time.setAccessible(true);date.set(a,"2026-11-20");time.set(a,"09:15");
            }catch(Exception e){throw new AssertionError(e);}});
            s.recreate(); act(s,R.string.p4_date);
            s.onActivity(a->{a.getSupportFragmentManager().executePendingTransactions();
                com.google.android.material.datepicker.MaterialDatePicker<?> p=(com.google.android.material.datepicker.MaterialDatePicker<?>)a.getSupportFragmentManager().findFragmentByTag("p4_date_picker");
                assertEquals(Long.valueOf(java.time.LocalDate.parse("2026-11-20").atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()),p.getSelection());p.dismissNow();});
            act(s,R.string.p4_time);
            s.onActivity(a->{a.getSupportFragmentManager().executePendingTransactions();
                com.google.android.material.timepicker.MaterialTimePicker p=(com.google.android.material.timepicker.MaterialTimePicker)a.getSupportFragmentManager().findFragmentByTag("p4_time_picker");
                assertEquals(9,p.getHour());assertEquals(15,p.getMinute());p.dismissNow();});
        }
    }

    @Test public void profileAndOwnerConsoleRoutes() throws Exception {
        try (ActivityScenario<QueuerHomeActivity> home = ActivityScenario.launch(
                QueuerHomeActivity.intent(ins.getTargetContext(), QueuerHomeActivity.TAB_PROFILE))) {
            home.onActivity(a -> a.findViewById(R.id.profile_appointments).performClick());
            androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withText(R.string.profile_explore_appointments)).perform(new androidx.test.espresso.ViewAction(){
                public org.hamcrest.Matcher<View> getConstraints(){return isAssignableFrom(View.class);}
                public String getDescription(){return "Activate appointment sheet route after native sheet opens";}
                public void perform(androidx.test.espresso.UiController controller,View view){while(!view.isClickable() && view.getParent() instanceof View)view=(View)view.getParent();assertTrue(view.performClick());}
            });
            Phase4Activity routed = resumed();
            inspect(routed, "profile-appointments");
            ins.runOnMainSync(() -> routed.getOnBackPressedDispatcher().onBackPressed());
            ins.waitForIdleSync();
        }
        Queue q=FakeData.ownedQueues().get(0);
        for(int label:new int[]{R.string.p4_appointments,R.string.p4_staff}) {
            try(ActivityScenario<LiveConsoleActivity> console=ActivityScenario.launch(new Intent(ins.getTargetContext(),LiveConsoleActivity.class)
                    .putExtra(LiveConsoleActivity.EXTRA_QUEUE_ID,q.getId()))) {
                console.onActivity(a -> a.findViewById(R.id.console_menu).performClick());
                onView(withText(label)).inRoot(isDialog()).perform(new androidx.test.espresso.ViewAction(){
                    public org.hamcrest.Matcher<View> getConstraints(){return isAssignableFrom(View.class);}
                    public String getDescription(){return "Activate real Phase 4 Console row";}
                    public void perform(androidx.test.espresso.UiController c,View v){while(!v.isClickable() && v.getParent() instanceof View)v=(View)v.getParent();assertTrue(v.performClick());}
                });
                Phase4Activity routed=resumed();
                inspect(routed,label==R.string.p4_staff?"console-staff":"console-appointments");
                ins.runOnMainSync(routed::finish);
            }
        }
    }

    @Test public void renderedDraftsAndUnavailableActions() throws Exception {
        Queue q=FakeData.ownedQueues().get(0);
        try(ActivityScenario<Phase4Activity> s=ActivityScenario.launch(Phase4Activity.ownerIntent(ins.getTargetContext(),Phase4Activity.APPOINTMENTS,q.getId()))) {
            act(s,R.string.p4_services); inspect(s,"appointment-preferences");
            act(s,R.string.p4_review); inspect(s,"appointment-review");
            s.onActivity(a->assertFalse(button(a,R.string.p4_reserve).isEnabled()));
            act(s,R.string.p4_upcoming); act(s,R.string.p4_details); inspect(s,"appointment-unavailable");
        }
        try(ActivityScenario<Phase4Activity> s=ActivityScenario.launch(Phase4Activity.intent(ins.getTargetContext(),Phase4Activity.FAMILY))) {
            act(s,R.string.p4_family_intro); act(s,R.string.p4_review); inspect(s,"family-empty-validation");
            s.onActivity(a->{List<TextInputLayout> f=fields(a.findViewById(R.id.p4_content));
                assertNotNull(f.get(0).getError());assertNotNull(f.get(1).getError());
                f.get(0).getEditText().setText("Aleksandra Wiśniewska-Kowalczyk 家族 🌿");
                f.get(1).getEditText().setText("Jo\nMaría de los Ángeles Fernández\n公共服务 👩🏽‍💼\nJean-Baptiste de La Rochefoucauld");
                List<TextView> labels=new ArrayList<>();collect(a.findViewById(R.id.p4_content),labels);
                for(TextInputLayout input:f){boolean linked=false;for(TextView label:labels)if(label.getLabelFor()==input.getEditText().getId())linked=true;assertTrue("Input has persistent linked label",linked);}
                f.get(1).getEditText().requestFocus();
                ((android.view.inputmethod.InputMethodManager)a.getSystemService(Activity.INPUT_METHOD_SERVICE)).showSoftInput(f.get(1).getEditText(),0);
            });
            inspect(s,"family-input-keyboard");
            act(s,R.string.p4_review); inspect(s,"family-unicode-review");
            s.recreate(); inspect(s,"family-recreated");
            act(s,R.string.p4_group_manage); inspect(s,"family-management");
            act(s,R.string.p4_clear); inspect(s,"family-cleared");
        }
        try(ActivityScenario<Phase4Activity> s=ActivityScenario.launch(Phase4Activity.ownerIntent(ins.getTargetContext(),Phase4Activity.STAFF,q.getId()))) {
            act(s,R.string.p4_invite_draft); inspect(s,"staff-draft");
            act(s,R.string.p4_review); inspect(s,"staff-review");
            act(s,R.string.p4_invitation_status); act(s,R.string.p4_invitation_status); inspect(s,"staff-unavailable");
            s.onActivity(a->{assertFalse(button(a,R.string.p4_change_role).isEnabled());assertFalse(button(a,R.string.p4_revoke).isEnabled());});
        }
    }
    private void act(ActivityScenario<Phase4Activity> s,int label){s.onActivity(a->assertTrue(button(a,label).performClick()));ins.waitForIdleSync();}
    private void inspect(ActivityScenario<Phase4Activity> s,String label)throws Exception{final Phase4Activity[] a={null};s.onActivity(v->a[0]=v);inspect(a[0],label);}
    private void inspect(Phase4Activity a,String label)throws Exception {
        ins.waitForIdleSync();
        final List<TextView> texts=new ArrayList<>();
        ins.runOnMainSync(()->collect(a.findViewById(R.id.p4_content),texts));
        for(TextView t:texts){
            long end=android.os.SystemClock.uptimeMillis()+5000;
            final boolean[] ready={false};
            do{ins.waitForIdleSync();ins.runOnMainSync(()->{
                ready[0]=!t.isLayoutRequested() && t.getLayout()!=null;
                if(ready[0]){
                    for(int i=0;i<t.getLineCount();i++)assertEquals("Text ellipsized: "+t.getText(),0,t.getLayout().getEllipsisCount(i));
                    assertTrue("Text vertically clipped: "+t.getText(),t.getLayout().getHeight()<=t.getHeight()-t.getCompoundPaddingTop()-t.getCompoundPaddingBottom());
                    if(t instanceof MaterialButton)assertTrue("Touch target",t.getHeight()>=a.getResources().getDimensionPixelSize(R.dimen.touch_target_min));
                    if(t instanceof MaterialButton && !t.isEnabled()) {
                        MaterialButton button=(MaterialButton)t;
                        int surface=androidx.core.content.ContextCompat.getColor(a,R.color.paper);
                        int fill=button.getBackgroundTintList().getColorForState(button.getDrawableState(),surface);
                        int background=androidx.core.graphics.ColorUtils.compositeColors(fill,surface);
                        assertTrue("Disabled action remains readable",androidx.core.graphics.ColorUtils.calculateContrast(button.getCurrentTextColor(),background)>=4.5);
                    }
                }
            });if(!ready[0])android.os.SystemClock.sleep(16);}while(!ready[0]&&android.os.SystemClock.uptimeMillis()<end);
            assertTrue("View never laid out",ready[0]);
        }
        final int[] height={0},viewport={0};
        ins.runOnMainSync(()->{NestedScrollView sc=a.findViewById(R.id.p4_scroll);height[0]=sc.getChildAt(0).getHeight();viewport[0]=sc.getHeight();
            android.graphics.Rect bounds=new android.graphics.Rect();assertTrue(sc.getGlobalVisibleRect(bounds));
            androidx.core.graphics.Insets bars=androidx.core.view.ViewCompat.getRootWindowInsets(sc).getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
            assertTrue("Scroll viewport below system bars",bounds.top>=bars.top);
            assertTrue("Scroll viewport above navigation",bounds.bottom<=a.getWindow().getDecorView().getHeight()-bars.bottom);
        });
        assertTrue("Usable viewport",viewport[0]>0);
        int page=0;
        for(int y=0;y<height[0];y+=Math.max(1,viewport[0]*3/4)){
            final int pos=y;ins.runOnMainSync(()->((NestedScrollView)a.findViewById(R.id.p4_scroll)).scrollTo(0,pos));ins.waitForIdleSync();
            capture(a,label+"-"+page++);
        }
    }
    private void capture(Activity a,String label)throws Exception{
        Configuration c=a.getResources().getConfiguration();String config=c.fontScale>1?"large":c.orientation==Configuration.ORIENTATION_LANDSCAPE?"landscape":"normal";
        android.graphics.Bitmap image=ins.getUiAutomation().takeScreenshot();assertNotNull(image);
        java.io.File folder=new java.io.File(ins.getTargetContext().getExternalFilesDir(null),"phase4-acceptance");
        assertTrue(folder.isDirectory()||folder.mkdirs());
        try(java.io.FileOutputStream output=new java.io.FileOutputStream(new java.io.File(folder,config+"-"+label+".png"))){
            assertTrue(image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,output));
        }finally{image.recycle();}
    }
    private Phase4Activity resumed(){final Phase4Activity[] a={null};long end=android.os.SystemClock.uptimeMillis()+5000;
        do{ins.waitForIdleSync();ins.runOnMainSync(()->{for(Activity v:ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED))if(v instanceof Phase4Activity)a[0]=(Phase4Activity)v;});if(a[0]!=null)return a[0];android.os.SystemClock.sleep(16);}while(android.os.SystemClock.uptimeMillis()<end);throw new AssertionError("Phase 4 route not resumed");}
    private static void collect(View v,List<TextView> out){if(v.getVisibility()!=View.VISIBLE)return;if(v instanceof TextView)out.add((TextView)v);if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)collect(((ViewGroup)v).getChildAt(i),out);}
    private static MaterialButton button(Activity a,int label){List<TextView> views=new ArrayList<>();collect(a.findViewById(R.id.p4_content),views);for(TextView v:views)if(v instanceof MaterialButton&&a.getString(label).contentEquals(v.getText()))return (MaterialButton)v;throw new AssertionError("Button missing: "+label);}
    private static List<TextInputLayout> fields(View v){List<TextInputLayout> out=new ArrayList<>();if(v instanceof TextInputLayout)out.add((TextInputLayout)v);else if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)out.addAll(fields(((ViewGroup)v).getChildAt(i)));return out;}
}
