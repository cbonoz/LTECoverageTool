package gov.nist.oism.asd.ltecoveragetool;

import android.app.Application;

import com.mapbox.mapboxsdk.Mapbox;

public class LteApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Mapbox.getInstance(getApplicationContext(), getString(R.string.testing_token));
    }
}
