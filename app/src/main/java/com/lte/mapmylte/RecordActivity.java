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
package com.lte.mapmylte;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.SettingsClient;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lte.mapmylte.util.SignalGrade;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.snapshotter.MapSnapshotter;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.layers.Property;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import com.lte.mapmylte.util.LteLog;
import com.lte.mapmylte.util.PrefManager;

import static com.google.android.gms.location.LocationServices.getFusedLocationProviderClient;
import static com.mapbox.mapboxsdk.style.expressions.Expression.get;
import static com.mapbox.mapboxsdk.style.expressions.Expression.match;
import static com.mapbox.mapboxsdk.style.expressions.Expression.rgb;
import static com.mapbox.mapboxsdk.style.expressions.Expression.stop;
import static com.lte.mapmylte.maps.MapMode.FLOOR_OPTIONS;
import static com.lte.mapmylte.maps.MapMode.GPS_OPTION;
import static com.lte.mapmylte.maps.MapMode.SEEN_FLOOR_OPTION;
import static com.lte.mapmylte.util.GenericFileProvider.getExternalDataFile;
import static com.lte.mapmylte.maps.MapMode.getHumanReadableOption;

/*
 * Base activity for map-based signal strength recording.
 */
public abstract class RecordActivity extends AppCompatActivity implements LocationListener,
        OnMapReadyCallback {

    /*
     * Notifications from UsbService will be received here.
     */
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case UsbService.ACTION_USB_PERMISSION_GRANTED: // USB PERMISSION GRANTED
                    Toast.makeText(context, "USB Ready", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_PERMISSION_NOT_GRANTED: // USB PERMISSION NOT GRANTED
                    Toast.makeText(context, "USB Permission not granted", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_NO_USB: // NO USB CONNECTED
                    Toast.makeText(context, "No USB connected", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_DISCONNECTED: // USB DISCONNECTED
                    Toast.makeText(context, "USB disconnected", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_NOT_SUPPORTED: // USB NOT SUPPORTED
                    Toast.makeText(context, "USB device not supported", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };
    private UsbService usbService;
    private RecordActivity.MyHandler mHandler;
    private final ServiceConnection usbConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1) {

            usbService = ((UsbService.UsbBinder) arg1).getService();
            usbService.setHandler(mHandler);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            usbService = null;
        }
    };

    public static final String DATA_READINGS_KEY = "data_readings_key";
    public static final String SENSOR_READINGS_KEY = "sensor_readings_key";
    public static final String OFFSET_KEY = "offset_key";

    private static final String TAG = RecordActivity.class.getSimpleName();
    private static final Object MUTEX = new Object();
    private static final long SAMPLE_RATE = 2000; // ms
    private static final int ANIMATION_MS = 3000;
    private static final int MY_PERMISSIONS_REQUEST_FINE_LOCATION = 100;
    protected static String mapMode;

    protected MapView mapView;
    protected List<Point> routeCoordinates;
    protected ImageView cameraView;
    protected TextView findLocationBanner;

    protected MapboxMap mapboxMap;
    protected boolean setInitialPosition;
    public Style mapstyle;

    protected LocationManager locationManager;

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
    private TextView mSensorDataTxt;
    CollectionReference LTECollections;
    CollectionReference SensorCollections;

    int count = 0;

    public JSONObject rawFeature;

    private LocationRequest mLocationRequest;

    protected static final String ID_IMAGE_LAYER = "layer-id-%d";

    protected String getImageLayerId(int layerId) {
        return String.format(Locale.US, ID_IMAGE_LAYER, layerId);
    }

    // https://docs.mapbox.com/android/maps/examples/share-a-snapshot/
    protected MapSnapshotter mapSnapshotter;

    private long UPDATE_INTERVAL = 10 * 1000;  /* 10 secs */
    private long FASTEST_INTERVAL = 2000; /* 2 sec */

    private String provider;
    private PrefManager prefManager;

    protected int numFloors = 3;
    private int currentFloor = 0;

    FirebaseFirestore mFirestore;

    private void initFirestore() {
        mFirestore = FirebaseFirestore.getInstance();
        LTECollections = mFirestore.collection("LTECoordinates/" + Calendar.getInstance().getTime().toString() + "/LTE");
        SensorCollections= mFirestore.collection("SensorCoordinates/" + Calendar.getInstance().getTime().toString() + "/Carbon");
    }

    protected void setCurrentFloor(int i) {
        currentFloor = Math.max(0, i); // Currently 0 baseline
        final String title = String.format("%s (Floor %s)",
                getString(R.string.record_floor_plan),
                i == 0 ? "G" : i);
        setTitle(title);
    }

    protected int getCurrentFloor() {
        return currentFloor;
    }

    private boolean hasStartedSnapshotGeneration = false;

    @Override
    public void onMapReady(@NonNull MapboxMap mapboxMap) {
        this.mapboxMap = mapboxMap;
        LteLog.i("map_ready", mapboxMap.toString());

        cameraView.setVisibility(View.VISIBLE);
        cameraView.setOnClickListener(view -> {
            if (mapView == null) {
                makeToast("Map initializing...", Toast.LENGTH_SHORT);
                return;
            }

            if (!hasStartedSnapshotGeneration) {
                hasStartedSnapshotGeneration = true;
                makeToast("Saving snapshot", Toast.LENGTH_LONG);
                startSnapShot(
                        mapboxMap.getProjection().getVisibleRegion().latLngBounds,
                        mapView.getMeasuredHeight(),
                        mapView.getMeasuredWidth());
            }
        });

        this.mapboxMap.addOnMapLongClickListener(point -> {
            makeToast("Corrected location", Toast.LENGTH_SHORT);
            LteLog.i("location_corrected", point.toString());
            lastLat = point.getLatitude();
            lastLng = point.getLongitude();
            return false;
        });
    }

    private void startSnapShot(LatLngBounds latLngBounds, int height, int width) {
        mapboxMap.getStyle(style -> {
            if (mapSnapshotter == null) {
                // Initialize snapshotter with map dimensions and given bounds
                MapSnapshotter.Options options =
                        new MapSnapshotter.Options(width, height)
                                .withRegion(latLngBounds)
                                .withLogo(true)
                                .withStyle(style.getUri());

                mapSnapshotter = new MapSnapshotter(getApplicationContext(), options);
            } else {
                // Reuse pre-existing MapSnapshotter instance
                mapSnapshotter.setSize(width, height);
                mapSnapshotter.setRegion(latLngBounds);
                mapSnapshotter.setCameraPosition(mapboxMap.getCameraPosition());
            }

            mapboxMap.snapshot(snapshot -> {
                shareBitmap(snapshot);
                hasStartedSnapshotGeneration = false;
            });


        });
    }

    private void shareBitmap(Bitmap bitmap) {
        Uri bmpUri = getLocalBitmapUri(bitmap);
        Intent shareIntent = new Intent();
        shareIntent.putExtra(Intent.EXTRA_STREAM, bmpUri);
        shareIntent.setType("image/png");
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, "Share map image"));
    }

    private Uri getLocalBitmapUri(Bitmap bmp) {
        Uri bmpUri = null;
        File file = getExternalDataFile(getApplicationContext(), "map_my_lte" + System.currentTimeMillis() + ".png");
        FileOutputStream out;

        try {
            out = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.PNG, 90, out);
            try {
                out.close();
            } catch (IOException exception) {
                exception.printStackTrace();
            }
            bmpUri = FileProvider.getUriForFile(getApplicationContext(), getApplicationContext().getPackageName() + ".provider", file);
        } catch (FileNotFoundException exception) {
            exception.printStackTrace();
            makeToast("Error creating snapshot file", Toast.LENGTH_SHORT);
        }
        return bmpUri;
    }

    protected void showTutorialDialog(Context context, String title, String tutorial, String pref) {
//        if (!prefManager.getBoolPreference(pref)) { // To avoid showing multiple times (currently show every time)
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(tutorial)
                .setPositiveButton(this.getString(R.string.got_it), (dialog, which) -> {
                    // show the dialog for floor option every time.
                    if (SEEN_FLOOR_OPTION.equals(pref)) {
                        Toast.makeText(context, getString(R.string.start_floor_plan), Toast.LENGTH_LONG).show();
                    }
                    dialog.dismiss();
                });

        if (SEEN_FLOOR_OPTION.equals(pref)) {
            alertBuilder.setItems(FLOOR_OPTIONS, (dialogInterface, i) -> numFloors = i + 1);
        }
        alertBuilder.show();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.i("onStatusChanged", provider + " " + status);
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.d("Location", "disable");
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.d("Location", "enable");
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initFirestore();
        initRouteCoordinates();

        mHandler = new RecordActivity.MyHandler(this);

        prefManager = new PrefManager(this);
        provider = mapMode.equals(GPS_OPTION) ? LocationManager.GPS_PROVIDER : LocationManager.NETWORK_PROVIDER;
        setTitle(getHumanReadableOption(mapMode));

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
        mPauseRecordButton.setText(getString(R.string.start));

        findLocationBanner = findViewById(R.id.findingLocationBanner);
        cameraView = findViewById(R.id.camera_icon);
        mRecordingImage = findViewById(R.id.activity_record_record_image_ui);
        mRecordingImageLabel = findViewById(R.id.activity_record_record_image_label_ui);
        mRsrpText = findViewById(R.id.activity_record_lte_rsrp_text_ui);
        mRsrqText = findViewById(R.id.activity_record_lte_rsrq_text_ui);
        mPciText = findViewById(R.id.activity_record_lte_pci_text_ui);
        mDataPointsText = findViewById(R.id.activity_record_data_points_text_ui);
        mOffsetText = findViewById(R.id.activity_record_offset_text_ui);
        mSignalStrengthText = findViewById(R.id.activity_record_signal_strength_text_ui);
        mSensorDataTxt = findViewById(R.id.sensor_data);
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

        startLocationUpdates();
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

                                    dataReadingCopy.setFloor(getCurrentFloor());
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

                dataReadingCopy.setFloor(currentFloor);

                if (isRecording()) {
                    mDataReadings.add(dataReadingCopy);
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_FINE_LOCATION:
                makeToast("Permission Granted", Toast.LENGTH_SHORT);
                break;
        }
    }

    protected void makeToast(String message, int duration) {
        Toast.makeText(this, message, duration).show();
    }

    private void setupLocation() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                Toast.makeText(this, "Location permission not granted, please grant permission", Toast.LENGTH_LONG).show();
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_FINE_LOCATION);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }
        // Permission already granted
