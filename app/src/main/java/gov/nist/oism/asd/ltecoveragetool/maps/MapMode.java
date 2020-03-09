package gov.nist.oism.asd.ltecoveragetool.maps;

import android.content.Context;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapMode {

    public static final String GPS_OPTION = "gps_option";
    public static final String NO_GPS_OPTION = "no_gps_option";
    public static final String FLOOR_OPTION = "floor_option";

    public static final String SEEN_FLOOR_OPTION = "seen_floor_option";
    public static final String SEEN_GPS_OPTION = "seen_gps_option";
    public static final String SEEN_NO_GPS_OPTION = "seen_no_gps_option";

    public static final String getHumanReadableOption(String option) {
        switch (option) {
            case NO_GPS_OPTION:
                return "Record route (no GPS)";
            case FLOOR_OPTION:
                return "Record floor plan";
            case GPS_OPTION:
            default:
                return "Record route (GPS)";
        }
    }

    public static final String[] FLOOR_OPTIONS = {"One Floor", "Two Floors", "Three Floors"};

}
