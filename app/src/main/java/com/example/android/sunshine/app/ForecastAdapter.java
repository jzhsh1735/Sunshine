package com.example.android.sunshine.app;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.android.sunshine.app.data.WeatherContract;

public class ForecastAdapter extends RecyclerView.Adapter<ForecastAdapter.ForecastAdapterViewHolder> {

    private static final String LOG_TAG = ForecastAdapter.class.getSimpleName();

    private static final int VIEW_TYPE_TODAY = 0;
    private static final int VIEW_TYPE_FUTURE_DAY = 1;

    private boolean mUseTodayLayout = true;

    private Cursor mCursor;
    private final Context mContext;
    private final ForecastAdapterOnClickHandler mClickHandler;
    private final View mEmptyView;
    private final ItemChoiceManager mChoiceManager;

    public static interface ForecastAdapterOnClickHandler {
        void onClick(long date, ForecastAdapterViewHolder holder);
    }

    public class ForecastAdapterViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public final ImageView mIconView;
        public final TextView mDateView;
        public final TextView mDescriptionView;
        public final TextView mHighTempView;
        public final TextView mLowTempView;

        public ForecastAdapterViewHolder(View view) {
            super(view);
            mIconView = (ImageView) view.findViewById(R.id.list_item_icon);
            mDateView = (TextView) view.findViewById(R.id.list_item_date_textview);
            mDescriptionView = (TextView) view.findViewById(R.id.list_item_forecast_textview);
            mHighTempView = (TextView) view.findViewById(R.id.list_item_high_textview);
            mLowTempView = (TextView) view.findViewById(R.id.list_item_low_textview);
            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            int adapterPosition = getAdapterPosition();
            mCursor.moveToPosition(adapterPosition);
            int dateColumnIndex = mCursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_DATE);
            mClickHandler.onClick(mCursor.getLong(dateColumnIndex), this);
            mChoiceManager.onClick(this);
        }
    }

    public ForecastAdapter(Context context, ForecastAdapterOnClickHandler clickHandler, View emptyView, int choiceMode) {
        mContext = context;
        mClickHandler = clickHandler;
        mEmptyView = emptyView;
        mChoiceManager = new ItemChoiceManager(this);
        mChoiceManager.setChoiceMode(choiceMode);
    }

    public void setUseTodayLayout(boolean useTodayLayout) {
        mUseTodayLayout = useTodayLayout;
    }

    public Cursor getCursor() {
        return mCursor;
    }

    public void swapCursor(Cursor cursor) {
        mCursor = cursor;
        notifyDataSetChanged();
        mEmptyView.setVisibility(getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        mChoiceManager.onRestoreInstanceState(savedInstanceState);
    }

    public void onSaveInstanceState(Bundle outState) {
        mChoiceManager.onSaveInstanceState(outState);
    }

    public int getSelectedItemPosition() {
        return mChoiceManager.getSelectedItemPosition();
    }

    public void selectView(RecyclerView.ViewHolder holder) {
        if (holder instanceof ForecastAdapterViewHolder) {
            ForecastAdapterViewHolder viewHolder = (ForecastAdapterViewHolder) holder;
            viewHolder.onClick(viewHolder.itemView);
        }
    }

    @Override
    public ForecastAdapterViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (parent instanceof RecyclerView) {
            int layoutId = -1;
            switch (viewType) {
                case VIEW_TYPE_TODAY: {
                    layoutId = R.layout.list_item_forecast_today;
                    break;
                }
                case VIEW_TYPE_FUTURE_DAY: {
                    layoutId = R.layout.list_item_forecast;
                    break;
                }
            }
            View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
            view.setFocusable(true);
            return new ForecastAdapterViewHolder(view);
        } else {
            throw new RuntimeException("Not bound to RecyclerView");
        }
    }

    @Override
    public void onBindViewHolder(ForecastAdapterViewHolder holder, int position) {
        mCursor.moveToPosition(position);
        int weatherId = mCursor.getInt(ForecastFragment.COL_WEATHER_CONDITION_ID);
        int viewType = getItemViewType(position);
        int fallbackIconId;

        switch (viewType) {
            case VIEW_TYPE_TODAY: {
                fallbackIconId = Utility.getArtResourceForWeatherCondition(weatherId);
                break;
            }
            default: {
                fallbackIconId = Utility.getIconResourceForWeatherCondition(weatherId);
                break;
            }
        }

        if (Utility.usingLocalGraphics(mContext)) {
            holder.mIconView.setImageResource(fallbackIconId);
        } else {
            Glide.with(mContext)
                    .load(Utility.getArtUrlForWeatherCondition(mContext, weatherId))
                    .error(fallbackIconId)
                    .crossFade()
                    .into(holder.mIconView);
        }

        ViewCompat.setTransitionName(holder.mIconView, "iconView" + position);

        long dateInMillis = mCursor.getLong(ForecastFragment.COL_WEATHER_DATE);
        holder.mDateView.setText(Utility.getFriendlyDayString(mContext, dateInMillis));

        String description = Utility.getStringForWeatherCondition(mContext, weatherId);
        holder.mDescriptionView.setText(description);
        holder.mDescriptionView.setContentDescription(mContext.getString(R.string.a11y_forecast, description));
        holder.mIconView.setContentDescription(description);

        String high = Utility.formatTemperature(mContext, mCursor.getDouble(ForecastFragment.COL_WEATHER_MAX_TEMP));
        holder.mHighTempView.setText(high);
        holder.mHighTempView.setContentDescription(mContext.getString(R.string.a11y_high_temp, high));

        String low = Utility.formatTemperature(mContext, mCursor.getDouble(ForecastFragment.COL_WEATHER_MIN_TEMP));
        holder.mLowTempView.setText(low);
        holder.mLowTempView.setContentDescription(mContext.getString(R.string.a11y_low_temp, low));

        mChoiceManager.onBindViewHolder(holder, position);
    }

    @Override
    public int getItemViewType(int position) {
        return (position == 0 && mUseTodayLayout)? VIEW_TYPE_TODAY : VIEW_TYPE_FUTURE_DAY;
    }

    @Override
    public int getItemCount() {
        if (mCursor == null) {
            return 0;
        }
        return mCursor.getCount();
    }
}