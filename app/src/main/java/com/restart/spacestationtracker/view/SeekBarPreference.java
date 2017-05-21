package com.restart.spacestationtracker.view;

/* The following code was written by Matthew Wiggins
 * and is released under the APACHE 2.0 license
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Improvements done:
 * - Save the value on positive button click, not on seekbar change
 * - handle @string/... values in xml file
 */

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;


public class SeekBarPreference extends DialogPreference implements SeekBar.OnSeekBarChangeListener, OnClickListener {
    /**
     * Padding for the dialogs. Gives a good amount of distance so the layout is not very close
     * to the edges. We do this for all four sides.
     */
    private static final float mDPI = Resources.getSystem().getDisplayMetrics().density;
    private static final int DIALOG_DPI_BOTTOM = 5;
    private static final int DIALOG_DPI_RIGHT = 14;
    private static final int DIALOG_DPI_LEFT = 19;
    private static final int DIALOG_DPI_TOP = 5;
    private static final int DIALOG_PADDING_BOTTOM = (int) (DIALOG_DPI_BOTTOM * mDPI);
    private static final int DIALOG_PADDING_RIGHT = (int) (DIALOG_DPI_RIGHT * mDPI);
    private static final int DIALOG_PADDING_LEFT = (int) (DIALOG_DPI_LEFT * mDPI);
    private static final int DIALOG_PADDING_TOP = (int) (DIALOG_DPI_TOP * mDPI);

    // ------------------------------------------------------------------------------------------
    // Private attributes :
    private static final String androidns = "http://schemas.android.com/apk/res/android";
    private SeekBar mSeekBar;
    private TextView mValueText;
    private final Context mContext;
    private final String mDialogMessage;
    private final String mSuffix;
    private final int mDefault;
    private int mMax;
    private int mValue = 0;
    private boolean mDecimalType;

    // ------------------------------------------------------------------------------------------


    // ------------------------------------------------------------------------------------------
    // Constructor :
    public SeekBarPreference(Context context, AttributeSet attrs) {

        super(context, attrs);
        mContext = context;

        // Get string value for dialogMessage :
        int mDialogMessageId = attrs.getAttributeResourceValue(androidns, "dialogMessage", 0);
        if (mDialogMessageId == 0)
            mDialogMessage = attrs.getAttributeValue(androidns, "dialogMessage");
        else mDialogMessage = mContext.getString(mDialogMessageId);

        // Get string value for suffix (text attribute in xml file) :
        int mSuffixId = attrs.getAttributeResourceValue(androidns, "text", 0);
        if (mSuffixId == 0) mSuffix = attrs.getAttributeValue(androidns, "text");
        else mSuffix = mContext.getString(mSuffixId);

        // Get default and max seekbar values :
        mDefault = attrs.getAttributeIntValue(androidns, "defaultValue", 0);
        mMax = attrs.getAttributeIntValue(androidns, "max", 100);

        if (mDefault == 9) {
            mDecimalType = true;
        }
    }
    // ------------------------------------------------------------------------------------------


    // ------------------------------------------------------------------------------------------
    // DialogPreference methods :
    @Override
    protected View onCreateDialogView() {
        LinearLayout.LayoutParams params;
        LinearLayout layout = new LinearLayout(mContext);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(DIALOG_PADDING_LEFT, DIALOG_PADDING_TOP, DIALOG_PADDING_RIGHT, DIALOG_PADDING_BOTTOM);

        TextView mSplashText = new TextView(mContext);
        mSplashText.setGravity(Gravity.CENTER_HORIZONTAL);
        mSplashText.setPadding(DIALOG_PADDING_LEFT, DIALOG_PADDING_TOP, DIALOG_PADDING_RIGHT, DIALOG_PADDING_BOTTOM);
        if (mDialogMessage != null) {
            mSplashText.setText(mDialogMessage);
        }
        layout.addView(mSplashText);

        mValueText = new TextView(mContext);
        mValueText.setGravity(Gravity.CENTER_HORIZONTAL);
        mValueText.setPadding(DIALOG_PADDING_LEFT, DIALOG_PADDING_TOP, DIALOG_PADDING_RIGHT, DIALOG_PADDING_BOTTOM);
        mValueText.setTextSize(24);
        params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layout.addView(mValueText, params);

        mSeekBar = new SeekBar(mContext);
        mSeekBar.setOnSeekBarChangeListener(this);
        layout.addView(mSeekBar, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        if (shouldPersist())
            mValue = getPersistedInt(mDefault);

        mSeekBar.setMax(mMax);
        mSeekBar.setProgress(mValue);

        return layout;
    }

    @Override
    protected void onBindDialogView(View v) {
        super.onBindDialogView(v);
        mSeekBar.setMax(mMax);
        mSeekBar.setProgress(mValue);
    }

    @Override
    protected void onSetInitialValue(boolean restore, Object defaultValue) {
        super.onSetInitialValue(restore, defaultValue);
        if (restore)
            mValue = shouldPersist() ? getPersistedInt(mDefault) : 0;
        else
            mValue = (Integer) defaultValue;
    }
    // ------------------------------------------------------------------------------------------


    // ------------------------------------------------------------------------------------------
    // OnSeekBarChangeListener methods :
    @Override
    public void onProgressChanged(SeekBar seek, int value, boolean fromTouch) {
        if (mDecimalType) {
            value += 1;
        }

        String t = String.valueOf(value);
        mValueText.setText(mSuffix == null ? t : t.concat(" " + mSuffix));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seek) {}

    @Override
    public void onStopTrackingTouch(SeekBar seek) {}

    public void setMax(int max) { mMax = max; }

    public int getMax() { return mMax; }

    public void setProgress(int progress) {
        mValue = progress;
        if (mSeekBar != null)
            mSeekBar.setProgress(progress);
    }

    public int getProgress() { return mValue; }
    // ------------------------------------------------------------------------------------------


    // ------------------------------------------------------------------------------------------
    // Set the positive button listener and onClick action :
    @Override
    public void showDialog(Bundle state) {

        super.showDialog(state);

        Button positiveButton = ((AlertDialog) getDialog()).getButton(AlertDialog.BUTTON_POSITIVE);
        positiveButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {

        if (shouldPersist()) {

            mValue = mSeekBar.getProgress();

            if (mDecimalType) {
                mValue += 1;
            }

            persistInt(mSeekBar.getProgress());
            callChangeListener(mSeekBar.getProgress());
        }

        getDialog().dismiss();
    }
    // ------------------------------------------------------------------------------------------
}
