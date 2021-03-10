package com.humaxdigital.rtplib.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.humaxdigital.rtplib.RTPService;

public class OnBootReceiver extends BroadcastReceiver {
    private String TAG = "OnBootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
//        Toast.makeText(context,"OnBootReceiver", Toast.LENGTH_LONG).show();
        Log.d(TAG, "OnBootReceiver");
        Intent serviceIntent = new Intent(context, RTPService.class);
        String IPAddress = "192.168.40.2";
        Log.d(TAG, "host = " + IPAddress);
        serviceIntent.putExtra("IPADDRESS", IPAddress);
        context.startForegroundService(serviceIntent);

    }
}
