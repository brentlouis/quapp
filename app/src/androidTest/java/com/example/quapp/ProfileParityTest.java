package com.example.quapp;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;
import com.google.android.material.materialswitch.MaterialSwitch;
import org.junit.*;
import org.junit.runner.RunWith;
import java.io.*;
import java.util.*;
import static org.junit.Assert.*;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.action.ViewActions.click;

@RunWith(AndroidJUnit4.class)
public class ProfileParityTest {
    private final Instrumentation ins = InstrumentationRegistry.getInstrumentation();
    private final Map<String, Map<String, ?>> backup = new HashMap<>();
    private boolean debug;
    private List<Report> priorReports;
    private final String[] stores = {"quapp_session", "quapp_device", "quapp_profile_preferences", "quapp_saved_services"};
    @Before public void setup() throws Exception {
        debug=FakeData.isDebugMode(); priorReports=new ArrayList<>(FakeData.reports()); FakeData.setDebugMode(false);
        for (String name : stores) backup.put(name, new HashMap<>(ins.getTargetContext().getSharedPreferences(name, 0).getAll()));
        ins.runOnMainSync(() -> { ins.getTargetContext().getSharedPreferences("quapp_device",0).edit().clear().commit(); ins.getTargetContext().getSharedPreferences("quapp_session",0).edit().clear().commit(); new Session(ins.getTargetContext()).register("Alex", "09000000001"); new Session(ins.getTargetContext()).setRole(Session.Role.QUEUER); ProfilePreferences.get(ins.getTargetContext()).edit().clear().commit(); });
    }
    @After @SuppressWarnings("unchecked") public void cleanup() throws Exception {
        java.lang.reflect.Field field=FakeData.class.getDeclaredField("reports"); field.setAccessible(true);
        List<Report> live=(List<Report>)field.get(null); live.clear(); live.addAll(priorReports); FakeData.setDebugMode(debug);
        ins.runOnMainSync(() -> {
            for (Stage stage : new Stage[]{Stage.RESUMED, Stage.PAUSED, Stage.STOPPED}) for (Activity activity : new ArrayList<>(ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(stage))) activity.finish();
            for (String name : stores) {
                SharedPreferences prefs = ins.getTargetContext().getSharedPreferences(name, 0); SharedPreferences.Editor edit = prefs.edit().clear();
                for (Map.Entry<String, ?> e : backup.get(name).entrySet()) { Object v=e.getValue(); if (v instanceof Boolean) edit.putBoolean(e.getKey(), (Boolean)v); else if (v instanceof String) edit.putString(e.getKey(), (String)v); else if (v instanceof Integer) edit.putInt(e.getKey(), (Integer)v); else if (v instanceof Long) edit.putLong(e.getKey(), (Long)v); else if (v instanceof Float) edit.putFloat(e.getKey(), (Float)v); else edit.putStringSet(e.getKey(), new HashSet<>((Set<String>)v)); }
                assertTrue(edit.commit()); assertEquals(backup.get(name), prefs.getAll());
            }
        });
    }
    @Test public void shortestWaitKeepsLegacyPreference() {
        List<Queue> legacy = new ArrayList<>(FakeData.queues());
        List<Queue> current = new ArrayList<>(legacy);
        ProfilePreferences.get(ins.getTargetContext()).edit().putString("order", "Shortest demo wait").commit();
        ProfilePreferences.order(ins.getTargetContext(), legacy);
        ProfilePreferences.get(ins.getTargetContext()).edit().putString("order", "Shortest wait").commit();
        ProfilePreferences.order(ins.getTargetContext(), current);
        assertEquals(legacy, current);
        for (int i = 1; i < current.size(); i++)
            assertTrue(current.get(i - 1).getEstimatedWaitMinutes() <= current.get(i).getEstimatedWaitMinutes());
    }

