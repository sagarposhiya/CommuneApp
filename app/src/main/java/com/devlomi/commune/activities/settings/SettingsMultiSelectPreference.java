package com.devlomi.commune.activities.settings;

import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.MultiSelectListPreference;

import com.devlomi.commune.R;
import com.devlomi.commune.utils.StringUtils;

/**
 * Created by Devlomi on 25/03/2018.
 */

/**
 * this class is to make Custom Multi Select List.
 * it is used in Media Auto Download Settings
 */

public class SettingsMultiSelectPreference extends MultiSelectListPreference {

    Context context;


    public SettingsMultiSelectPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        initContext(context);
        setSummary();
    }

    public SettingsMultiSelectPreference(Context context) {
        super(context);
        initContext(context);
        setSummary();
    }


    private void initContext(Context context) {
        this.context = context;
    }

    //setting summary values depending on what settings provided
    private void setSummary() {

        String summaryText = "";
        String separator = " , ";
        if (getValues().isEmpty()) {
            setSummary(R.string.no_media_summary);

        } else {

            for (String s : getValues()) {
                if (s.equals("0"))
                    summaryText += context.getString(R.string.photos) + separator;

                if (s.equals("1"))
                    summaryText += context.getString(R.string.audio) + separator;
                if (s.equals("2"))
                    summaryText += context.getString(R.string.videos) + separator;
                if (s.equals("3"))
                    summaryText += context.getString(R.string.files) + separator;

            }


            //removing separator from last word
            summaryText = StringUtils.removeExtraSeparators(summaryText, separator);


            setSummary(summaryText);
        }

    }




    @Override
    protected void notifyChanged() {
        super.notifyChanged();
        setSummary();
    }

    @Override
    protected void onSetInitialValue(Object defaultValue) {
        super.onSetInitialValue(defaultValue);
        setSummary();

    }
}
