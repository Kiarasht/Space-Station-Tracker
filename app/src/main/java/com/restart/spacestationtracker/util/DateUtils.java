package com.restart.spacestationtracker.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Helper method to parse and format any date related values and compare them to a user's
 * current date to give context of when an earth quake occurred.
 */
public class DateUtils {
    private final static long MILLISECONDS_TO_MINUTES = 60000;
    private final static long MINUTES_TO_HOURS = 60;
    private final static long HOURS_TO_DAYS = 24;
    private final static long DAYS_TO_MONTHS = 30;
    private final static long MONTHS_TO_YEAR = 12;
    private final static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss", Locale.getDefault());

    /**
     * Parse a date from the input string
     *
     * @param dateTime A string representing the date of an earth quake from the API.
     * @return A date parsed from the string.
     * @throws ParseException if the beginning of the specified string cannot be parsed.
     */
    public static Date parseDate(String dateTime) throws ParseException {
        return DATE_FORMAT.parse(dateTime);
    }

    /**
     * Parse a string from a given previous timestamp and user's current time which represents the
     * difference between the two.
     *
     * @param past A previous timestamp date, representing a previous earth quake.
     * @return The difference between the two dates in string.
     */
    public static String getDateDifference(long past) {
        return getStringDateDifference(new Date().getTime() - new Date(past).getTime());
    }

    /**
     * Parse a string from a given previous timestamp and a custom timestamp to compare it to.
     * Current can't be smaller than past.
     *
     * @param past   Timestamp representing a previous time.
     * @param future Timestamp representing a future timestamp.
     * @return A string representing the difference between the two dates.
     */
    public static String getDateDifference(long past, long future) {
        return getStringDateDifference(new Date(future).getTime() - new Date(past).getTime());
    }

    /**
     * Parses a string based on how big difference is. Always show the biggest unit of time and uses.
     * Doesn't take leap year, months with different days, etc. into account.
     *
     * @param difference A timestamp which represents a range of time
     * @return A parsed string to represent this timeline
     */
    private static String getStringDateDifference(long difference) {
        int difference_minutes = (int) (difference / MILLISECONDS_TO_MINUTES);
        int difference_hours = (int) (difference_minutes / MINUTES_TO_HOURS);
        int difference_days = (int) (difference_hours / HOURS_TO_DAYS);
        int difference_months = (int) (difference_days / DAYS_TO_MONTHS);
        int difference_years = (int) (difference_months / MONTHS_TO_YEAR);

        if (difference_hours < 24) {
            return "1 day in space";
        } else if (difference_days < 30) {
            return String.valueOf(difference_days) + " day" + isPlural(difference_days) + " in space";
        } else if (difference_months < 12) {
            return String.valueOf(difference_months) + " month" + isPlural(difference_months) + " in space";
        } else {
            return String.valueOf(difference_years) + " year" + isPlural(difference_years) + " in space";
        }
    }

    /**
     * If the time unit is plural or not
     *
     * @param numbers Amount of a time unit
     * @return s or empty
     */
    private static String isPlural(int numbers) {
        return numbers > 1 ? "s" : "";
    }
}