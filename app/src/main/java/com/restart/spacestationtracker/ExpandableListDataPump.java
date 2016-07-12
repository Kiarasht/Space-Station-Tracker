package com.restart.spacestationtracker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

public class ExpandableListDataPump {
    public static HashMap<String, List<String>> getData() {
        LinkedHashMap<String, List<String>> expandableListDetail = new LinkedHashMap<>();

        List<String> general = new ArrayList<>();
        general.add("ISS Tracker is an android application that allows you to easily track the position" +
                " of the International Space Station (ISS) and find out what astronauts are currently in space." +
                " The application comes with a notification system that can alert you whenever ISS" +
                " approaches your location and/or the astronauts in space change. The system will also try to" +
                " give you a heads up before ISS approaches your location by bringing live data right into" +
                " your notification bar.");

        List<String> contribute = new ArrayList<>();
        contribute.add("The source code for this application can be found at Github." +
                " If there are any issues that needs to be addressed please do tell.");

        List<String> thanks = new ArrayList<>();
        thanks.add("AVLoadingIndicatorView from Jack Wang");
        thanks.add("MaterialSeekBarPreference from Pavel Sikun");
        thanks.add("Spotlight from Jitender Chaudhary");
        thanks.add("BoomMenu from Weiping Huang");
        thanks.add("API service from Nathan Bergey");
        thanks.add("Image from Luis Prado");
        thanks.add("Image from Luis Prado");

        List<String> about = new ArrayList<>();
        about.add("Version 2.11");
        about.add("Build on: 07/11/2016");

        expandableListDetail.put("ISS Tracker", general);
        expandableListDetail.put("Contribute", contribute);
        expandableListDetail.put("Special Thanks", thanks);
        expandableListDetail.put("About ISS Tracker", about);
        return expandableListDetail;
    }
}
