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
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.telephony.CellIdentityLte;
import android.telephony.CellInfo;
import android.telephony.CellInfoLte;
import android.telephony.CellSignalStrengthLte;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.text.SpannableString;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.layers.Property;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import gov.nist.oism.asd.ltecoveragetool.util.LteLog;

import static com.mapbox.mapboxsdk.style.expressions.Expression.get;
import static com.mapbox.mapboxsdk.style.expressions.Expression.match;
import static com.mapbox.mapboxsdk.style.expressions.Expression.rgb;
import static com.mapbox.mapboxsdk.style.expressions.Expression.stop;
import static gov.nist.oism.asd.ltecoveragetool.maps.MapMode.FLOOR_OPTION;
import static gov.nist.oism.asd.ltecoveragetool.maps.MapMode.GPS_OPTION;

/*
 * Base activity for map-based signal strength recording.
 */
public abstract class RecordActivity extends AppCompatActivity {

    public static final String DATA_READINGS_KEY = "data_readings_key";
    public static final String OFFSET_KEY = "offset_key";

    private static final String TAG = RecordActivity.class.getSimpleName();
    private static final Object MUTEX = new Object();
    private static final long SAMPLE_RATE = 2000; // ms
    private static final int ANIMATION_MS = 3000;
    protected static String mapMode;

    protected MapView mapView;
    protected List<Point> routeCoordinates;

    protected MapboxMap mapboxMap;
    protected boolean setInitialPosition;
    public Style mapstyle;

    protected LocationManager locationManager;
    protected LocationListener locationListener;

    protected double lastLat = 0;
    protected double lastLng = 0;
    protected double lastAcc;
    protected double lastElevation;

    private Button mPauseRecordButton;
    private ImageView mRecordingImage;
    private TextView mRecordingImageLabel;
    private AlphaAnimation mRecordingImageAnimation;
    private SignalStrengthListener mSignalStrengthListener;
    private TextView mRsrpText, mRsrqText, mPciText, mDataPointsText, mOffsetText, mSignalStrengthText;
    private DataReading mCurrentReading;
    private double mOffset;
    private Timer mTimer;
    private List<DataReading> mDataReadings;

    private PermissionsManager permissionsManager;
    int count = 0;

    public JSONObject rawFeature;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupLocation();


        try {
            rawFeature = new JSONObject("{\"type\":\"FeatureCollection\",\"features\":[]}");
        } catch (
                JSONException e) {
            e.printStackTrace();
        }
        mCurrentReading = new DataReading();
        mDataReadings = new ArrayList<>();

        mOffset = getIntent().getDoubleExtra(NewRecordingActivity.OFFSET_KEY, 0.0);

        mPauseRecordButton = findViewById(R.id.activity_record_pause_resume_button_ui);
        mRecordingImage = findViewById(R.id.activity_record_record_image_ui);
        mRecordingImageLabel = findViewById(R.id.activity_record_record_image_label_ui);
        mRsrpText = findViewById(R.id.activity_record_lte_rsrp_text_ui);
        mRsrqText = findViewById(R.id.activity_record_lte_rsrq_text_ui);
        mPciText = findViewById(R.id.activity_record_lte_pci_text_ui);
        mDataPointsText = findViewById(R.id.activity_record_data_points_text_ui);
        mOffsetText = findViewById(R.id.activity_record_offset_text_ui);
        mSignalStrengthText = findViewById(R.id.activity_record_signal_strength_text_ui);

        // Make part of text clickable.
        ClickableSpan clickableSpan = new ClickableSpan() {

            @Override
            public void onClick(@NonNull View view) {
                Intent intent = new Intent(RecordActivity.this, UncertaintyNoticeActivity.class);
                startActivity(intent);
            }

            @Override
            public void updateDrawState(@NonNull TextPaint textPaint) {
                super.updateDrawState(textPaint);
                textPaint.setColor(getResources().getColor(R.color.activity_record_clickable_color));
            }
        };
        TextView rsrpLabel = findViewById(R.id.activity_record_lte_rsrp_label_ui);
        SpannableString rsrpSpan = new SpannableString(getString(R.string.activity_record_lte_rsrp_label_text));
        rsrpSpan.setSpan(clickableSpan, rsrpSpan.length() - 6, rsrpSpan.length(), 0);
        rsrpLabel.setMovementMethod(LinkMovementMethod.getInstance());
        rsrpLabel.setText(rsrpSpan);

