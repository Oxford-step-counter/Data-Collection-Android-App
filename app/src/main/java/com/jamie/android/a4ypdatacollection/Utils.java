package com.jamie.android.a4ypdatacollection;

import android.hardware.Sensor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
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

    public static void compress(File toFile, File[] files) throws IOException{

            FileOutputStream fos = new FileOutputStream(toFile);
            ZipOutputStream outs = new ZipOutputStream(fos);


            for (File f : files) {
                String filePath = f.getAbsolutePath();
                String dirPath = f.getParentFile().getAbsolutePath();
                String entryName =  filePath.substring(dirPath.length() + 1).replace('\\', '/');
                ZipEntry zipEntry = new ZipEntry(entryName);
                zipEntry.setTime(f.lastModified());
                FileInputStream ins = new FileInputStream(f);
                outs.putNextEntry(zipEntry);
                pipe(ins, outs);
                outs.closeEntry();
                ins.close();
            }
            outs.close();
    }


    private static void pipe(InputStream ins, OutputStream outs) throws IOException{
        byte[] buffer = new byte[1024];
        int count = ins.read(buffer, 0, 1024);
        while (count != -1) {
            outs.write(buffer, 0, count);
            count = ins.read(buffer, 0, 1024);
        }
    }
}
