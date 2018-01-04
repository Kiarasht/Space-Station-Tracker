package com.restart.spacestationtracker.adapter;

import android.app.Activity;
import android.content.Intent;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Events;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.restart.spacestationtracker.R;
import com.restart.spacestationtracker.data.SightSee;

import java.util.List;

/**
 * The type Location adapter.
 */
public class LocationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_ITEM = 1;
    private final Activity mActivity;
    private List<SightSee> mSightSees;
    private final View mHeaderView;

    /**
     * Instantiates a new Location adapter.
     *
     * @param activity   The calling activity
     * @param headerView The header view
     */
    public LocationAdapter(Activity activity, View headerView) {
        this.mActivity = activity;
        this.mHeaderView = headerView;
    }

    private class LocationAdapterViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private final TextView mDate;
        private final TextView mDuration;
        private final Button mCalendar;
        private final Button mShare;

        /**
         * Instantiates a new Location adapter view holder.
         *
         * @param view The parent view holding the widgets.
         */
        LocationAdapterViewHolder(View view) {
            super(view);
            mDate = view.findViewById(R.id.date);
            mDuration = view.findViewById(R.id.duration);
            mCalendar = view.findViewById(R.id.calendarButton);
            mShare = view.findViewById(R.id.shareButton);
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
            switch (v.getId()) {
                case R.id.calendarButton: // User is saving the ISS flyby event to phone calendar
                    Intent intent = new Intent(Intent.ACTION_INSERT)
                            .setData(Events.CONTENT_URI)
                            .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME,
                                    mSightSees.get(getAdapterPosition()).getRiseTimeDate().getTime())
                            .putExtra(CalendarContract.EXTRA_EVENT_END_TIME,
                                    mSightSees.get(getAdapterPosition()).getSetTimeDate().getTime())
                            .putExtra(Events.TITLE, "ISS Sighting")
                            .putExtra(Events.DESCRIPTION, "ISS will be visible here. Going to check it out!")
                            .putExtra(Events.EVENT_LOCATION, SightSee.getLocation())
                            .putExtra(Events.AVAILABILITY, Events.AVAILABILITY_BUSY);
                    mActivity.startActivity(intent);
                    break;
                case R.id.shareButton: // User is sharing the ISS flyby event
                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.putExtra(Intent.EXTRA_TEXT, "Check it out!\n\nISS will be visible at " +
                            SightSee.getLocation() + " on " +
                            mSightSees.get(getAdapterPosition()).getRiseTime() + ".\n\n" + mActivity.getString(R.string.msg_get_it_on_play_store_url));
                    sendIntent.setType("text/plain");
                    mActivity.startActivity(sendIntent);
                    break;
            }
        }
    }

    /**
     * The first position of the RecyclerView is its header. The proceeding are the list itself.
     *
     * @param position The index of the view
     * @return If the view is the header of a regular item
     */
    @Override
    public int getItemViewType(int position) {
        return (position == 0) ? VIEW_TYPE_HEADER : VIEW_TYPE_ITEM;
    }

    /**
     * Create the view holder but the header and list items have their own separate ones.
     *
     * @param parent   To inflate the view
     * @param viewType To differentiate between header vs. item
     * @return The new ViewHolder
     */
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_HEADER) {
            return new HeaderViewHolder(mHeaderView);
        } else {
            return new LocationAdapter.LocationAdapterViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.location_row, parent, false));
        }

    }

    /**
     * Set widget values to of their corresponding values from the data set. Only if we are managing
     * the list items and not the header.
     *
     * @param holder   Incoming ViewHolder holding the widgets references
     * @param position The index we are interested in updating
     */
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof LocationAdapterViewHolder) {
            ((LocationAdapterViewHolder) holder).mDate.setText(mSightSees.get(position).getRiseTime());
            ((LocationAdapterViewHolder) holder).mDuration.setText(mSightSees.get(position).getDuration());
        }
    }

    /**
     * The type Header view holder.
     */
    private static class HeaderViewHolder extends RecyclerView.ViewHolder {
        /**
         * Instantiates a new Header view holder.
         *
         * @param view the view
         */
        HeaderViewHolder(View view) {
            super(view);
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
    public void setDataSet(List<SightSee> dataSet) {
        mSightSees = dataSet;
        notifyDataSetChanged();
    }
}