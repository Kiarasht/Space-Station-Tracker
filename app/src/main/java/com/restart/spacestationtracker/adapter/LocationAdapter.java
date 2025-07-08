package com.restart.spacestationtracker.adapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Events;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.restart.spacestationtracker.R;
import com.restart.spacestationtracker.data.SightSee;

import java.util.List;

/**
 * The type Location adapter.
 */
public class LocationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_ITEM = 1;
    private final Activity mActivity;
    private List<SightSee> mSightSees;

    /**
     * Instantiates a new Location adapter.
     *
     * @param activity The calling activity
     */
    public LocationAdapter(Activity activity) {
        this.mActivity = activity;
    }

    private class LocationAdapterViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private final TextView mDate;
        private final TextView mDuration;

        /**
         * Instantiates a new Location adapter view holder.
         *
         * @param view The parent view holding the widgets.
         */
        LocationAdapterViewHolder(View view) {
            super(view);
            mDate = view.findViewById(R.id.date);
            mDuration = view.findViewById(R.id.duration);
            ImageButton mCalendar = view.findViewById(R.id.calendarButton);
            ImageButton mShare = view.findViewById(R.id.shareButton);
            mCalendar.setOnClickListener(this);
            mShare.setOnClickListener(this);
        }

        /**
         * Called when the calendar or share button views have been clicked.
         *
         * @param v The view that was clicked.
         */
        @Override
        public void onClick(View v) {
            if (v.getId() == R.id.calendarButton) {
                Intent intent = new Intent(Intent.ACTION_INSERT)
                        .setData(Events.CONTENT_URI)
                        .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME,
                                mSightSees.get(getBindingAdapterPosition()).getRiseTimeDate().getTime())
                        .putExtra(CalendarContract.EXTRA_EVENT_END_TIME,
                                mSightSees.get(getBindingAdapterPosition()).getSetTimeDate().getTime())
                        .putExtra(Events.TITLE, mActivity.getString(R.string.iss_sighting_share_title))
                        .putExtra(Events.DESCRIPTION, mActivity.getString(R.string.iss_sighting_share_description))
                        .putExtra(Events.EVENT_LOCATION, SightSee.getLocation())
                        .putExtra(Events.AVAILABILITY, Events.AVAILABILITY_BUSY);
                mActivity.startActivity(intent);
            } else if (v.getId() == R.id.shareButton) {
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, mActivity.getString(R.string.iss_sighting_share_body) +
                        SightSee.getLocation() + mActivity.getString(R.string.on) +
                        mSightSees.get(getBindingAdapterPosition()).getRiseTime() + ".\n\n" + mActivity.getString(R.string.msg_get_it_on_play_store_url));
                sendIntent.setType("text/plain");
                mActivity.startActivity(Intent.createChooser(sendIntent, mActivity.getString(R.string.iss_sighting_share_chooser)));
            }
        }
    }

    /**
     * Returns the view type of the item in the recycler view
     *
     * @param position The index of the view
     * @return If the view is the header of a regular item
     */
    @Override
    public int getItemViewType(int position) {
        return VIEW_TYPE_ITEM;
    }

    /**
     * Create the view holder but the header and list items have their own separate ones.
     *
     * @param parent   To inflate the view
     * @param viewType To differentiate between header vs. item
     * @return The new ViewHolder
     */
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new LocationAdapter.LocationAdapterViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.location_row, parent, false));
    }

    /**
     * Set widget values to of their corresponding values from the data set. Only if we are managing
     * the list items and not the header.
     *
     * @param holder   Incoming ViewHolder holding the widgets references
     * @param position The index we are interested in updating
     */
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof LocationAdapterViewHolder) {
            ((LocationAdapterViewHolder) holder).mDate.setText(mSightSees.get(position).getRiseTime());
            ((LocationAdapterViewHolder) holder).mDuration.setText(mSightSees.get(position).getDuration());
        }
    }

    /**
     * Size of the data set.
     *
     * @return Size of the data set.
     */
    @Override
    public int getItemCount() {
        if (mSightSees == null) return 0;
        return mSightSees.size();
    }

    /**
     * Set the data list of RecyclerView to a new one
     *
     * @param dataSet The new data list
     */
    @SuppressLint("NotifyDataSetChanged")
    public void setDataSet(List<SightSee> dataSet) {
        mSightSees = dataSet;
        notifyDataSetChanged();
    }
}