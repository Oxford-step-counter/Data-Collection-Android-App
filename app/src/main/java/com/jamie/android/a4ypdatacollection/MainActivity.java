package com.jamie.android.a4ypdatacollection;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v4.os.AsyncTaskCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private Button mStartCollectionButton;
    private Button mStopCollectionButton;
    private Button mSendDataButton;
    private SensorLogger mLogger;
    private SensorManager sensorManager;
    private File filesDir;
    private String[] sensors;

    private static final int[] sensorTypes = {Sensor.TYPE_ACCELEROMETER,
            Sensor.TYPE_GYROSCOPE,
            Sensor.TYPE_GRAVITY,
            Sensor.TYPE_ROTATION_VECTOR,
            Sensor.TYPE_MAGNETIC_FIELD};

    private static int FILE_PERMISSIONS_CALLBACK = 1;
    private static final String[] FILE_PERMISSIONS = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private static final String LOG = "MainActivity";
    private static final String SERVER_URL = "http://jamiebrynes.com/php/upload_4yp_data.php";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Permissions for Android >=6.0
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
        sensors = new String[types.size()];
        int k = 0;
        for (int i : types) {
            sensors[k] = Utils.mapSensorType(i);
            k++;
        }

        //Wire up start data collection button
        mStartCollectionButton = (Button) findViewById(R.id.start_service_button);
        mStartCollectionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mStopCollectionButton.setEnabled(true);
                mStartCollectionButton.setEnabled(false);
                try {
                    mLogger = new SensorLogger(MainActivity.this, sensors);
                } catch (IOException e) {
                    Log.e(LOG, "Cannot create SensorLogger object.");
                }
                mLogger.start();
            }
        });

        //Wire up stop data collection button
        mStopCollectionButton = (Button) findViewById(R.id.stop_service_button);
        mStopCollectionButton.setEnabled(false);
        mStopCollectionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mStartCollectionButton.setEnabled(true);
                mStopCollectionButton.setEnabled(false);
                mSendDataButton.setEnabled(true);
                mLogger.stop();

            }
        });

        //Initialize send data button to not enabled.
        mSendDataButton = (Button) findViewById(R.id.send_collected_data_button);
        mSendDataButton.setEnabled(false);
        mSendDataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Create zip file.
                File[] listOfFiles = filesDir.listFiles();
                File zip = Utils.zip(Arrays.asList(listOfFiles), filesDir.getAbsolutePath() + "/data.zip");

                //Remove csv files.
                for (File f : listOfFiles) {
                    f.delete();
                }
                Log.d(LOG, "Zip file created!");


                //Upload file to server.
                FileUpload fileUpload = new FileUpload();
                fileUpload.execute(zip.getAbsolutePath());
                resetState();
           }
        });
    }

    //Function to reset state --> create new Logger object.
    private void resetState() {

        mSendDataButton.setEnabled(false);
        mStartCollectionButton.setEnabled(true);
        mStopCollectionButton.setEnabled(false);

    }

    private class FileUpload extends AsyncTask<String, Void, String> {

        private ProgressDialog progressDialog;
        private File file;

        @Override
        protected String doInBackground(String... params) {

            String fileURI = params[0];

            file = new File(fileURI);
            int serverReturnCode = Utils.uploadFile(file, SERVER_URL);

            return "complete";
        }

        @Override
        protected void onPreExecute() {

            progressDialog = ProgressDialog.show(MainActivity.this,
                    "File Upload",
                    "Waiting for data collection to upload... ");
        }

        @Override
        protected void onPostExecute(String result) {
            progressDialog.dismiss();
            file.delete();
        }


    }
}
