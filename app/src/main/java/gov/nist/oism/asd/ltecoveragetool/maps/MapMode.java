package gov.nist.oism.asd.ltecoveragetool.maps;

import android.content.Context;

import java.io.File;

public class MapMode {

    public static final String GPS_OPTION = "gps_option";
    public static final String NO_GPS_OPTION = "no_gps_option";
    public static final String FLOOR_OPTION = "floor_option";

    public static final String getHumanReadableOption(String option) {
        switch (option) {
            case NO_GPS_OPTION:
                return "Map without GPS Result";
            case FLOOR_OPTION:
                return "Floor Plan Map Result";
            case GPS_OPTION:
            default:
                return "Map with GPS Result";
        }
    }

    public static File getExternalDataFile(Context context, String fileName) {
        return new File(context.getExternalFilesDir(null), fileName + ".csv");
    }

}
