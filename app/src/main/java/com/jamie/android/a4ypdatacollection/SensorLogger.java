/**
 * Sensor logger by Dario Salvi </dariosalvi78@gmail.com>
 * License: MIT
 */
package com.jamie.android.a4ypdatacollection;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.LinkedList;
import java.util.List;

/**
 * Logs a set of sensors into separate files
 * Created by Dario Salvi on 05/11/2015.
 */
public class SensorLogger {

    //standard sensors stuff
    SensorManager sensorMng;
    List<Sensor> sensors = new LinkedList<Sensor>();
    List<SensorEventListener> sensorListeners = new LinkedList<SensorEventListener>();

    //orientation stuff
    float[] mGravity;
    float[] mGeomagnetic;
    SensorEventListener orientationListener;

    //location stuff
    LocationManager locationMng;
    LocationListener locListener = null;

    List<OutputStreamWriter> writers = new LinkedList<OutputStreamWriter>();

    boolean logging = false;

	/**
	* Constructor
	* @param sensortypes an array of sensors to be logged, as strung. Available values are:
	* location, orientation, accelerometer, gravity, gyroscope, rotation, magnetometer,
	* light, setpcounter, heartrate
	*/
    public SensorLogger(Context ctx, String[] sensortypes) throws IOException {
        Log.d(SensorLogger.class.getName(), "Creating SensorLogger object.");
        locationMng = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
        sensorMng = (SensorManager) ctx.getSystemService(Context.SENSOR_SERVICE);

        for (String type : sensortypes) {
            //start file
            if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                Log.e(SensorLogger.class.getName(), "SD card not writeable");
                throw new IOException("External storage is not writeable");
            }

            File file = new File(ctx.getExternalFilesDir(null) + "/" + type + ".csv");
            Log.d(SensorLogger.class.getName(), file.getPath());
            FileOutputStream outputStream = new FileOutputStream(file);
            final OutputStreamWriter out = new OutputStreamWriter(outputStream);
            writers.add(out);

            if(type.equalsIgnoreCase("location")){
                locListener = new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                        long ts = location.getTime();
                        float acc = location.getAccuracy();
                        String line = ts + "," + acc + "," + location.getLatitude() + "," + location.getLongitude() + "," + location.getAltitude() + "," + location.getSpeed() + "," + location.getBearing() + "\n";
                        try {
                            out.append(line);
                            out.flush();
                        } catch (IOException ex) {
                            Log.e(SensorLogger.class.getName(), "Error while writing log on file", ex);
                        }
                    }
                    @Override
                    public void onStatusChanged(String provider, int status, Bundle extras) {
                        //nothing to do
                    }
                    @Override
                    public void onProviderEnabled(String provider) {
                        //nothing to do
                    }
                    @Override
                    public void onProviderDisabled(String provider) {
                        //nothing to do
                    }
                };
            } else if (type.equalsIgnoreCase("orientation")){
                orientationListener = new SensorEventListener() {
                    @Override
                    public void onSensorChanged(SensorEvent event) {
                        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
                            mGravity = event.values;
                        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
                            mGeomagnetic = event.values;
                        if (mGravity != null && mGeomagnetic != null) {
                            float R[] = new float[9];
                            float I[] = new float[9];
                            boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
                            if (success && (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)) {
                                float orientation[] = new float[3];
                                SensorManager.getOrientation(R, orientation);
                                long ts = event.timestamp;
                                int acc = event.accuracy;
                                String line = ts + "," + acc;
                                for (int j = 0; j < orientation.length; j++) {
                                    line += "," + orientation[j];
                                }
                                line += "\n";
                                try {
                                    out.append(line);
                                } catch (IOException ex) {
                                    Log.e(SensorLogger.class.getName(), "Error while writing log on file", ex);
                                }
                            }
                        }
                    }
                    @Override
                    public void onAccuracyChanged(Sensor sensor, int accuracy) {
                    }
                };
            } else {
                if (type.equals("accelerometer")) {
                    sensors.add(sensorMng.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
                } else if (type.equals("gravity")) {
                    sensors.add(sensorMng.getDefaultSensor(Sensor.TYPE_GRAVITY));
                } else if (type.equals("gyroscope")) {
                    sensors.add(sensorMng.getDefaultSensor(Sensor.TYPE_GYROSCOPE));
                } else if (type.equals("rotation")) {
                    sensors.add(sensorMng.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR));
                } else if (type.equals("magnetometer")) {
                    sensors.add(sensorMng.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD));
                } else if (type.equals("light")) {
                    sensors.add(sensorMng.getDefaultSensor(Sensor.TYPE_LIGHT));
                } else if (type.equals("setpcounter")) {
                    sensors.add(sensorMng.getDefaultSensor(Sensor.TYPE_STEP_COUNTER));
                } else if (type.equals("heartrate")) {
                    sensors.add(sensorMng.getDefaultSensor(Sensor.TYPE_HEART_RATE));
                } else {
                    throw new IllegalArgumentException("Unknown sensor type " + type);
                }

                sensorListeners.add(new SensorEventListener() {
                    @Override
                    public void onSensorChanged(SensorEvent event) {
                        long ts = event.timestamp;
                        int acc = event.accuracy;
                        String line = ts + "," + acc;
                        for (int j = 0; j < event.values.length; j++) {
                            line += "," + event.values[j];
                        }
                        line += "\r\n";
                        try {
                            out.append(line);
                        } catch (IOException ex) {
                            Log.e(SensorLogger.class.getName(), "Error while writing log on file", ex);
                        }
                    }
                    @Override
                    public void onAccuracyChanged(Sensor sensor, int accuracy) {
                        //nothing to do here
                    }
                });
            }
        }
    }

    public void start() {
        if(logging)
            return;

        for (int i = 0; i < sensors.size(); i++) {
            sensorMng.registerListener(sensorListeners.get(i), sensors.get(i), SensorManager.SENSOR_DELAY_FASTEST);
        }
        if(orientationListener != null){
            sensorMng.registerListener(orientationListener, sensorMng.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_FASTEST);
            sensorMng.registerListener(orientationListener, sensorMng.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_FASTEST);
        }
        if (locListener != null) {
            Criteria crit = new Criteria();
            crit.setAccuracy(Criteria.ACCURACY_FINE);
            try {
                locationMng.requestLocationUpdates(500, 0, crit, locListener, null);
            } catch (SecurityException ex) {
                Log.e(SensorLogger.class.getName(), "Cannot access location, permission denied", ex);
            }
        }
        logging = true;
    }

    public void stop() {
        if(!logging)
            return;

        for (int i = 0; i < sensors.size(); i++) {
            sensorMng.unregisterListener(sensorListeners.get(i));
        }
        if(orientationListener != null){
            sensorMng.unregisterListener(orientationListener);
            orientationListener = null;
        }
        if (locListener != null) {
            try{
                locationMng.removeUpdates(locListener);
            } catch (SecurityException ex){
                Log.e(SensorLogger.class.getName(), "Cannot access location, permission denied", ex);
            }
            locListener = null;
        }

        for(OutputStreamWriter writer : writers){
            try {
                writer.close();
            } catch (IOException ex) {
                Log.e(SensorLogger.class.getName(), "Error while closing log file", ex);
            }
        }
        logging = false;
    }

    public boolean isLogging(){
        return logging;
    }
}
