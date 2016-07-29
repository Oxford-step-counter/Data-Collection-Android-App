package com.jamie.android.a4ypdatacollection;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;

/**
 * Created by Jamie Brynes on 7/28/2016.
 */
public class AccelService extends Service implements SensorEventListener{

    private DataStructure data;
    private SensorManager manager;
    private Sensor accelerometer;
    private long startTime;

    public static final String RETURN_DATA = "com.jamie.android.4yp.datacollection.return_data";

    @Override
    public IBinder onBind(Intent i)
    {
        return null;
    }

    @Override
    public void onCreate()
    {
        manager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null)
        {
            //There is an accelerometer.
            accelerometer = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
    }


    @Override
    public int onStartCommand(Intent i, int flags, int startID)
    {
        startTime = System.currentTimeMillis();
        manager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL );
        data = new DataStructure();
        return super.onStartCommand(i, flags, startID);
    }

    @Override
    public void onDestroy()
    {
        //Return data
        Intent i = new Intent();
        i.setAction(RETURN_DATA);
        i.putExtra("data", data);
        sendBroadcast(i);

        super.onDestroy();
    }

    @Override
    public void onAccuracyChanged(Sensor s, int acc)
    {

    }

    @Override
    public void onSensorChanged(SensorEvent e)
    {
        //Calculate time difference
        long currTime = System.currentTimeMillis();
        long delta_t = currTime - startTime;


        float x = e.values[0];
        float y = e.values[1];
        float z = e.values[2];

        data.addDataPoint(delta_t, x, y, z);
    }


}
