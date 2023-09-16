package com.restart.spacestationtracker.adapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.restart.spacestationtracker.Info;
import com.restart.spacestationtracker.R;
import com.restart.spacestationtracker.data.Astronaut;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * The type People in space adapter.
 */
public class PeopleInSpaceAdapter extends RecyclerView.Adapter<PeopleInSpaceAdapter.PeopleInSpaceAdapterViewHolder> {

    private final Activity mActivity;
    private final SimpleDateFormat mDateFormat;
    private List<Astronaut> mAstronauts;

    /**
     * Instantiates a new People in space adapter.
     *
     * @param activity   The calling activity
     * @param astronauts The incoming list of astronauts to be displayed
     */
    public PeopleInSpaceAdapter(Activity activity, List<Astronaut> astronauts) {
        mActivity = activity;
        mAstronauts = astronauts;
        mDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    }

    /**
     * The type People in space adapter view holder.
     */
    static class PeopleInSpaceAdapterViewHolder extends RecyclerView.ViewHolder {
        /**
         * All widgets that will hold values unique to an astronaut
         */
        private final CircleImageView mProfileImage;
        private final ProgressBar mAstronautPictureProgress;
        private final ImageView mCountryFlag;
        private final ImageView mAstronautTwitter;
        private final ImageView mAstronautWiki;
        private final ImageView mAstronautGoogle;
        private final TextView mName;
        private final TextView mRole;
        private final TextView mDate;
        private final TextView mBio;

        /**
         * Instantiates a new People in space adapter view holder.
         *
         * @param view The incoming parent view
         */
        PeopleInSpaceAdapterViewHolder(View view) {
            super(view);

            /* Get references to each view. */
            mName = view.findViewById(R.id.name);
            mRole = view.findViewById(R.id.role);
            mDate = view.findViewById(R.id.days_since_launch);
            mBio = view.findViewById(R.id.bio);
            mProfileImage = view.findViewById(R.id.img);
            mCountryFlag = view.findViewById(R.id.countryFlag);
            mAstronautTwitter = view.findViewById(R.id.astronautTwitter);
            mAstronautWiki = view.findViewById(R.id.astronautWiki);
            mAstronautPictureProgress = view.findViewById(R.id.astronaut_picture_progress);
            mAstronautGoogle = view.findViewById(R.id.astronautGoogle);
        }
    }

    /**
     * Inflate the new list view.
     *
     * @param parent   To inflate the view
     * @param viewType N/A
     * @return New inflated view
     */
    @NonNull
    @Override
    public PeopleInSpaceAdapter.PeopleInSpaceAdapterViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new PeopleInSpaceAdapterViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.people_row, parent, false));
    }

    /**
     * Setup the values for each widgets. Use the incoming ViewHolder to gain access to their corresponding
     * references.
     *
     * @param holder   ViewHolder containing the widget references
     * @param position The index in our list being updated with new data
     */
    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull final PeopleInSpaceAdapterViewHolder holder, int position) {
        final int pos = position;

        /* If an astronaut exists in the data set, let update the values */
        if (mAstronauts.get(pos) != null) {
            String location = "Tiangong-2";

            // Are the astronauts on the ISS?
            if (mAstronauts.get(pos).getLocation().contains("International Space Station")) {
                location = "ISS";
            }

            // Does the astronaut have a twitter handle?
            if (!mAstronauts.get(pos).getTwitter().isEmpty()) {
                holder.mAstronautTwitter.setImageDrawable(ContextCompat.getDrawable(mActivity, R.drawable.ic_action_twitter));
            } else {
                holder.mAstronautTwitter.setImageDrawable(ContextCompat.getDrawable(mActivity, R.drawable.ic_action_twitter_grey));
            }

            holder.mAstronautTwitter.setOnClickListener(view -> {
                if (mAstronauts.get(pos).getTwitter().length() != 0) {
                    mActivity.startActivity(new Intent(mActivity, Info.class)
                            .putExtra("url", mAstronauts.get(pos).getTwitter())
                            .putExtra("astro", mAstronauts.get(pos).getName()));
                } else {
                    Toast.makeText(mActivity, mAstronauts.get(pos).getName().split(" ")[0] + " " + mActivity.getString(R.string.errorNoTwitter), Toast.LENGTH_SHORT).show();
                }
            });

            // Does the astronaut have a wiki handle?
            holder.mAstronautWiki.setOnClickListener(view -> {
                if (mAstronauts.get(pos).getWiki().length() != 0) {
                    mActivity.startActivity(new Intent(mActivity, Info.class)
                            .putExtra("url", mAstronauts.get(pos).getWiki())
                            .putExtra("astro", mAstronauts.get(pos).getName()));
                } else {
                    Toast.makeText(mActivity, mAstronauts.get(pos).getName().split(" ")[0] + " " + mActivity.getString(R.string.errorNoWiki), Toast.LENGTH_SHORT).show();
                }
            });

            holder.mAstronautGoogle.setOnClickListener(view -> mActivity.startActivity(new Intent(mActivity, Info.class)
                    .putExtra("url", "https://www.google.com/search?q=" + mAstronauts.get(pos).getName())
                    .putExtra("astro", mAstronauts.get(pos).getName())));

            /* Set the astro info and images */
            holder.mName.setText(mAstronauts.get(position).getName());
            holder.mRole.setText(mAstronauts.get(position).getRole() + " " + mActivity.getString(R.string.midAt) + " " + location);
            holder.mBio.setText(mAstronauts.get(position).getBio());

            try {
                holder.mDate.setText(mActivity.getString(R.string.since) + mDateFormat.format(mAstronauts.get(position).getLaunchDate()));
            } catch (Exception e) {
                holder.mDate.setVisibility(View.INVISIBLE);
            }

            Picasso.get().load(mAstronauts.get(position).getImage()).into(holder.mProfileImage, new Callback() {
                @Override
                public void onSuccess() {
                    holder.mAstronautPictureProgress.setVisibility(View.GONE);
                }

                @Override
                public void onError(Exception e) {
                    Picasso.get().load(R.drawable.ic_failure_profile).into(holder.mProfileImage);
                    holder.mAstronautPictureProgress.setVisibility(View.GONE);
                }
            });
            Picasso.get().load(mAstronauts.get(position).getCountryLink()).into(holder.mCountryFlag);
        }
    }

    /**
     * Size of our data base. Equals number of astronauts in space.
     *
     * @return An int representing the size
     */
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
    @SuppressLint("NotifyDataSetChanged")
    public void setDataSet(List<Astronaut> dataSet) {
        mAstronauts = dataSet;
        notifyDataSetChanged();
    }
}
