package com.devlomi.commune.activities.settings;

import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceFragmentCompat;

import com.devlomi.commune.R;

/**
 * Created by Devlomi on 25/03/2018.
 */

public class PrivacyPolicyFragment extends PreferenceFragmentCompat {
    private TextView tvPrivacyPolicy;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.privacy_policy_fragment, container, false);

        initViews(view);


        getHtml(tvPrivacyPolicy);

        return view;
    }

    private void getHtml( TextView textView){
        String html = getResources().getString(R.string.privacy_policy_html);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {

            textView.setText(Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT));
        } else {
            textView.setText(Html.fromHtml(html));
        }
    }

    private void initViews(View view) {


        tvPrivacyPolicy = view.findViewById(R.id.tv_privacy_policy);

    }





}

