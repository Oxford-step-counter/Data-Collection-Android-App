package com.jamie.android.a4ypdatacollection;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    private Button mStartCollectionButton;
    private Button mStopCollectionButton;
    private Button mSendDataButton;
    private EditText mFirstNameEditText;
    private EditText mLastNameEditTest;

    private SensorLogger mLogger;
    private SensorManager sensorManager;
    private File filesDir;
    private String[] sensors;

    private String firstName;
    private String lastName;

    private static final int[] sensorTypes = {Sensor.TYPE_ACCELEROMETER};

    private static int FILE_PERMISSIONS_CALLBACK = 1;
    private static final String[] FILE_PERMISSIONS = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private static final String LOG = "MainActivity";
    private static final String SERVER_URL = Server_Details.SERVER_URL;
    private static final String SP_FIRST_NAME_KEY = "com.jamie.android.a4ypdatacollection.sp_first_name";
    private static final String SP_LAST_NAME_KEY = "com.jamie.android.a4ypdatacollection.sp_last_name";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Get name data.
        SharedPreferences sp = getPreferences(MODE_PRIVATE);
        firstName = sp.getString(SP_FIRST_NAME_KEY, "");
        lastName = sp.getString(SP_LAST_NAME_KEY, "");


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

        mFirstNameEditText = (EditText) findViewById(R.id.first_name_edit_text);
        mFirstNameEditText.setText(firstName);

        mLastNameEditTest = (EditText) findViewById(R.id.last_name_edit_text);
        mLastNameEditTest.setText(lastName);

        mFirstNameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

                SharedPreferences sp = getPreferences(MODE_PRIVATE);
                SharedPreferences.Editor editor = sp.edit();
                editor.putString(SP_FIRST_NAME_KEY, s.toString());
                editor.commit();
                firstName = s.toString();
                Log.d(LOG, lastName);
            }
        });

        mLastNameEditTest.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

                SharedPreferences sp = getPreferences(MODE_PRIVATE);
                SharedPreferences.Editor editor = sp.edit();
                editor.putString(SP_LAST_NAME_KEY, s.toString());
                editor.commit();

                lastName = s.toString();
                Log.d(LOG, lastName);

            }
        });

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

                //Create name of zip: UUID + timestamp.
                TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
                Calendar c = Calendar.getInstance();
                String zipName = firstName + "." + lastName + '.' + Utils.formatDate(c.getTime()) + ".zip";
                Log.d(LOG, zipName);

                createMetadataFile(filesDir);

                File[] listOfFiles = filesDir.listFiles();
                File zip = Utils.zip(Arrays.asList(listOfFiles), filesDir.getAbsolutePath() + "/" + zipName);

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

    private void createMetadataFile(File filesDir) {

        String output = "";
        for (int type : sensorTypes) {
            Sensor sensor;
            if (sensorManager.getDefaultSensor(type) != null) {
                sensor = sensorManager.getDefaultSensor(type);
                String sensor_type = Utils.mapSensorType(type);
                String sensor_name = sensor.getName();

                output += sensor_type + " : " + sensor_name + "\n";
            }
        }

        try {
            File metaData = new File(filesDir + "/metadata.txt");
            FileOutputStream outputStream = new FileOutputStream(metaData);
            OutputStreamWriter out = new OutputStreamWriter(outputStream);
            out.append(output);
            out.flush();
            out.close();
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
