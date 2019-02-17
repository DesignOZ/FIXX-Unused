package com.tistory.overimagine.voltecalllogfix.Util;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.tistory.overimagine.voltecalllogfix.R;

/**
 * Created by 경비 on 2015-12-29.
 */
public class  AdmobPreference extends Preference {
    LayoutInflater mLayoutInFlater;

    public AdmobPreference(Context context) {
        super(context, null);
    }

    public AdmobPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        //override here to return the admob ad instead of a regular preference display
        View view;

        super.onCreateView(parent);
        mLayoutInFlater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        view = mLayoutInFlater.inflate(R.layout.admob_preference, null);

        AdView adView = (AdView) view.findViewById(R.id.ad);
        AdRequest adRequest = new AdRequest.Builder()
                .setRequestAgent("android_studio:ad_template").build();
        adView.loadAd(adRequest);
        return view;

    }

}