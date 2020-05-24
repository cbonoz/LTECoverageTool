package com.lte.mapmylte.mapper.www;

import android.app.Application;

import com.lte.mapmylte.mapper.www.R;
import com.mapbox.mapboxsdk.Mapbox;

public class LteApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Mapbox.getInstance(getApplicationContext(), getString(R.string.testing_token));
    }
}
