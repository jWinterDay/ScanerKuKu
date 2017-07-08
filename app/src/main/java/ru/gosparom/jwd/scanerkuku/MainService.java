package ru.gosparom.jwd.scanerkuku;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

public class MainService extends Service {
    //Notification.Builder builder;// = new Notification.Builder(getApplicationContext());

    private Context ctx = this;
    private Socket mSocket;
    private SharedPreferences settings;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Toast.makeText(this, "ServiceKuKu created", Toast.LENGTH_SHORT).show();
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSocket.close();
        Toast.makeText(this, "ServiceKuKu stopped", Toast.LENGTH_SHORT).show();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        settings = getSharedPreferences(Support.PREFS_NAME, 0);

        String host = settings.getString("host", "").toString();
        if(host.matches("")) {
            //stopSelf();
            return START_NOT_STICKY;
        }

        try {
            mSocket = IO.socket(host);
        } catch (URISyntaxException e) {
            //stopSelf();
            return START_NOT_STICKY;
        }

        mSocket.connect();
        mSocket.off();//delete all listeners

        //showNotification();
        startForeground();

        //server kuku
        mSocket.on("kuku", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Thread t = new Thread(new Runnable() {
                    public void run() {
                        try {
                            Support support = new Support();

                            String ferry = settings.getString("ferry", "").toString();
                            String networkType = settings.getString("networktype", "").toString();

                            if ((networkType.equals("wifi") && support.checkWifi(ctx)) || networkType.equals("all")) {
                                String ip4 = support.getIp4(ctx);
                                String mac = support.getMac(ctx);
                                String wifiName = support.getWifiName(ctx);
                                String sn = support.getSerialNum();
                                String host = settings.getString("host", "").toString();
                                String uuid = support.getUuid(ctx);
                                String curTimeStamp = support.getFormattedDate();
                                //Date curTimeStamp = support.getCurTimeStamp();


                                Map<String, String> params = new HashMap<>();
                                params.put("ip4", ip4);
                                params.put("mac", mac);
                                params.put("uuid", uuid);
                                params.put("wifiname", wifiName);
                                params.put("sn", sn);
                                params.put("sn", sn);
                                params.put("host", host);
                                params.put("ferry", ferry);
                                params.put("networktype", networkType);
                                params.put("devicetimestamp", curTimeStamp.toString());

                                String jsonParams = support.getJsonParams(params);

                                updateNotification();
                                mSocket.emit("kukuanswer", jsonParams);
                            }
                        } catch (Exception e) {

                        }
                    }
                });
                t.start();
            }
        });

        //get server message
        mSocket.on("download", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                String rowid = "n/a";
                if (args.length != 0) {
                    rowid = (String) args[0];
                }

                final String finalRowid = rowid;
                Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String networkType = settings.getString("networktype", "").toString();
                        Support support = new Support();

                        if ((networkType.equals("wifi") && support.checkWifi(ctx)) || networkType.equals("all")) {
                            String deviceUuid = support.getUuid(ctx);
                            String sn = support.getSerialNum();

                            Map<String, String> params = new HashMap<>();
                            params.put("rowid", finalRowid);
                            params.put("uuid", deviceUuid);
                            params.put("sn", sn);

                            TransferNode tn = new TransferNode();
                            boolean res = tn.doSendFile(ctx);
                            String info = tn.getInfo();

                            params.put("info", info);
                            params.put("success", String.valueOf(res));
                            String jsonAnsw = support.getJsonParams(params);
                            mSocket.emit("dlanswer", jsonAnsw);
                        }
                    }
                });
                t.start();
            }
        });

        return START_STICKY;//super.onStartCommand(intent, flags, startId);
    }


    //=============================
    private Notification getMyActivityNotification(String text){
        CharSequence title = getText(R.string.service_notification_name);

        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 01, intent, PendingIntent.FLAG_ONE_SHOT);

        return new Notification.Builder(this)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(R.drawable.m24)
                .setContentIntent(pendingIntent).build();//getNotification();
    }

    private void updateNotification() {
        Support support = new Support();
        String timeStamp = support.getFormattedDate();

        Notification notification = getMyActivityNotification(timeStamp);
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(Support.NOTIFY_ID, notification);
    }

    private void startForeground() {
        startForeground(Support.NOTIFY_ID, getMyActivityNotification("service started"));
    }
}
