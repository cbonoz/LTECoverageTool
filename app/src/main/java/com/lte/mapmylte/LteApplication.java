package com.lte.mapmylte;

import android.app.Application;

import com.lte.mapmylte.R;
import com.mapbox.mapboxsdk.Mapbox;
import com.revenuecat.purchases.Purchases;

public class LteApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Mapbox.getInstance(getApplicationContext(), getString(R.string.testing_token));
        Purchases configure = Purchases.configure(this, getString(R.string.rev_sdk));
        Purchases.setDebugLogsEnabled(true);
    }
}
