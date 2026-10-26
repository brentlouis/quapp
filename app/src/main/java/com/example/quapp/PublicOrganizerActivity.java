package com.example.quapp;

import android.content.Context;
import android.content.Intent;

/** Public organizer posting identity, verification explanation and public queues. */
public class PublicOrganizerActivity extends PublicProfileActivity {
    public static final String EXTRA_ORGANIZER_ID="com.example.quapp.EXTRA_PUBLIC_ORGANIZER_ID";
    public static Intent intent(Context context,String id){return new Intent(context,PublicOrganizerActivity.class).putExtra(EXTRA_ORGANIZER_ID,id);}
    @Override protected boolean organizationView(){return false;}
}
