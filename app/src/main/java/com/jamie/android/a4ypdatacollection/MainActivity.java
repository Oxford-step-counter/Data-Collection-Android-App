package com.jamie.android.a4ypdatacollection;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.Layout;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity implements BluetoothModule.BluetoothModuleCallback{

    final Context context = this;

    private Button mStartCollectionButton;
    private Button mStopCollectionButton;
    private Button mSendDataButton;
    private Button mConnectButton;
    private Button mNotesEditButton;

    private EditText mFileNameEditText;

    private SensorLogger mLogger;
    private SensorManager sensorManager;
    private BluetoothModule mBtModule;
    private File filesDir;
    private String[] sensors;

    private String fileName;
    private String notes;

    private Handler mConnectedHandler;
    private ProgressDialog mConnectedDialog;

    private static final int[] sensorTypes = {Sensor.TYPE_ACCELEROMETER};

    private static int FILE_PERMISSIONS_CALLBACK = 1;
    private static final String[] FILE_PERMISSIONS = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private static final String[] BLUETOOTH_PERMISSIONS = {Manifest.permission.BLUETOOTH, Manifest.permission.ACCESS_COARSE_LOCATION};
    private static final String LOG = "MainActivity";
    private static final String SERVER_URL = Server_Details.SERVER_URL;
    private static final String SP_FILE_NAME_KEY = "com.jamie.android.a4ypdatacollection.sp_first_name";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Keep screen on.
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mConnectedHandler = new Handler();

        //Get name data.
        SharedPreferences sp = getPreferences(MODE_PRIVATE);
        fileName = sp.getString(SP_FILE_NAME_KEY, "");


        getPermissions();

        //Get application files directory.
        filesDir = getExternalFilesDir(null);

        mBtModule = new BluetoothModule(this, this);

        setUpSensors();
        setUpEditText();
        setUpButtons();
    }

    //Function to reset state --> create new Logger object.
    private void resetState() {

        mSendDataButton.setEnabled(false);
        mStartCollectionButton.setEnabled(true);
        mStopCollectionButton.setEnabled(false);
        notes = null;

    }

    private void createNotesFile(File filesDir) {

        try {
            File noteFile = new File(filesDir + "/notes.txt");
            FileOutputStream outputStream = new FileOutputStream(noteFile);
            OutputStreamWriter out = new OutputStreamWriter(outputStream);
            out.append(notes);
            out.flush();
            out.close();
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    private void getPermissions() {
        //Permissions for Android >=6.0
        int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, FILE_PERMISSIONS, FILE_PERMISSIONS_CALLBACK);
        }
        permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, BLUETOOTH_PERMISSIONS, FILE_PERMISSIONS_CALLBACK);
        }
    }

    private void setUpSensors(){
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
    }

    private void setUpEditText() {
        mFileNameEditText = (EditText) findViewById(R.id.file_name_edit_text);
        mFileNameEditText.setText(fileName);

        mFileNameEditText.addTextChangedListener(new TextWatcher() {
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
                editor.putString(SP_FILE_NAME_KEY, s.toString());
                editor.commit();
                fileName = s.toString();
            }
        });

    }

    private void setUpButtons() {

        //Wire up notes editing button
        mNotesEditButton = (Button) findViewById(R.id.notes_input_button);
        mNotesEditButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Kick off a text input dialog.

                // Inflate dialog layout
                LayoutInflater li = LayoutInflater.from(context);
                View promptView = li.inflate(R.layout.note_dialog, null);
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
                alertDialogBuilder.setView(promptView);

                // Wire up edit text handling.
                final EditText userNotes = (EditText) promptView.findViewById(R.id.notes_dialog_edit_text);
                if (notes != null) {
                    userNotes.setText(notes);
                }

                alertDialogBuilder
                        .setCancelable(false)
                        .setPositiveButton("Okay", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Save data from edit text
                                notes = userNotes.getText().toString();
                                Log.d(LOG, notes);
                                dialog.dismiss();
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Do nothing
                                dialog.cancel();
                            }
                        });

                AlertDialog alert = alertDialogBuilder.create();
                alert.show();
            }
        });


        //Wire up start data collection button
        mStartCollectionButton = (Button) findViewById(R.id.start_service_button);
        mStartCollectionButton.setEnabled(false);
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
                mBtModule.start();
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
                mBtModule.stop();

            }
        });

        //Initialize send data button to not enabled.
        mSendDataButton = (Button) findViewById(R.id.send_collected_data_button);
        mSendDataButton.setEnabled(false);
        mSendDataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                //Create name of zip: UUID + timestamp.
                Calendar c = Calendar.getInstance();
                String zipName = fileName + "." + Utils.formatDate(c.getTime()) + ".zip";

                //Create supplementary files
                createMetadataFile(filesDir);
                if (notes != null) {
                    createNotesFile(filesDir);
                }

                //Get list of files + zip these.
                File[] listOfFiles = filesDir.listFiles();
                File zip = Utils.zip(Arrays.asList(listOfFiles), filesDir.getAbsolutePath() + "/" + zipName);

                //Remove created files.
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

        mConnectButton = (Button) findViewById(R.id.connect_bluetooth_button);
        mConnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBtModule.getConnected()) {
                    //Disconnect
                    mBtModule.connect();

                } else {
                    //Connect bluetooth
                    mBtModule.connect();

                    //Kickoff a dialog
                    mConnectedDialog = ProgressDialog.show(MainActivity.this, "Bluetooth Connecting", "Waiting for Bluetooth to connect...");

                    //Runnable to dismiss dialog.
                    mConnectedHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (mBtModule.getConnecting()) {
                                mConnectedHandler.postDelayed(this, 500);
                            } else {
                                mConnectedDialog.dismiss();
                                if (!mBtModule.getConnected()) {
                                    Toast toast = Toast.makeText(MainActivity.this, "Unable to connect", Toast.LENGTH_SHORT);
                                    toast.show();
                                } else {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            mConnectButton.setText(R.string.disconnect_btn);
                                            mStartCollectionButton.setEnabled(true);
                                        }
                                    });
                                }
                            }
                        }
                    });
                }
            }
        });
    }

    @Override
    public void onDisconnect() {
        if (mLogger != null) {
            mLogger.stop();
        }
        mBtModule.stop();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectButton.setText(R.string.connect_btn);
                mStartCollectionButton.setEnabled(false);
                mStopCollectionButton.setEnabled(false);
                mSendDataButton.setEnabled(false);
            }
        });
        Toast toast = Toast.makeText(this, "RFduino disconnected", Toast.LENGTH_SHORT);
        toast.show();
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
