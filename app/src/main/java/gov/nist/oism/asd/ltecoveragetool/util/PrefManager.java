package gov.nist.oism.asd.ltecoveragetool.util;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

public class PrefManager {


    public final Activity context;

    public PrefManager(Activity context) {
        this.context = context;
    }

    public boolean getBoolPreference(String key) {
        SharedPreferences sharedPref = context.getPreferences(Context.MODE_PRIVATE);
        return sharedPref.getBoolean(key, false);
    }

    public void saveBoolPreference(String key, boolean value) {
        SharedPreferences sharedPref = context.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }


}
