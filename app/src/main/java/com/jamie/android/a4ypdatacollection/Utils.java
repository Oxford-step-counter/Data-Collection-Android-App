package com.jamie.android.a4ypdatacollection;

import android.hardware.Sensor;

/**
 * Created by Jamie Brynes on 10/6/2016.
 */
public class Utils {


    //Returns string mapping to sensor type as listed in SensorLogger.java
    public static String mapSensorType(int type) {

        switch (type) {
            case Sensor.TYPE_ACCELEROMETER :
                return "accelerometer";
            case Sensor.TYPE_GRAVITY :
                return "gravity";
            case Sensor.TYPE_GYROSCOPE :
                return "gyroscope";
            case Sensor.TYPE_MAGNETIC_FIELD :
                return "magnetometer";
            case Sensor.TYPE_ROTATION_VECTOR :
                return "rotation";
        }

        return "";
    }
}
