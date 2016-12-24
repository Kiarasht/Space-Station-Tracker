package com.restart.spacestationtracker.data;

import android.content.res.Resources;

import com.restart.spacestationtracker.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

public class ExpandableListDataPump {

    public static HashMap<String, List<String>> getData(Resources resources) {
        LinkedHashMap<String, List<String>> expandableListDetail = new LinkedHashMap<>();

        List<String> general = new ArrayList<>();
        general.add(resources.getString(R.string.rateHelp));

        List<String> contribute = new ArrayList<>();
        contribute.add(resources.getString(R.string.contributeHelp));

        List<String> thanks = new ArrayList<>();
        thanks.add(resources.getString(R.string.thanksPicasso));

        thanks.add(resources.getString(R.string.thanksKashima));

        thanks.add(resources.getString(R.string.thanksDodenhof));

        thanks.add(resources.getString(R.string.thanksSikun));

        thanks.add(resources.getString(R.string.thanksChaudhary));

        thanks.add(resources.getString(R.string.thanksHuang));

        thanks.add(resources.getString(R.string.thanksBergey));

        thanks.add(resources.getString(R.string.thanksTrain));

        thanks.add(resources.getString(R.string.thanksPrado));

        List<String> about = new ArrayList<>();
        about.add(resources.getString(R.string.helpVersion) + "3.3");
        about.add(resources.getString(R.string.helpBuild) + "12/17/2016");

        expandableListDetail.put(resources.getString(R.string.rateMeHelp), general);
        expandableListDetail.put(resources.getString(R.string.helpContribute), contribute);
        expandableListDetail.put(resources.getString(R.string.helpThanks), thanks);
        expandableListDetail.put(resources.getString(R.string.helpAbout), about);
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
