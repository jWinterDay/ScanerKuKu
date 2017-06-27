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

                        dialog.cancel();
                        if(txt.equals("1501")) {
                            checkSettings();
                        } else {
                            Toast.makeText(ctx, getResources().getString(R.string.wrong_password), Toast.LENGTH_SHORT).show();
                            finish();
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

        String host = settings.getString("host", getResources().getString(R.string.host)).toString();
        String ferry = settings.getString("ferry", getResources().getString(R.string.ferry)).toString();
        String networktype = settings.getString("networktype", getResources().getString(R.string.networktype)).toString();
        String sourcefolder = settings.getString("sourcefolder", getResources().getString(R.string.sourcefolder)).toString();
        String destzipfile = settings.getString("destzipfile", getResources().getString(R.string.destzipfile)).toString();

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
            Toast.makeText(this, getResources().getString(R.string.failed_to_save_empty_settings), Toast.LENGTH_SHORT).show();
            return;
        }

        editor.putString("host", host);
        editor.putString("ferry", ferry);
        editor.putString("networktype", networktype);
        editor.putString("sourcefolder", sourcefolder);
        editor.putString("destzipfile", destzipfile);
        editor.commit();

        startService(new Intent(this, MainService.class));
    }
}
