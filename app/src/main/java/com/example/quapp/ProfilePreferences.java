package com.example.quapp;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;

/** Device choices; never clears authentication, tickets or reports. */
final class ProfilePreferences {
    static SharedPreferences get(Context context) {
        return context.getApplicationContext().getSharedPreferences("quapp_profile_preferences", 0);
    }
    static void order(Context context, List<Queue> queues) {
        String order = get(context).getString("order", "Recommended");
        if ("Name".equals(order)) queues.sort(Comparator.comparing(Queue::getName, String.CASE_INSENSITIVE_ORDER));
        if ("Shortest wait".equals(order) || "Shortest demo wait".equals(order)) queues.sort(Comparator.comparingInt(Queue::getEstimatedWaitMinutes));
    }
    static void clear(Context context) {
        new SavedServices(context).clear();
        SharedPreferences.Editor reset = get(context).edit();
        for (String key : new String[]{"large", "reduced", "area", "order", "alerts", "queueAlerts", "serviceAlerts", "appointmentAlerts"}) reset.remove(key);
        reset.apply();
    }
    static List<Report> reports(Context context) {
        List<Report> own = new ArrayList<>();
        String phone = new Session(context).getPhone();
        if (!new Session(context).isLoggedIn()) return own;
        for (Report report : FakeData.reports()) if (phone.equals(report.getReporterPhone())) own.add(report);
        return own;
    }
}