        TextView rsrqLabel = findViewById(R.id.activity_record_lte_rsrq_label_ui);
        SpannableString rsrqSpan = new SpannableString(getString(R.string.activity_record_lte_rsrq_label_text));
        rsrqSpan.setSpan(clickableSpan, rsrqLabel.length() - 6, rsrqLabel.length(), 0);
        rsrqLabel.setMovementMethod(LinkMovementMethod.getInstance());
        rsrqLabel.setText(rsrqSpan);

        mRecordingImageAnimation = new AlphaAnimation(1, 0);
        mRecordingImageAnimation.setDuration(750);
        mRecordingImageAnimation.setInterpolator(new LinearInterpolator());
        mRecordingImageAnimation.setRepeatCount(Animation.INFINITE);
        mRecordingImageAnimation.setRepeatMode(Animation.REVERSE);

        mSignalStrengthListener = new SignalStrengthListener();

        // Now do the recording. We want to keep recording in the background so start and stop
        // in onCreate and onDestroy.
        ((TelephonyManager) getSystemService(TELEPHONY_SERVICE)).listen(mSignalStrengthListener, SignalStrengthListener.LISTEN_SIGNAL_STRENGTHS);
        setResumeRecordingState();

        mTimer = new Timer();
        mTimer.scheduleAtFixedRate(new TimerTask() {

            @Override
            public void run() {

                final DataReading dataReadingCopy;
                synchronized (MUTEX) {

                    // To be used on the UI thread.
                    dataReadingCopy = new DataReading(mCurrentReading);
                }

                try {
                    if (ActivityCompat.checkSelfPermission(RecordActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
                        List<CellInfo> allCellInfo = telephonyManager.getAllCellInfo();
                        if (allCellInfo != null) {
                            for (int i = 0; i < allCellInfo.size(); i++) {
                                if (allCellInfo.get(i).isRegistered() && allCellInfo.get(i) instanceof CellInfoLte) {
                                    CellInfoLte cellInfoLte = (CellInfoLte) allCellInfo.get(i);
                                    CellSignalStrengthLte signalStrengthLte = cellInfoLte.getCellSignalStrength();

                                    // Overwrite the SignalStrengthListener values if build is greater than 26 and only the rsrp if value less than 26.
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        dataReadingCopy.setRsrp(signalStrengthLte.getRsrp());
                                        dataReadingCopy.setRsrq(signalStrengthLte.getRsrq());
                                        LteLog.i(TAG, "(VERSION >= 26) rsrp: " + signalStrengthLte.getRsrp() + ", rsrq: " + signalStrengthLte.getRsrq());
                                    } else {
                                        dataReadingCopy.setRsrp(signalStrengthLte.getDbm()); // dbm = rsrp for values less than build 26.
                                        LteLog.i(TAG, String.format(Locale.getDefault(), "(VERSION < 26) rsrp: %d", signalStrengthLte.getDbm()));
                                    }

                                    // Now get the pci.
                                    CellIdentityLte identityLte = cellInfoLte.getCellIdentity();
                                    if (identityLte != null) {
                                        int pci = identityLte.getPci();
                                        if (pci == DataReading.UNAVAILABLE) {
                                            pci = -1;
                                        }
                                        dataReadingCopy.setPci(pci);
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception caught) {
                    LteLog.e(TAG, caught.getMessage(), caught);
                }

                // Adjust the rsrp;
                if (dataReadingCopy.getRsrp() == DataReading.UNAVAILABLE) {
                    dataReadingCopy.setRsrp(DataReading.LOW_RSRP);
                } else {
                    dataReadingCopy.setRsrp(dataReadingCopy.getRsrp() + (int) mOffset);
                }

                // Adjust the rsrq.
                if (dataReadingCopy.getRsrq() == DataReading.UNAVAILABLE) {
                    dataReadingCopy.setRsrq(DataReading.LOW_RSRQ);
                }

                if (isRecording()) {
                    mDataReadings.add(new DataReading(dataReadingCopy));
                }

                // To be used on the UI thread.
                final int numDataReadings = mDataReadings.size() == 0 ? 1 : mDataReadings.size();

                runOnUiThread(() -> {
                    if (isRecording()) {
                        if (dataReadingCopy.getRsrp() >= DataReading.EXECELLENT_RSRP_THRESHOLD) {
                            mSignalStrengthText.setText(getResources().getString(R.string.activity_record_signal_strength_excellent));
                        } else if (DataReading.EXECELLENT_RSRP_THRESHOLD > dataReadingCopy.getRsrp() && dataReadingCopy.getRsrp() >= DataReading.GOOD_RSRP_THRESHOLD) {
                            mSignalStrengthText.setText(getResources().getString(R.string.activity_record_signal_strength_good));
                        } else if (DataReading.GOOD_RSRP_THRESHOLD > dataReadingCopy.getRsrp() && dataReadingCopy.getRsrp() >= DataReading.POOR_RSRP_THRESHOLD) {
                            mSignalStrengthText.setText(getResources().getString(R.string.activity_record_signal_strength_poor));
                        } else {
                            mSignalStrengthText.setText(getResources().getString(R.string.activity_record_signal_strength_no_signal));
                        }
                        mRsrpText.setText(String.format(Locale.getDefault(), "%d", dataReadingCopy.getRsrp()));
                        mRsrqText.setText(String.format(Locale.getDefault(), "%d", dataReadingCopy.getRsrq()));
                        mPciText.setText(dataReadingCopy.getPci() == -1 ? "N/A" : String.format(Locale.getDefault(), "%d", dataReadingCopy.getPci()));
                        mDataPointsText.setText(String.format(Locale.getDefault(), "%d", numDataReadings));
                        mOffsetText.setText(String.format(Locale.getDefault(), "%.1f", mOffset));
                    }
                });
            }
        }, 1000, SAMPLE_RATE);
    }


    private void setupLocation() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            Toast.makeText(this, "Location permission not granted, please grant permission", Toast.LENGTH_LONG).show();
            return;
        }
//        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0, this);
    }

    protected void initRouteCoordinates() {
        // Create a list to store our line coordinates.
        routeCoordinates = new ArrayList<>();
//        routeCoordinates.add(Point.fromLngLat(-118.39439114221236, 33.397676454651766));
//        routeCoordinates.add(Point.fromLngLat(-118.39421054012902, 33.39769799454838));
//        routeCoordinates.add(Point.fromLngLat(-118.39408583869053, 33.39761901490136));
//        routeCoordinates.add(Point.fromLngLat(-118.39388373635917, 33.397328225582285));
//        routeCoordinates.add(Point.fromLngLat(-118.39372033447427, 33.39728514560042));
//        routeCoordinates.add(Point.fromLngLat(-118.3930882271826, 33.39756875508861));
//        routeCoordinates.add(Point.fromLngLat(-118.3928216241072, 33.39759029501192));

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (mSignalStrengthListener != null) {
                ((TelephonyManager) getSystemService(TELEPHONY_SERVICE)).listen(mSignalStrengthListener, SignalStrengthListener.LISTEN_NONE);
            }
        } catch (Exception caught) {
            LteLog.e(TAG, caught.getMessage(), caught);
        }

        mTimer.cancel();
        mTimer.purge();
        mTimer = null;
    }

    @Override
    public void onBackPressed() {
        AlertDialog alert = new AlertDialog.Builder(this)
                .setTitle(R.string.confirm)
                .setMessage("Do you want to cancel recording? All data will be lost.")
                .setPositiveButton("YES", (dialog, which) -> finish())
                .setNegativeButton("NO", (dialog, which) -> dialog.dismiss())
                .create();
        alert.show();
    }

    public void pauseRecordButtonClicked(View view) {
        if (isRecording()) {
            setPauseRecordingState();
        } else {
            setResumeRecordingState();
        }
    }

    public void stopButtonClicked(View view) {
        Intent intent = new Intent(this, DisplayResultsActivity.class);
        intent.putExtra(DATA_READINGS_KEY, (ArrayList<DataReading>) mDataReadings);
        intent.putExtra(OFFSET_KEY, mOffset);
        startActivity(intent);
        finish();
    }

    public void uncertaintyStatementButtonClicked(View view) {
        Intent intent = new Intent(this, UncertaintyNoticeActivity.class);
        startActivity(intent);
    }

    private boolean isRecording() {
        return getString(R.string.activity_record_pause_button_text).equals(mPauseRecordButton.getText().toString());
    }

    private void setPauseRecordingState() {
        mPauseRecordButton.setText(getString(R.string.activity_record_resume_button_text));
        mPauseRecordButton.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.ic_new_recording), null, null, null);
        mRecordingImage.setImageDrawable(getResources().getDrawable(R.drawable.ic_recording_paused));
        mRecordingImageLabel.setText(R.string.activity_record_recording_paused_image_label_text);
        mRecordingImage.clearAnimation();
    }

    private void setResumeRecordingState() {
        mPauseRecordButton.setText(getString(R.string.activity_record_pause_button_text));
        mPauseRecordButton.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.ic_pause), null, null, null);
        mRecordingImage.setImageDrawable(getResources().getDrawable(R.drawable.ic_recording));
        mRecordingImageLabel.setText(R.string.activity_record_record_image_label_text);
        mRecordingImage.startAnimation(mRecordingImageAnimation);
    }

    private Location getLastKnownLocation() {
        List<String> providers = locationManager.getProviders(true);
        Location bestLocation = null;
        for (String provider : providers) {
            Location l;
            try {
                l = locationManager.getLastKnownLocation(provider);
            } catch (SecurityException e) {
                l = null;
            }
            if (l == null) {
                continue;
            }
            if (bestLocation == null || l.getAccuracy() < bestLocation.getAccuracy()) {
                // Found best last known location: %s", l);
                bestLocation = l;
            }
        }
        return bestLocation;
    }

    private class SignalStrengthListener extends PhoneStateListener {

        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            LteLog.i(TAG, "onSignalStrengthsChanged: " + signalStrength.toString());
            //String[] values = signalStrength.toString().split(" ");
//            if (values.length > 12) {
            String temp = signalStrength.toString();
            String[] values = signalStrength.toString().split(" ");

            int rsrp = 0;
            int rsrq = 0;
            boolean found = false;
            if (temp.contains("rsrp=") && temp.contains("rsrq=")) {
                rsrp = Integer.parseInt(temp.substring(temp.indexOf("rsrp=") + 5, temp.indexOf(" ", temp.indexOf("rsrp=") + 5)));
                rsrq = Integer.parseInt(temp.substring(temp.indexOf("rsrq=") + 5, temp.indexOf(" ", temp.indexOf("rsrq=") + 5)));
                found = true;
            } else if (values != null && values.length > 12) {
                rsrp = Integer.parseInt(values[9]);
                rsrq = Integer.parseInt(values[10]);
                found = true;
            }

            if (found) {
                synchronized (MUTEX) {
                    mCurrentReading.setRsrp(rsrp);
                    mCurrentReading.setRsrq(rsrq);
                    mCurrentReading.setPci(DataReading.PCI_NA);
                    try {
                        final String provider = mapMode.equals(GPS_OPTION) ? LocationManager.GPS_PROVIDER : LocationManager.NETWORK_PROVIDER;

                        final Location lastKnownLocation = locationManager.getLastKnownLocation(provider); // or getLastKnownLocation() for provider agnostic.
                        if (lastKnownLocation == null) {
                            return;
                        }
                        lastLat = lastKnownLocation.getLatitude();
                        lastLng = lastKnownLocation.getLongitude();
                        lastAcc = lastKnownLocation.getAccuracy();
                        lastElevation = lastKnownLocation.getAltitude();

                        if (!setInitialPosition && canSetCameraPosition()) {
                            setCameraPosition(new LatLng(lastLat, lastLng));
                            setInitialPosition = true;
                        }
                    } catch (SecurityException e) {
                        LteLog.e("location error", e.getMessage(), e);
                    }
                    mCurrentReading.setLat(lastLat);
                    mCurrentReading.setLng(lastLng);
                    mCurrentReading.setAcc(lastAcc);
                    mCurrentReading.setElevation(lastElevation);
                }

                LteLog.i(TAG, String.format(Locale.getDefault(), "rsrp: %d, rsrq: %d", rsrp, rsrq));

                if (mapboxMap != null) {
                    Location tmp = new Location("");
                    tmp.setLatitude(lastLat);
                    tmp.setLongitude(lastLng);
                    try {
                        mapboxMap.getLocationComponent().forceLocationUpdate(tmp);
                    } catch (Exception e) {
                        LteLog.e("error forcing location update", e.getMessage(), e);
                    }

                    routeCoordinates.add(Point.fromLngLat(lastLng, lastLat));
                    count++;
                    if (routeCoordinates.size() >= 2 && rawFeature != null && rawFeature.has("features")) {
                        Log.d("plot", rawFeature.toString());

                        String grade = "";
                        if (mDataReadings != null) {
                            for (DataReading dataReading : mDataReadings) {
                                if (rsrp >= -95) {
                                    grade = "top";
                                } else if (rsrp < -95 && rsrp >= -103) {
                                    grade = "middlelow";
                                } else if (rsrp < -103 && rsrp >= -110) {
                                    grade = "middle";
                                } else {
                                    grade = "low";
                                }
                            }
                        }
                        String finalGrade = grade;
                        if (!mapMode.equals(FLOOR_OPTION)) {
                            // TODO: allow multiple style overlays for floor.
                            mapboxMap.setStyle(Style.OUTDOORS,
                                    style -> {
                                        // Retrieve GeoJSON from local file and add it to the map

                                        JSONObject tmp1 = new JSONObject();
                                        try {
                                            tmp1.put("type", "Feature");
                                            JSONObject color = new JSONObject();
                                            color.put("color", finalGrade);
                                            tmp1.put("properties", color);

                                            JSONObject geometry = new JSONObject();
                                            JSONArray coordinates = new JSONArray();

                                            for (int i = 0; i < routeCoordinates.size(); i++) {
                                                JSONArray coordinate = new JSONArray();
                                                coordinate.put(routeCoordinates.get(i).longitude());
                                                coordinate.put(routeCoordinates.get(i).latitude());
                                                coordinates.put(coordinate);
                                            }

                                            geometry.put("type", "LineString");
                                            geometry.put("coordinates", coordinates);
                                            tmp1.put("geometry", geometry);

                                            rawFeature.getJSONArray("features").put(tmp1);

                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }


                                        style.addSource(new GeoJsonSource("lines", rawFeature.toString()));
                                        style.addLayer(new LineLayer("finalLines", "lines").withProperties(
                                                PropertyFactory.lineColor(
                                                        match(
                                                                get("color"), rgb(0, 0, 0),
                                                                stop("top", rgb(0, 255, 0)),
                                                                stop("middle", rgb(255, 255, 0)),
                                                                stop("middlelow", rgb(255, 165, 0)),
                                                                stop("low", rgb(255, 0, 0))
                                                        )),
                                                PropertyFactory.visibility(Property.VISIBLE),
                                                PropertyFactory.lineWidth(3f)
                                        ));
                                        routeCoordinates.remove(0);
                                    });
                        }
                    }

                    //mapboxMap.setStyle();
                }

            }
//            }
            super.onSignalStrengthsChanged(signalStrength);
        }
    }

    private boolean canSetCameraPosition() {
        return lastLng != 0 && lastLat != 0 && mapboxMap != null;
    }

    private void setCameraPosition(LatLng location) {
        CameraPosition position = new CameraPosition.Builder()
                .target(location) // Sets the new camera position
                .zoom(13) // https://docs.mapbox.com/help/glossary/zoom-level/
                .bearing(0) // Rotate the camera
                .tilt(0) // Set the camera tilt
                .build();
        mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(position), ANIMATION_MS);
    }

}
