package com.humaxdigital.rtplib;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class MainActivity extends AppCompatActivity implements RTPService.Callbacks{

    public static final String CLUSTER_REQUEST_RTP_STREAM = "com.humaxdigital.settings.CLUSTER_REQUEST_RTP_STREAM";
    public static final int CLUSTER_RTP_STREAM_STOP = 0;
    public static final int CLUSTER_RTP_STREAM_START = 1;
    public static final int CLUSTER_RTP_STREAM_INIT = CLUSTER_RTP_STREAM_STOP;
    Socket mSocket;
    byte[] data = new byte[13000];
    int count = 0;
    protected LocalSocket mReceiver, mSender = null;
    private LocalServerSocket mLss = null;
    private int mSocketId;
    protected AbstractPacketizer mPacketizer = null;
    private int mTTL = 64;

    InetAddress mDestination;

    int mRtpPort = 5004;
    int mRtcpPort = 5005;
    String IPAddress = "";
    Thread socketThread = null;
     int stopSocket = 1;

    RTPService rtpService;
    Intent serviceIntent;
    EditText edtIP;
    boolean mStreaming = false;
    RetrieveFeedTask mRetrieveFeedTask = new RetrieveFeedTask();
    public static final int DEFAULT_BUFFER_SIZE = 181920;

    // Used to load the 'native-lib' library on application startup.
//    static {
//        System.loadLibrary("native-lib");
//    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Example of a call to a native method
        ToggleButton btnStart = findViewById(R.id.sample_text);
        btnStart.setText("Start");

//        edtIP = findViewById(R.id.edt_IP);
//        serviceIntent = new Intent(MainActivity.this, RTPService.class);
//
//        try {
//            IPAddress = edtIP.getText().toString();
//            Log.d("Franky", "host = " + IPAddress);
//            mDestination = InetAddress.getByName(IPAddress);
//        } catch (UnknownHostException e) {
//            e.printStackTrace();
//        }
//
//        serviceIntent.putExtra("IPADDRESS", IPAddress);
//        startService(serviceIntent); //Starting the service
//        bindService(serviceIntent, mConnection,            Context.BIND_AUTO_CREATE); //Binding to the service!

        //mRetrieveFeedTask.execute();

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (btnStart.isChecked()) {
                    stopSocket = 1;
                    btnStart.setText("Stop");
                    Settings.Global.putInt(getContentResolver(),"com.humaxdigital.settings.CLUSTER_REQUEST_RTP_STREAM", 1);

                    //IPAddress = edtIP.getText().toString();
                    //rtpService.startCounter(IPAddress);

                    //mRetrieveFeedTask.execute();

                    Log.d("Franky", "1");
                } else {
                    btnStart.setText("Start");
//                    stop();
                    Settings.Global.putInt(getContentResolver(), "com.humaxdigital.settings.CLUSTER_REQUEST_RTP_STREAM", 0);
                    //mRetrieveFeedTask.cancel(true);
                    //rtpService.stopCounter();
                }

            }
        });
        finish();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
//        unbindService(mConnection);
//        stopService(serviceIntent);
    }


    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've binded to LocalService, cast the IBinder and get LocalService instance
            RTPService.LocalBinder binder = (RTPService.LocalBinder) service;
            rtpService = binder.getServiceInstance(); //Get instance of your service!
            rtpService.registerClient(MainActivity.this); //Activity register in the service as client for callabcks!
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
        }
    };

    class RetrieveFeedTask extends AsyncTask<String, Void, Void> {

        protected Void doInBackground(String... urls) {
            javaSocket();
            return null;
        }

    }

    private static void copyInputStreamToFile(InputStream inputStream, File file)
            throws IOException {

        // append = false
        try (FileOutputStream outputStream = new FileOutputStream(file, false)) {
            int read;
            byte[] bytes = new byte[DEFAULT_BUFFER_SIZE];
            while ((read = inputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, read);
            }
        }

    }

    private void javaSocket() {
        try {
            mSocket = new Socket("localhost", 5151);

//            try (InputStream inputStream = mSocket.getInputStream()) {
//
//                File file = new File("RTP_Java.mp4");
//
//                copyInputStreamToFile(inputStream, file);
//
//            }
            
            byte[] pps = new byte[6];
                pps[  0 ]  = 104;
                pps[  1 ]  = -50;
                pps[  2 ]  = 1;
                pps[  3 ]  = -88;
                pps[  4 ]  = 53;
                pps[  5 ]  = -56;

            byte[] sps = new byte[14];
                sps[  0 ]  = 103; sps[  1 ]  = 66; sps[  2 ]  = -64;
                sps[  3 ]  = 41;  sps[  4 ]  = -115; sps[  5 ]  = 104;
                sps[  6 ]  = 5; sps[  7 ]  = 0;  sps[  8 ]  = 91;
                sps[  9 ]  = -96; sps[  10 ]  = 30; sps[  11 ]  = 17;
                sps[  12 ]  = 8; sps[  13 ]  = -44;

            try {
                IPAddress = edtIP.getText().toString();
                Log.d("Franky", "host = " + IPAddress);
                mDestination = InetAddress.getByName(IPAddress);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }

            //Toast.makeText(getApplicationContext(), "IP = " + mDestination, Toast.LENGTH_LONG).show();
            mPacketizer = new H264Packetizer();
            ((H264Packetizer)mPacketizer).setStreamParameters(pps, sps);
            mPacketizer.setTimeToLive(mTTL);
            ((H264Packetizer)mPacketizer).setInputStream( mSocket.getInputStream());
            mPacketizer.setDestination(mDestination, mRtpPort, mRtcpPort);
            mPacketizer.start();
            mStreaming = true;

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stop() {

        if (mStreaming) {
            try {
                //closeSockets();
                mPacketizer.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
            mStreaming = false;
        }
    }

    @Override
    public void updateClient(long data) {

    }
}