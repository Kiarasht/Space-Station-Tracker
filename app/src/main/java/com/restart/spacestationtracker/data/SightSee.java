package com.restart.spacestationtracker.data;


import java.text.DecimalFormat;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * The type Sight see. Includes data to be able to identify an ISS flyby. Each flyby is represented
 * by a single SightSee.
 */
public class SightSee {
    /**
     * Variables to define a single SightSee. A list of SightSee all are represented by a single location.
     */
    private static final Format mSimpleDateFormat;
    private static final Format mDecimalFormat;
    private static String location;
    private final Date mRiseTimeDate;
    private final Date mSetTimeDate;
    private final String mDuration;
    private final String mRiseTime;

    /* Format all dates and decimals in the same manner. */
    static {
        mSimpleDateFormat = new SimpleDateFormat("MM/dd/yyyy hh:mm a", Locale.getDefault());
        mDecimalFormat = new DecimalFormat("#.#");
    }

    /**
     * Instantiates a new Sight see. Format any initialize any other variables.
     *
     * @param duration The duration the ISS will be approximately visible from rise to set.
     * @param risetime The risetime date time ISS will start to be visible.
     */
    public SightSee(int duration, int risetime) {
        mDuration = mDecimalFormat.format((double) duration / 60) + " minutes";

        mRiseTimeDate = new Date(risetime * 1000L);
        mRiseTime = mSimpleDateFormat.format(mRiseTimeDate);

        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        calendar.setTime(mRiseTimeDate);
        calendar.add(Calendar.SECOND, duration);

        mSetTimeDate = new Date(calendar.getTimeInMillis());
    }

    /**
     * Gets duration.
     *
     * @return the duration
     */
    public String getDuration() {
        return mDuration;
    }

    /**
     * Gets rise time.
     *
     * @return the rise time
     */
    public String getRiseTime() {
        return mRiseTime;
    }

    /**
     * Gets location.
     *
     * @return the location
     */
    public static String getLocation() {
        return SightSee.location;
    }

    /**
     * Sets location.
     *
     * @param location the location
     */
    public static void setLocation(String location) {
        SightSee.location = location;
    }

    /**
     * Gets rise time date.
     *
     * @return the rise time date
     */
    public Date getRiseTimeDate() {
        return mRiseTimeDate;
    }

    /**
     * Gets set time date.
     *
     * @return the set time date
     */
    public Date getSetTimeDate() {
        return mSetTimeDate;
    }
}
