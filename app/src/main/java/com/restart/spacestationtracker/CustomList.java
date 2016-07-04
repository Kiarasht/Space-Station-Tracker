package com.restart.spacestationtracker;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.koushikdutta.urlimageviewhelper.UrlImageViewHelper;

import de.hdodenhof.circleimageview.CircleImageView;

public class CustomList extends ArrayAdapter<String> {

    private final Activity context;
    private final Astronaut[] astronauts;

    public CustomList(Activity context, String[] astro, Astronaut[] astronauts) {
        super(context, R.layout.layout_listview, astro);
        this.context = context;
        this.astronauts = astronauts;
    }

    @Override
    public View getView(final int position, View view, ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();
        View rowView = inflater.inflate(R.layout.layout_listview, null, true);
        final TextView txtTitle = (TextView) rowView.findViewById(R.id.txt);
        final CircleImageView imageView = (CircleImageView) rowView.findViewById(R.id.img);
        txtTitle.setText(astronauts[position].getName());
        UrlImageViewHelper.setUrlDrawable(imageView, astronauts[position].getImage());

        return rowView;
    }
}
