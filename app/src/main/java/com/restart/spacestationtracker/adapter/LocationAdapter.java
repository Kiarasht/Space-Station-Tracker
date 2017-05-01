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

public class LocationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_ITEM = 1;
    private final Activity mActivity;
    private List<SightSee> mSightSees;
    private View mHeaderView;

    public LocationAdapter(Activity activity, View headerView) {
        this.mActivity = activity;
        this.mHeaderView = headerView;
    }

    class LocationAdapterViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private TextView mDate;
        private TextView mDuration;
        private Button mCalendar;
        private Button mShare;

        LocationAdapterViewHolder(View view) {
            super(view);
            mDate = (TextView) view.findViewById(R.id.date);
            mDuration = (TextView) view.findViewById(R.id.duration);
            mCalendar = (Button) view.findViewById(R.id.calendarButton);
            mShare = (Button) view.findViewById(R.id.shareButton);
            mCalendar.setOnClickListener(this);
            mShare.setOnClickListener(this);
        }

        /**
         * Called when a view has been clicked.
         *
         * @param v The view that was clicked.
         */
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.calendarButton:
                    Intent intent = new Intent(Intent.ACTION_INSERT)
                            .setData(Events.CONTENT_URI)
                            .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME,
                                    mSightSees.get(getAdapterPosition()).getRiseTimeDate().getTime())
                            .putExtra(CalendarContract.EXTRA_EVENT_END_TIME,
                                    mSightSees.get(getAdapterPosition()).getSetTimeDate().getTime())
                            .putExtra(Events.TITLE, "ISS Sighting")
                            .putExtra(Events.DESCRIPTION, "ISS will be visible here, going to check it out.")
                            .putExtra(Events.EVENT_LOCATION, mSightSees.get(getAdapterPosition()).getLocation())
                            .putExtra(Events.AVAILABILITY, Events.AVAILABILITY_BUSY);
                    mActivity.startActivity(intent);
                    break;
                case R.id.shareButton:
                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.putExtra(Intent.EXTRA_TEXT, "Check it out! ISS will be visible at " +
                            mSightSees.get(getAdapterPosition()).getLocation() + " on " +
                            mSightSees.get(getAdapterPosition()).getRiseTime() + ". ");
                    sendIntent.setType("text/plain");
                    mActivity.startActivity(sendIntent);
                    break;
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        return (position == 0) ? VIEW_TYPE_HEADER : VIEW_TYPE_ITEM;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_HEADER) {
            return new HeaderViewHolder(mHeaderView);
        } else {
            return new LocationAdapter.LocationAdapterViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.location_row, parent, false));
        }

    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof LocationAdapterViewHolder) {
            ((LocationAdapterViewHolder) holder).mDate.setText(mSightSees.get(position).getRiseTime());
            ((LocationAdapterViewHolder) holder).mDuration.setText(mSightSees.get(position).getDuration());
        }
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        public HeaderViewHolder(View view) {
            super(view);
        }
    }

    @Override
    public int getItemCount() {
        if (mSightSees == null) return 0;
        return mSightSees.size();
    }

    /**
     * Set the data list of recyclerview to a new one
     *
     * @param dataSet the new data list
     */
    public void setDataSet(List<SightSee> dataSet) {
        mSightSees = dataSet;
        notifyDataSetChanged();
    }
}