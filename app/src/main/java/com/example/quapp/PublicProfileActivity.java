package com.example.quapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

/** Shared readonly projection of public posting identity and queues, never account/verification records. */
abstract class PublicProfileActivity extends AppCompatActivity {
    private String organizerId;
    private LoadingStateHelper states;
    private Queue identity;
    private QueueAdapter queues;
    private HeaderAdapter header;
    private boolean empty;
    protected abstract boolean organizationView();
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);setContentView(R.layout.activity_public_profile);SystemBars.applyPadding(findViewById(R.id.public_profile_root));
        organizerId=getIntent().getStringExtra(organizationView()?PublicOrganizationActivity.EXTRA_ORGANIZER_ID:PublicOrganizerActivity.EXTRA_ORGANIZER_ID);
        ((TextView)findViewById(R.id.public_profile_title)).setText(organizationView()?R.string.public_organization_title:R.string.public_organizer_title);
        findViewById(R.id.public_profile_back).setOnClickListener(v->finish());
        RecyclerView list=findViewById(R.id.public_profile_content);list.setLayoutManager(new LinearLayoutManager(this));
        queues=new QueueAdapter(queue->{Intent detail=new Intent(this,QueueDetailActivity.class);detail.putExtra(QueueDetailActivity.EXTRA_QUEUE_ID,queue.getId());startActivity(detail);});
        header=new HeaderAdapter();list.setAdapter(new ConcatAdapter(header,queues));
        states=new LoadingStateHelper(list,null,findViewById(R.id.public_profile_loading),findViewById(R.id.public_profile_error));
        states.setOnRetryListener(this::load);load();
    }
    private void load(){
        if(Validation.isBlank(organizerId)){states.showError(getString(R.string.public_profile_unavailable));return;}
        states.showLoading(getString(R.string.public_profile_loading));
        Repository.queues(this,new Repository.Callback<List<Queue>>(){
            @Override public void onSuccess(List<Queue> result){
                if(isFinishing()||isDestroyed())return;
                Queue postingIdentity=null;List<Queue> publicQueues=new ArrayList<>();
                for(Queue queue:result)if(organizerId.equals(queue.getOrganizerId())){
                    if(postingIdentity==null)postingIdentity=queue;
                    if(queue.getStatus()!=Queue.Status.CLOSED)publicQueues.add(queue);
                }
                if(postingIdentity==null){states.showError(getString(R.string.public_profile_unavailable));return;}
                identity=postingIdentity;empty=publicQueues.isEmpty();header.notifyDataSetChanged();queues.submitQueues(publicQueues);states.showContent();
            }
            @Override public void onError(ApiError error){if(!isFinishing()&&!isDestroyed())states.showError(getString(R.string.public_profile_unavailable));}
        });
    }
    private final class HeaderAdapter extends RecyclerView.Adapter<HeaderHolder>{
        @Override public HeaderHolder onCreateViewHolder(ViewGroup parent,int type){return new HeaderHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.view_public_profile_header,parent,false));}
        @Override public void onBindViewHolder(HeaderHolder holder,int position){
            String name=identity.getOrganizerName();holder.name.setText(Validation.isBlank(name)?getString(R.string.badge_organizer_fallback):name);
            holder.note.setText(organizationView()?R.string.public_organization_demo:R.string.public_profile_scope);
            holder.badge.setText(identity.isOrganizerVerified()?R.string.badge_verified_title:R.string.badge_unverified_title);
            holder.badge.setOnClickListener(v->TrustSheets.showOrganizer(PublicProfileActivity.this,identity,false));
            holder.related.setText(organizationView()?R.string.public_view_organizer:R.string.public_view_organization);
            holder.related.setOnClickListener(v->startActivity(organizationView()?PublicOrganizerActivity.intent(PublicProfileActivity.this,organizerId):PublicOrganizationActivity.intent(PublicProfileActivity.this,organizerId)));
            holder.empty.setVisibility(empty?View.VISIBLE:View.GONE);
        }
        @Override public int getItemCount(){return identity==null?0:1;}
    }
    private static final class HeaderHolder extends RecyclerView.ViewHolder{
        final TextView name,note,badge,related;final View empty;
        HeaderHolder(View view){super(view);name=view.findViewById(R.id.public_profile_name);note=view.findViewById(R.id.public_profile_note);badge=view.findViewById(R.id.public_profile_badge);related=view.findViewById(R.id.public_profile_related);empty=view.findViewById(R.id.public_profile_empty);}
    }
}
