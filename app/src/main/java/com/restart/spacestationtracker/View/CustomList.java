package com.restart.spacestationtracker.view;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.restart.spacestationtracker.Info;
import com.restart.spacestationtracker.R;
import com.restart.spacestationtracker.data.Astronaut;
import com.squareup.picasso.Picasso;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class CustomList extends ArrayAdapter<String> {

    private final List<Astronaut> astronauts;
    private final Activity context;

    public CustomList(Activity context, List<Astronaut> astronauts, List<String> astronautNames) {
        super(context, R.layout.layout_listview, astronautNames);
        this.context = context;
        this.astronauts = astronauts;
    }

    @NonNull
    @Override
    public View getView(final int position, View view, @NonNull ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();
        View rowView = inflater.inflate(R.layout.layout_listview, null, true);
        final TextView name = (TextView) rowView.findViewById(R.id.name);
        final TextView role = (TextView) rowView.findViewById(R.id.role);
        final CircleImageView imageView = (CircleImageView) rowView.findViewById(R.id.img);
        final ImageView countryFlag = (ImageView) rowView.findViewById(R.id.countryFlag);
        final ImageView astronautTwitter = (ImageView) rowView.findViewById(R.id.astronautTwitter);
        final ImageView astronautWiki = (ImageView) rowView.findViewById(R.id.astronautWiki);

        if (astronauts.get(position) != null) {
            String location = "Tiangong-2";

            if (astronauts.get(position).getLocation().contains("International Space Station")) {
                location = "ISS";
            }

            if (astronauts.get(position).getTwitter().length() != 0) {
                astronautTwitter.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_action_twitter));
            }

            astronautTwitter.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (astronauts.get(position).getTwitter().length() != 0) {
                        context.startActivity(new Intent(context, Info.class)
                                .putExtra("url", astronauts.get(position).getTwitter())
                                .putExtra("astro", astronauts.get(position).getName()));
                    } else {
                        Toast.makeText(context, astronauts.get(position).getName().split(" ")[0] + " " + getContext().getString(R.string.errorNoTwitter), Toast.LENGTH_SHORT).show();
                    }
                }
            });

            astronautWiki.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (astronauts.get(position).getWiki().length() != 0) {
                        context.startActivity(new Intent(context, Info.class)
                                .putExtra("url", astronauts.get(position).getWiki())
                                .putExtra("astro", astronauts.get(position).getName()));
                    } else {
                        Toast.makeText(context, astronauts.get(position).getName().split(" ")[0] + " " + getContext().getString(R.string.errorNoWiki), Toast.LENGTH_SHORT).show();
                    }
                }
            });

            name.setText(astronauts.get(position).getName());
            role.setText(astronauts.get(position).getRole() + context.getString(R.string.midAt) + location);
            Picasso.with(context).load(astronauts.get(position).getImage()).into(imageView);
            Picasso.with(context).load(astronauts.get(position).getCountryLink()).into(countryFlag);
        }

        return rowView;
    }
}
