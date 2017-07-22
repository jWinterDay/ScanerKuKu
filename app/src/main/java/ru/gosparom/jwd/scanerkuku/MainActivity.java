package ru.gosparom.jwd.scanerkuku;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends Activity {
    Context ctx;
    EditText etHost;
    EditText etFerry;
    EditText etNetworkType;
    EditText etSourceFolder;
    EditText etDestZipFile;
    Button btnSave;

    public static boolean isVisible = false;
    private void setIsVisible(boolean visible) {
        isVisible = visible;
    }

    @Override
    protected void onResume() {
        super.onResume();

        setIsVisible(true);
    }

    @Override
    protected void onPause() {
        super.onPause();

        setIsVisible(false);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        //elements
        etHost = (EditText) findViewById(R.id.etHost);
        etFerry = (EditText) findViewById(R.id.etFerry);
        etNetworkType = (EditText) findViewById(R.id.etNetworkType);
        etSourceFolder = (EditText) findViewById(R.id.etSourceFolder);
        etDestZipFile = (EditText) findViewById(R.id.etDestZipFile);
        btnSave = (Button) findViewById(R.id.btnSave);
        btnSave.setEnabled(true);

        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        createPasswordDialog();
    }

    private void createPasswordDialog() {
        final EditText txtUrl = new EditText(this);
        txtUrl.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        txtUrl.setText("1501");

        new AlertDialog.Builder(this)
                .setTitle("password")
                .setView(txtUrl)
                .setCancelable(false)
                .setPositiveButton("go", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String txt = txtUrl.getText().toString();

                        //dialog.cancel();
                        String pass = getResources().getString(R.string.password);
                        if(txt != null && txt != "" && txt.equals(pass)) {
                            checkSettings();
                        } else {
                            createPasswordDialog();
                            dialog.cancel();
                            //Toast.makeText(ctx, getResources().getString(R.string.wrong_password), Toast.LENGTH_SHORT).show();
                            //finish();
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        finish();
                    }
                })
                .show();
    }

    private void checkSettings() {
        //user settings
        SharedPreferences settings = getSharedPreferences(Support.PREFS_NAME, 0);

        String host = settings.getString(Support.spHost, getResources().getString(R.string.host)).toString();
        String ferry = settings.getString(Support.spFerry, getResources().getString(R.string.ferry)).toString();
        String networktype = settings.getString(Support.spNetworkType, getResources().getString(R.string.networktype)).toString();
        String sourcefolder = settings.getString(Support.spSourceFolder, getResources().getString(R.string.sourcefolder)).toString();
        String destzipfile = settings.getString(Support.spDestzipFile, getResources().getString(R.string.destzipfile)).toString();

        etHost.setText(host);
        etFerry.setText(ferry);
        etNetworkType.setText(networktype);
        etSourceFolder.setText(sourcefolder);
        etDestZipFile.setText(destzipfile);
    }

    //btn save settings click
    public void Save(View v) {
        SharedPreferences settings = getSharedPreferences(Support.PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();

        String host = etHost.getText().toString();
        String ferry = etFerry.getText().toString();
        String networktype = etNetworkType.getText().toString();
        String sourcefolder = etSourceFolder.getText().toString();
        String destzipfile = etDestZipFile.getText().toString();

        if(host.matches("") || ferry.matches("") || networktype.matches("") || sourcefolder.matches("") || destzipfile.matches("")) {
            String txt = getResources().getString(R.string.failed_to_save_empty_settings);
            Toast.makeText(this, txt, Toast.LENGTH_SHORT).show();
            return;
        }

        editor.putString(Support.spHost, host);
        editor.putString(Support.spFerry, ferry);
        editor.putString(Support.spNetworkType, networktype);
        editor.putString(Support.spSourceFolder, sourcefolder);
        editor.putString(Support.spDestzipFile, destzipfile);
        editor.commit();

        startService(new Intent(this, MainService.class));
    }

    //btn stop
    public void Stop(View v) {
        stopService(new Intent(this, MainService.class));
    }
}
