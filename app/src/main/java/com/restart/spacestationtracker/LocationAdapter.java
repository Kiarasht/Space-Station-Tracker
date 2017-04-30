package com.restart.spacestationtracker;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

public class LocationAdapter extends RecyclerView.Adapter<LocationAdapter.LocationAdapterViewHolder> {

    private final Activity mActivity;
    private List<String> mDates;

    public LocationAdapter(Activity activity, List<String> dates) {
        this.mActivity = activity;
        this.mDates = dates;
    }

    class LocationAdapterViewHolder extends RecyclerView.ViewHolder {
        private TextView mDate;

        LocationAdapterViewHolder(View view) {
            super(view);

            mDate = (TextView) view.findViewById(R.id.date);
        }
    }

    @Override
    public LocationAdapterViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new LocationAdapter.LocationAdapterViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.location_row, parent, false));
    }

    @Override
    public void onBindViewHolder(LocationAdapter.LocationAdapterViewHolder holder, int position) {
        holder.mDate.setText(mDates.get(position));
    }

    @Override
    public int getItemCount() {
        if (mDates == null) return 0;
        return mDates.size();
    }

    /**
     * Set the data list of recyclerview to a new one
     *
     * @param dataSet the new data list
     */
    public void setDataSet(List<String> dataSet) {
        mDates = dataSet;
        notifyDataSetChanged();
    }
}