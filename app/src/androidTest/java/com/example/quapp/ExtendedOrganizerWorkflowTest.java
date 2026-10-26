package com.example.quapp;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Rect;
import android.view.View;
import android.widget.TextView;
import com.google.android.material.textfield.TextInputLayout;
import static org.junit.Assert.*;

/** Reuses the isolated counter fixture for the additional approved organizer workflows. */
public class ExtendedOrganizerWorkflowTest extends CounterWorkflowTest {
    @Override void extraWorkflows()throws Exception {
        Activity selectedDisplay=activity;ins.runOnMainSync(selectedDisplay::finish);idle();activity=resumed(CounterActivity.class);orient(activity);
        click(activity,R.id.managed_counter_back);activity=resumed(CounterListActivity.class);orient(activity);
        click(activity,R.id.counters_back);activity=resumed(LiveConsoleActivity.class);orient(activity);
        click(activity,R.id.console_menu);sheetText(R.string.announcement_title);activity=resumed(AnnouncementActivity.class);orient(activity);
        assertEquals(id,activity.getIntent().getStringExtra(AnnouncementActivity.EXTRA_QUEUE_ID));
        click(activity,R.id.announcement_publish);assertNotNull(((TextInputLayout)activity.findViewById(R.id.announcement_input_layout)).getError());capture("announcement-blank");
        setMessage("🧑".repeat(501));click(activity,R.id.announcement_publish);assertNull(FakeData.announcement(id));assertNotNull(((TextInputLayout)activity.findViewById(R.id.announcement_input_layout)).getError());
        setMessage("🧑".repeat(500));click(activity,R.id.announcement_publish);assertEquals(500,FakeData.announcement(id).codePointCount(0,FakeData.announcement(id).length()));
        String message="Please wait for your number. 王秀英 👩🏽‍💻";setMessage(message);click(activity,R.id.announcement_publish);assertEquals(message,FakeData.announcement(id));capture("announcement-published");
        ins.runOnMainSync(() -> {
            TextView editor = activity.findViewById(R.id.announcement_input);
            TextView label = activity.findViewById(R.id.announcement_field_label);
            assertEquals(editor.getId(), label.getLabelFor());
            assertTrue(label.getBottom() <= activity.findViewById(R.id.announcement_input_layout).getTop());
            assertTrue("Announcement editor stays bounded after long validation input: height=" + editor.getHeight()
                            + ", line=" + editor.getLineHeight() + ", minHeight=" + editor.getMinimumHeight()
                            + ", minLines=" + editor.getMinLines() + ", maxLines=" + editor.getMaxLines(),
                    editor.getHeight() <= 6 * editor.getLineHeight()
                            + editor.getCompoundPaddingTop() + editor.getCompoundPaddingBottom()
                            + Math.round(editor.getTextSize()));
        });
        denied(()->FakeData.publishAnnouncement("foreign","Not permitted"),SecurityException.class);denied(()->FakeData.clearAnnouncement("foreign"),SecurityException.class);
        activity=launch(CounterDisplayActivity.intent(ins.getTargetContext(),id));
        assertMessage(message);
        ins.runOnMainSync(()->FakeData.publishAnnouncement(id,"Updated while display is open"));awaitMessage("Updated while display is open");capture("announcement-display-live");
        String longMessage=(worst+" Please wait for the next available counter. ").repeat(3).trim();assertTrue(longMessage.codePointCount(0,longMessage.length())<=500);
        ins.runOnMainSync(()->FakeData.publishAnnouncement(id,longMessage));awaitMessage(longMessage);
        final View announcement=activity.findViewById(R.id.counter_announcement);
        ins.runOnMainSync(()->{View parent=(View)announcement.getParent();while(!(parent instanceof androidx.core.widget.NestedScrollView))parent=(View)parent.getParent();((androidx.core.widget.NestedScrollView)parent).fullScroll(View.FOCUS_DOWN);});idle();
        ins.runOnMainSync(()->{TextView text=(TextView)announcement;for(int line=0;line<text.getLineCount();line++)assertEquals(0,text.getLayout().getEllipsisCount(line));
            View parent=(View)announcement.getParent();while(!(parent instanceof androidx.core.widget.NestedScrollView))parent=(View)parent.getParent();assertFalse(parent.canScrollVertically(1));
        });capture("announcement-display-long-end");
        if(activity.getResources().getConfiguration().orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
            for(int viewId:new int[]{R.id.counter_number,R.id.counter_qr,R.id.counter_qr_plate}) {
                ins.runOnMainSync(()->{View v=activity.findViewById(viewId);v.requestRectangleOnScreen(new Rect(0,0,v.getWidth(),v.getHeight()),true);});idle();
                ins.runOnMainSync(()->{View v=activity.findViewById(viewId);Rect visible=new Rect();String name=activity.getResources().getResourceEntryName(viewId);assertTrue(name,v.getGlobalVisibleRect(visible));assertEquals(name+" visible height",v.getHeight(),visible.height());assertEquals(name+" visible width",v.getWidth(),visible.width());if(viewId==R.id.counter_qr)assertEquals("QR stays square",v.getWidth(),v.getHeight());});
            }
            capture("announcement-display-qr-reachable");
        }
        Activity display=activity;ins.runOnMainSync(display::finish);idle();activity=resumed(AnnouncementActivity.class);orient(activity);
        click(activity,R.id.announcement_clear);assertNull(FakeData.announcement(id));assertFalse(activity.findViewById(R.id.announcement_clear).isEnabled());capture("announcement-cleared");
        previewChecks();
        lookupChecks();
        publicProfileChecks();
    }
    void setMessage(String message){ins.runOnMainSync(()->((TextView)activity.findViewById(R.id.announcement_input)).setText(message));idle();}
    void assertMessage(String message){assertEquals(activity.getString(R.string.announcement_display_format,message),((TextView)activity.findViewById(R.id.counter_announcement)).getText().toString());}
    void awaitMessage(String message){long until=android.os.SystemClock.uptimeMillis()+6000;String expected=activity.getString(R.string.announcement_display_format,message);do{idle();if(expected.contentEquals(((TextView)activity.findViewById(R.id.counter_announcement)).getText()))break;}while(android.os.SystemClock.uptimeMillis()<until);assertMessage(message);}
    @Override String captureFolder(){return "phase3-extended-organizer";}
    @SuppressWarnings({"rawtypes","unchecked"}) void previewChecks()throws Exception{
        java.util.List beforeQueues=new java.util.ArrayList((java.util.List)field(FakeData.class,"queues").get(null));
        java.util.List beforeOwned=new java.util.ArrayList((java.util.List)field(FakeData.class,"ownedIds").get(null));
        java.util.Map beforeLive=new java.util.LinkedHashMap((java.util.Map)field(FakeData.class,"live").get(null));
        Object beforeId=field(FakeData.class,"nextQueueNumber").get(null);
        for(boolean edit:new boolean[]{false,true}){
            Intent intent=new Intent(ins.getTargetContext(),CreateQueueActivity.class);if(edit)intent.putExtra(CreateQueueActivity.EXTRA_QUEUE_ID,id);
            activity=launch(intent);if(!edit){click(activity,R.id.create_preview_button);assertEquals(View.VISIBLE,activity.findViewById(R.id.create_pick_error).getVisibility());}
            ins.runOnMainSync(()->{try{
                ((TextView)activity.findViewById(R.id.create_name_input)).setText("Unsaved preview 王秀英");
                ((TextView)activity.findViewById(R.id.create_venue_input)).setText("Preview venue");
                ((TextView)activity.findViewById(R.id.create_short_input)).setText("Readonly preview");
                ((TextView)activity.findViewById(R.id.create_details_input)).setText(worst+" Long details remain in the form.");
                field(CreateQueueActivity.class,"municipality").set(activity,"Tagbilaran City");field(CreateQueueActivity.class,"category").set(activity,Category.values()[0]);
                java.time.LocalDate tomorrow=Format.today().plusDays(1);field(CreateQueueActivity.class,"startDate").set(activity,tomorrow);field(CreateQueueActivity.class,"endDate").set(activity,tomorrow);
                java.lang.reflect.Method m=CreateQueueActivity.class.getDeclaredMethod("showChoices");m.setAccessible(true);m.invoke(activity);
            }catch(Exception e){throw new AssertionError(e);}});idle();
            click(activity,R.id.create_preview_button);activity=resumed(OrganizerPreviewActivity.class);orient(activity);
            String snapshotId=activity.getIntent().getStringExtra(OrganizerPreviewActivity.EXTRA_SNAPSHOT_ID);assertNotNull(snapshotId);assertEquals(1,activity.getIntent().getExtras().keySet().size());assertEquals(edit ? "Unsaved preview 王秀英" : activity.getString(R.string.create_generated_name, activity.getString(Category.values()[0].label), "Tagbilaran City"),PreviewSnapshots.get(snapshotId).getName());
            assertEquals(beforeQueues,field(FakeData.class,"queues").get(null));assertEquals(beforeOwned,field(FakeData.class,"ownedIds").get(null));assertEquals(beforeLive,field(FakeData.class,"live").get(null));assertEquals(beforeId,field(FakeData.class,"nextQueueNumber").get(null));
            capture(edit?"preview-edit-readonly":"preview-create-readonly");
            ins.runOnMainSync(()->{androidx.recyclerview.widget.RecyclerView list=activity.findViewById(R.id.preview_fields);list.scrollToPosition(list.getAdapter().getItemCount()-1);});idle();capture(edit?"preview-edit-end":"preview-create-end");
            click(activity,R.id.preview_back);activity=resumed(CreateQueueActivity.class);assertEquals("Unsaved preview 王秀英",((TextView)activity.findViewById(R.id.create_name_input)).getText().toString());
            assertEquals(beforeQueues,field(FakeData.class,"queues").get(null));assertEquals(beforeId,field(FakeData.class,"nextQueueNumber").get(null));
            Activity form=activity;ins.runOnMainSync(form::finish);long destroyedBy=android.os.SystemClock.uptimeMillis()+4000;do{idle();}while(!form.isDestroyed()&&android.os.SystemClock.uptimeMillis()<destroyedBy);assertTrue("Form destruction completes before snapshot cleanup assertion",form.isDestroyed());assertNull(PreviewSnapshots.get(snapshotId));
        }
        activity=launch(OrganizerPreviewActivity.intent(ins.getTargetContext(),"missing-snapshot"));assertEquals(activity.getString(R.string.preview_unavailable),((TextView)activity.findViewById(R.id.preview_notice)).getText().toString());assertEquals(View.GONE,activity.findViewById(R.id.preview_fields).getVisibility());capture("preview-unavailable");
    }
    @SuppressWarnings({"rawtypes","unchecked"}) void lookupChecks()throws Exception{
        ins.runOnMainSync(()->{try{FakeData.saveQueue(queue);freshLine();((java.util.Map)field(FakeData.class,"finished").get(null)).clear();field(fixture.getClass(),"nextTicketNumber").set(fixture,0);
            FakeData.join(id,worst,"09171234567");FakeData.addWalkIn(id,"");Ticket called=FakeData.addWalkIn(id,"Called fixture");Ticket served=FakeData.addWalkIn(id,"Served fixture");FakeData.callTicket(id,served.getId());FakeData.markServed(id,served.getId());FakeData.callTicket(id,called.getId());
            Ticket foreign=new Ticket.Builder().setId("foreign-777").setQueue("foreign","Other queue","").setHolder("PRIVATE FOREIGN","09999999999").setTicketNumber(777).setStatus(Ticket.Status.SERVED).build();((java.util.Map)field(FakeData.class,"finished").get(null)).put(foreign.getId(),foreign);
        }catch(Exception e){throw new AssertionError(e);}});
        denied(()->FakeData.findTicketByNumber("foreign",0),SecurityException.class);denied(()->FakeData.findTicketByNumber(id,-1),IllegalArgumentException.class);assertNull(FakeData.findTicketByNumber(id,777));
        click(activity,R.id.preview_back);activity=resumed(AnnouncementActivity.class);orient(activity);click(activity,R.id.announcement_back);activity=resumed(LiveConsoleActivity.class);orient(activity);
        click(activity,R.id.console_menu);sheetText(R.string.lookup_title);activity=resumed(OrganizerLookupActivity.class);orient(activity);assertEquals(id,activity.getIntent().getStringExtra(OrganizerLookupActivity.EXTRA_QUEUE_ID));
        java.util.List beforeWaiting=new java.util.ArrayList((java.util.List)field(fixture.getClass(),"waiting").get(fixture));Object beforeServing=field(fixture.getClass(),"nowServing").get(fixture);java.util.Map beforeFinished=new java.util.LinkedHashMap((java.util.Map)field(FakeData.class,"finished").get(null));int beforeServed=FakeData.stats(id).getServedToday(),beforeNoShows=FakeData.stats(id).getNoShowsToday();
        searchTicket("0");assertEquals(View.VISIBLE,activity.findViewById(R.id.lookup_result).getVisibility());assertTrue(((TextView)activity.findViewById(R.id.lookup_name)).getText().toString().contains(worst));
        String phone=((TextView)activity.findViewById(R.id.lookup_phone)).getText().toString();assertTrue(phone.contains(WaitingTicketAdapter.maskPhone(activity,"09171234567")));assertFalse(phone.contains("09171234567"));capture("lookup-zero-unicode-masked");captureLookupEnd("lookup-zero-unicode-end");
        searchTicket("1");assertTrue(((TextView)activity.findViewById(R.id.lookup_phone)).getText().toString().contains(activity.getString(R.string.preview_not_provided)));capture("lookup-missing-phone");
        searchTicket("2");assertTrue(((TextView)activity.findViewById(R.id.lookup_status)).getText().toString().contains(activity.getString(R.string.my_tickets_label_called)));
        searchTicket("3");assertTrue(((TextView)activity.findViewById(R.id.lookup_status)).getText().toString().contains(activity.getString(R.string.history_status_served)));capture("lookup-finished");captureLookupEnd("lookup-finished-end");
        for(String invalid:new String[]{"", "abc", "-1", "2147483648"}){searchTicket(invalid);assertNotNull(((TextInputLayout)activity.findViewById(R.id.lookup_input_layout)).getError());assertEquals(View.GONE,activity.findViewById(R.id.lookup_result).getVisibility());assertEquals("",((TextView)activity.findViewById(R.id.lookup_name)).getText().toString());}capture("lookup-invalid-clears-result");
        searchTicket("777");assertEquals(View.GONE,activity.findViewById(R.id.lookup_result).getVisibility());assertEquals(activity.getString(R.string.lookup_not_found),((TextView)activity.findViewById(R.id.lookup_message)).getText().toString());capture("lookup-not-found-no-foreign-leak");
        assertEquals(beforeWaiting,field(fixture.getClass(),"waiting").get(fixture));assertEquals(beforeServing,field(fixture.getClass(),"nowServing").get(fixture));assertEquals(beforeFinished,field(FakeData.class,"finished").get(null));assertEquals(beforeServed,FakeData.stats(id).getServedToday());assertEquals(beforeNoShows,FakeData.stats(id).getNoShowsToday());
    }
    void searchTicket(String value){ins.runOnMainSync(()->{android.widget.EditText input=activity.findViewById(R.id.lookup_input);input.setKeyListener(android.text.method.TextKeyListener.getInstance());input.setText(value);});idle();
        ins.runOnMainSync(()->{TextView label=activity.findViewById(R.id.lookup_field_label);assertEquals(R.id.lookup_input,label.getLabelFor());assertTrue(label.getBottom()<=activity.findViewById(R.id.lookup_input_layout).getTop());});
        click(activity,R.id.lookup_search);}
    void captureLookupEnd(String state)throws Exception{
        ins.runOnMainSync(()->{View status=activity.findViewById(R.id.lookup_status);status.requestRectangleOnScreen(new Rect(0,0,status.getWidth(),status.getHeight()),true);});idle();
        ins.runOnMainSync(()->{for(int id:new int[]{R.id.lookup_phone,R.id.lookup_status}){TextView text=activity.findViewById(id);Rect visible=new Rect();assertTrue("Lookup result end reachable",text.getGlobalVisibleRect(visible));assertEquals(text.getHeight(),visible.height());assertEquals(text.getWidth(),visible.width());assertNotNull(text.getLayout());for(int line=0;line<text.getLineCount();line++)assertEquals("Lookup result has no ellipsis",0,text.getLayout().getEllipsisCount(line));}});
        capture(state);
    }
    void publicProfileChecks()throws Exception{
        ins.runOnMainSync(()->{try{
            FakeData.saveQueue(queue.toBuilder().setId("public-open").setName("Public community service 王秀英").setOrganizer("public-owner",worst,true).setStatus(Queue.Status.OPEN).build());
            FakeData.saveQueue(queue.toBuilder().setId("public-paused").setName("Public paused service").setOrganizer("public-owner",worst,true).setStatus(Queue.Status.PAUSED).build());
            FakeData.saveQueue(queue.toBuilder().setId("public-closed").setOrganizer("public-owner",worst,true).setStatus(Queue.Status.CLOSED).build());
            FakeData.saveQueue(queue.toBuilder().setId("public-foreign").setOrganizer("different-owner","Other public organizer",false).build());
            FakeData.saveQueue(queue.toBuilder().setId("public-empty").setOrganizer("empty-owner",null,false).setStatus(Queue.Status.CLOSED).build());
            field(FakeData.class,"myRequest").set(null,new VerificationRequest("private-fixture",FakeData.MY_ORGANIZER_ID,"PRIVATE REGISTRATION",VerificationRequest.OrganizationType.OTHER,"PRIVATE POSITION","09998887777",VerificationStatus.PENDING,"PRIVATE ADMIN NOTE",java.time.Instant.now(),null));
        }catch(Exception e){throw new AssertionError(e);}});
        activity=launch(new Intent(ins.getTargetContext(),QueueDetailActivity.class).putExtra(QueueDetailActivity.EXTRA_QUEUE_ID,"public-open"));idle();
        click(activity,R.id.detail_organizer_row);assertEquals(View.VISIBLE,window(R.id.notice_secondary).getVisibility());dialogClick(R.id.notice_primary);
        ins.runOnMainSync(()->{FakeData.setDebugMode(true);FakeData.setDebugDelayMs(3000);});
        click(activity,R.id.detail_public_profile);activity=resumed(PublicOrganizerActivity.class);
        assertEquals("public-owner",activity.getIntent().getStringExtra(PublicOrganizerActivity.EXTRA_ORGANIZER_ID));assertEquals(View.VISIBLE,activity.findViewById(R.id.public_profile_loading).getVisibility());assertEquals(View.GONE,activity.findViewById(R.id.public_profile_content).getVisibility());capture("public-organizer-loading");awaitPublicContent();
        ins.runOnMainSync(()->FakeData.setDebugMode(false));
        assertEquals(worst,((TextView)activity.findViewById(R.id.public_profile_name)).getText().toString());
        androidx.recyclerview.widget.RecyclerView profileList=activity.findViewById(R.id.public_profile_content);assertEquals(3,profileList.getAdapter().getItemCount());assertPublicPrivacy();capture("public-organizer-profile");
        click(activity,R.id.public_profile_badge);assertEquals(View.GONE,window(R.id.notice_secondary).getVisibility());dialogClick(R.id.notice_primary);
        click(activity,R.id.public_profile_related);activity=resumed(PublicOrganizationActivity.class);awaitPublicContent();
        assertEquals("public-owner",activity.getIntent().getStringExtra(PublicOrganizationActivity.EXTRA_ORGANIZER_ID));assertEquals(activity.getString(R.string.public_organization_title),((TextView)activity.findViewById(R.id.public_profile_title)).getText().toString());assertPublicPrivacy();capture("public-organization-profile");
        click(activity,R.id.public_profile_related);activity=resumed(PublicOrganizerActivity.class);awaitPublicContent();
        ins.runOnMainSync(()->((androidx.recyclerview.widget.RecyclerView)activity.findViewById(R.id.public_profile_content)).scrollToPosition(1));idle();
        ins.runOnMainSync(()->{androidx.recyclerview.widget.RecyclerView list=activity.findViewById(R.id.public_profile_content);assertTrue(list.findViewHolderForAdapterPosition(1).itemView.performClick());});activity=resumed(QueueDetailActivity.class);String selectedQueue=activity.getIntent().getStringExtra(QueueDetailActivity.EXTRA_QUEUE_ID);assertTrue(selectedQueue.equals("public-open")||selectedQueue.equals("public-paused"));
        click(activity,R.id.detail_report);activity=resumed(ReportQueueActivity.class);assertEquals(selectedQueue,activity.getIntent().getStringExtra(ReportQueueActivity.EXTRA_QUEUE_ID));capture("public-queue-specific-report");
        activity=launch(PublicOrganizationActivity.intent(ins.getTargetContext(),"empty-owner"));awaitPublicContent();assertEquals(activity.getString(R.string.badge_organizer_fallback),((TextView)activity.findViewById(R.id.public_profile_name)).getText().toString());assertEquals(View.VISIBLE,activity.findViewById(R.id.public_profile_empty).getVisibility());assertEquals(1,((androidx.recyclerview.widget.RecyclerView)activity.findViewById(R.id.public_profile_content)).getAdapter().getItemCount());capture("public-organization-empty-missing-name");
        activity=launch(PublicOrganizerActivity.intent(ins.getTargetContext(),"public-owner"));awaitPublicContent();
        // Inject after orientation recreation, which otherwise consumes the one-shot failure.
        ins.runOnMainSync(()->{FakeData.setDebugFailOnce(true);assertTrue(activity.findViewById(R.id.error_retry_button).performClick());});idle();
        assertEquals(View.VISIBLE,activity.findViewById(R.id.public_profile_error).getVisibility());capture("public-profile-error");click(activity,R.id.error_retry_button);awaitPublicContent();capture("public-profile-retry-content");
        activity=launch(PublicOrganizerActivity.intent(ins.getTargetContext(),"missing-public-owner"));assertEquals(View.VISIBLE,activity.findViewById(R.id.public_profile_error).getVisibility());assertEquals(View.GONE,activity.findViewById(R.id.public_profile_content).getVisibility());capture("public-profile-unknown-identity");
    }
    void awaitPublicContent(){long until=android.os.SystemClock.uptimeMillis()+6000;do{idle();if(activity.findViewById(R.id.public_profile_content).getVisibility()==View.VISIBLE)return;}while(android.os.SystemClock.uptimeMillis()<until);fail("Public profile did not reach content");}
    void assertPublicPrivacy(){String text=allText(activity.findViewById(R.id.public_profile_root));for(String privateValue:new String[]{"PRIVATE REGISTRATION","PRIVATE POSITION","09998887777","PRIVATE ADMIN NOTE"})assertFalse(text.contains(privateValue));}
    String allText(View view){StringBuilder text=new StringBuilder();if(view instanceof TextView)text.append(((TextView)view).getText());if(view instanceof android.view.ViewGroup){android.view.ViewGroup group=(android.view.ViewGroup)view;for(int i=0;i<group.getChildCount();i++)text.append(allText(group.getChildAt(i))).append(' ');}return text.toString();}
}
