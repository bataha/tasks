package com.todoroo.astrid.gcal;

import java.util.ArrayList;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.timsu.astrid.R;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.andlib.utility.Preferences;
import com.todoroo.astrid.utility.Constants;

@SuppressWarnings("nls")
public class CalendarAlarmReceiver extends BroadcastReceiver {

    public static final int REQUEST_CODE_CAL_REMINDER = 100;
    public static final String BROADCAST_CALENDAR_REMINDER = Constants.PACKAGE + ".CALENDAR_EVENT";
    public static final String TOKEN_EVENT_ID = "eventId";

    private static final String[] EVENTS_PROJECTION = {
        Calendars.EVENTS_DTSTART_COL,
        Calendars.EVENTS_NAME_COL,
    };

    private static final String[] ATTENDEES_PROJECTION = {
        Calendars.ATTENDEES_NAME_COL,
        Calendars.ATTENDEES_EMAIL_COL,
    };

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Preferences.getBoolean(R.string.p_calendar_reminders, true))
            return;
        try {
            ContentResolver cr = context.getContentResolver();
            long eventId = intent.getLongExtra(TOKEN_EVENT_ID, -1);
            if (eventId > 0) {
                Uri eventUri = Calendars.getCalendarContentUri(Calendars.CALENDAR_CONTENT_EVENTS);

                String[] eventArg = new String[] { Long.toString(eventId) };
                Cursor event = cr.query(eventUri,
                        EVENTS_PROJECTION,
                        Calendars.ID_COLUMN_NAME + " = ?",
                        eventArg,
                        null);
                try {
                    if (event.moveToFirst()) {
                        int timeIndex = event.getColumnIndexOrThrow(Calendars.EVENTS_DTSTART_COL);
                        int titleIndex = event.getColumnIndexOrThrow(Calendars.EVENTS_NAME_COL);

                        String title = event.getString(titleIndex);
                        long startTime = event.getLong(timeIndex);
                        long timeUntil = startTime - DateUtilities.now();

                        if (timeUntil > 0 && timeUntil < DateUtilities.ONE_MINUTE * 20) {
                            // Get attendees
                            Cursor attendees = cr.query(Calendars.getCalendarContentUri(Calendars.CALENDAR_CONTENT_ATTENDEES),
                                    ATTENDEES_PROJECTION,
                                    Calendars.ATTENDEES_EVENT_ID_COL + " = ? ",
                                    eventArg,
                                    null);
                            try {
                                // Do something with attendees
                                int emailIndex = attendees.getColumnIndexOrThrow(Calendars.ATTENDEES_EMAIL_COL);
                                int nameIndex = attendees.getColumnIndexOrThrow(Calendars.ATTENDEES_NAME_COL);

                                ArrayList<String> names = new ArrayList<String>();
                                ArrayList<String> emails = new ArrayList<String>();

                                for (attendees.moveToFirst(); !attendees.isAfterLast(); attendees.moveToNext()) {
                                    String name = attendees.getString(nameIndex);
                                    String email = attendees.getString(emailIndex);
                                    if (!TextUtils.isEmpty(email)) {
                                        if (Constants.DEBUG)
                                            Log.w(CalendarAlarmScheduler.TAG, "Attendee: " + name + ", email: " + email);
                                        names.add(name);
                                        emails.add(email);
                                    }
                                }

                                if (emails.size() > 0) {
                                    Intent reminderActivity = new Intent(context, CalendarReminderActivity.class);
                                    reminderActivity.putStringArrayListExtra(CalendarReminderActivity.TOKEN_NAMES, names);
                                    reminderActivity.putStringArrayListExtra(CalendarReminderActivity.TOKEN_EMAILS, emails);
                                    reminderActivity.putExtra(CalendarReminderActivity.TOKEN_EVENT_NAME, title);
                                    reminderActivity.putExtra(CalendarReminderActivity.TOKEN_EVENT_TIME, startTime);
                                    reminderActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                                    context.startActivity(reminderActivity);
                                }
                            } finally {
                                attendees.close();
                            }
                        }
                    }
                } finally {
                    event.close();
                }
            }
        } catch (IllegalArgumentException e) { // Some cursor read failed
            e.printStackTrace();
        }
    }

}
