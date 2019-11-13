package gov.nist.oism.asd.ltecoveragetool.maps;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.LineString;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.layers.Property;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import java.util.ArrayList;

import gov.nist.oism.asd.ltecoveragetool.R;
import gov.nist.oism.asd.ltecoveragetool.RecordActivity;
import gov.nist.oism.asd.ltecoveragetool.util.LteLog;

import static gov.nist.oism.asd.ltecoveragetool.NewRecordingActivity.GPS_OPTION;

/**
 * Add a GeoJSON line to a map.
 */
public class GpsLineLayerActivity extends RecordActivity implements LocationListener {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Mapbox.getInstance(this, getString(R.string.testing_token));
        setContentView(R.layout.activity_style_line_layer);
        setupLocation();
        super.onCreate(savedInstanceState);
        RECORD_TYPE = GPS_OPTION;

        // This contains the MapView in XML and needs to be called after the access token is configured.
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(mapboxMap -> mapboxMap.setStyle(Style.OUTDOORS, style -> {
            this.mapboxMap = mapboxMap;

            initRouteCoordinates();

            // Create the LineString from the list of coordinates and then make a GeoJSON
            // FeatureCollection so we can add the line to our map as a layer.
            style.addSource(new GeoJsonSource("line-source",
                    FeatureCollection.fromFeatures(new Feature[] {Feature.fromGeometry(
                            LineString.fromLngLats(routeCoordinates)
                    )})));

            // The layer properties for our line. This is where we make the line dotted, set the
            // color, etc.
            style.addLayer(new LineLayer("linelayer", "line-source").withProperties(
                    PropertyFactory.lineDasharray(new Float[] {0.01f, 2f}),
                    PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                    PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                    PropertyFactory.lineWidth(5f),
                    PropertyFactory.lineColor(Color.parseColor("#e55e5e"))
            ));
        }));

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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);


    }

    private void initRouteCoordinates() {
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
    public void onLocationChanged(Location location) {
        lastLat = location.getLatitude();
        lastLng = location.getLongitude();
        LteLog.d("loc", String.format("%d, %d", lastLat, lastLng));
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.d("Latitude","disable");
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.d("Latitude","enable");
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.d("Latitude","status");
    }
}