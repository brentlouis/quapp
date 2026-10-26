package com.example.quapp;

import android.content.Context;
import android.content.Intent;

/** Separate public organization destination under the approved one-to-one demo association. */
public class PublicOrganizationActivity extends PublicProfileActivity {
    public static final String EXTRA_ORGANIZER_ID="com.example.quapp.EXTRA_PUBLIC_ORGANIZATION_ORGANIZER_ID";
    public static Intent intent(Context context,String id){return new Intent(context,PublicOrganizationActivity.class).putExtra(EXTRA_ORGANIZER_ID,id);}
    @Override protected boolean organizationView(){return true;}
}
