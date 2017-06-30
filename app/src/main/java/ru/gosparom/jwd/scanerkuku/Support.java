package ru.gosparom.jwd.scanerkuku;

import android.content.Context;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyManager;
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
    public static final String PREFS_NAME = "jwdServiceInfo";

    public static final int NOTIFY_ID = 6678;


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
        String timeStamp = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(Calendar.getInstance().getTime());
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
    public boolean zipFolder(Context ctx, File sourceFolder, File destFile) {
        Map<String, String> result = new HashMap<>();
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

            //statusEl.append(errors.toString());
            //result.put("result", "false");
            //result.put("message", ctx.getResources().getString(R.string.error_due_zipping));
            return false;
        } finally {
            IOUtils.closeQuietly(out);
        }

        //result.put("result", "true");
        //result.put("message", ctx.getResources().getString(R.string.zipping_is_finished));
        return true;
    }

}