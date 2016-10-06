package com.jamie.android.a4ypdatacollection;

import android.hardware.Sensor;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
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

    public static int uploadFile(File f, String uploadServerUrl) {

        String fileName = f.getAbsolutePath()
                .substring(f.getParentFile()
                        .getAbsolutePath()
                        .length()
                        + 1)
                .replace('\\', '/');

        HttpURLConnection conn = null;
        DataOutputStream dos = null;
        String lineEnd = "\r\n";
        String twoHypens = "--";
        String boundary = "*****";
        int bytesRead, bytesAvailable, bufferSize, serverResponseCode;
        serverResponseCode = -1;
        byte[] buffer;
        int maxBufferSize = 1 * 1024 * 1024;

        try {

            //Open a new connection to the URL
            FileInputStream fileInputStream = new FileInputStream(f);
            URL url = new URL(uploadServerUrl);

            conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("ENCTYPE", "multipart/form-data");
            conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
            conn.setRequestProperty("uploaded_file", fileName);

            dos = new DataOutputStream(conn.getOutputStream());

            dos.writeBytes(twoHypens + boundary + lineEnd);
            dos.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\";filename=\""
                    + fileName
                    + "\""
                    + lineEnd);
            dos.writeBytes(lineEnd);

            //Create a buffer of maximum size
            bytesAvailable = fileInputStream.available();
            Log.d("Utils", Integer.toString(bytesAvailable));
            bufferSize = Math.min(bytesAvailable, maxBufferSize);
            buffer = new byte[bufferSize];

            bytesRead = fileInputStream.read(buffer, 0, bufferSize);

            while (bytesRead > 0) {

                Log.d("Utils", Integer.toString(bytesRead));

                dos.write(buffer, 0, bufferSize);
                bytesAvailable = fileInputStream.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);
            }

            //Write multipart data necessary after file data...
            dos.writeBytes(lineEnd);
            dos.writeBytes(twoHypens + boundary + twoHypens + lineEnd);

            serverResponseCode = conn.getResponseCode();
            String serverResponseMessage = conn.getResponseMessage();

            Log.d("Utils", "HTTP response is: " + serverResponseMessage + ": " + serverResponseCode);


            fileInputStream.close();
            dos.flush();
            dos.close();



        } catch (MalformedURLException e) {
            Log.e("Utils", e.toString());
        } catch (IOException e) {
            Log.e("Utils", e.toString());
        }


        return serverResponseCode;
    }

    public static File zip(List<File> files, String filename) {
        File zipfile = new File(filename);
        // Create a buffer for reading the files
        byte[] buf = new byte[1024];
        try {
            // create the ZIP file
            ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipfile));
            // compress the files
            for(int i=0; i<files.size(); i++) {
                FileInputStream in = new FileInputStream(files.get(i).getCanonicalPath());
                // add ZIP entry to output stream
                out.putNextEntry(new ZipEntry(files.get(i).getName()));
                // transfer bytes from the file to the ZIP file
                int len;
                while((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                // complete the entry
                out.closeEntry();
                in.close();
            }
            // complete the ZIP file
            out.close();
            return zipfile;
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
        }
        return null;
    }
}
