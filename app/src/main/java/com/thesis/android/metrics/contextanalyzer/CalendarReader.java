package com.thesis.android.metrics.contextanalyzer;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.SystemClock;
import android.support.annotation.NonNull;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created by Srinivas on 10/18/2015.
 */
public class CalendarReader
{
    public static List<String> events = new ArrayList<>();

    public static List<String> readCalendarEvent(Context context) {
        Cursor cursor = context.getContentResolver()
                .query(
                        Uri.parse("content://com.android.calendar/events"),
                        new String[] { "calendar_id", "title", "description",
                                "dtstart", "dtend", "eventLocation" }, null,
                        null, null);

        cursor.moveToFirst();

        // fetching calendars name
        String[] calendarNames = new String[cursor.getCount()];

        // fetching calendars id
        events.clear();

        Date yesterday = getYesterdaysDate(new Date());
        Date tomorrow = getTomorrowsDate(new Date());

        for (int i = 0; i < calendarNames.length; i++) {

            Date eventDate = getDate(Long.parseLong(cursor.getString(3)));

            if(eventDate.compareTo(yesterday) >= 0 && eventDate.compareTo(tomorrow) <= 0)
                events.add(cursor.getString(1));
            cursor.moveToNext();
        }

        return events;
    }

    @NonNull
    private static Date getTomorrowsDate(Date tomorrow) {

        Calendar calendar;
        calendar = Calendar.getInstance();
        calendar.setTime(tomorrow);
        calendar.add(Calendar.DATE, 1);
        tomorrow = calendar.getTime();
        return tomorrow;
    }

    @NonNull
    private static Date getYesterdaysDate(Date yesterday) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(yesterday);
        calendar.add(Calendar.DATE, -1);
        yesterday = calendar.getTime();
        return yesterday;
    }

    private static Date getDate(long milliSeconds) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(milliSeconds);
        return calendar.getTime();
    }
}