//        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0, this);
    }

    protected void initRouteCoordinates() {
        // Create a list to store our line coordinates.
        routeCoordinates = new ArrayList<>();

        // TODO: Support multiple levels
        // ex:
//        for (int i = 0; i < FLOOR_OPTIONS.length; i++) {
//            routeCoordinates.append(i, new ArrayList<>());
//        }

        // Sample data
//        routeCoordinates.add(Point.fromLngLat(-118.39439114221236, 33.397676454651766));
//        routeCoordinates.add(Point.fromLngLat(-118.39421054012902, 33.39769799454838));
//        routeCoordinates.add(Point.fromLngLat(-118.39408583869053, 33.39761901490136));
//        routeCoordinates.add(Point.fromLngLat(-118.39388373635917, 33.397328225582285));
//        routeCoordinates.add(Point.fromLngLat(-118.39372033447427, 33.39728514560042));
//        routeCoordinates.add(Point.fromLngLat(-118.3930882271826, 33.39756875508861));
//        routeCoordinates.add(Point.fromLngLat(-118.3928216241072, 33.39759029501192));

    }


    @Override
    public void onResume() {
        super.onResume();
        setFilters();  // Start listening notifications from UsbService
        startService(UsbService.class, usbConnection, null); // Start UsbService(if it was not started before) and Bind it
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mUsbReceiver);
        unbindService(usbConnection);
        LteLog.i("record", "on pause");
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
        for (int i = 0; i < mDataReadings.size(); i++) {
            LTECollections.add(mDataReadings.get(i));
        }
        for (int i = 0; i < mHandler.mDataReadings.size(); i++) {
            SensorCollections.add(mHandler.mDataReadings.get(i));
        }

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
        // Set resume or start depending on if initial data present.
        final String activeRecordingText = count > 0 ? getString(R.string.resume) : getString(R.string.start);
        mPauseRecordButton.setText(activeRecordingText);

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


    // Trigger new location updates at interval
    protected void startLocationUpdates() {

        // Create the location request to start receiving updates
        mLocationRequest = new LocationRequest();
        if (GPS_OPTION.equals(mapMode)) {
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        } else {
            mLocationRequest.setPriority(LocationRequest.PRIORITY_LOW_POWER);
        }
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);

        // Create LocationSettingsRequest object using location request
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        LocationSettingsRequest locationSettingsRequest = builder.build();

        // Check whether location settings are satisfied
        // https://developers.google.com/android/reference/com/google/android/gms/location/SettingsClient
        SettingsClient settingsClient = LocationServices.getSettingsClient(this);
        settingsClient.checkLocationSettings(locationSettingsRequest);

        // new Google API SDK v11 uses getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        getFusedLocationProviderClient(this).requestLocationUpdates(mLocationRequest, new LocationCallback() {
                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        // do work here
                        Location lastLocation = locationResult.getLastLocation();
                        onLocationChanged(lastLocation);
                    }
                },
                Looper.myLooper());
        LteLog.i("location", "started location updates");
    }

    @Override
    public void onLocationChanged(Location location) {
        lastLat = location.getLatitude();
        lastLng = location.getLongitude();

        LteLog.d("update loc", String.format(Locale.US, "%f, %f", lastLat, lastLng));
        //this.mapboxMap.getLocationComponent().forceLocationUpdate(location);
    }


    private Location getLastBestLocation() {
        /*
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
        return bestLocation;*/
        if (lastLat != 0 && lastLng != 0) {
            Location location = new Location("");
            location.setLatitude(lastLat);
            location.setLongitude(lastLng);
            mapboxMap.getLocationComponent().forceLocationUpdate(location);
            return location;
        } else return null;
    }



    /**
     * Initialize the Maps SDK's LocationComponent
     */
    @SuppressWarnings({"MissingPermission"})
    protected void enableLocationComponent(@NonNull Style loadedMapStyle) {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {

            // Get an instance of the component
            LocationComponent locationComponent = mapboxMap.getLocationComponent();

            // Set the LocationComponent activation options
            LocationComponentActivationOptions locationComponentActivationOptions =
                    LocationComponentActivationOptions.builder(this, loadedMapStyle)
                            .useDefaultLocationEngine(false)
                            .build();

            // Activate with the LocationComponentActivationOptions object
            locationComponent.activateLocationComponent(locationComponentActivationOptions);

            // Enable to make component visible
            locationComponent.setLocationComponentEnabled(true);

            // Set the component's camera mode
            locationComponent.setCameraMode(CameraMode.TRACKING);

            // Set the component's render mode
            locationComponent.setRenderMode(RenderMode.COMPASS);

            initLocationEngine();
        }
    }

    /**
     * Set up the LocationEngine and the parameters for querying the device's location
     */
    @SuppressLint("MissingPermission")
    private void initLocationEngine() {
        /*
        locationEngine = LocationEngineProvider.getBestLocationEngine(this);

        LocationEngineRequest request = new LocationEngineRequest.Builder(DEFAULT_INTERVAL_IN_MILLISECONDS)
                .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                .setMaxWaitTime(DEFAULT_MAX_WAIT_TIME).build();

        locationEngine.requestLocationUpdates(request, callback, getMainLooper());
        locationEngine.getLastLocation(callback);*/
    }

    private class SignalStrengthListener extends PhoneStateListener {

        private int lastSignalGradeChangedIndex = 0;
        private SignalGrade lastSignalGrade = SignalGrade.UNKNOWN;

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
                        //final Location lastKnownLocation = mapMode.equals(GPS_OPTION) ? getLastBestLocation() : locationManager.getLastKnownLocation(provider); // or getLastKnownLocation() for provider agnostic.
                        Location lastKnownLocation = getLastBestLocation();
                        if (lastKnownLocation == null) {
                            return;
                        }
                        lastLat = lastKnownLocation.getLatitude();
                        lastLng = lastKnownLocation.getLongitude();
                        lastAcc = lastKnownLocation.getAccuracy();
                        lastElevation = lastKnownLocation.getAltitude();

                        if (!setInitialPosition && canSetCameraPosition()) {
                            initializeUserLocationOnMap();
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
                    mCurrentReading.setFloor(getCurrentFloor());
                }

                LteLog.i(TAG, String.format(Locale.getDefault(), "rsrp: %d, rsrq: %d", rsrp, rsrq));

                if (mapboxMap != null) {
                    final List<Point> points = routeCoordinates;
                    points.add(Point.fromLngLat(lastLng, lastLat));
                    LteLog.i("points", points.size() + ", floor " + getCurrentFloor());
                    count++;
                    if (points.size() >= 2 && rawFeature != null && rawFeature.has("features")) {
                        Log.d("plot", rawFeature.toString());

                        final SignalGrade grade;
                        if (mDataReadings != null) {
                            if (rsrp >= -95) {
                                grade = SignalGrade.TOP;
                            } else if (rsrp >= -103) {
                                grade = SignalGrade.MIDDLE_LOW;
                            } else if (rsrp >= -110) {
                                grade = SignalGrade.MIDDLE;
                            } else {
                                grade = SignalGrade.LOW;
                            }
                        } else {
                            grade = SignalGrade.UNKNOWN;
                        }
                        mapboxMap.getStyle(style -> {
                                    LteLog.i("record_activity", "set style");
                                    // Retrieve GeoJSON from local file and add it to the map

                                    JSONObject tmp1 = new JSONObject();
                                    try {
                                        tmp1.put("type", "Feature");
                                        JSONObject color = new JSONObject();
                                        color.put("color", grade.getValue());
                                        tmp1.put("properties", color);

                                        JSONObject geometry = new JSONObject();
                                        JSONArray coordinates = new JSONArray();

                                        if (lastSignalGrade != grade) {
                                            lastSignalGradeChangedIndex = points.size() - 1;
                                        }

                                        for (int i = lastSignalGradeChangedIndex; i < points.size(); i++) {
                                            JSONArray coordinate = new JSONArray();
                                            coordinate.put(points.get(i).longitude());
                                            coordinate.put(points.get(i).latitude());
                                            coordinates.put(coordinate);
                                        }

                                        lastSignalGrade = grade;

                                        geometry.put("type", "LineString");
                                        geometry.put("coordinates", coordinates);
                                        tmp1.put("geometry", geometry);

                                        rawFeature.getJSONArray("features").put(tmp1);

                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }

                                    GeoJsonSource lines = style.getSourceAs("lines");
                                    // TODO: add logic for floor plan to separate by floor.
                                    if (lines == null) {
                                        style.addSource(new GeoJsonSource("lines", rawFeature.toString()));
                                        style.addLayer(new LineLayer("finalLines", "lines").withProperties(
                                                PropertyFactory.lineColor(
                                                        match(
                                                                get("color"), rgb(0, 0, 0),
                                                                stop(SignalGrade.TOP.getValue(), rgb(0, 255, 0)),
                                                                stop(SignalGrade.MIDDLE.getValue(), rgb(255, 255, 0)),
                                                                stop(SignalGrade.MIDDLE_LOW.getValue(), rgb(255, 165, 0)),
                                                                stop(SignalGrade.LOW.getValue(), rgb(255, 0, 0))
                                                        )),
                                                PropertyFactory.visibility(Property.VISIBLE),
                                                PropertyFactory.lineWidth(3f)
                                        ));
//                                        routeCoordinates.remove(0);
                                    } else {
                                        LteLog.i("record_activity", "update lines " + rawFeature.length());
                                        lines.setGeoJson(rawFeature.toString());
                                    }

                                    enableLocationComponent(style);
                                });
                    }
                }
            }

            super.onSignalStrengthsChanged(signalStrength);
        }
    }

    private boolean canSetCameraPosition() {
        return lastLng != 0 && lastLat != 0 && mapboxMap != null;
    }

    private void setCameraPosition(LatLng location) {
        CameraPosition position = new CameraPosition.Builder()
                .target(location) // Sets the new camera position
                .zoom(14) // https://docs.mapbox.com/help/glossary/zoom-level/
                .bearing(0) // Rotate the camera
                .tilt(0) // Set the camera tilt
                .build();
        mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(position), ANIMATION_MS);
    }

    private void initializeUserLocationOnMap() {
        if (View.VISIBLE == findLocationBanner.getVisibility()) {
            findLocationBanner.setVisibility(View.GONE);
        }
        Location tmp = new Location("");
        tmp.setLatitude(lastLat);
        tmp.setLongitude(lastLng);
        try {
            mapboxMap.getLocationComponent().forceLocationUpdate(tmp);
        } catch (Exception e) {
            LteLog.e("error forcing location update", e.getMessage(), e);
        }
    }


    private void startService(Class<?> service, ServiceConnection serviceConnection, Bundle extras) {
        if (!UsbService.SERVICE_CONNECTED) {
            Intent startService = new Intent(this, service);
            if (extras != null && !extras.isEmpty()) {
                Set<String> keys = extras.keySet();
                for (String key : keys) {
                    String extra = extras.getString(key);
                    startService.putExtra(key, extra);
                }
            }
            startService(startService);
        }
        Intent bindingIntent = new Intent(this, service);
        bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void setFilters() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbService.ACTION_USB_PERMISSION_GRANTED);
        filter.addAction(UsbService.ACTION_NO_USB);
        filter.addAction(UsbService.ACTION_USB_DISCONNECTED);
        filter.addAction(UsbService.ACTION_USB_NOT_SUPPORTED);
        filter.addAction(UsbService.ACTION_USB_PERMISSION_NOT_GRANTED);
        registerReceiver(mUsbReceiver, filter);
    }

    /*
     * This handler will be passed to UsbService. Data received from serial port is displayed through this handler
     */
    private static class MyHandler extends Handler {
        private final WeakReference<RecordActivity> mActivity;
        private final RecordActivity mActivityValue;
        public String partial = "";
        public String mUSBData = "";
        public List<DataReading> mDataReadings = new ArrayList<DataReading>();

        public MyHandler(RecordActivity activity) {
            mActivity = new WeakReference<>(activity);
            mActivityValue = activity;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UsbService.MESSAGE_FROM_SERIAL_PORT:
                    String data = (String) msg.obj;

                    if(data.contains("\n"))
                    {
                        partial += data.replace("\n", "");
                        mUSBData = partial + " ppm";
                        mActivityValue.mSensorDataTxt.setText(mUSBData);
                        partial = "";

                    }
                    else
                    {
                        partial += data;
                        return;
                    }

                    if(mUSBData != data) {
                        mUSBData = data;
                        if(tryParseDouble(data))
                        {
                            if(mActivityValue.lastLat != 0 && mActivityValue.lastLng != 0) {
                                DataReading mCurrentReading = new DataReading();
                                mCurrentReading.setLat(mActivityValue.lastLat);
                                mCurrentReading.setLng(mActivityValue.lastLng);
                                mCurrentReading.setAcc(mActivityValue.lastAcc);
                                mCurrentReading.setElevation(mActivityValue.lastElevation);
                                mCurrentReading.setSensorvalue(Double.valueOf(data));
                                mCurrentReading.setUsesensor(true);
                                mDataReadings.add(mCurrentReading);
                            }
                        }
                    }
                    break;
                case UsbService.CTS_CHANGE:
                    Toast.makeText(mActivity.get(), "CTS_CHANGE",Toast.LENGTH_LONG).show();
                    break;
                case UsbService.DSR_CHANGE:
                    Toast.makeText(mActivity.get(), "DSR_CHANGE",Toast.LENGTH_LONG).show();
                    break;
            }
        }

        boolean tryParseDouble(String value) {
            try {
                Double.parseDouble(value);
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
    }
}