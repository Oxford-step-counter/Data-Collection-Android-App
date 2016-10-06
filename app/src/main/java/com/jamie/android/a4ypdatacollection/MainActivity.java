package com.jamie.android.a4ypdatacollection;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private Button mStartCollectionButton;
    private Button mStopCollectionButton;
    private Button mSendDataButton;
    private SensorLogger mLogger;
    private SensorManager sensorManager;
    private File filesDir;

    private static final int[] sensorTypes = {Sensor.TYPE_ACCELEROMETER,
            Sensor.TYPE_GYROSCOPE,
            Sensor.TYPE_GRAVITY,
            Sensor.TYPE_ROTATION_VECTOR,
            Sensor.TYPE_MAGNETIC_FIELD};

    private static int FILE_PERMISSIONS_CALLBACK = 1;
    private static final String[] FILE_PERMISSIONS = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};

    private static final String LOG = "MainActivity";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Permissions for Android 6.0
        int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, FILE_PERMISSIONS, FILE_PERMISSIONS_CALLBACK);
        }

        //Get application files directory.
        filesDir = getExternalFilesDir(null);

        //Create list of sensors.
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        ArrayList<Integer> types = new ArrayList<Integer>();
        for (int i : sensorTypes) {
            //Check if we have this
            if (sensorManager.getDefaultSensor(i) != null) {
                types.add(i);
            }
        }
        //Create String array based on mappings.
        String[] sensors = new String[types.size()];
        int k = 0;
        for (int i : types) {
            sensors[k] = Utils.mapSensorType(i);
            k++;
        }

        //Create logger object.
        try {
            mLogger = new SensorLogger(this, sensors);
        } catch (IOException e) {
            Log.e(LOG, "Cannot create SensorLogger object.");
        }


        //Wire up start data collection button
        mStartCollectionButton = (Button) findViewById(R.id.start_service_button);
        mStartCollectionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLogger.start();
                if (mSendDataButton.isEnabled()) {
                    mSendDataButton.setEnabled(false);
                }
            }
        });

        //Wire up stop data collection button
        mStopCollectionButton = (Button) findViewById(R.id.stop_service_button);
        mStopCollectionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLogger.stop();
                if (!mSendDataButton.isEnabled()) {
                    mSendDataButton.setEnabled(true);
                }
            }
        });

        //Initialize send data button to not enabled.
        mSendDataButton = (Button) findViewById(R.id.send_collected_data_button);
        mSendDataButton.setEnabled(false);
        mSendDataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                /*
                TODO: Zip files that exist in the directory together and send this via POST to PHP script on website.
                */
                File[] listOfFiles = filesDir.listFiles();

                for (File f : listOfFiles) {
                    if (f.isFile()) {
                        Log.d(MainActivity.class.getName(), "File: " + f.getName());
                    } else {
                        Log.d(MainActivity.class.getName(), "Directory: " + f.getName());
                    }
                }
           }

        });


    }


}
