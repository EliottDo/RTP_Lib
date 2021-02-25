package com.humaxdigital.rtplib;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;


public class RTPService extends Service {

    public static final String CLUSTER_REQUEST_RTP_STREAM = "com.humaxdigital.settings.CLUSTER_REQUEST_RTP_STREAM";
    public static final int CLUSTER_RTP_STREAM_STOP = 0;
    public static final int CLUSTER_RTP_STREAM_START = 1;
    public static final int CLUSTER_RTP_STREAM_INIT = CLUSTER_RTP_STREAM_STOP;


    NotificationManager notificationManager;
    NotificationCompat.Builder mBuilder;
    Callbacks activity;
    private long startTime = 0;
    private long millis = 0;
    String IPAddress = "";
    private final IBinder mBinder = new LocalBinder();
    RetrieveFeedTask mRetrieveFeedTask = new RetrieveFeedTask();
    int stopSocket = 1;
    String TAG = "RTPService";
    private Thread thread;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {


        IPAddress = intent.getStringExtra("IPADDRESS");
        Log.d(TAG,"RTPService onStartCommand IPAddress= " +IPAddress);
        //Do what you need in onStartCommand when service has been started
        registerStartStopStreamListener();
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
    //returns the instance of the service
    public class LocalBinder extends Binder {
        public RTPService getServiceInstance(){
            return RTPService.this;
        }
    }

    //Here Activity register to the service as Callbacks client
    public void registerClient(Activity activity){
        this.activity = (Callbacks)activity;
    }

    public void startCounter(String ip){
        IPAddress = ip;
        Toast.makeText(getApplicationContext(), "Start RTP Socket Service with Host IP = " + IPAddress, Toast.LENGTH_LONG).show();
        //mRetrieveFeedTask.execute();
        stopSocket = 1;
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                start(IPAddress);
            }
        });
        thread.start();
    }

    public void startSend() {
        startSending();
    }

    public void stopCounter(){
        stopSocket = 0;
        stopSending();
        if (thread != null) {
            thread.interrupt();
        }
        //stopSocket();
        //mRetrieveFeedTask.cancel(true);
        //Toast.makeText(getApplicationContext(), "Stop RTP Socket Service", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopRunning();
    }

    class RetrieveFeedTask extends AsyncTask<String, Void, Void> {

        protected Void doInBackground(String... urls) {
            return null;
        }

    }

    public native String start(String ip);
    public native String stopRunning();
    public native String stopSending();
    public native String startSending();

    //callbacks interface for communication with service clients!
    public interface Callbacks{
        public void updateClient(long data);
    }


    private void startStream() {
        startCounter(IPAddress);
    }

    private void stopStream() {
        stopCounter();
    }


    private void registerStartStopStreamListener() {
        getApplicationContext().getContentResolver().registerContentObserver(Settings.Global.getUriFor(CLUSTER_REQUEST_RTP_STREAM),
                false,
                mStartStopStreamServiceObserver);
    }

    private final ContentObserver mStartStopStreamServiceObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
        @Override
        public void onChange(boolean selfChange) {
            int status = Settings.Global.getInt(getApplicationContext().getContentResolver(), CLUSTER_REQUEST_RTP_STREAM, CLUSTER_RTP_STREAM_STOP);
            // status is 0 : stop stream
            // status is 1 : start stream
            Log.d(TAG, "status stream= " + status);
            if (status == CLUSTER_RTP_STREAM_STOP) {
                Log.d(TAG, "stop stream");
                stopStream();
            } else if (status == CLUSTER_RTP_STREAM_START) {
                Log.d(TAG, "start stream");
                startStream();
            }

        }
    };
}
