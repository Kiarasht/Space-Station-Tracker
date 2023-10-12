package com.restart.spacestationtracker.adapter;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabColorSchemeParams;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.restart.spacestationtracker.R;
import com.restart.spacestationtracker.data.Astronaut;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * The type People in space adapter.
 */
public class PeopleInSpaceAdapter extends RecyclerView.Adapter<PeopleInSpaceAdapter.PeopleInSpaceAdapterViewHolder> {

    private final AppCompatActivity mActivity;
    private final SimpleDateFormat mDateFormat;
    private List<Astronaut> mAstronauts;

    /**
     * Instantiates a new People in space adapter.
     *
     * @param activity   The calling activity
     * @param astronauts The incoming list of astronauts to be displayed
     */
    public PeopleInSpaceAdapter(AppCompatActivity activity, List<Astronaut> astronauts) {
        mActivity = activity;
        mAstronauts = astronauts;
        mDateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
    }

    /**
     * The type People in space adapter view holder.
     */
    static class PeopleInSpaceAdapterViewHolder extends RecyclerView.ViewHolder {
        /**
         * All widgets that will hold values unique to an astronaut
         */
        private final ImageView mProfileImage;
        private final ProgressBar mAstronautPictureProgress;
        private final ImageView mCountryFlag;
        private final ImageView mAstronautTwitter;
        private final ImageView mAstronautFacebook;
        private final ImageView mAstronautInstagram;
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
            mAstronautInstagram = view.findViewById(R.id.astronautInstagram);
            mAstronautFacebook = view.findViewById(R.id.astronautFacebook);
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
        final Astronaut astronaut = mAstronauts.get(position);

        /* If an astronaut exists in the data set, let update the values */
        if (astronaut != null) {
            String location;
            if (astronaut.getIsIss()) location = "ISS";
            else location = "Tiangong-2";

            // Does the astronaut have a twitter handle?
            if (!astronaut.getTwitter().isEmpty()) {
                holder.mAstronautTwitter.setImageDrawable(ContextCompat.getDrawable(mActivity, R.drawable.ic_action_twitter));
            } else {
                holder.mAstronautTwitter.setImageDrawable(ContextCompat.getDrawable(mActivity, R.drawable.ic_action_twitter_grey));
            }

            holder.mAstronautTwitter.setOnClickListener(view -> {
                if (!astronaut.getTwitter().isEmpty()) {
                    startCustomTab(astronaut.getTwitter());
                } else {
                    Toast.makeText(mActivity, astronaut.getName().split(" ")[0] + " " + mActivity.getString(R.string.errorNoTwitter), Toast.LENGTH_SHORT).show();
                }
            });

            // Does the astronaut have a instagram handle?
            if (!astronaut.getInstagram().isEmpty()) {
                holder.mAstronautInstagram.setImageDrawable(ContextCompat.getDrawable(mActivity, R.drawable.ic_action_instagram));
            } else {
                holder.mAstronautInstagram.setImageDrawable(ContextCompat.getDrawable(mActivity, R.drawable.ic_action_instagram_grey));
            }

            holder.mAstronautInstagram.setOnClickListener(view -> {
                if (!astronaut.getInstagram().isEmpty()) {
                    startCustomTab(astronaut.getInstagram());
                } else {
                    Toast.makeText(mActivity, astronaut.getName().split(" ")[0] + " " + mActivity.getString(R.string.errorNoInstagram), Toast.LENGTH_SHORT).show();
                }
            });

            // Does the astronaut have a facebook handle?
            if (!astronaut.getFacebook().isEmpty()) {
                holder.mAstronautFacebook.setImageDrawable(ContextCompat.getDrawable(mActivity, R.drawable.ic_action_facebook));
            } else {
                holder.mAstronautFacebook.setImageDrawable(ContextCompat.getDrawable(mActivity, R.drawable.ic_action_facebook_grey));
            }

            holder.mAstronautFacebook.setOnClickListener(view -> {
                if (!astronaut.getFacebook().isEmpty()) {
                    startCustomTab(astronaut.getFacebook());
                } else {
                    Toast.makeText(mActivity, astronaut.getName().split(" ")[0] + " " + mActivity.getString(R.string.errorNoFacebook), Toast.LENGTH_SHORT).show();
                }
            });

            // Does the astronaut have a wiki handle?
            holder.mAstronautWiki.setOnClickListener(view -> {
                if (astronaut.getWiki().length() != 0) {
                    startCustomTab(astronaut.getWiki());
                } else {
                    Toast.makeText(mActivity, astronaut.getName().split(" ")[0] + " " + mActivity.getString(R.string.errorNoWiki), Toast.LENGTH_SHORT).show();
                }
            });

            holder.mAstronautGoogle.setOnClickListener(view ->
                    startCustomTab("https://www.google.com/search?q=" + astronaut.getName()));

            /* Set the astronaut info and images */
            holder.mName.setText(astronaut.getName());
            holder.mRole.setText(astronaut.getRole() + " " + mActivity.getString(R.string.midAt) + " " + location);
            holder.mBio.setText(astronaut.getBio());

            try {
                holder.mDate.setText(mActivity.getString(R.string.since) + mDateFormat.format(new Date(astronaut.getLaunchDate() * 1000L)));
            } catch (Exception e) {
                holder.mDate.setVisibility(View.INVISIBLE);
            }

            Glide.with(mActivity).load(astronaut.getImage()).listener(new RequestListener<Drawable>() {
                @Override
                public boolean onLoadFailed(@Nullable GlideException e, @Nullable Object model, @NonNull Target<Drawable> target, boolean isFirstResource) {
                    holder.mAstronautPictureProgress.setVisibility(View.GONE);
                    return false;
                }

                @Override
                public boolean onResourceReady(@NonNull Drawable resource, @NonNull Object model, Target<Drawable> target, @NonNull DataSource dataSource, boolean isFirstResource) {
                    holder.mAstronautPictureProgress.setVisibility(View.GONE);
                    return false;
                }
            }).centerCrop().error(R.drawable.ic_failure_profile).into(holder.mProfileImage);

            Glide.with(mActivity).load("https://flagsapi.com/" + astronaut.getFlagCode() + "/flat/64.png").into(holder.mCountryFlag);
        }
    }

    public void startCustomTab(String url) {
        CustomTabsIntent intent = new CustomTabsIntent.Builder().setDefaultColorSchemeParams(
                new CustomTabColorSchemeParams.Builder()
                .setToolbarColor(ContextCompat.getColor(mActivity, R.color.colorPrimary))
                .build())
                .setUrlBarHidingEnabled(true)
                .setShowTitle(true)
                .build();
        intent.launchUrl(mActivity, Uri.parse(url));
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
