package ru.gosparom.jwd.scanerkuku;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ServiceAutoStart extends BroadcastReceiver{

    @Override
    public void onReceive(Context context, Intent arg1) {
        Intent intent = new Intent(context, MainService.class);
        context.startService(intent);
    }
}
