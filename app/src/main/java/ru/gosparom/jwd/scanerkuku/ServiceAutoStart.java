package ru.gosparom.jwd.scanerkuku;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

public class ServiceAutoStart extends BroadcastReceiver{

    @Override
    public void onReceive(Context context, Intent arg1) {
        //Intent in = new Intent(context, ServiceAutoStart.class);
        //PendingIntent pi = PendingIntent.getBroadcast(context, 0, in, 0);
        //AlarmManager alarm0 = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        //alarm0.setRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 5000, 10*1000, pi);

        //Intent intent = new Intent(context, MainService.class);
        //context.startService(intent);

        //boolean isServiceRunning = new Support().isServiceRunning(context, MainService.class);
        //if(isServiceRunning) {
        //    alarm0.cancel(pi);
        //}

        Intent intent = new Intent(context, MainService.class);

        PendingIntent startPIntent = PendingIntent.getService(context, 0, intent, 0);
        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarm.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 35*1000, startPIntent);

        //context.startService(intent);
    }
}
