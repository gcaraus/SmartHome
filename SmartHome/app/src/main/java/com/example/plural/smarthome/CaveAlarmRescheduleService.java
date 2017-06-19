package com.example.plural.smarthome;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import java.text.DateFormatSymbols;

//Service runs in separate process and called by program, when system is booted or when alarm clock is rescheduled in the system
//Usage is to schedule the alarm for CaveAlarmService.
public class CaveAlarmRescheduleService extends Service {
    public CaveAlarmRescheduleService() {
    }
    boolean stoppedService=false;
    Context context;
    String nextAlarm;
    AlarmManager.AlarmClockInfo clockInfo;
    long alarmUTC;
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        //Service is not binded
        return null;
    }

    @Override
    public void onDestroy() {
        //Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show();
        stoppedService=true;
    }

    @Override
    public void onCreate() {

    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //Toast.makeText(this, "Service starting", Toast.LENGTH_SHORT).show();
        context=this;

        //Getting the time of next alarm for different versions
        if(android.os.Build.VERSION.SDK_INT>20) {
            AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

            clockInfo = alarm.getNextAlarmClock();
            if(clockInfo!=null){
                alarmUTC =clockInfo.getTriggerTime();}
        }
        else
        {
            nextAlarm = Settings.System.getString(getContentResolver(), Settings.System.NEXT_ALARM_FORMATTED);
            if (nextAlarm != null && !nextAlarm.isEmpty()) {
                Log.d(getClass().getSimpleName(), nextAlarm);
                String[] weekdays = DateFormatSymbols.getInstance().getShortWeekdays();
                String[] amPm = DateFormatSymbols.getInstance().getAmPmStrings();
                alarmUTC = AlarmTimeTool.getNextAlarm(nextAlarm, weekdays, amPm);
//            SimpleDateFormat simpleDateFormat =
//                    new SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.US);
            }

        }
        Log.d(getClass().getSimpleName(), String.valueOf(alarmUTC));


        Intent startIntent = new Intent("AlarmAction");
        PendingIntent startPIntent = PendingIntent.getBroadcast(context, 0, startIntent, 0);
        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if(alarmUTC!=0) {
            alarm.set(AlarmManager.RTC_WAKEUP, alarmUTC, startPIntent);
        }

        //Toast.makeText(context, "Next alarm:"+String.valueOf(alarmUTC), Toast.LENGTH_SHORT).show();

        stopSelf();
        return START_STICKY;
    }
}
