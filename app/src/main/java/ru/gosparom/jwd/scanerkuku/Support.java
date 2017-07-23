package ru.gosparom.jwd.scanerkuku;

import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import com.google.gson.Gson;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static android.content.Context.WIFI_SERVICE;

public class Support {
    public static final int NOTIFY_ID = 6678;
    public static final String SDF = "dd.MM.yyyy HH:mm:ss";


    //shared preferences settings name
    public static final String PREFS_NAME = "jwdServiceInfo";
    public static final String spHost = "host";
    public static final String spFerry = "ferry";
    public static final String spNetworkType = "networktype";
    public static final String spSourceFolder = "sourcefolder";
    public static final String spDestzipFile = "destzipfile";
    public static final String spAutoStart = "autostart";

    //url settings
    public static final String nwUrlPath = "urlpath";
    public static final String nwUuid = "uuid";
    public static final String nwSn = "sn";
    public static final String nwIp4 = "ip4";
    public static final String nwMac = "mac";
    public static final String nwWifiName = "wifiname";
    public static final String nwDeviceTimeStamp = "devicetimestamp";



    //network types
    public static final String networkWifi = "wifi";
    public static final String networkAll = "all";


    private StringBuilder info = new StringBuilder("");

    public String getInfo() {
        return info.toString();
    }

    public String getUrlParams(Map<String, String> params){
        boolean first = true;
        StringBuilder result = new StringBuilder();

        for(Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            if (first)
                first = false;
            else
                result.append("&");

            result.append(key);
            result.append("=");
            result.append(value);
        }
        return result.toString();
    }

    //map to json
    public String getJsonParams(Map<String, String> params) {
        Gson gson = new Gson();
        String json = gson.toJson(params);

        return json.toString();
    }

    //get android serial num
    public String getSerialNum(){
        String serial;

        try {
            Class<?> c = Class.forName("android.os.SystemProperties");
            Method get = c.getMethod("get", String.class);
            serial = (String) get.invoke(c, "ro.serialno");
        } catch (Exception ignored) {
            serial = "n/a";
        }

        return serial;
    }

    //get android uid
    public String getUuid(Context ctx) {
        String uuid;

        try {
            TelephonyManager tManager = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
            uuid = tManager.getDeviceId();
        } catch(Exception e) {
            uuid = "n/a";
        }

        return uuid;
    }

    public String getIp4(Context ctx) {
        WifiManager wifiMan = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiMan.getConnectionInfo();

        if (wifiInfo == null)
            return "n/a";

        int ipAddress = wifiInfo.getIpAddress();
        String ip = String.format("%d.%d.%d.%d", (ipAddress & 0xff),(ipAddress >> 8 & 0xff),(ipAddress >> 16 & 0xff),(ipAddress >> 24 & 0xff));

        return ip;
    }

    public String getMac(Context ctx) {
        WifiManager wifiMan = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiMan.getConnectionInfo();

        if (wifiInfo == null)
            return "n/a";

        String macAddress = wifiInfo.getMacAddress();
        return macAddress;
    }

    public String getWifiName(Context ctx) {
        WifiManager wifiMan = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiMan.getConnectionInfo();

        if (wifiInfo != null) {
            NetworkInfo.DetailedState state = WifiInfo.getDetailedStateOf(wifiInfo.getSupplicantState());
            if (state == NetworkInfo.DetailedState.CONNECTED || state == NetworkInfo.DetailedState.OBTAINING_IPADDR) {
                return wifiInfo.getSSID();
            }
        }

        return "n/a";
    }

    //check wifi
    public Map<String, String> getWifiInfo(Context ctx){
        Map<String, String> result = new HashMap<>();
        WifiManager wifiMgr = (WifiManager) ctx.getSystemService(WIFI_SERVICE);

        if (wifiMgr.isWifiEnabled()) {//Wi-Fi adapter is ON
            WifiInfo wifiInfo = wifiMgr.getConnectionInfo();

            if(wifiInfo.getNetworkId() == -1) {
                result.put("result", "off");
                result.put("message", ctx.getResources().getString(R.string.not_connected_to_an_access_point));
                return result;
            }
            result.put("result", "on");
            result.put("message", ctx.getResources().getString(R.string.connected_to_an_access_point));
            return result;
        }
        else {
            result.put("result", "off");
            result.put("message", ctx.getResources().getString(R.string.wifi_adapter_is_off));
            return result;
        }
    }

    public Date getCurTimeStamp() {
        return new Date();//timeStamp;
    }

    public String getFormattedDate() {
        String timeStamp = new SimpleDateFormat(Support.SDF).format(Calendar.getInstance().getTime());
        return timeStamp;
    }

    //check wifi
    public boolean checkWifi(Context ctx){
        WifiManager wifiMgr = (WifiManager) ctx.getSystemService(WIFI_SERVICE);

        if (wifiMgr.isWifiEnabled()) {//Wi-Fi adapter is ON
            WifiInfo wifiInfo = wifiMgr.getConnectionInfo();

            if(wifiInfo.getNetworkId() == -1) {
                return false;
            }

            return true;
        }
        else {
            return false;
        }
    }

    //zip folder
    public boolean zipFolder(File sourceFolder, File destFile) {
        ZipOutputStream out = null;

        try {
            out = new ZipOutputStream(new FileOutputStream(destFile));
            File[] files = sourceFolder.listFiles();

            for (int i=0; i<files.length; i++) {
                File file = files[i];
                ZipEntry entry = new ZipEntry(file.getName());
                entry.setSize(file.length());
                entry.setTime(file.lastModified());
                out.putNextEntry(entry);

                FileInputStream in = new FileInputStream(file);
                try {
                    IOUtils.copy(in, out);
                } finally {
                    IOUtils.closeQuietly(in);
                }
                out.closeEntry();
            }
        } catch (Exception e) {
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            info.append("zipping error\n");
            info.append(errors.toString());

            return false;
        } finally {
            IOUtils.closeQuietly(out);
        }

        return true;
    }

    //set device time
    public boolean setDeviceSetting(Context ctx, String name, String value){
        SharedPreferences settings = ctx.getSharedPreferences(Support.PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();

        try {
            editor.putString(name, value);
            editor.commit();
        } catch(Exception e) {
            info.append(String.format("error on changing %s setting", name));
            return false;
        }

        info.append(String.format("setting %s changed to %s", name, value));
        return true;
    }

    public boolean isServiceRunning(Context ctx, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public static void toastMkText(Context ctx, String txt, int duration) {
        if (!MainActivity.isVisible) {
            return;
        }

        if (txt == null) {
            return;
        }

        Toast.makeText(ctx, txt, duration).show();
    }

    public static void toastMkText(Context ctx, CharSequence txt, int duration) {
        if (!MainActivity.isVisible) {
            return;
        }

        if (txt == null) {
            return;
        }

        Toast.makeText(ctx, txt, duration).show();
    }
}
