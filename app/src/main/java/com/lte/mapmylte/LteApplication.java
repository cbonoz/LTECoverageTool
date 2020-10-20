package com.lte.mapmylte;

import android.app.Application;

import com.lte.mapmylte.R;
import com.mapbox.mapboxsdk.Mapbox;

public class LteApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Mapbox.getInstance(getApplicationContext(), getString(R.string.testing_token));
    }
}
