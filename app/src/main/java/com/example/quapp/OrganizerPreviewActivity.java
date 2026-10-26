package com.example.quapp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

/** Read-only review of a validated, unsaved Create/Edit form. */
public class OrganizerPreviewActivity extends AppCompatActivity {
    public static final String EXTRA_SNAPSHOT_ID="com.example.quapp.EXTRA_QUEUE_PREVIEW_SNAPSHOT_ID";
    public static Intent intent(Context context,String id){return new Intent(context,OrganizerPreviewActivity.class).putExtra(EXTRA_SNAPSHOT_ID,id);}
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);setContentView(R.layout.activity_organizer_preview);SystemBars.applyPadding(findViewById(R.id.preview_root));
        findViewById(R.id.preview_back).setOnClickListener(v->finish());
        Queue queue=PreviewSnapshots.get(getIntent().getStringExtra(EXTRA_SNAPSHOT_ID));
        if(queue==null){((TextView)findViewById(R.id.preview_notice)).setText(R.string.preview_unavailable);findViewById(R.id.preview_fields).setVisibility(View.GONE);return;}
        List<String[]> fields=new ArrayList<>();
        add(fields,R.string.create_name_label,queue.getName());add(fields,R.string.create_venue_label,queue.getVenue());
        add(fields,R.string.create_municipality_label,queue.getMunicipality());add(fields,R.string.create_category_label,getString(queue.getCategory().label));
        add(fields,R.string.create_short_label,queue.getShortDescription());add(fields,R.string.create_details_label,queue.getDetails());add(fields,R.string.create_bring_label,queue.getBring());
        add(fields,R.string.create_start_date_label,Format.day(this,queue.getStartDate()));add(fields,R.string.create_end_date_label,Format.day(this,queue.getEndDate()));
        add(fields,R.string.create_opens_label,Format.time(this,queue.getOpensAt()));add(fields,R.string.create_closes_label,Format.time(this,queue.getClosesAt()));
        add(fields,R.string.create_grace_label,onOff(queue.isGracePeriodEnabled()));add(fields,R.string.create_penalty_label,onOff(queue.isNoShowCooldownEnabled()));
        add(fields,R.string.create_proximity_label,queue.isProximityCheckEnabled()?getString(R.string.preview_radius_format,queue.getJoinRadiusMeters()):onOff(false));
        add(fields,R.string.create_otp_label,getString(R.string.create_otp_summary));
        RecyclerView list=findViewById(R.id.preview_fields);list.setLayoutManager(new LinearLayoutManager(this));list.setAdapter(new FieldsAdapter(fields));
    }
    private String onOff(boolean enabled){return getString(enabled?R.string.preview_enabled:R.string.preview_disabled);}
    private void add(List<String[]> fields,int label,String value){fields.add(new String[]{getString(label),Validation.isBlank(value)?getString(R.string.preview_not_provided):value});}
    private static final class FieldsAdapter extends RecyclerView.Adapter<FieldHolder>{
        final List<String[]> fields;FieldsAdapter(List<String[]> fields){this.fields=fields;}
        @Override public FieldHolder onCreateViewHolder(ViewGroup parent,int type){return new FieldHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_preview_field,parent,false));}
        @Override public void onBindViewHolder(FieldHolder holder,int position){String[] field=fields.get(position);holder.label.setText(field[0]);holder.value.setText(field[1]);}
        @Override public int getItemCount(){return fields.size();}
    }
    private static final class FieldHolder extends RecyclerView.ViewHolder{
        final TextView label,value;FieldHolder(View view){super(view);label=view.findViewById(R.id.preview_field_label);value=view.findViewById(R.id.preview_field_value);}
    }
}
