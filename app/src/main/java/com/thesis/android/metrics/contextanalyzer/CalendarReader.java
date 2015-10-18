package com.thesis.android.metrics.contextanalyzer;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.SystemClock;

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
    public static List<String> startDates = new ArrayList<>();
    public static List<String> endDates = new ArrayList<>();
    public static List<String> descriptions = new ArrayList<>();

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
        startDates.clear();
        endDates.clear();
        descriptions.clear();

        Date currentDate = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat(
                "dd/MM/yyyy hh:mm:ss a");

        Date tomorrow = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(tomorrow);
        calendar.add(Calendar.DATE, 1);
        tomorrow = calendar.getTime();
        String tomorrowDate = formatter.format(tomorrow);

        for (int i = 0; i < calendarNames.length; i++) {

            String date = formatter.format(currentDate);
            String eventDate = getDate(Long.parseLong(cursor.getString(3)));

            if(eventDate.compareTo(date) >= 0 && eventDate.compareTo(tomorrowDate) <= 0)
                events.add(cursor.getString(1));
            cursor.moveToNext();
        }

        return events;
    }

    public static String getDate(long milliSeconds) {
        SimpleDateFormat formatter = new SimpleDateFormat(
                "dd/MM/yyyy hh:mm:ss a");
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(milliSeconds);
        return formatter.format(calendar.getTime());
    }
}
