package com.example.plural.smarthome;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;
import java.util.Set;
import java.util.UUID;


public class mainActivity extends AppCompatActivity implements SensorEventListener {

    //Initializing variables for activity
    int maxTempValue=40;
    Button animateButton;
    float degrees;
    ProgressCircleCustom progressCircle;
    final int REQUEST_ENABLE_BT=1;
    TextView textName;
    View barsLayout;
    ImageView logo;
    ImageView loader;
    ImageView setLoader;
    Button buttonSet;
    final Handler handler = new Handler();
    Thread workerThread;
    BluetoothAdapter mBluetoothAdapter;
    BluetoothDevice arduinoDevice;
    BluetoothSocket mSocket=null;
    BluetoothSocket tmp=null;
    OutputStream mOutputStream;
    InputStream mInputStream;
    boolean mConnected=false;
    volatile boolean stopWorker;
    byte[] readBuffer;
    int readBufferPosition;
    boolean stopSpinning=false;
    boolean stopLoading=false;
    VerticalSeekBar_Reverse scrubberLeft;
    VerticalSeekBar_Reverse scrubberRight;
    int[] scrubbersProgress=new int[2];
    boolean deviceFound=false;
    MediaPlayer mp;
    private SensorManager mSensorManager;
    private Sensor mSensor;
    float upSensorValue =-8.0f;
    float downSensorValue =7.0f;
    boolean sensorResponsive=true;
    SharedPreferences sp;
    boolean motionSensor;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ui);

        //Assigning variables and setting up the view
        final Context context=this;
        sp=PreferenceManager.getDefaultSharedPreferences(this);

        scrubbersProgress[0]=50;
        scrubbersProgress[1]=50;

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);



        logo=(ImageView)findViewById(R.id.logoView);
        loader=(ImageView)findViewById(R.id.loaderView);
        setLoader=(ImageView)findViewById(R.id.setLoader);
        animateButton =(Button)findViewById(R.id.animateButton);
        scrubberLeft =(VerticalSeekBar_Reverse)findViewById(R.id.seekBarLeft);
        scrubberRight =(VerticalSeekBar_Reverse)findViewById(R.id.seekBarRight);
        buttonSet =(Button)findViewById(R.id.buttonSet);
        progressCircle = (ProgressCircleCustom) findViewById(R.id.progress_circle);
        textName=(TextView)findViewById(R.id.nameView);
        barsLayout=findViewById(R.id.layoutGrid);
        mp = MediaPlayer.create(this, R.raw.connected);
        mp.setVolume(1f, 1f);


        setLoader.setVisibility(View.GONE);
        buttonSet.setVisibility(View.GONE);
        barsLayout.setVisibility(View.GONE);
        progressCircle.setVisibility(View.GONE);
        animateButton.setVisibility(View.GONE);
        textName.setVisibility(View.GONE);

        //If alarm service is on in settings schedule an alarm
        //with rescheduling service
        //It runs in separate process, so it will not block the ui
        if(sp.getBoolean("alarm_service_setup",true))
        {
            Intent intent = new Intent(context, CaveAlarmRescheduleService.class);
            startService(intent);
        }



        //Setting listeners through method in program
        setButton(buttonSet);
        setScrubber(scrubberLeft);
        setScrubber(scrubberRight);


        //start the logo spinning, indicating that loading is in process
        handler.post(spinMainLogo);



        //Check bluetooth program settings and start bluetooth connection with Arduino
        if(sp.getBoolean("bluetooth_switch", false))
        {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            handler.postDelayed(checkBT, 500);
        }
        else
        {
            //cancel loading and inform user
            stopSpinning=true;
            CharSequence text = "Bluetooth is not connected!";
            int duration = Toast.LENGTH_LONG;

            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
        }




        //Setting listeners for views
        progressCircle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, DatabaseActivity.class);
                startActivity(intent);

            }
        });
        progressCircle.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Intent intent = new Intent(context, SettingsActivity.class);
                startActivity(intent);
                return true;
            }
        });

        logo.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Intent intent = new Intent(context, SettingsActivity.class);
                startActivity(intent);
                return true;
            }
        });

    }

    //System methods called by activity

    //Register listeners when program starts
    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }
    //Unregister listeners when program pauses
    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }


    //Close connection if activity is destroyed

    @Override
    protected void onDestroy() {
        try {
            closeBT();

        }
        catch (IOException e) { }

        super.onDestroy();
    }


    //Runnables

    private final Runnable checkBT = new Runnable(){
        public void run(){
            if (mBluetoothAdapter == null) {
                // Device does not support Bluetooth
                // End of story
            }
            else
            {
                if (!mBluetoothAdapter.isEnabled()) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT); //Result is passed to onActivity result
                }
                else
                {
                    beginListenForData();
                }
            }

        }
    };

    //spinMainLogo - method that is called recursively to animate the first view loader
    //Stopped when stopSpinning was set to true
    private final Runnable spinMainLogo = new Runnable(){
        public void run(){
            if(!stopSpinning)
            {
                loader.animate().rotationBy(90)
                        .setDuration(2800)
                        .setListener(null);
                handler.postDelayed(this, 2800);
            }
            else
            {
                crossFade();
            }
        }
    };

    //Recursive runnable to upload the data to the database
    private final Runnable sendToDatabase = new Runnable(){
        public void run(){
            int syncFrequency;
            new AsyncSendDatabase().execute(String.valueOf(degrees));
            syncFrequency=Integer.parseInt(sp.getString("sync_frequency","300000"));
            handler.postDelayed(this, syncFrequency);
        }
    };

    //loaderLoading - method that is called recursively to animate the loading when the button is pressed
    //Stopped when stopLoading was set to true
    private final Runnable loaderLoading = new Runnable(){
        public void run(){
            if(!stopLoading)
            {
                setLoader.animate().rotationBy(180)
                        .setDuration(1400)
                        .setListener(null);
                handler.postDelayed(this, 1400);
            }
            else
            {
                loadingFinished();

                stopLoading=false;
            }

        }
    };


    //Sensors
    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
    }

    @Override
    public final void onSensorChanged(SensorEvent event) {
        // The linear acceleration sensor returns 3 values corresponding to xyz.

        float linearY = event.values[2];
        motionSensor=sp.getBoolean("motion_switch",true);

        //Checking if the value is in range
        //Animating scrubbers to go up or down
        //Performing click on the set button to send the message to arduino
        if(linearY> downSensorValue && sensorResponsive && motionSensor)
        {
            sensorResponsive=false;
            scrubbersProgress[0]=100;
            scrubbersProgress[1] =100;

            animateScrubber(scrubberLeft, scrubberLeft.getProgress(),scrubbersProgress[0]);
            animateScrubber(scrubberRight, scrubberRight.getProgress(), scrubbersProgress[1]);
            buttonSet.performClick();

        }
        else if(linearY< upSensorValue && sensorResponsive && motionSensor)
        {
            sensorResponsive=false;
            scrubbersProgress[0]=0;
            scrubbersProgress[1]=0;
            animateScrubber(scrubberLeft, scrubberLeft.getProgress(),scrubbersProgress[0]);
            animateScrubber(scrubberRight, scrubberRight.getProgress(),scrubbersProgress[1]);
            buttonSet.performClick();

        }


    }

    //Method to animate the scrubber called from onSensorChanged
    private void animateScrubber(final VerticalSeekBar_Reverse scrubberItem, int initialPos, int finalPos)
    {
        //Animates numbers for scrubbers and sets seekbars
        ValueAnimator scrubberAnim = ValueAnimator.ofInt(initialPos, finalPos);
        scrubberAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                scrubberItem.setProgressAndThumb((Integer) valueAnimator.getAnimatedValue());
            }
        });
        scrubberAnim.setDuration(1000);
        scrubberAnim.setInterpolator(new AccelerateDecelerateInterpolator());
        scrubberAnim.start();
    }


    //Methods

    //Method setScrubber is for assigning listeners to seekbar
    private void setScrubber(final VerticalSeekBar_Reverse scrubber)
    {
        scrubber.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                switch (seekBar.getId()) {
                    case R.id.seekBarLeft:
                        scrubbersProgress[0] = progress;

                    case R.id.seekBarRight:

                        scrubbersProgress[1] = progress;
                    default:
                        break;
                }

            }
        });
    }




    //Returned when the user chose an option in Bluetooth settings of the system
    //Can be launched only when the intent was started for result
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==REQUEST_ENABLE_BT) {
            if(resultCode==RESULT_OK){
                Log.v("onActivityResult", "BT enabled by the user");
                beginListenForData();
            }
            else if(resultCode==RESULT_CANCELED)
            {
                Log.v("onActivityResult", "BT enabling is cancelled");
            }
        }
    }



    private void loadingFinished()
    {
        buttonSet.setVisibility(View.VISIBLE);
        setLoader.animate().alpha(0f)
                .setDuration(400)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        loader.setVisibility(View.GONE);
                    }
                });

        buttonSet.animate().alpha(1f)
                .setDuration(400)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        buttonSet.setClickable(true);
                    }
                });
        sensorResponsive=true;
    }


    private void setButton(Button button)
    {
        if(button!=null){
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    switch (v.getId())
                    {
                        case R.id.buttonSet:
                            loading();
                            try{
                            sendData();}
                            catch (IOException e) { }
                            break;
                        default:
                            break;
                    }
                }
            });
            button.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {

                    return true;
                }
            });
        }
    }

    public void animate(View view) {
        float val;

        degrees = new Random().nextInt(maxTempValue);

        val=degrees/maxTempValue;
        progressCircle.setProgress(val, degrees);
        progressCircle.startAnimation();
    }

    //Loading set by pressing setButton
    private void loading()
    {
        buttonSet.setClickable(false);
        setLoader.setAlpha(0f);
        setLoader.setVisibility(View.VISIBLE);
        buttonSet.animate().alpha(0f)
                .setDuration(400)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        buttonSet.setVisibility(View.GONE);
                    }
                });
        setLoader.animate().alpha(1f)
                .setDuration(400)
                .setListener(null);
        handler.post(loaderLoading);
    }

    //Cross fade the loading view and main view
    private void crossFade() {

        //Start the beep
        mp.start();

        // Set the content view to 0% opacity but visible, so that it is visible
        // (but fully transparent) during the animation.
        barsLayout.setAlpha(0f);
        progressCircle.setAlpha(0f);
        animateButton.setAlpha(0f);
        textName.setAlpha(0f);
        buttonSet.setAlpha(0f);

        buttonSet.setVisibility(View.VISIBLE);
        barsLayout.setVisibility(View.VISIBLE);
        progressCircle.setVisibility(View.VISIBLE);
        animateButton.setVisibility(View.VISIBLE);
        textName.setVisibility(View.VISIBLE);


        // Animate the content view to 100% opacity, and clear any animation
        // listener set on the view.

        // Animate the loading view to 0% opacity. After the animation ends,
        // set its visibility to GONE as an optimization step (it won't
        // participate in layout passes, etc.)

        loader.animate().alpha(0f)
                .setDuration(400)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        loader.setVisibility(View.GONE);
                    }
                });

        buttonSet.animate()
                .alpha(1f)
                .setDuration(500)
                .setListener(null);
        barsLayout.animate()
                .alpha(1f)
                .setDuration(500)
                .setListener(null);
        animateButton.animate()
                .alpha(1f)
                .setDuration(500)
                .setListener(null);
        textName.animate()
                .alpha(1f)
                .setDuration(500)
                .setListener(null);

        progressCircle.animate()
                .alpha(1f)
                .setDuration(500)
                .setListener(null);

        logo.animate()
                .alpha(0f)
                .setDuration(500)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        logo.setVisibility(View.GONE);
                    }
                });
    }


    //Starts bluetooth connection

    void beginListenForData()
    {
        final Handler handler = new Handler();
        final byte delimiter = 10; //This is the ASCII code for a newline character

        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        //Thread for bluetooth connection
        //Will not block the ui
        workerThread = new Thread(new Runnable()
        {
            public void run()
            {
                while (!deviceFound) {
                Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
                // If there are paired devices
                if (pairedDevices.size() > 0) {
                    // Loop through paired devices

                        for (BluetoothDevice device : pairedDevices) {
                            //Compares paired devices to the device in settings
                            Log.v("Devices info", device.getName() + "\n" + device.getAddress());
                            String controllerName=sp.getString("controller_name", "");
                            if (device.getName().substring(0, controllerName.length()).equals(controllerName)) {
                                arduinoDevice = device;
                                deviceFound=true;
                            }

                        }

                    }
                }
                if(arduinoDevice!=null)
                {
                    try {
                        //Connects to device
                        mBluetoothAdapter.cancelDiscovery();
                        // MY_UUID is the app's UUID string, also used by the server code
                        UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
                        tmp = arduinoDevice.createRfcommSocketToServiceRecord(MY_UUID);
                        mSocket = tmp;
                        mSocket.connect();
                        mOutputStream = mSocket.getOutputStream();
                        mInputStream = mSocket.getInputStream();
                        Log.v("connection state", "Connected");
                        handler.postDelayed(sendToDatabase, 5000);
                        mConnected=true;
                        stopSpinning=true;
                    } catch (IOException e) { }
                }
                else{}
                while(!Thread.currentThread().isInterrupted() && !stopWorker)
                {
                    //Listens for new data

                    try
                    {
                        int bytesAvailable;

                        bytesAvailable=mInputStream!=null?mInputStream.available():0;



                        if(bytesAvailable > 0)
                        {
                            byte[] packetBytes = new byte[bytesAvailable];
                            mInputStream.read(packetBytes);
                            for(int i=0;i<bytesAvailable;i++)
                            {
                                byte b = packetBytes[i];
                                if(b == delimiter)
                                {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");

                                    readBufferPosition = 0;

                                    handler.post(new Runnable()
                                    {
                                        public void run() {
                                            int equalPos;
                                            equalPos = data.indexOf("=");

                                            if (equalPos > 1){

                                                    if(data.substring(0,equalPos).equals("temp")) {
                                                        float val;
                                                        try {
                                                            degrees = Float.parseFloat(data.substring(equalPos + 1, data.length() - 1));
                                                            //Log.v("Degrees", String.valueOf(degrees));
                                                        } catch (NumberFormatException e) {

                                                        }
                                                        if (degrees > 0 && degrees < maxTempValue) {
                                                            val = degrees / maxTempValue;
                                                            progressCircle.setProgress(val, degrees);
                                                            progressCircle.startAnimation();
                                                        }

                                                    }
                                                    else if(data.substring(0,equalPos).equals("set"))
                                                    {
                                                        stopLoading = true;
                                                    }
                                            }


                                        }
                                    });
                                }
                                else
                                {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    }
                    catch (IOException ex)
                    {
                        stopWorker = true;
                    }
                }
            }
        });
        //Start the thread
        workerThread.start();
    }

    //Sends data to bluetooth
    void sendData() throws IOException
    {
        if(mOutputStream!=null) {
            String msg = "left=" + scrubbersProgress[0];
            msg += "\n";
            mOutputStream.write(msg.getBytes());
            msg = "right=" + scrubbersProgress[1];
            msg += "\n";
            mOutputStream.write(msg.getBytes());
            Log.v("Data sent:", "true");
        }
    }

    //Closes sockets, streams and cancels listening for data
    void closeBT() throws IOException
    {
        try {
            stopWorker = true;
            if(mOutputStream!=null && mInputStream!=null && mSocket!=null) {
                mOutputStream.close();
                mInputStream.close();
                mSocket.close();
            }
        }
        catch (IOException e) { }

    }
}
