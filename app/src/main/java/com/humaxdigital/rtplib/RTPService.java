package com.humaxdigital.rtplib;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.car.settings.CarSettings;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
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


    public static final String CLUSTER_REQUEST_CHANGE_IP_ADDRESS = "com.humaxdigital.settings.CLUSTER_REQUEST_CHANGE_IP_ADDRESS";


    NotificationManager notificationManager;
    NotificationCompat.Builder mBuilder;
    private String channelId = "rtpDisplayStreamChannel";
    Callbacks activity;
    private long startTime = 0;
    private long millis = 0;
    String IPAddress = "";
    private final IBinder mBinder = new LocalBinder();
    RetrieveFeedTask mRetrieveFeedTask = new RetrieveFeedTask();
    int stopSocket = 1;
    String TAG = "RTPService";
    private Thread thread;
    private boolean isFirstTimesStream = true;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(getApplicationContext(),"RTPService", Toast.LENGTH_LONG).show();
        isFirstTimesStream= true;
        IPAddress = intent.getStringExtra("IPADDRESS");
        Log.d(TAG, "RTPService onStartCommand IPAddress= " + IPAddress);
        //Do what you need in onStartCommand when service has been started
        registerStartStopStreamListener();
        registerChangeIPListener();
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, channelId, NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }
        keepAliveTrick();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    //returns the instance of the service
    public class LocalBinder extends Binder {
        public RTPService getServiceInstance() {
            return RTPService.this;
        }
    }

    //Here Activity register to the service as Callbacks client
    public void registerClient(Activity activity) {
        this.activity = (Callbacks) activity;
    }

    public void startCounter(String ip) {
        IPAddress = ip;
        Toast.makeText(getApplicationContext(), "Start RTP Socket Service with Host IP = " + IPAddress, Toast.LENGTH_LONG).show();
        //mRetrieveFeedTask.execute();
        stopSocket = 1;
        if (thread != null) {
            thread.interrupt();
        }
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                start(IPAddress);
            }
        });
        thread.start();
    }


    public void stopCounter() {
        stopSocket = 0;
        stopRunning();
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
        stopCounter();
        stopSending();
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
    public interface Callbacks {
        public void updateClient(long data);
    }


    private void registerStartStopStreamListener() {
        Log.d(TAG, "registerStartStopStreamListener");
        getApplicationContext().getContentResolver().registerContentObserver(Settings.Global.getUriFor(CarSettings.Global.CLUSTER_REQUEST_RTP_STREAM),
                false,
                mStartStopStreamServiceObserver);
    }

    private final ContentObserver mStartStopStreamServiceObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
        @Override
        public void onChange(boolean selfChange) {
            int status = Settings.Global.getInt(getApplicationContext().getContentResolver(), CarSettings.Global.CLUSTER_REQUEST_RTP_STREAM, CarSettings.Global.CLUSTER_RTP_STREAM_STOP);
            // status is 0 : stop stream
            // status is 1 : start stream
            Log.d(TAG, "status stream= " + status);
            if (status == CarSettings.Global.CLUSTER_RTP_STREAM_STOP) {
                Log.d(TAG, "stop stream");
                stopSending();
            } else if (status == CarSettings.Global.CLUSTER_RTP_STREAM_START) {
                Log.d(TAG, "start stream isFirstTimesStream= "+isFirstTimesStream);
                if (isFirstTimesStream) {
                    isFirstTimesStream = false;
                    startCounter(IPAddress);
                } else {
                    startSending();
                }

            }

        }
    };

    private void registerChangeIPListener() {
        Log.d(TAG, "registerChangeIPListener");
        getApplicationContext().getContentResolver().registerContentObserver(Settings.Global.getUriFor(CLUSTER_REQUEST_CHANGE_IP_ADDRESS),
                false,
                mChangeIPObserver);
    }

    private final ContentObserver mChangeIPObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
        @Override
        public void onChange(boolean selfChange) {
            IPAddress = Settings.Global.getString(getApplicationContext().getContentResolver(), CLUSTER_REQUEST_CHANGE_IP_ADDRESS);
            Log.d(TAG, "mChangeIPObserver IPAddress= " + IPAddress);
            startCounter(IPAddress);

        }
    };

    private void keepAliveTrick() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            Notification notification = new NotificationCompat.Builder(this, channelId)
                    .setOngoing(true)
                    .setContentTitle("")
                    .setContentText("").build();
            startForeground(1, notification);
        } else {
            startForeground(1, new Notification());
        }
    }

}
