package com.lte.mapmylte.maps;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;

import com.lte.mapmylte.NewRecordingActivity;
import com.lte.mapmylte.R;
import com.lte.mapmylte.RecordActivity;
import com.lte.mapmylte.util.LteLog;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.LineString;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.layers.Property;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import static com.lte.mapmylte.maps.MapMode.GPS_OPTION;
import static com.lte.mapmylte.maps.MapMode.SEEN_GPS_OPTION;
import static com.lte.mapmylte.maps.MapMode.SEEN_NO_GPS_OPTION;

/**
 * Add a GeoJSON line to a map.
 * Used for both modes 1 and 2.
 */
public class GpsLineLayerActivity extends RecordActivity implements OnMapReadyCallback {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // These items should be set up before the parent onCreate is called.
        setContentView(R.layout.activity_record_mapping);
        findViewById(R.id.floor_button_layout).setVisibility(View.GONE); // hide floor buttons.
        mapMode = getIntent().getStringExtra(NewRecordingActivity.MAP_MODE_KEY);
        LteLog.d("selected_mapmode", mapMode);

        super.onCreate(savedInstanceState);

        if (GPS_OPTION.equals(mapMode)) {
            showTutorialDialog(this, getString(R.string.record_gps_tutorial), getString(R.string.gps_tutorial), SEEN_GPS_OPTION);
        } else {
            showTutorialDialog(this, getString(R.string.record_no_gps_tutorial), getString(R.string.no_gps_tutorial), SEEN_NO_GPS_OPTION);
        }


        // This contains the MapView in XML and needs to be called after the access token is configured.
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onMapReady(@NonNull final MapboxMap mapboxMap) {
        super.onMapReady(mapboxMap);


        /*
        this.mapboxMap -> mapboxMap.setStyle(Style.OUTDOORS, style -> {
            this.mapboxMap = mapboxMap;

            initRouteCoordinates();

            // Create the LineString from the list of coordinates and then make a GeoJSON
            // FeatureCollection so we can add the line to our map as a layer.

        })*/

        mapboxMap.setStyle(Style.OUTDOORS,
                style -> {
                    mapstyle = style;

                    style.addSource(new GeoJsonSource("line-source",
                            FeatureCollection.fromFeatures(new Feature[]{Feature.fromGeometry(
                                    LineString.fromLngLats(routeCoordinates)
                            )})));


                    // The layer properties for our line. This is where we make the line dotted, set the
                    // color, etc.
                    style.addLayer(new LineLayer("high", "line-source").withProperties(
                            //PropertyFactory.lineDasharray(new Float[]{0.01f, 2f}),
                            PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                            PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                            PropertyFactory.lineWidth(5f),
                            PropertyFactory.lineColor(Color.parseColor("#00FF00"))
                    ));

                    style.addLayer(new LineLayer("mid", "line-source").withProperties(
                            //PropertyFactory.lineDasharray(new Float[]{0.01f, 2f}),
                            PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                            PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                            PropertyFactory.lineWidth(5f),
                            PropertyFactory.lineColor(Color.parseColor("#FFFF00"))
                    ));

                    style.addLayer(new LineLayer("low", "line-source").withProperties(
                            //PropertyFactory.lineDasharray(new Float[]{0.01f, 2f}),
                            PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                            PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                            PropertyFactory.lineWidth(5f),
                            PropertyFactory.lineColor(Color.parseColor("#FF0000"))
                    ));
                    enableLocationComponent(style);
                });

    }

}