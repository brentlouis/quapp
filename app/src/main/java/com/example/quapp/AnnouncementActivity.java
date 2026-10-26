package com.example.quapp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/** One active owner-published plain-text message for this queue's counter displays. */
public class AnnouncementActivity extends AppCompatActivity {
    public static final String EXTRA_QUEUE_ID="com.example.quapp.EXTRA_ANNOUNCEMENT_QUEUE_ID";
    private String queueId;
    private TextInputEditText input;
    private TextInputLayout layout;
    public static Intent intent(Context context,String queueId){return new Intent(context,AnnouncementActivity.class).putExtra(EXTRA_QUEUE_ID,queueId);}
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);setContentView(R.layout.activity_announcement);SystemBars.applyPaddingWithKeyboard(findViewById(R.id.announcement_root));
        queueId=getIntent().getStringExtra(EXTRA_QUEUE_ID);if(!FakeData.ownsQueue(queueId)){finish();return;}
        input=findViewById(R.id.announcement_input);layout=findViewById(R.id.announcement_input_layout);
        if(state==null)input.setText(FakeData.announcement(queueId));
        findViewById(R.id.announcement_back).setOnClickListener(v->finish());
        findViewById(R.id.announcement_publish).setOnClickListener(v->{
            String message=Forms.text(input);
            if(!Forms.check(layout,!Validation.isBlank(message),getString(R.string.announcement_required)))return;
            if(!Forms.check(layout,message.codePointCount(0,message.length())<=500,getString(R.string.announcement_too_long)))return;
            try{FakeData.publishAnnouncement(queueId,message);render();feedback(R.string.announcement_published);}catch(SecurityException|IllegalArgumentException error){feedback(R.string.announcement_unavailable);}
        });
        findViewById(R.id.announcement_clear).setOnClickListener(v->{try{FakeData.clearAnnouncement(queueId);input.setText("");layout.setError(null);render();feedback(R.string.announcement_cleared);}catch(SecurityException error){feedback(R.string.announcement_unavailable);}});
        render();
    }
    @Override protected void onResume(){super.onResume();if(input!=null){if(!FakeData.ownsQueue(queueId)){finish();return;}render();}}
    private void render(){String message=FakeData.announcement(queueId);((TextView)findViewById(R.id.announcement_current)).setText(message==null?getString(R.string.announcement_none):getString(R.string.announcement_current_format,message));findViewById(R.id.announcement_clear).setEnabled(message!=null);}
    private void feedback(int message){QuappMotion.snackbar(findViewById(R.id.announcement_root),message,Snackbar.LENGTH_LONG).setTextMaxLines(Integer.MAX_VALUE).show();}
}
