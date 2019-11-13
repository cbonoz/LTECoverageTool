/*
 * This software was developed by employees of the National Institute of Standards and Technology (NIST), an agency of the Federal Government
 * and is being made available as a public service. Pursuant to title 17 United States Code Section 105, works of NIST employees are not
 * subject to copyright protection in the United States.  This software may be subject to foreign copyright.  Permission in the United States
 * and in foreign countries, to the extent that NIST may hold copyright, to use, copy, modify, create derivative works, and distribute
 * this software and its documentation without fee is hereby granted on a non-exclusive basis, provided that this notice and disclaimer of
 * warranty appears in all copies.
 *
 * THE SOFTWARE IS PROVIDED 'AS IS' WITHOUT ANY WARRANTY OF ANY KIND, EITHER EXPRESSED, IMPLIED, OR STATUTORY, INCLUDING, BUT NOT LIMITED TO,
 * ANY WARRANTY THAT THE SOFTWARE WILL CONFORM TO SPECIFICATIONS, ANY IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE,
 * AND FREEDOM FROM INFRINGEMENT, AND ANY WARRANTY THAT THE DOCUMENTATION WILL CONFORM TO THE SOFTWARE, OR ANY WARRANTY THAT THE SOFTWARE WILL
 * BE ERROR FREE.  IN NO EVENT SHALL NIST BE LIABLE FOR ANY DAMAGES, INCLUDING, BUT NOT LIMITED TO, DIRECT, INDIRECT, SPECIAL OR CONSEQUENTIAL
 * DAMAGES, ARISING OUT OF, RESULTING FROM, OR IN ANY WAY CONNECTED WITH THIS SOFTWARE, WHETHER OR NOT BASED UPON WARRANTY, CONTRACT, TORT, OR OTHERWISE,
 * WHETHER OR NOT INJURY WAS SUSTAINED BY PERSONS OR PROPERTY OR OTHERWISE, AND WHETHER OR NOT LOSS WAS SUSTAINED FROM, OR AROSE OUT OF THE RESULTS OF,
 * OR USE OF, THE SOFTWARE OR SERVICES PROVIDED HEREUNDER.
 */
package gov.nist.oism.asd.ltecoveragetool;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.Locale;

import gov.nist.oism.asd.ltecoveragetool.maps.FloorPlanActivity;
import gov.nist.oism.asd.ltecoveragetool.maps.GpsLineLayerActivity;
import gov.nist.oism.asd.ltecoveragetool.util.LteLog;

import static gov.nist.oism.asd.ltecoveragetool.maps.MapMode.FLOOR_OPTION;
import static gov.nist.oism.asd.ltecoveragetool.maps.MapMode.GPS_OPTION;
import static gov.nist.oism.asd.ltecoveragetool.maps.MapMode.NO_GPS_OPTION;
/*
 * Resources:
 * https://docs.mapbox.com/android/maps/examples/create-a-line-layer/
 * https://docs.mapbox.com/android/maps/examples/floor-plan/
 * https://docs.mapbox.com/android/maps/examples/click-to-add-image/
 */
public class NewRecordingActivity extends AppCompatActivity {

    public static final String OFFSET_KEY = "offset_key";
    public static final String MAP_MODE_KEY = "map_mode_key";

    private static final String TAG = NewRecordingActivity.class.getSimpleName();
    private static final int PERMISSION_REQUEST_ACCESS_COARSE_LOCATION = 1;
    private static final int PERMISSION_REQUEST_ACCESS_COARSE_LOCATION_START_ACTIVITY = 2;

    private EditText mOffsetUi;

    private Button gpsButton;
    private Button noGpsButton;
    private Button floorPlanButton;

    private String lastOptionSelected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_recording);

        gpsButton = findViewById(R.id.gps_map_button);
        noGpsButton = findViewById(R.id.no_gps_map_button);
        floorPlanButton = findViewById(R.id.floor_plan_map_button);

        gpsButton.setOnClickListener(view -> newRecordingButtonClicked(GPS_OPTION));
        noGpsButton.setOnClickListener(view -> newRecordingButtonClicked(NO_GPS_OPTION));
        floorPlanButton.setOnClickListener(view -> newRecordingButtonClicked(FLOOR_OPTION));

        mOffsetUi = findViewById(R.id.activity_new_recording_offset_ui);
        mOffsetUi.setText(String.format(Locale.getDefault(), "%.1f", 0.0));
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_ACCESS_COARSE_LOCATION);
        }
    }

    public void newRecordingButtonClicked(String option) {
        lastOptionSelected = option;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_ACCESS_COARSE_LOCATION_START_ACTIVITY);
        } else {
            startRecordingActivity();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_ACCESS_COARSE_LOCATION_START_ACTIVITY && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startRecordingActivity();
        }
    }

    private void startRecordingActivity() {
        final double offset;
        try {
            final String offsetValue = mOffsetUi.getText().toString().trim();
            if (!offsetValue.isEmpty()) {
                offset = Double.parseDouble(offsetValue);
            } else {
                offset = 0;
            }
        } catch (Exception caught) {
            LteLog.e(TAG, caught.getMessage(), caught);
            Toast.makeText(this, getString(R.string.bad_offset_value), Toast.LENGTH_SHORT).show();
            return;
        }

        final Intent intent;
        switch (lastOptionSelected) {
            case NO_GPS_OPTION:
            case GPS_OPTION:
                intent = new Intent(this, GpsLineLayerActivity.class);
                break;
            case FLOOR_OPTION:
                intent = new Intent(this, FloorPlanActivity.class);
                break;
            default:
                Toast.makeText(this, getString(R.string.unknown_mode), Toast.LENGTH_SHORT).show();
                return;
        }

        intent.putExtra(OFFSET_KEY, offset);
        intent.putExtra(MAP_MODE_KEY, lastOptionSelected);
        startActivity(intent);
    }
}
