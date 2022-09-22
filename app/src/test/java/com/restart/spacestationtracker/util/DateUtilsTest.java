package com.restart.spacestationtracker.util;

import junit.framework.TestCase;

public class DateUtilsTest extends TestCase {

    public void testGetDateDifference() {
        long past = 1663632590000L;
        long future = 1663718990000L;
        assertEquals(DateUtils.getDateDifference(past, future), "1 day in space");

        past = 1663546190000L;
        assertEquals(DateUtils.getDateDifference(past, future), "2 days in space");

        past = 1663286990000L;
        assertEquals(DateUtils.getDateDifference(past, future), "5 days in space");

        past = 1661040590000L;
        assertEquals(DateUtils.getDateDifference(past, future), "1 month in space");

        past = 1658362190000L;
        assertEquals(DateUtils.getDateDifference(past, future), "2 months in space");

        past = 1650499790000L;
        assertEquals(DateUtils.getDateDifference(past, future), "5 months in space");

        past = 1632182990000L;
        assertEquals(DateUtils.getDateDifference(past, future), "1 year in space");

        past = 1600646990000L;
        assertEquals(DateUtils.getDateDifference(past, future), "2 years in space");
    }
}