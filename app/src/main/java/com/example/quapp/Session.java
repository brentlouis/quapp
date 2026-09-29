package com.example.quapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

/**
 * Who's logged in and which side of the app they use. Stored in SharedPreferences
 * so it survives the app being closed — that's the "session persistence".
 *
 * Passwords are never stored. With no backend yet, login accepts any valid
 * phone and password; the real check moves to the server's auth endpoint.
 */
public class Session {

    public enum Role {
        QUEUER,
        OWNER
    }

    private static final String PREFS_NAME = "quapp_session";
    private static final String KEY_LOGGED_IN = "logged_in";
    private static final String KEY_NAME = "name";
    private static final String KEY_PHONE = "phone";
    private static final String KEY_ROLE = "role";
    private static final String KEY_TOWN = "town";
    private static final String KEY_NOTIFICATIONS_ASKED = "notifications_asked";

    /**
     * What this install remembers across accounts, so logging out doesn't reset it. Stands in
     * for the server's device_install_id count (MODELS.md) until there's a backend.
     */
    private static final String DEVICE_PREFS_NAME = "quapp_device";
    private static final String KEY_DEVICE_ACCOUNTS = "accounts";
    /** One phone can hold 2 accounts, so a shared family phone still works (canvas 56). */
    public static final int MAX_ACCOUNTS_PER_DEVICE = 2;

    private final SharedPreferences prefs;
    private final SharedPreferences devicePrefs;

    public Session(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        devicePrefs = context.getApplicationContext()
                .getSharedPreferences(DEVICE_PREFS_NAME, Context.MODE_PRIVATE);
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_LOGGED_IN, false);
    }

    /** Null if this phone never registered on this device. */
    public String getName() {
        return prefs.getString(KEY_NAME, null);
    }

    public String getPhone() {
        return prefs.getString(KEY_PHONE, "");
    }

    /** Null until the user picks one on Role Select. */
    public Role getRole() {
        String stored = prefs.getString(KEY_ROLE, null);
        return stored == null ? null : Role.valueOf(stored);
    }

    public void register(String name, String phone) {
        Set<String> accounts = deviceAccounts();
        accounts.add(phone);
        devicePrefs.edit().putStringSet(KEY_DEVICE_ACCOUNTS, accounts).apply();

        prefs.edit()
                .putBoolean(KEY_LOGGED_IN, true)
                .putString(KEY_NAME, name)
                .putString(KEY_PHONE, phone)
                .remove(KEY_ROLE)
                .apply();
    }

    /** Keeps the registered name only if it's the same phone logging back in. */
    public void logIn(String phone) {
        SharedPreferences.Editor editor = prefs.edit()
                .putBoolean(KEY_LOGGED_IN, true)
                .remove(KEY_ROLE);

        if (!phone.equals(getPhone())) {
            editor.remove(KEY_NAME);
        }

        editor.putString(KEY_PHONE, phone).apply();
    }

    /** The town Browse is set to. Null until the first-visit question is answered. */
    public String getTown() {
        return prefs.getString(KEY_TOWN, null);
    }

    public void setTown(String town) {
        prefs.edit().putString(KEY_TOWN, town).apply();
    }

    /**
     * The phone numbers that created an account on this install. A copy: Android's own set
     * from getStringSet must never be changed in place.
     */
    public Set<String> deviceAccounts() {
        Set<String> accounts = new HashSet<>(
                devicePrefs.getStringSet(KEY_DEVICE_ACCOUNTS, new HashSet<String>()));
        // Registered before this list existed: that account still counts.
        if (accounts.isEmpty() && getName() != null && !getPhone().isEmpty()) {
            accounts.add(getPhone());
        }
        return accounts;
    }

    /** The device limit (canvas 56): no third account on the same phone. */
    public boolean deviceAccountLimitReached() {
        return deviceAccounts().size() >= MAX_ACCOUNTS_PER_DEVICE;
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

    public void logOut() {
        prefs.edit()
                .putBoolean(KEY_LOGGED_IN, false)
                .remove(KEY_ROLE)
                .apply();
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
