package com.example.quapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import java.util.UUID;

/**
 * Who's signed in and which side of the app they use. Stored in SharedPreferences so it
 * survives the app being closed — that's the "session persistence".
 *
 * Signed in means holding a token from the server (/auth/login or /auth/register). ApiClient
 * sends it with every request. The password is never stored; only the server sees it.
 */
public class Session {

    public enum Role {
        QUEUER,
        OWNER
    }

    private static final String PREFS_NAME = "quapp_session";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_NAME = "name";
    private static final String KEY_PHONE = "phone";
    private static final String KEY_ROLE = "role";
    private static final String KEY_TOWN = "town";
    private static final String KEY_NOTIFICATIONS_ASKED = "notifications_asked";

    /**
     * What this install keeps across accounts, so signing out doesn't reset it: its install
     * id. The server allows 2 accounts per id (the device limit, canvas 56; MODELS.md "User").
     */
    private static final String DEVICE_PREFS_NAME = "quapp_device";
    private static final String KEY_INSTALL_ID = "install_id";

    private final SharedPreferences prefs;
    private final SharedPreferences devicePrefs;

    public Session(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        devicePrefs = context.getApplicationContext()
                .getSharedPreferences(DEVICE_PREFS_NAME, Context.MODE_PRIVATE);
    }

    public boolean isLoggedIn() {
        return getToken() != null;
    }

    /** The server's token for "Authorization: Bearer …", or null when signed out. */
    @Nullable
    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    @Nullable
    public String getUserId() {
        return prefs.getString(KEY_USER_ID, null);
    }

    /** Null before anyone has signed in on this install. */
    @Nullable
    public String getName() {
        return prefs.getString(KEY_NAME, null);
    }

    public String getPhone() {
        return prefs.getString(KEY_PHONE, "");
    }

    /** Null until the user picks one on Role Select. */
    @Nullable
    public Role getRole() {
        String stored = prefs.getString(KEY_ROLE, null);
        return stored == null ? null : Role.valueOf(stored);
    }

    /**
     * Signed in, by register or login: keeps the token and who it belongs to. The role is
     * asked again (Role Select), since it may be a different person on a shared phone.
     */
    public void signIn(String token, User user) {
        prefs.edit()
                .putString(KEY_TOKEN, token)
                .putString(KEY_USER_ID, user.getId())
                .putString(KEY_NAME, user.getName())
                .putString(KEY_PHONE, user.getPhone())
                .remove(KEY_ROLE)
                .apply();
    }

    /** Forgets the token. The phone number stays, so Login can offer it again. */
    public void logOut() {
        prefs.edit()
                .remove(KEY_TOKEN)
                .remove(KEY_USER_ID)
                .remove(KEY_ROLE)
                .apply();
    }

    /**
     * This install's id, made once and kept for good: a random UUID, so it says nothing about
     * the phone itself. Sent with registration for the device limit.
     */
    public String installId() {
        String id = devicePrefs.getString(KEY_INSTALL_ID, null);
        if (id == null) {
            id = UUID.randomUUID().toString();
            devicePrefs.edit().putString(KEY_INSTALL_ID, id).apply();
        }
        return id;
    }

    /** The town Browse is set to. Null until the first-visit question is answered. */
    @Nullable
    public String getTown() {
        return prefs.getString(KEY_TOWN, null);
    }

    public void setTown(String town) {
        prefs.edit().putString(KEY_TOWN, town).apply();
    }

    /** The notification permission screen (canvas 20) is shown once, after the first join. */
    public boolean wasAskedForNotifications() {
        return prefs.getBoolean(KEY_NOTIFICATIONS_ASKED, false);
    }

    public void setAskedForNotifications() {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS_ASKED, true).apply();
    }

    public void setRole(Role role) {
        prefs.edit().putString(KEY_ROLE, role.name()).apply();
    }

    /**
     * Where this user belongs right now: Login, Role Select, or one of the two homes.
     * CLEAR_TASK wipes the back stack, so Back from home exits the app instead of
     * walking back through Login.
     */
    public Intent homeIntent(Context context) {
        Class<?> destination;

        if (!isLoggedIn()) {
            destination = LoginActivity.class;
        } else if (getRole() == Role.OWNER) {
            destination = OwnerHomeActivity.class;
        } else if (getRole() == Role.QUEUER) {
            destination = QueuerHomeActivity.class;
        } else {
            destination = RoleSelectActivity.class;
        }

        Intent intent = new Intent(context, destination);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        return intent;
    }
}
