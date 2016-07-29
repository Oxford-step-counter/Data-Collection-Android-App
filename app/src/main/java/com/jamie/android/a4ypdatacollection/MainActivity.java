package com.jamie.android.a4ypdatacollection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Parcel;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private Button mStartCollectionButton;
    private Button mStopCollectionButton;
    private Button mSendDataButton;
    private Receiver mReceiver;
    private DataStructure data;

    private static final String LOG = "MainActivity";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Wire up start data collection button
        mStartCollectionButton = (Button) findViewById(R.id.start_service_button);
        mStartCollectionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, AccelService.class);
                startService(i);
            }
        });

        //Wire up stop data collection button
        mStopCollectionButton = (Button) findViewById(R.id.stop_service_button);
        mStopCollectionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, AccelService.class);
                stopService(i);
            }
        });

        //Initialize send data button to not enabled.
        mSendDataButton = (Button) findViewById(R.id.send_collected_data_button);
        mSendDataButton.setEnabled(false);
        mSendDataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String data_s = data.getString();

                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("*/*");
                i.putExtra(Intent.EXTRA_EMAIL, "jamiebrynes7@gmail.com");
                i.putExtra(Intent.EXTRA_SUBJECT, "4YP Data Collection");
                i.putExtra(Intent.EXTRA_TEXT, data_s);

                if (i.resolveActivity(getPackageManager()) != null )
                {
                    startActivity(i);
                }
            }
        });


    }

    @Override
    protected void onStart() {

        super.onStart();

        mReceiver = new Receiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(AccelService.RETURN_DATA);
        registerReceiver(mReceiver, intentFilter);


    }

    @Override
    protected void onStop()
    {
        super.onStop();
        unregisterReceiver(mReceiver);
    }




    private class Receiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context c, Intent i)
        {
            Bundle b_data = i.getExtras();

            data = b_data.getParcelable("data");

            Log.d(LOG, Integer.toString(data.getLength()));


            if (!mSendDataButton.isEnabled())
            {
                mSendDataButton.setEnabled(true);
            }


        }
    }
}
