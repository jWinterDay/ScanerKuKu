package ru.gosparom.jwd.scanerkuku;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.widget.Toast;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONException;
import org.json.JSONObject;

public class MainService extends Service {
    private Context ctx = this;
    private Socket mSocket = null;
    private SharedPreferences settings;

    TimerTask checkingTimerTask;
    Handler handler = new Handler();
    Timer t = new Timer();
    private long alarmDelay = 10*1000;
    private long alarmRepeat = 10*1000;

    private boolean isServiceRunning = false;



    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Support.toastMkText(ctx, "onCreate()", Toast.LENGTH_SHORT);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if(mSocket != null && mSocket.connected()) {
            try {
                mSocket.disconnect();
                mSocket.close();
            } catch (Exception e) {

            }
        }

        Support.toastMkText(ctx, "onDestroy()", Toast.LENGTH_SHORT);
    }

    private void selfChecking() {
        checkingTimerTask = new TimerTask() {
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        connectToServer();
                        //boolean isConnected = connectToServer();
                        //if(isConnected) {
                        //   t.cancel();
                        //}
                    }
                });
            }};

        t.schedule(checkingTimerTask, alarmDelay, alarmRepeat);
    }

    private boolean connectToServer() {
        if(mSocket != null && mSocket.connected()) {
            Support.toastMkText(ctx, "mSocket already connected", Toast.LENGTH_SHORT);
            return true;
        }

        settings = getSharedPreferences(Support.PREFS_NAME, 0);
        String host = settings.getString(Support.spHost, "").toString();
        String networkType = settings.getString(Support.spNetworkType, "").toString();

        if(host == null || networkType == null || host.matches("") || networkType.matches("")) {
            Support.toastMkText(ctx, "host or network is null", Toast.LENGTH_SHORT);
            return false;
        }

        Support support = new Support();
        boolean isWifi = support.checkWifi(this);
        if(networkType.equals(Support.networkWifi) && !isWifi) {
            Support.toastMkText(ctx, "wifi adapter must be switched on", Toast.LENGTH_SHORT);
            return false;
        }

        if ( networkType.equals(Support.networkWifi) || networkType.equals(Support.networkAll) ) {
            try {
                mSocket = IO.socket(host);
                mSocket.io().reconnection(true);
                mSocket.connect();
                mSocket.off();//delete all listeners
                setSocketListeners();

                //Support.toastMkText(ctx, "mSocket is OK", Toast.LENGTH_SHORT);
            } catch (URISyntaxException e) {
                Support.toastMkText(ctx, "mSocket exception", Toast.LENGTH_SHORT);
                return false;
            }
        } else {
            Support.toastMkText(ctx, String.format("network type must be '%s' or '%s'", Support.networkWifi, Support.networkAll), Toast.LENGTH_SHORT);
            return false;
        }

        boolean isConnected = mSocket.connected();
        Support.toastMkText(ctx, "mSocket.isConnected=" + String.valueOf(isConnected), Toast.LENGTH_SHORT);
        return isConnected;
    }

    private boolean setSocketListeners() {
        //server kuku
        mSocket.on("dev_kuku", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Thread t = new Thread(new Runnable() {
                    public void run() {
                        try {
                            Support support = new Support();
                            String host = settings.getString(Support.spHost, "").toString();
                            String ferry = settings.getString(Support.spFerry, "").toString();
                            String networkType = settings.getString(Support.spNetworkType, "").toString();

                            String ip4 = support.getIp4(ctx);
                            String mac = support.getMac(ctx);
                            String wifiName = support.getWifiName(ctx);
                            String sn = support.getSerialNum();
                            String uuid = support.getUuid(ctx);
                            String curTimeStamp = support.getFormattedDate();

                            Map<String, String> params = new HashMap<>();
                            params.put(Support.nwIp4, ip4);
                            params.put(Support.nwMac, mac);
                            params.put(Support.nwUuid, uuid);
                            params.put(Support.nwWifiName, wifiName);
                            params.put(Support.nwSn, sn);
                            params.put(Support.spHost, host);
                            params.put(Support.spFerry, ferry);
                            params.put(Support.spNetworkType, networkType);
                            params.put(Support.nwDeviceTimeStamp, curTimeStamp.toString());

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
            String rowid;
            String browserSourceId;

            @Override
            public void call(Object... args) {
                Map<String, String> params = new HashMap<>();

                JSONObject data = (JSONObject) args[0];

                try {
                    rowid = data.getString("rowid");
                    browserSourceId = data.getString("browserSourceId");
                } catch (JSONException e) {
                    params.put("browserSourceId", browserSourceId);
                    params.put("info", "DEVMESSAGE. Wrong input data");
                    params.put("success", "false");

                    Support support = new Support();
                    String jsonAnsw = support.getJsonParams(params);
                    mSocket.emit("dev_dlanswer", jsonAnsw);

                    return;
                }

                Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Support support = new Support();

                        Map<String, String> params = new HashMap<>();
                        params.put("browserSourceId", browserSourceId);
                        params.put("rowid", rowid);
                        params.put("uuid", support.getUuid(ctx));
                        params.put("sn", support.getSerialNum());

                        TransferNode tn = new TransferNode();
                        boolean res = tn.doSendFile(ctx);

                        params.put("info", tn.getInfo());
                        params.put("success", String.valueOf(res));
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
                    params.put("info", "DEVMESSAGE. Wrong input data");
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

        return true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!isServiceRunning) {
            selfChecking();
            startForeground();

            isServiceRunning = true;
        }
        return START_STICKY;
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
