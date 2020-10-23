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
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.lte.mapmylte.maps.FloorPlanActivity;
import com.lte.mapmylte.maps.GpsLineLayerActivity;
import com.lte.mapmylte.util.LteLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static com.lte.mapmylte.maps.MapMode.FLOOR_OPTION;
import static com.lte.mapmylte.maps.MapMode.GPS_OPTION;
import static com.lte.mapmylte.maps.MapMode.NO_GPS_OPTION;

/*
 * Resources:
 * https://docs.mapbox.com/android/maps/examples/create-a-line-layer/
 * https://docs.mapbox.com/android/maps/examples/floor-plan/
 * https://docs.mapbox.com/android/maps/examples/click-to-add-image/
 */
public class NewRecordingActivity extends AppCompatActivity {

    public static final String OFFSET_KEY = "offset_key";
    public static final String MAP_MODE_KEY = "map_mode_key";
    public static final String ADVANCED_SENSORS_SKU = "advanced_sensors";

    private static final String TAG = NewRecordingActivity.class.getSimpleName();
    private static final int PERMISSION_REQUEST_ACCESS_COARSE_LOCATION = 1;
    private static final int PERMISSION_REQUEST_ACCESS_COARSE_LOCATION_START_ACTIVITY = 2;

    private EditText mOffsetUi;

    private Button gpsButton;
    private Button noGpsButton;
    private Button floorPlanButton;
    private Button offsetInfoButton;

    private String lastOptionSelected;

    private PurchasesUpdatedListener purchaseUpdateListener = (billingResult, purchases) -> {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                && purchases != null) {
            for (Purchase purchase : purchases) {
                handlePurchase(purchase);
            }
        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
            showToast("Purchase cancelled");
            // Handle an error caused by a user cancelling the purchase flow.
        } else {
            showToast("Error completing purchase, please try again later or contact us for support");
            // Handle any other error codes.
        }
        // To be implemented in a later section.
    };

    private void handlePurchase(Purchase purchase) {
        // Verify the purchase.
        // Ensure entitlement was not already granted for this purchaseToken.
        // Grant entitlement to the user.

        ConsumeParams consumeParams =
                ConsumeParams.newBuilder()
                        .setPurchaseToken(purchase.getPurchaseToken())
                        .build();

        ConsumeResponseListener listener = (billingResult, purchaseToken) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                // Handle the success of the consume operation.
                showToast("Purchase completed, token: " + purchaseToken);
            }
        };

        billingClient.consumeAsync(consumeParams, listener);

    }

    private BillingClient billingClient;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // TODO readd
//        MenuInflater inflater = getMenuInflater();
//        inflater.inflate(R.menu.mainmenu, menu);
        return true;
    }

    private void showToast(String msg) {
        Toast.makeText(NewRecordingActivity.this, msg, Toast.LENGTH_LONG)
                .show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // action with ID action_refresh was selected
            case R.id.action_upgrade:
                startBillingFlow();
                break;
            default:
                break;
        }

        return true;
    }

    private Map<String, SkuDetails> skuMap;

    private void startBillingFlow() {
        SkuDetails skuDetails = skuMap.get(ADVANCED_SENSORS_SKU);
        if (skuDetails == null) {
            return;
        }
        // Retrieve a value for "skuDetails" by calling querySkuDetailsAsync().
        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                .setSkuDetails(skuDetails)
                .build();
        int responseCode = billingClient.launchBillingFlow(this, billingFlowParams).getResponseCode();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_recording);
        setTitle(R.string.app_name);

        billingClient = BillingClient.newBuilder(this)
                .setListener(purchaseUpdateListener)
                .enablePendingPurchases()
                .build();

        skuMap = new HashMap<>();
        skuMap.put(ADVANCED_SENSORS_SKU, null);


        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult result) {
                if (result.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    // The BillingClient is ready. You can query purchases here.
                    SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
                    params.setSkusList(new ArrayList<>(skuMap.keySet())).setType(BillingClient.SkuType.INAPP);
                    billingClient.querySkuDetailsAsync(params.build(),
                            (billingResult, skuDetailsList) -> {
                                if (skuDetailsList != null) {
                                    for (SkuDetails skuDetails : skuDetailsList) {
                                        skuMap.put(skuDetails.getSku(), skuDetails);
                                    }
                                }
                            });
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
            }
        });


        gpsButton = findViewById(R.id.gps_map_button);
        noGpsButton = findViewById(R.id.no_gps_map_button);
        floorPlanButton = findViewById(R.id.floor_plan_map_button);
        offsetInfoButton = findViewById(R.id.offset_info_button);

        gpsButton.setOnClickListener(view -> newRecordingButtonClicked(GPS_OPTION));
        noGpsButton.setOnClickListener(view -> newRecordingButtonClicked(NO_GPS_OPTION));
        floorPlanButton.setOnClickListener(view -> newRecordingButtonClicked(FLOOR_OPTION));
        offsetInfoButton.setOnClickListener(view -> offsetInfoButtonClicked());

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

    public void offsetInfoButtonClicked() {
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle("Offset");
        alertDialog.setMessage(getString(R.string.offest_info_message));
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                (dialog, which) -> dialog.dismiss());
        alertDialog.show();
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
