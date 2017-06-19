package com.example.plural.smarthome;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by Andrii on 22-Mar-16.
 */
public class CaveAlarmBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent startServiceIntent = new Intent(context, CaveAlarmService.class);
        context.startService(startServiceIntent);
    }
}
