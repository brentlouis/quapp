package com.example.quapp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/** Owner-only exact ticket-number lookup within one queue. No account or cross-queue search. */
public class OrganizerLookupActivity extends AppCompatActivity {
    public static final String EXTRA_QUEUE_ID="com.example.quapp.EXTRA_LOOKUP_QUEUE_ID";
    private String queueId;
    public static Intent intent(Context context,String queueId){return new Intent(context,OrganizerLookupActivity.class).putExtra(EXTRA_QUEUE_ID,queueId);}
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);setContentView(R.layout.activity_organizer_lookup);SystemBars.applyPaddingWithKeyboard(findViewById(R.id.lookup_root));
        queueId=getIntent().getStringExtra(EXTRA_QUEUE_ID);if(!FakeData.ownsQueue(queueId)){finish();return;}
        ((TextView)findViewById(R.id.lookup_queue)).setText(FakeData.queueById(queueId).getName());
        findViewById(R.id.lookup_back).setOnClickListener(v->finish());findViewById(R.id.lookup_search).setOnClickListener(v->search());
    }
    @Override protected void onResume(){super.onResume();if(queueId!=null&&!FakeData.ownsQueue(queueId))finish();}
    private void search(){
        findViewById(R.id.lookup_result).setVisibility(View.GONE);findViewById(R.id.lookup_message).setVisibility(View.GONE);
        for(int id:new int[]{R.id.lookup_number,R.id.lookup_name,R.id.lookup_phone,R.id.lookup_status})((TextView)findViewById(id)).setText("");
        TextInputLayout layout=findViewById(R.id.lookup_input_layout);String value=Forms.text((TextInputEditText)findViewById(R.id.lookup_input));
        int number;try{if(!value.matches("[0-9]+"))throw new NumberFormatException();number=Integer.parseInt(value);}catch(NumberFormatException error){layout.setError(getString(R.string.lookup_invalid));return;}
        layout.setError(null);
        Ticket ticket;try{ticket=FakeData.findTicketByNumber(queueId,number);}catch(SecurityException|IllegalArgumentException error){message(R.string.lookup_denied);return;}
        if(ticket==null){message(R.string.lookup_not_found);return;}
        ((TextView)findViewById(R.id.lookup_number)).setText(getString(R.string.ticket_number_format,ticket.getTicketNumber()));
        ((TextView)findViewById(R.id.lookup_name)).setText(getString(R.string.lookup_name_format,available(ticket.getHolderName())));
        ((TextView)findViewById(R.id.lookup_phone)).setText(getString(R.string.lookup_phone_format,available(WaitingTicketAdapter.maskPhone(this,ticket.getHolderPhone()))));
        ((TextView)findViewById(R.id.lookup_status)).setText(getString(R.string.lookup_status_format,getString(statusLabel(ticket.getStatus()))));
        findViewById(R.id.lookup_result).setVisibility(View.VISIBLE);
    }
    private String available(String value){return Validation.isBlank(value)?getString(R.string.preview_not_provided):value;}
    private void message(int text){((TextView)findViewById(R.id.lookup_message)).setText(text);findViewById(R.id.lookup_message).setVisibility(View.VISIBLE);}
    private int statusLabel(Ticket.Status status){switch(status){case CALLED:return R.string.my_tickets_label_called;case SERVED:return R.string.history_status_served;case NO_SHOW:return R.string.history_status_no_show;case QUEUE_CLOSED:return R.string.history_status_closed;case REMOVED:return R.string.history_status_removed;default:return R.string.ticket_status_waiting;}}
}
