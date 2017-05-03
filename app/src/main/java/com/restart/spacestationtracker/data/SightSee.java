package com.restart.spacestationtracker.data;


import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class SightSee {
    private static final SimpleDateFormat mSimpleDateFormat;
    private static String location;
    private int duration;
    private Date riseTimeDate, setTimeDate;

    static {
        mSimpleDateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm a", Locale.getDefault());
    }

    public SightSee(int duration, int risetime) {
        this.duration = duration;

        riseTimeDate = new Date(risetime * 1000L);

        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        calendar.setTime(riseTimeDate);
        calendar.add(Calendar.SECOND, duration);

        setTimeDate = new Date(calendar.getTimeInMillis());
    }

    public String getDuration() {
        return Math.ceil(duration / 60) + " minutes";
    }

    public String getRiseTime() {
        return mSimpleDateFormat.format(riseTimeDate);
    }

    public static String getLocation() {
        return SightSee.location;
    }

    public static void setLocation(String location) {
        SightSee.location = location;
    }

    public Date getRiseTimeDate() {
        return riseTimeDate;
    }

    public Date getSetTimeDate() {
        return setTimeDate;
    }
}
