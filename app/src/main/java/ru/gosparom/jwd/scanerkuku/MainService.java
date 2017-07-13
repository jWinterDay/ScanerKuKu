package ru.gosparom.jwd.scanerkuku;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.widget.Toast;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONException;
import org.json.JSONObject;

public class MainService extends Service {
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

        Support support = new Support();
        String networkType = settings.getString("networktype", "").toString();
        if ((networkType.equals("wifi") && support.checkWifi(ctx)) || networkType.equals("all")) {
            //stub
        } else {
            return START_NOT_STICKY;
        }

        try {
            mSocket = IO.socket(host);
            mSocket.connect();
        } catch (URISyntaxException e) {
            //stopSelf();
            return START_NOT_STICKY;
        }

        mSocket.off();//delete all listeners
        startForeground();

        //server kuku
        mSocket.on("dev_kuku", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Thread t = new Thread(new Runnable() {
                    public void run() {
                        try {
                            Support support = new Support();
                            String ferry = settings.getString("ferry", "").toString();
                            String networkType = settings.getString("networktype", "").toString();

                            String ip4 = support.getIp4(ctx);
                            String mac = support.getMac(ctx);
                            String wifiName = support.getWifiName(ctx);
                            String sn = support.getSerialNum();
                            String host = settings.getString("host", "").toString();
                            String uuid = support.getUuid(ctx);
                            String curTimeStamp = support.getFormattedDate();

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
                            mSocket.emit("dev_kukuanswer", jsonParams);
                        } catch (Exception e) {

                        }
                    }
                });
                t.start();
            }
        });

        //get server message
        mSocket.on("dev_download", new Emitter.Listener() {
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
                        Support support = new Support();

                        Map<String, String> params = new HashMap<>();
                        params.put("rowid", finalRowid);
                        params.put("uuid", support.getUuid(ctx));
                        params.put("sn", support.getSerialNum());

                        TransferNode tn = new TransferNode();

                        params.put("info", tn.getInfo());
                        params.put("success", String.valueOf(tn.doSendFile(ctx)));
                        String jsonAnsw = support.getJsonParams(params);

                        mSocket.emit("dev_dlanswer", jsonAnsw);
                    }
                });
                t.start();
            }
        });

        //get server message
        mSocket.on("dev_setsetting", new Emitter.Listener() {
            Support gSupport = new Support();

            @Override
            public void call(Object... args) {
                Map<String, String> params = new HashMap<>();

                JSONObject data = (JSONObject) args[0];
                final String name;
                final String value;
                String browserSourceId = null;
                try {
                    name = data.getString("name");
                    value = data.getString("value");
                    browserSourceId = data.getString("browserSourceId");
                } catch (JSONException e) {
                    params.put("browserSourceId", browserSourceId);
                    params.put("info", "Неверные входные данные");
                    params.put("success", "false");

                    String jsonAnsw = gSupport.getJsonParams(params);
                    mSocket.emit("dev_setsettinganswer", jsonAnsw);

                    return;
                }

                final String finalBrowserSourceId = browserSourceId;
                Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Support support = new Support();
                        Map<String, String> params = new HashMap<>();

                        boolean res = support.setDeviceSetting(ctx, name, value);
                        String info = support.getInfo();

                        params.put("browserSourceId", finalBrowserSourceId);
                        params.put("info", info);
                        params.put("success", String.valueOf(res));

                        String jsonAnsw = support.getJsonParams(params);

                        mSocket.emit("dev_setsettinganswer", jsonAnsw);
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
