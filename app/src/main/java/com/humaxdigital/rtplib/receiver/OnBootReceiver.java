package com.humaxdigital.rtplib.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.humaxdigital.rtplib.RTPService;

public class OnBootReceiver extends BroadcastReceiver {
    private String TAG = "OnBootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "OnBootReceiver");
        Intent serviceIntent = new Intent(context, RTPService.class);
        String IPAddress = "192.168.98.118";
        Log.d(TAG, "host = " + IPAddress);
        serviceIntent.putExtra("IPADDRESS", IPAddress);

    }
}
