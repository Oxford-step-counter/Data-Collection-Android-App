package com.jamie.android.a4ypdatacollection;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Created by Jamie Brynes on 11/22/2016.
 */
public class BluetoothModule {

    private boolean mConnected;
    private boolean mConnecting;
    private boolean mActive;

    private Context ctx;
    private BluetoothModuleCallback callback;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattCharacteristic mCharacteristic;

    private static final String SERIVCE_UUID = "00002220-0000-1000-8000-00805f9b34fb";
    private static final String RECEIVE_UUID = "00002221-0000-1000-8000-00805f9b34fb";
    private static final String FILE_NAME = "stepcounter.csv";
    private static final String TAG = "BluetoothModule";
    private static final String DEVICE_NAME = "RFduino";

    private Handler mScanHandler;
    private Handler mReadHandler;
    private Runnable mReadState;
    private final int SCAN_PERIOD = 40000; //40 seconds

    private OutputStreamWriter mWriter;
    private FileOutputStream mFos;

    public interface BluetoothModuleCallback {
        void onDisconnect();
        void onUpdate(int leftState, int rightState);
    }

    public BluetoothModule(Context context, BluetoothModuleCallback callback) {

        ctx = context;
        this.callback = callback;
        mScanHandler = new Handler();
        mReadHandler = new Handler();

        // Set up bluetooth stuff.
        mConnected = false;
        mConnecting = false;
        mActive = false;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        setUpRead();

    }

    public void connect() {
        if (!mConnected){
            // Attempt to connect.
            mConnecting = true;
            mBluetoothAdapter.startLeScan(handleScan);

            mScanHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!mConnected){
                        Log.d(TAG, "Scanning timeout, stopping it.");
                        mBluetoothAdapter.stopLeScan(handleScan);
                        mConnected = false;
                        mConnecting = false;
                    }
                }
            }, SCAN_PERIOD);
        } else {
            // Disconnect
            Log.d(TAG, "Attempting disconnect");
            callback.onDisconnect();
            mConnected = false;
            if (mBluetoothGatt != null) {
                mBluetoothGatt.disconnect();
            }
        }
    }

    public void start(){
        //Set up IO
        Log.d(TAG, "Starting Bluetooth Collection.");
        try {
            File file = new File(ctx.getExternalFilesDir(null) + "/" + FILE_NAME);
            mFos = new FileOutputStream(file);
            mWriter = new OutputStreamWriter(mFos);
        } catch(IOException e) {
            Log.e(TAG, "Unable to start file output");
            e.printStackTrace();
        }
        setUpRead();

    }

    public void stop(){
        mActive = false;
    }

    private BluetoothAdapter.LeScanCallback handleScan = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int i, byte[] bytes) {
            if (device.getName() == null) {
                return;
            }
            Log.d(TAG, "Found BTLE device: " + device.getName());
            if (device.getName().equalsIgnoreCase(DEVICE_NAME)) {
                mBluetoothAdapter.stopLeScan(handleScan);

                Log.d(TAG, "Trying to connect to RFduino");
                mBluetoothGatt = device.connectGatt(ctx, true, new BluetoothGattCallback() {
                    @Override
                    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                        super.onConnectionStateChange(gatt, status, newState);
                        if (newState == BluetoothProfile.STATE_CONNECTED){
                            Log.d(TAG, "Connected to RFduino, attempting to start service discovery");
                            mBluetoothGatt.discoverServices();
                        } else if (newState == BluetoothProfile.STATE_DISCONNECTED){
                            Log.d(TAG, "Disconnected from RFduino");
                            mConnected = false;
                            callback.onDisconnect();
                        }
                    }

                    @Override
                    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                        super.onServicesDiscovered(gatt, status);

                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            BluetoothGattService serv = mBluetoothGatt.getService(UUID.fromString(SERIVCE_UUID));
                            mCharacteristic = serv.getCharacteristic(UUID.fromString(RECEIVE_UUID));
                            mConnected = true;
                            mConnecting = false;
                        }
                    }

                    @Override
                    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                        long ts = SystemClock.elapsedRealtimeNanos();
                        super.onCharacteristicRead(gatt, characteristic, status);

                        byte[] value = characteristic.getValue();
                        value = reverseArray(value);
                        ByteBuffer wrapped = ByteBuffer.wrap(value);
                        int val = wrapped.getInt();
                        writeState(val, ts);
                    }
                });
            }
        }
    };

    private void setUpRead() {


        mReadState = new Runnable() {
            @Override
            public void run() {
                if (mConnected && mActive){
                    Log.d(TAG, "Starting read lel");
                    mReadHandler.postDelayed(this, 10);
                    if(mBluetoothGatt != null && mCharacteristic != null) {
                        mBluetoothGatt.readCharacteristic(mCharacteristic);
                    }
                }
            }
        };

        mReadHandler.post(mReadState);

    }

    private byte[] reverseArray(byte[] array) {
        byte[] out = new byte[array.length];

        for (int i = array.length - 1; i > -1; i--) {
            out[array.length - 1 -i] = array[i];
        }

        return out;
    }

    private void writeState(int value, long ts) {

        int rightState;
        int leftState;

        //Decode state

        switch(value) {
            case 0:
                rightState = 0;
                leftState = 0;
                break;
            case 1:
                rightState = 1;
                leftState = 0;
                break;
            case 2:
                rightState = 0;
                leftState = 1;
                break;
            case 3:
                rightState = 1;
                leftState = 1;
                break;
            default:
                rightState = 0;
                leftState = 0;
        }



        //Write to file.
        if (mActive) {
            String line = Long.toString(ts) + "," + Integer.toString(rightState) + "," + Integer.toString(leftState) + "\n";
            try {
                mWriter.append(line);
                mWriter.flush();
            } catch (IOException e) {
                Log.e(TAG, "Error writing file.");
                e.printStackTrace();
            }
        }

        callback.onUpdate(leftState, rightState);
    }


    //Getters and setters
    public boolean getConnected(){
        return mConnected;
    }

    public boolean getConnecting() {
        return mConnecting;
    }

    public boolean getActive() {
        return mActive;
    }

    public void setmActive(boolean active) {
        mActive = active;
    }

}
