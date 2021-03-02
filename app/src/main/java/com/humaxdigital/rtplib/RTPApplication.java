package com.humaxdigital.rtplib;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.widget.EditText;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class RTPApplication extends Application {

    Socket mSocket;
    private LocalServerSocket mLss = null;
    protected AbstractPacketizer mPacketizer = null;
    private int mTTL = 64;

    InetAddress mDestination;

    int mRtpPort = 5004;
    int mRtcpPort = 5005;
    String IPAddress = "";

    Intent serviceIntent;
    EditText edtIP;
    boolean mStreaming = false;
    public static final int DEFAULT_BUFFER_SIZE = 181920;
    String TAG = "RTPApplication";

    static {
        System.loadLibrary("native-lib");
    }


    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG, "RTPApplication started");
        serviceIntent = new Intent(getApplicationContext(), RTPService.class);
        IPAddress = "192.168.100.206";
        Log.d(TAG, "host = " + IPAddress);
        try {
            mDestination = InetAddress.getByName(IPAddress);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        serviceIntent.putExtra("IPADDRESS", IPAddress);
        startForegroundService(serviceIntent);

    }

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

            byte[] pps = new byte[6];
            pps[0] = 104;
            pps[1] = -50;
            pps[2] = 1;
            pps[3] = -88;
            pps[4] = 53;
            pps[5] = -56;

            byte[] sps = new byte[14];
            sps[0] = 103;
            sps[1] = 66;
            sps[2] = -64;
            sps[3] = 41;
            sps[4] = -115;
            sps[5] = 104;
            sps[6] = 5;
            sps[7] = 0;
            sps[8] = 91;
            sps[9] = -96;
            sps[10] = 30;
            sps[11] = 17;
            sps[12] = 8;
            sps[13] = -44;

            try {
                Log.d(TAG, "host = " + IPAddress);
                mDestination = InetAddress.getByName(IPAddress);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }

            //Toast.makeText(getApplicationContext(), "IP = " + mDestination, Toast.LENGTH_LONG).show();
            mPacketizer = new H264Packetizer();
            ((H264Packetizer) mPacketizer).setStreamParameters(pps, sps);
            mPacketizer.setTimeToLive(mTTL);
            ((H264Packetizer) mPacketizer).setInputStream(mSocket.getInputStream());
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


}
