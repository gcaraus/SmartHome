package com.example.plural.smarthome;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class CaveAlarmService extends Service {
    public CaveAlarmService() {
    }

    Context context;
    SharedPreferences sp;
    BluetoothAdapter mBluetoothAdapter;
    BluetoothDevice arduinoDevice;
    BluetoothSocket mSocket=null;
    BluetoothSocket tmp=null;
    OutputStream mOutputStream;
    InputStream mInputStream;
    boolean deviceFound;
    boolean mConnected=false;
    Thread workerThread;
    final Handler handler = new Handler();


    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        //throw new UnsupportedOperationException("Not yet implemented");
        return null;
    }

    @Override
    public void onDestroy() {
        Log.v(getClass().getSimpleName(), "Service done");
        try {
            closeBT();

        }
        catch (IOException e) { }
    }

    @Override
    public void onCreate() {
        // Start up the thread running the service.  Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.  We also make it
        // background priority so CPU-intensive work will not disrupt our UI.

    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.v(getClass().getSimpleName(), "Service starting");
        context=this;
        sp= PreferenceManager.getDefaultSharedPreferences(this);
        if(sp.getBoolean("bluetooth_switch", false) )
        {
            if(sp.getBoolean("alarm_service_setup",true))
            {
                mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                if (mBluetoothAdapter == null) {
                    // Device does not support Bluetooth
                    // End of story
                }
                else
                {
                    if (!mBluetoothAdapter.isEnabled()) {

                    }
                    else
                    {
                        beginBTBroadcast();
                    }
                }
            }
        } else {

            Log.d(getClass().getSimpleName(), "Bluetooth is disabled in settings!");


        }
        stopSelf();
        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    private final Runnable checkBTConnection = new Runnable(){
        public void run(){
            if(!mConnected) {
                Log.d(getClass().getSimpleName(), "Bluetooth could not connect in 5 seconds!");
                stopSelf();
            }

        }
    };
    void beginBTBroadcast()
    {
        handler.postDelayed(checkBTConnection, 10000);
        workerThread = new Thread(new Runnable()
        {
            public void run() {

                Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
                // If there are paired devices
                if (pairedDevices.size() > 0) {
                    // Loop through paired devices

                    for (BluetoothDevice device : pairedDevices) {
                        // Add the name and address to an array adapter to show in a ListView
                        Log.v("Devices info", device.getName() + "\n" + device.getAddress());
                        String controllerName = sp.getString("controller_name", "");
                        if (device.getName().substring(0, controllerName.length()).equals(controllerName)) {
                            arduinoDevice = device;
                            deviceFound = true;
                        }

                    }

                }

                if (arduinoDevice != null) {
                    try {
                        mBluetoothAdapter.cancelDiscovery();
                        // MY_UUID is the app's UUID string, also used by the server code
                        UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
                        tmp = arduinoDevice.createRfcommSocketToServiceRecord(MY_UUID);
                        mSocket = tmp;
                        mSocket.connect();
                        mOutputStream = mSocket.getOutputStream();
                        mInputStream = mSocket.getInputStream();
                        Log.v("Connection state", "Connected");
                        mConnected = true;
                        sendData();
                    } catch (IOException e) {
                    }
                } else {
                }
            }
            });

            workerThread.start();
    }
    void sendData() throws IOException
    {
        if(mOutputStream!=null) {
            String msg = "left=" + 0;
            msg += "\n";
            mOutputStream.write(msg.getBytes());
            msg = "right=" + 0;
            msg += "\n";
            mOutputStream.write(msg.getBytes());
            Log.v("Data sent", "true");
        }
    }
    void closeBT() throws IOException {
        try {

            if (mOutputStream != null && mInputStream != null && mSocket != null) {
                mOutputStream.close();
                mInputStream.close();
                mSocket.close();
            }
        } catch (IOException e) {
        }
    }
}
