package ru.gosparom.jwd.scanerkuku;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.view.MenuItem;

public class ServiceAutoStart extends BroadcastReceiver{

    @Override
    public void onReceive(Context context, Intent arg1) {
        //get autostart setting
        SharedPreferences settings = context.getSharedPreferences(Support.PREFS_NAME, 0);
        boolean isAutoStart = settings.getBoolean(Support.spAutoStart, false);
        if (!isAutoStart) {
            return;
        }

        Intent intent = new Intent(context, MainService.class);

        PendingIntent startPIntent = PendingIntent.getService(context, 0, intent, 0);
        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarm.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 35*1000, startPIntent);

        //context.startService(intent);
    }
}
