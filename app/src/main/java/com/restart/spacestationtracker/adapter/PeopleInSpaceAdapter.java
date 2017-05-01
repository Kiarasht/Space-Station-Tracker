package com.restart.spacestationtracker.adapter;

import android.app.Activity;
import android.content.Intent;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.restart.spacestationtracker.Info;
import com.restart.spacestationtracker.R;
import com.restart.spacestationtracker.data.Astronaut;
import com.restart.spacestationtracker.util.DateUtils;
import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class PeopleInSpaceAdapter extends RecyclerView.Adapter<PeopleInSpaceAdapter.PeopleInSpaceAdapterViewHolder> {

    private final Activity mActivity;
    private List<Astronaut> mAstronauts;

    public PeopleInSpaceAdapter(Activity activity, List<Astronaut> mAstronauts) {
        this.mActivity = activity;
        this.mAstronauts = mAstronauts;
    }

    class PeopleInSpaceAdapterViewHolder extends RecyclerView.ViewHolder {
        private CircleImageView mProfileImage;
        private ImageView mCountryFlag, mAstronautTwitter, mAstronautWiki;
        private TextView mName, mRole, mDate, mBio;

        PeopleInSpaceAdapterViewHolder(View view) {
            super(view);

            mName = (TextView) view.findViewById(R.id.name);
            mRole = (TextView) view.findViewById(R.id.role);
            mDate = (TextView) view.findViewById(R.id.days_since_launch);
            mBio = (TextView) view.findViewById(R.id.bio);
            mProfileImage = (CircleImageView) view.findViewById(R.id.img);
            mCountryFlag = (ImageView) view.findViewById(R.id.countryFlag);
            mAstronautTwitter = (ImageView) view.findViewById(R.id.astronautTwitter);
            mAstronautWiki = (ImageView) view.findViewById(R.id.astronautWiki);
        }
    }

    @Override
    public PeopleInSpaceAdapter.PeopleInSpaceAdapterViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new PeopleInSpaceAdapterViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.people_row, parent, false));
    }

    @Override
    public void onBindViewHolder(PeopleInSpaceAdapterViewHolder holder, int position) {
        final int pos = position;

        if (mAstronauts.get(pos) != null) {
            String location = "Tiangong-2";

            if (mAstronauts.get(pos).getLocation().contains("International Space Station")) {
                location = "ISS";
            }

            if (mAstronauts.get(pos).getTwitter().length() != 0) {
                holder.mAstronautTwitter.setImageDrawable(ContextCompat.getDrawable(mActivity, R.drawable.ic_action_twitter));
            }

            holder.mAstronautTwitter.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mAstronauts.get(pos).getTwitter().length() != 0) {
                        mActivity.startActivity(new Intent(mActivity, Info.class)
                                .putExtra("url", mAstronauts.get(pos).getTwitter())
                                .putExtra("astro", mAstronauts.get(pos).getName()));
                    } else {
                        Toast.makeText(mActivity, mAstronauts.get(pos).getName().split(" ")[0] + " " + mActivity.getString(R.string.errorNoTwitter), Toast.LENGTH_SHORT).show();
                    }
                }
            });

            holder.mAstronautWiki.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mAstronauts.get(pos).getWiki().length() != 0) {
                        mActivity.startActivity(new Intent(mActivity, Info.class)
                                .putExtra("url", mAstronauts.get(pos).getWiki())
                                .putExtra("astro", mAstronauts.get(pos).getName()));
                    } else {
                        Toast.makeText(mActivity, mAstronauts.get(pos).getName().split(" ")[0] + " " + mActivity.getString(R.string.errorNoWiki), Toast.LENGTH_SHORT).show();
                    }
                }
            });

            DateFormat sinceLaunch = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

            holder.mName.setText(mAstronauts.get(position).getName());
            holder.mRole.setText(mAstronauts.get(position).getRole() + " " + mActivity.getString(R.string.midAt) + " " + location);
            holder.mBio.setText(mAstronauts.get(position).getBio());
            try {
                holder.mDate.setText(DateUtils.getDateDifference(sinceLaunch.parse(mAstronauts.get(position).getLaunchDate()).getTime(),new Date().getTime()));
            } catch (ParseException e) {
                e.printStackTrace();
            }
            Picasso.with(mActivity).load(mAstronauts.get(position).getImage()).into(holder.mProfileImage);
            Picasso.with(mActivity).load(mAstronauts.get(position).getCountryLink()).into(holder.mCountryFlag);
        }
    }

    @Override
    public int getItemCount() {
        if (mAstronauts == null) return 0;
        return mAstronauts.size();
    }

    /**
     * Set the data list of recyclerview to a new one
     *
     * @param dataSet the new data list
     */
    public void setDataSet(List<Astronaut> dataSet) {
        mAstronauts = dataSet;
        notifyDataSetChanged();
    }
}
