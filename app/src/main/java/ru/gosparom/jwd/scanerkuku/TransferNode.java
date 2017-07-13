package ru.gosparom.jwd.scanerkuku;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.widget.Toast;

import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;


public class TransferNode {
    private Context ctx;
    private String URLServer;
    private SharedPreferences settings;
    private StringBuilder res = new StringBuilder("");

    public String getInfo() {
        return res.toString();
    }

    //send file
    public boolean doSendFile(Context ctx) {
        res = new StringBuilder("");
        this.ctx = ctx;
        Support support = new Support();

        settings = ctx.getSharedPreferences(Support.PREFS_NAME, 0);
        String host = settings.getString("host", "").toString();
        String ferry = settings.getString("ferry", "").toString();
        String networktype = settings.getString("networktype", "").toString();
        String sourcefolder = settings.getString("sourcefolder", "").toString();
        String destzipfile = settings.getString("destzipfile", "").toString();

        if(host.matches("") || ferry.matches("") || networktype.matches("") || sourcefolder.matches("") || destzipfile.matches("")) {
            res.append("DEVMESSAGE. One or more settings is null\n");
            return false;
        }

        String urlpath = ctx.getResources().getString(R.string.urlpath);
        String uuid = support.getUuid(ctx);
        String sn = support.getSerialNum();

        URLServer = host + urlpath;

        File sourceFile = new File(sourcefolder);
        if(!sourceFile.exists()) {
            res.append("DEVMESSAGE. source file not found\n");
            return false;
        }

        Map<String, String> params = new HashMap<>();
        params.put("host", host);
        params.put("urlpath", urlpath);
        params.put("ferry", ferry);
        params.put("networktype", networktype);
        params.put("sourcefolder", sourcefolder);
        params.put("destzipfile", destzipfile);
        params.put("uuid", uuid);
        params.put("sn", sn);

        String prms = support.getJsonParams(params);

        File destFile = new File(destzipfile);
        if (!destFile.exists()) {
            try {
                destFile.getParentFile().mkdirs();
                destFile.createNewFile();
            } catch (IOException e) {
                res.append("DEVMESSAGE. cannot create new dest zip file");
                return false;
            }
        }

        boolean zipRes = support.zipFolder(sourceFile, destFile);
        if(!zipRes) {
            String info = support.getInfo();
            res.append(info);
            //res.append("error on zip file\n");
            return false;
        }

        return sendToServer(prms, destFile);
    }


    //http send
    private boolean sendToServer(String params, File file) {
        String CRLF = "\r\n";// Line separator required by multipart/form-data.
        URLConnection connection;
        String boundary = Long.toHexString(System.currentTimeMillis());//Just generate some unique random value.

        OutputStream os = null;
        PrintWriter writer = null;
        FileInputStream fis = null;
        BufferedReader responseBr = null;


        try {
            //request
            connection = new URL(URLServer).openConnection();
            connection.setDoInput(true);
            connection.setDoOutput(true);

            connection.setConnectTimeout(15000);
            connection.setReadTimeout(10000);

            connection.setRequestProperty("Accept-Charset", "UTF-8");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            connection.setRequestProperty("Accept", "*/*" );

            //
            os = connection.getOutputStream();
            writer = new PrintWriter(new OutputStreamWriter(os, "UTF-8"), true);

            //params
            writer.append("--" + boundary).append(CRLF);
            writer.append("Content-Type: application/json").append(CRLF);
            writer.append("Content-Disposition: form-data; name=\"params\"").append(CRLF).flush();
            writer.append(CRLF).append(params).append(CRLF).flush();

            //--file
            writer.append("--" + boundary).append(CRLF);
            writer.append("Content-Disposition: form-data; name=\"binaryFile\"; filename=\"" + file.getName() + "\"").append(CRLF);
            writer.append("Content-Type: " + URLConnection.guessContentTypeFromName(file.getName())).append(CRLF);
            writer.append("Content-Transfer-Encoding: binary").append(CRLF);


            //writer.append("Content-Transfer-Encoding: binary").append(CRLF);
            writer.append(CRLF).flush();

            fis = new FileInputStream(file);
            final byte[] buffer = new byte[4096];
            int c;
            while ((c = fis.read(buffer)) != -1) {
                os.write(buffer, 0, c);
                os.flush();
            }

            os.flush();
            writer.append(CRLF).flush();//CRLF is important! It indicates end of boundary.
            // End of multipart/form-data.
            writer.append("--" + boundary + "--").append(CRLF).flush();

            //res
            HttpURLConnection http = (HttpURLConnection)connection;

            int status = http.getResponseCode();
            if (status >= 200 && status <= 299) {
                responseBr = new BufferedReader(new InputStreamReader((connection.getInputStream())));
            } else {
                responseBr = new BufferedReader(new InputStreamReader((((HttpURLConnection) connection).getErrorStream())));
            }

            StringBuilder sb = new StringBuilder();
            String output;
            while ((output = responseBr.readLine()) != null) {
                sb.append(output);
            }
            res.append("DEVMESSAGE. zip file transfered\n");
        } catch (Exception e) {
            res.append("DEVMESSAGE. error on transmitting zip file to server\n");
            return false;
        } finally {
            IOUtils.closeQuietly(writer);
            IOUtils.closeQuietly(os);
            IOUtils.closeQuietly(fis);
            IOUtils.closeQuietly(responseBr);
        }

        return true;
    }
}
