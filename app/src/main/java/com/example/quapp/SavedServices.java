package com.example.quapp;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashSet;
import java.util.Set;

/** Durable bookmarks on this device, separated by the existing account or guest session. */
public final class SavedServices {
    private final SharedPreferences prefs;
    private final String key;

    public SavedServices(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences("quapp_saved_services", Context.MODE_PRIVATE);
        Session session = new Session(context);
        String phone = session.isLoggedIn() ? session.getPhone() : "";
        key = phone == null || phone.isEmpty() ? "guest" : "account:" + phone;
    }

    public Set<String> ids() { return new HashSet<>(prefs.getStringSet(key, new HashSet<>())); }
    public boolean contains(String id) { return ids().contains(id); }
    public void toggle(String id) {
        Set<String> ids = ids();
        if (!ids.remove(id)) ids.add(id);
        prefs.edit().putStringSet(key, ids).apply();
    }
    public void clear() { prefs.edit().remove(key).apply(); }
}
