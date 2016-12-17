package com.restart.spacestationtracker.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

public class ExpandableListDataPump {

    public static HashMap<String, List<String>> getData() {
        LinkedHashMap<String, List<String>> expandableListDetail = new LinkedHashMap<>();

        List<String> general = new ArrayList<>();
        general.add("ISS Tracker is an android application that allows you to track the position" +
                " of the International Space Station (ISS) and find out what astronauts are currently in space." +
                " The application comes with a notification system that can alert you whenever ISS" +
                " approaches your location.\n\n" +
                "Make sure to rate me! Click here.");

        List<String> contribute = new ArrayList<>();
        contribute.add("Space Station Tracker" +
                "\n" +
                "Kiarash Torkian" +
                "\n" +
                "Mozilla Public License Version 2.0");

        List<String> thanks = new ArrayList<>();
        thanks.add("Picasso" +
                "\n" +
                "Copyright 2013 Square, Inc." +
                "\n" +
                "Apache License Version 2.0");

        thanks.add("ObservableScrollView" +
                "\n" +
                "Copyright 2014 Soichiro Kashima" +
                "\n" +
                "Apache License Version 2.0");

        thanks.add("CircleImageView" +
                "\n" +
                "Copyright 2014 - 2016 Henning Dodenhof" +
                "\n" +
                "Apache License Version 2.0");

        thanks.add("MaterialSeekBarPreference" +
                "\n" +
                "Pavel Sikun" +
                "\n" +
                "Apache License Version 2.0");

        thanks.add("Spotlight" +
                "\n" +
                "Jitender Chaudhary" +
                "\n" +
                "Apache License Version 2.0");

        thanks.add("BoomMenu" +
                "\n" +
                "Weiping Huang" +
                "\n" +
                "Apache License Version 2.0");

        thanks.add("API service" +
                "\n" +
                "Nathan Bergey" +
                "\n" +
                "Attribution 3.0 Unported");

        thanks.add("Image" +
                "\n" +
                "Morning Train" +
                "\n" +
                "Free for commercial use");

        thanks.add("Image" +
                "\n" +
                "Luis Prado" +
                "\n" +
                "Attribution 3.0 United States");

        List<String> about = new ArrayList<>();
        about.add("Version: 3.3");
        about.add("Build on: 12/17/2016");

        expandableListDetail.put("Rate me", general);
        expandableListDetail.put("Contribute", contribute);
        expandableListDetail.put("Special Thanks", thanks);
        expandableListDetail.put("About ISS Tracker", about);
        return expandableListDetail;
    }

    public static ArrayList<String> getUrlList() {
        ArrayList<String> urlList = new ArrayList<>();

        urlList.add("https://github.com/Kiarasht/Space-Station-Tracker");
        urlList.add("https://github.com/square/picasso");
        urlList.add("https://github.com/ksoichiro/Android-ObservableScrollView");
        urlList.add("https://github.com/hdodenhof/CircleImageView");
        urlList.add("https://github.com/MrBIMC/MaterialSeekBarPreference");
        urlList.add("https://github.com/wooplr/Spotlight");
        urlList.add("https://github.com/Nightonke/BoomMenu");
        urlList.add("http://open-notify.org");
        urlList.add("https://www.iconfinder.com/morningtrain");
        urlList.add("https://thenounproject.com/Luis");
        return urlList;
    }
}
