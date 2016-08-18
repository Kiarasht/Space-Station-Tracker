package com.restart.spacestationtracker.Data;

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
                " approaches your location. The system will also try to give you a heads up before" +
                " ISS approaches your location by bringing live data right into your notification bar.");

        List<String> contribute = new ArrayList<>();
        contribute.add("Issue? Crash? Please do tell." +
                "\n" +
                "Author: Kiarash Torkian" +
                "\n" +
                "Link: https://github.com/Kiarasht/Space-Station-Tracker");

        List<String> thanks = new ArrayList<>();
        thanks.add("Library: AVLoadingIndicatorView" +
                "\n" +
                "Author: Jack Wang" +
                "\n" +
                "Link: https://github.com/81813780/AVLoadingIndicatorView");

        thanks.add("Library: MaterialSeekBarPreference" +
                "\n" +
                "Author: Pavel Sikun" +
                "\n" +
                "Link: https://github.com/MrBIMC/MaterialSeekBarPreference");

        thanks.add("Library: Spotlight" +
                "\n" +
                "Author: Jitender Chaudhary" +
                "\n" +
                "Link: https://github.com/wooplr/Spotlight");

        thanks.add("Library: BoomMenu" +
                "\n" +
                "Author: Weiping Huang" +
                "\n" +
                "Link: https://github.com/Nightonke/BoomMenu");

        thanks.add("Library: API service" +
                "\n" +
                "Author: Nathan Bergey" +
                "\n" +
                "Link: http://open-notify.org/");

        thanks.add("Library: Image" +
                "\n" +
                "Author: Morning Train" +
                "\n" +
                "Link: https://www.iconfinder.com/morningtrain");

        thanks.add("Library: Image" +
                "\n" +
                "Author: Luis Prado" +
                "\n" +
                "Link: https://thenounproject.com/Luis/");

        List<String> about = new ArrayList<>();
        about.add("Version: 2.2");
        about.add("Build on: 07/28/2016");

        expandableListDetail.put("ISS Tracker", general);
        expandableListDetail.put("Contribute", contribute);
        expandableListDetail.put("Special Thanks", thanks);
        expandableListDetail.put("About ISS Tracker", about);
        return expandableListDetail;
    }
}
