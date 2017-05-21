package com.restart.spacestationtracker;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.util.Linkify;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

public class AboutActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about_layout);

        ((TextView) findViewById(R.id.app_version)).setText(getString(R.string.msg_app_version, getVersionName()));
        initLicenses();
        initVersions();
    }

    private void initLicenses() {
        LayoutInflater inflater = LayoutInflater.from(this);
        LinearLayout content = (LinearLayout) findViewById(R.id.licenses);

        String[] softwareList = getResources().getStringArray(R.array.software_list);
        String[] licenseList = getResources().getStringArray(R.array.license_list);
        content.addView(createItemsText(softwareList));
        for (int i = 0; i < softwareList.length; i++) {
            content.addView(createDivider(inflater, content));
            content.addView(createHeader(softwareList[i]));
            content.addView(createHtmlText(licenseList[i]));
        }
    }

    private void initVersions() {
        LayoutInflater inflater = LayoutInflater.from(this);
        LinearLayout content = (LinearLayout) findViewById(R.id.version);

        String[] versionTitleList = getResources().getStringArray(R.array.version_title_list);
        String[] versionList = getResources().getStringArray(R.array.version_list);
        for (int i = 0; i < versionTitleList.length; i++) {
            content.addView(createDivider(inflater, content));
            content.addView(createHeader(versionTitleList[i]));
            content.addView(createHtmlText(versionList[i]));
        }
    }

    private TextView createHeader(final String name) {
        String s = "<big><b>" + name + "</b></big>";
        return createHtmlText(s);
    }

    private TextView createItemsText(final String... names) {
        StringBuilder s = new StringBuilder();
        for (String name : names) {
            if (s.length() > 0) {
                s.append("<br>");
            }
            s.append("- ");
            s.append(name);
        }
        return createHtmlText(s.toString());
    }

    private TextView createHtmlText(final String s) {
        TextView text = new TextView(this);
        text.setAutoLinkMask(Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES);
        text.setText(Html.fromHtml(s));
        LinearLayout.LayoutParams layoutParams =
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
        int marginPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8,
                        getResources().getDisplayMetrics());
        layoutParams.setMargins(0, marginPx, 0, marginPx);
        text.setLayoutParams(layoutParams);
        return text;
    }

    private View createDivider(final LayoutInflater inflater, final ViewGroup parent) {
        return inflater.inflate(R.layout.divider, parent, false);
    }

    private String getVersionName() {
        final PackageManager manager = getPackageManager();
        String versionName;
        try {
            final PackageInfo info = manager.getPackageInfo(getPackageName(), PackageManager.GET_META_DATA);
            versionName = info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            versionName = "";
        }
        return versionName;
    }
}