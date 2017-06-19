package com.example.plural.smarthome;

import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Andrii on 23-Mar-16.
 */

//Class used to get UTC time from alarm format ddd hh-mm
public class AlarmTimeTool {

    public static long getNextAlarm(String alarm, String[] weekdays, String[] amPm) {
        int alarmHours = -1, alarmMinutes = -1;
        int alarmWeekday = -1; // Calendar.SUNDAY = 1; Calendar.SATURDAY = 7;
        int alarmAmPm = -1; // Calendar.AM = 0; Calendar.PM = 1;

        for (String piece: alarm.split("\\s")) {
            if (alarmWeekday == -1) {
                alarmWeekday = getWeekday(piece, weekdays);
                if (alarmWeekday != -1) {
                    continue;
                }
            }
            if (alarmHours == -1) {
                int[] hoursMinutes = getTime(piece);
                alarmHours = hoursMinutes[0];
                alarmMinutes = hoursMinutes[1];
                if (alarmHours != -1) {
                    continue;
                }
            }
            if (alarmAmPm == -1) {
                alarmAmPm = getAmPm(piece, amPm);
            }
        }

        if (alarmWeekday == -1 || alarmHours == -1) {
            throw new RuntimeException("could not fetch alarm week or hour");
        }

        Calendar now = Calendar.getInstance();
        Calendar nextAlarm = Calendar.getInstance();
        nextAlarm.set(Calendar.DAY_OF_WEEK, alarmWeekday);
        nextAlarm.set(Calendar.MINUTE, alarmMinutes);
        nextAlarm.set(Calendar.SECOND, 0);
        nextAlarm.set(Calendar.MILLISECOND, 0);
        if (alarmAmPm == -1) {
            nextAlarm.set(Calendar.HOUR_OF_DAY, alarmHours);
        } else {
            nextAlarm.set(Calendar.AM_PM, alarmAmPm);
            nextAlarm.set(Calendar.HOUR, alarmHours % 12);
        }
        if (nextAlarm.before(now)) {
            nextAlarm.add(Calendar.DAY_OF_MONTH, 7);
        }

        return nextAlarm.getTimeInMillis();
    }

    private static int[] getTime(String piece) {
        int hours = -1;
        int minutes = -1;

        // Android only (\d has different meanings):
        Pattern p = Pattern.compile("(\\d{1,2})\\D(\\d{2})");

        Matcher m = p.matcher(piece);
        if (m.find()) {
            hours = Integer.parseInt(m.group(1));
            minutes = Integer.parseInt(m.group(2));
        }
        int[] hoursMinutes = {hours, minutes};
        return hoursMinutes;
    }

    private static int getWeekday(String piece, String[] weekdays) {
        for (int i = 1; i < weekdays.length; i++) {
            if (piece.contains(weekdays[i])) {
                return i;
            }
        }
        return -1;
    }

    private static int getAmPm(String piece, String[] amPm) {
        for (int i = 0; i < amPm.length; i++) {
            if (piece.contains(amPm[i])) {
                return i;
            }
        }
        return -1;
    }
}