    @Test public void profileGroupsSheetsAndNavigation() throws Exception {
        Activity home = launch(QueuerHomeActivity.intent(ins.getTargetContext(), QueuerHomeActivity.TAB_PROFILE));
        capture("profile-collapsed");
        ins.runOnMainSync(() -> {
            TextView tools = text(home, R.string.profile_tools); View toggle=(View)tools.getParent().getParent();
            assertEquals(View.VISIBLE, toggle.findViewById(R.id.row_chevron).getVisibility());
            assertEquals(90f, toggle.findViewById(R.id.row_chevron).getRotation(), 0f);
            assertEquals("Collapsed", androidx.core.view.ViewCompat.getStateDescription(toggle)); toggle.performClick();
            assertEquals("Expanded", androidx.core.view.ViewCompat.getStateDescription(toggle));
            assertEquals(270f, toggle.findViewById(R.id.row_chevron).getRotation(), 0f);
            assertTrue(text(home,R.string.profile_privacy).isShown());
        });
        capture("profile-expanded");
        ins.runOnMainSync(() -> find(home.findViewById(android.R.id.content),androidx.core.widget.NestedScrollView.class).fullScroll(View.FOCUS_DOWN)); capture("profile-expanded-end");
        ins.runOnMainSync(() -> find(home.findViewById(android.R.id.content),androidx.core.widget.NestedScrollView.class).fullScroll(View.FOCUS_UP));
        ins.runOnMainSync(() -> home.findViewById(R.id.profile_details).performClick()); capture("account-sheet");
        onView(androidx.test.espresso.matcher.ViewMatchers.withContentDescription(R.string.profile_close_dialog)).perform(click());
        ins.runOnMainSync(() -> ((View)text(home,R.string.profile_preferences).getParent().getParent()).performClick());
        Activity prefs = resumed(ProfileSettingsActivity.class); assertEquals(prefs.getString(R.string.profile_preferences), ((TextView)prefs.findViewById(R.id.profile_settings_title)).getText()); capture("preferences");
        ins.runOnMainSync(() -> prefs.findViewById(R.id.profile_settings_back).performClick()); resumed(QueuerHomeActivity.class);
        ins.runOnMainSync(() -> ((View)text(home,R.string.profile_notifications).getParent().getParent()).performClick()); resumed(ProfileSettingsActivity.class); capture("notifications");
        ins.runOnMainSync(() -> { for (Activity a: ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)) a.finish(); }); resumed(QueuerHomeActivity.class);
        ins.runOnMainSync(() -> ((View)text(home,R.string.profile_help_reports).getParent().getParent()).performClick()); Activity help=resumed(ProfileSettingsActivity.class); capture("help");
        ins.runOnMainSync(() -> { com.google.android.material.textfield.TextInputEditText search=find(help.findViewById(android.R.id.content),com.google.android.material.textfield.TextInputEditText.class); search.setText("no-such-topic-xyz"); assertNotNull(text(help,R.string.profile_no_help)); }); capture("help-no-results");
    }
    @Test public void preferencesPersistAndClearPreservesAccountAndTickets() throws Exception {
        Activity prefs=launch(ProfileSettingsActivity.intent(ins.getTargetContext(),"preferences"));
        final float[] normal={0}; ins.runOnMainSync(() -> normal[0]=((TextView)prefs.findViewById(R.id.profile_settings_title)).getTextSize());
        ins.runOnMainSync(() -> { MaterialSwitch reduce=find(prefs.findViewById(android.R.id.content), MaterialSwitch.class); reduce.setChecked(true); assertFalse(QuappMotion.enabled(prefs)); });
        String town=new Session(prefs).getTown();
        assertEquals(town,new Session(prefs).getTown());
        onView(withText("Recommended")).perform(androidx.test.espresso.action.ViewActions.scrollTo(),click()); onView(withText("Name")).perform(click());
        List<Queue> sorted=new ArrayList<>(FakeData.queues()); ProfilePreferences.order(prefs,sorted);
        for(int i=1;i<sorted.size();i++) assertTrue(sorted.get(i-1).getName().compareToIgnoreCase(sorted.get(i).getName())<=0);
        Activity enlarged=prefs; capture("preferences-system-text");
        assertTrue(ProfilePreferences.get(enlarged).getBoolean("reduced",false));
        ins.runOnMainSync(() -> assertEquals(normal[0],((TextView)enlarged.findViewById(R.id.profile_settings_title)).getTextSize(),0f));
        ins.runOnMainSync(() -> ProfileSettingsActivity.sheet(enlarged,"privacy")); capture("privacy-sheet");
        onView(withText(R.string.profile_clear_data_ellipsis)).perform(androidx.test.espresso.action.ViewActions.scrollTo(),click()); capture("privacy-confirm");
        onView(withText(R.string.profile_cancel)).perform(click()); assertTrue(ProfilePreferences.get(enlarged).getBoolean("reduced",false));
        final List<Ticket> tickets = new ArrayList<>(ActiveTicketStore.liveTickets());
        ins.runOnMainSync(() -> { ProfilePreferences.get(enlarged).edit().putString("read:fixture","message").apply(); ProfileSettingsActivity.sheet(enlarged,"privacy"); });
        // Wait for the newly expanded sheet before its destructive-action tap.
        capture("privacy-reopened");
        onView(withText(R.string.profile_clear_data_ellipsis)).perform(androidx.test.espresso.action.ViewActions.scrollTo(),click());
        capture("privacy-clear-ready");
        onView(withText(R.string.profile_clear_data)).perform(click()); Activity cleared=awaitOther(enlarged);
        ins.runOnMainSync(() -> { assertEquals("message",ProfilePreferences.get(enlarged).getString("read:fixture","")); assertTrue(new Session(enlarged).isLoggedIn()); assertEquals("09000000001",new Session(enlarged).getPhone()); assertEquals(tickets,ActiveTicketStore.liveTickets()); assertFalse(ProfilePreferences.get(enlarged).getBoolean("reduced",false)); assertTrue(new SavedServices(enlarged).ids().isEmpty()); });
    }
    @Test public void authRetainsRequiredPhoneWithoutSampleDisplay() throws Exception {
        Activity login=launch(new Intent(ins.getTargetContext(),LoginActivity.class)); capture("login");
        ins.runOnMainSync(() -> { TextView phone=login.findViewById(R.id.login_phone_input); assertTrue(phone.getHint()==null||phone.getHint().length()==0); login.findViewById(R.id.login_submit_button).performClick(); assertNotNull(((com.google.android.material.textfield.TextInputLayout)login.findViewById(R.id.login_phone_layout)).getError()); });
        Activity register=launch(new Intent(ins.getTargetContext(),RegisterActivity.class)); capture("signup");
        ins.runOnMainSync(() -> { TextView phone=register.findViewById(R.id.register_phone_input); assertTrue(phone.getHint()==null||phone.getHint().length()==0); register.findViewById(R.id.register_submit_button).performClick(); assertNotNull(((com.google.android.material.textfield.TextInputLayout)register.findViewById(R.id.register_phone_layout)).getError()); });
    }
    @Test public void notificationsAndReportsRespectChoicesAndAccount() throws Exception {
        Activity notifications=launch(ProfileSettingsActivity.intent(ins.getTargetContext(),"notifications"));
        ins.runOnMainSync(() -> { ArrayList<MaterialSwitch> switches=new ArrayList<>(); collect(notifications.findViewById(android.R.id.content),switches); assertEquals(2,switches.size()); assertFalse(switches.get(0).isChecked()); assertTrue(switches.get(1).isChecked()); switches.get(1).setChecked(false); switches.get(0).setChecked(true); assertTrue(ProfilePreferences.get(notifications).getBoolean("alerts",false)); assertFalse(ProfilePreferences.get(notifications).getBoolean("queueAlerts",true)); assertTrue(ProfilePreferences.reports(notifications).isEmpty()); });
        ins.runOnMainSync(notifications::recreate); Activity second=awaitOther(notifications);
        ins.runOnMainSync(() -> { ArrayList<MaterialSwitch> switches=new ArrayList<>(); collect(second.findViewById(android.R.id.content),switches); assertTrue(switches.get(0).isChecked()); assertFalse(switches.get(1).isChecked()); });
    }
    @Test public void organizerReturnFamilyAndPastReceiptRemainReachable() throws Exception {
        Activity home=launch(QueuerHomeActivity.intent(ins.getTargetContext(),QueuerHomeActivity.TAB_PROFILE));
        ins.runOnMainSync(() -> { ((View)text(home,R.string.profile_tools).getParent().getParent()).performClick(); ((View)text(home,R.string.profile_organizer_today).getParent().getParent()).performClick(); });
        Activity owner=resumed(OwnerHomeActivity.class);
        ins.runOnMainSync(() -> { assertEquals(Session.Role.OWNER,new Session(owner).getRole()); ((com.google.android.material.bottomnavigation.BottomNavigationView)owner.findViewById(R.id.owner_home_nav)).setSelectedItemId(R.id.nav_profile); });
        ins.waitForIdleSync();
        ins.runOnMainSync(() -> { ((View)text(owner,R.string.profile_tools).getParent().getParent()).performClick(); ((View)text(owner,R.string.profile_customer_view).getParent().getParent()).performClick(); });
        Activity customer=resumed(QueuerHomeActivity.class);
        ins.runOnMainSync(() -> { assertEquals(Session.Role.QUEUER,new Session(customer).getRole()); ((com.google.android.material.bottomnavigation.BottomNavigationView)customer.findViewById(R.id.queuer_home_nav)).setSelectedItemId(R.id.nav_tickets); });
        ins.waitForIdleSync(); ins.runOnMainSync(() -> customer.findViewById(R.id.my_tickets_family).performClick()); resumed(Phase4Activity.class); capture("family-destination");
        Activity history=launch(new Intent(ins.getTargetContext(),HistoryActivity.class)); ins.waitForIdleSync(); capture("past-visits");
        if(!FakeData.history().isEmpty()) {
            ins.runOnMainSync(() -> { androidx.recyclerview.widget.RecyclerView list=history.findViewById(R.id.history_list); assertTrue(list.getAdapter().getItemCount()>0); list.findViewHolderForAdapterPosition(0).itemView.performClick(); });
            Activity receipt=resumed(ProfileSettingsActivity.class); capture("past-receipt"); assertEquals(receipt.getString(R.string.profile_receipt),((TextView)receipt.findViewById(R.id.profile_settings_title)).getText());
        }
    }
    @Test public void guestProfileDoesNotExposeRememberedIdentityOrVerification() throws Exception {
        new Session(ins.getTargetContext()).logOut();
        Activity home=launch(QueuerHomeActivity.intent(ins.getTargetContext(),QueuerHomeActivity.TAB_PROFILE));
        capture("profile-guest");
        ins.runOnMainSync(() -> { assertEquals(home.getString(R.string.profile_guest),((TextView)home.findViewById(R.id.profile_name)).getText()); assertTrue(((TextView)home.findViewById(R.id.profile_summary_status)).getText().toString().contains(home.getString(R.string.profile_not_verified))); home.findViewById(R.id.profile_details).performClick(); });
        capture("account-guest");
    }
    @Test public void validLoginAndGuestBookmarksKeepAccountBoundaries() throws Exception {
        Activity login=launch(new Intent(ins.getTargetContext(),LoginActivity.class));
        ins.runOnMainSync(() -> { ((TextView)login.findViewById(R.id.login_phone_input)).setText("09000000001"); ((TextView)login.findViewById(R.id.login_password_input)).setText("sample-pass-123"); login.findViewById(R.id.login_submit_button).performClick(); });
        resumed(RoleSelectActivity.class); assertTrue(new Session(ins.getTargetContext()).isLoggedIn()); assertEquals("09000000001",new Session(ins.getTargetContext()).getPhone());
        String id=FakeData.queues().get(0).getId(); SavedServices saved=new SavedServices(ins.getTargetContext()); if(!saved.contains(id))saved.toggle(id);
        new Session(ins.getTargetContext()).logOut(); assertFalse(new SavedServices(ins.getTargetContext()).contains(id));
        new Session(ins.getTargetContext()).logIn("09000000002"); assertFalse(new SavedServices(ins.getTargetContext()).contains(id));
        new Session(ins.getTargetContext()).logIn("09000000001"); assertTrue(new SavedServices(ins.getTargetContext()).contains(id));
    }
    @Test public void validSignupStillCreatesLocalSession() throws Exception {
        Activity register=launch(new Intent(ins.getTargetContext(),RegisterActivity.class));
        ins.runOnMainSync(() -> { ((TextView)register.findViewById(R.id.register_name_input)).setText("Alex Test"); ((TextView)register.findViewById(R.id.register_phone_input)).setText("09000000001"); ((TextView)register.findViewById(R.id.register_password_input)).setText("sample-pass-123"); ((TextView)register.findViewById(R.id.register_confirm_input)).setText("sample-pass-123"); register.findViewById(R.id.register_submit_button).performClick(); });
        resumed(RoleSelectActivity.class); assertTrue(new Session(ins.getTargetContext()).isLoggedIn()); assertEquals("09000000001",new Session(ins.getTargetContext()).getPhone());
    }
    @Test public void reportSubmissionIsScopedToSubmittingAccount() throws Exception {
        java.util.concurrent.CountDownLatch done=new java.util.concurrent.CountDownLatch(1);
        Throwable[] failure={null}; String id=FakeData.queues().get(0).getId();
        ins.runOnMainSync(() -> FakeData.reportAsync(ins.getTargetContext(),id,Report.Reason.FAKE,null,new FakeData.Callback<Void>() {
            public void onSuccess(Void value){done.countDown();} public void onError(Throwable error){failure[0]=error;done.countDown();}
        }));
        assertTrue(done.await(5,java.util.concurrent.TimeUnit.SECONDS)); assertNull(failure[0]);
        assertEquals(1,ProfilePreferences.reports(ins.getTargetContext()).size());
        new Session(ins.getTargetContext()).logIn("09000000002"); assertTrue(ProfilePreferences.reports(ins.getTargetContext()).isEmpty());
        new Session(ins.getTargetContext()).logIn("09000000001"); assertEquals(1,ProfilePreferences.reports(ins.getTargetContext()).size());
    }
    private Activity launch(Intent intent) { return ins.startActivitySync(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); }
    private Activity resumed(Class<?> type) { return waitActivity(type,null); }
    private Activity awaitOther(Activity previous) { return waitActivity(previous.getClass(),previous); }
    private Activity waitActivity(Class<?> type,Activity previous) { long end=SystemClock.uptimeMillis()+5000; do { ins.waitForIdleSync(); Activity[] found={null}; ins.runOnMainSync(() -> { for(Activity a:ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)) if(type.isInstance(a)&&a!=previous)found[0]=a; }); if(found[0]!=null)return found[0]; }while(SystemClock.uptimeMillis()<end); throw new AssertionError("Missing resumed "+type); }
    private TextView text(Activity activity,int id) { return findText(activity.findViewById(android.R.id.content),activity.getString(id)); }
    private TextView findText(View view,String value) { if(view instanceof TextView&&value.contentEquals(((TextView)view).getText()))return(TextView)view; if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){TextView found=findText(((ViewGroup)view).getChildAt(i),value);if(found!=null)return found;}return null; }
    private <T> T find(View view,Class<T> type) { if(type.isInstance(view))return type.cast(view); if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){T found=find(((ViewGroup)view).getChildAt(i),type);if(found!=null)return found;}return null; }
    private void collect(View view,List<MaterialSwitch> out) { if(view instanceof MaterialSwitch)out.add((MaterialSwitch)view); if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)collect(((ViewGroup)view).getChildAt(i),out); }
    private void capture(String name) throws Exception { ins.waitForIdleSync(); ins.getUiAutomation().waitForIdle(500,5000); Bitmap bitmap=ins.getUiAutomation().takeScreenshot(); assertNotNull(bitmap); File dir=new File(ins.getTargetContext().getExternalFilesDir(null),"profile-parity");dir.mkdirs(); try(FileOutputStream output=new FileOutputStream(new File(dir,name+".png"))){assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,output));}bitmap.recycle(); }
}
