package com.restart.spacestationtracker.view;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.restart.spacestationtracker.data.Astronaut;
import com.restart.spacestationtracker.R;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class CustomList extends ArrayAdapter<String> {

    private final Astronaut[] astronauts;
    private final Activity context;

    public CustomList(Activity context, Astronaut[] astronauts, String[] names) {
        super(context, R.layout.layout_listview, names);
        this.context = context;
        this.astronauts = astronauts;
    }

    @Override
    public View getView(final int position, View view, ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();
        View rowView = inflater.inflate(R.layout.layout_listview, null, true);
        final TextView name = (TextView) rowView.findViewById(R.id.name);
        final TextView role = (TextView) rowView.findViewById(R.id.role);
        final CircleImageView imageView = (CircleImageView) rowView.findViewById(R.id.img);

        // Everything as usual, our data base is updated
        if (astronauts[position] != null) {
            name.setText(astronauts[position].getName());
            role.setText(astronauts[position].getRole());
            Picasso.with(context).load(astronauts[position].getImage()).into(imageView);
        }

        return rowView;
    }
}