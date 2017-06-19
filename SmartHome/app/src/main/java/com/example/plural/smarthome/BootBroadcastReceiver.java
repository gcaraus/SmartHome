package com.example.plural.smarthome;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by Andrii on 22-Mar-16.
 */
public class BootBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        //Receiver for CaveAlarmRescheduleService
        Intent startServiceIntent = new Intent(context, CaveAlarmRescheduleService.class);
        context.startService(startServiceIntent);
    }
}
