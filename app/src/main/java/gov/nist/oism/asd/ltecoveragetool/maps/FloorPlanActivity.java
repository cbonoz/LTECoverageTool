package gov.nist.oism.asd.ltecoveragetool.maps;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngQuad;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.layers.CircleLayer;
import com.mapbox.mapboxsdk.style.layers.RasterLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.mapboxsdk.style.sources.ImageSource;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gov.nist.oism.asd.ltecoveragetool.NewRecordingActivity;
import gov.nist.oism.asd.ltecoveragetool.R;
import gov.nist.oism.asd.ltecoveragetool.RecordActivity;
import gov.nist.oism.asd.ltecoveragetool.util.LteLog;

import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.circleColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.circleRadius;
import static gov.nist.oism.asd.ltecoveragetool.maps.MapMode.SEEN_FLOOR_OPTION;

/**
 * Tap the map in four locations to set the bounds for an image that is selected from the device's gallery
 * and then added to the map.
 * Used for mode 3.
 */
public class FloorPlanActivity extends RecordActivity implements
        OnMapReadyCallback, MapboxMap.OnMapClickListener {


    private View levelButtons;

    private static final String ID_IMAGE_SOURCE = "source-id";
    private static final String CIRCLE_SOURCE_ID = "circle-source-id";
    private static final String ID_IMAGE_LAYER = "layer-id";
    private static int PHOTO_PICK_CODE = 4;
    private MapView mapView;
    private LatLngQuad quad;
    private List<Feature> boundsFeatureList;
    private List<Point> boundsCirclePointList;
    private int imageCountIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // These items should be set up before the parent onCreate is called.
        setContentView(R.layout.activity_record_mapping);
        mapMode = getIntent().getStringExtra(NewRecordingActivity.MAP_MODE_KEY);

        super.onCreate(savedInstanceState);

        showTutorialDialog(this, getString(R.string.record_floor_plan), getString(R.string.floor_plan_tutorial), SEEN_FLOOR_OPTION);

        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
    }


    @Override
    public void onMapReady(@NonNull final MapboxMap mapboxMap) {
        levelButtons = findViewById(R.id.floor_button_layout);
        this.mapboxMap = mapboxMap;


        this.mapboxMap.setStyle(Style.MAPBOX_STREETS, style -> {
            boundsFeatureList = new ArrayList<>();
            boundsCirclePointList = new ArrayList<>();
            mapboxMap.addOnMapClickListener(FloorPlanActivity.this);
            imageCountIndex = 0;
            initCircleSource(style);
            initCircleLayer(style);

        });

        initButtons();
    }

    private void initButtons() {
        int[] buttons = {R.id.ground_level_button, R.id.first_level_button, R.id.second_level_button};
        for (int i = 0; i < numFloors; i++) {
            Button button = findViewById(buttons[i]);
            button.setVisibility(View.VISIBLE); // show button.
            final int floor = i + 1;
            button.setOnClickListener(view -> {
                Toast.makeText(getApplicationContext(), "Floor " + button.getText(), Toast.LENGTH_SHORT).show();
                renderFloorImages(getCurrentFloor(), floor);
                setCurrentFloor(floor);
            });
        }

        setCurrentFloor(1);
    }

    private void renderFloorImages(int oldFloor, int newFloor) {
        List<SourceLayer> oldSourceLayers = floorImageMap.get(oldFloor, new ArrayList<>());
        for (SourceLayer layer : oldSourceLayers) {
            mapboxMap.getStyle(style -> {
                try {
                    style.removeLayer(layer.getRasterLayer());
                    style.removeSource(layer.getImageSource());
                } catch (Exception e) {
                    LteLog.e("render", "error removing layer" ,e);
                }
            });
        }

        List<SourceLayer> newSourceLayers = floorImageMap.get(newFloor, new ArrayList<>());
        for (SourceLayer layer : newSourceLayers) {
            mapboxMap.getStyle(style -> {
                try {
                style.addLayer(layer.getRasterLayer());
                style.addSource(layer.getImageSource());
                } catch (Exception e) {
                    LteLog.e("render", "error adding layer" ,e);
                }
            });
        }

    }

    @Override
    public boolean onMapClick(@NonNull LatLng point) {

        // Reset the lists once enough LatLngQuad points have been tapped
        if (boundsFeatureList.size() == 4) {
            boundsFeatureList = new ArrayList<>();
            boundsCirclePointList = new ArrayList<>();
        }

        boundsFeatureList.add(Feature.fromGeometry(Point.fromLngLat(point.getLongitude(), point.getLatitude())));

        // Add the click point to the CircleLayer and update the display of the CircleLayer data
        boundsCirclePointList.add(Point.fromLngLat(point.getLongitude(), point.getLatitude()));

        Style style = mapboxMap.getStyle();
        if (style != null) {
            GeoJsonSource circleSource = style.getSourceAs(CIRCLE_SOURCE_ID);
            if (circleSource != null) {
                circleSource.setGeoJson(FeatureCollection.fromFeatures(boundsFeatureList));
            }
        }

        // Once the 4 LatLngQuad points have been set for where the image will placed...
        if (boundsCirclePointList.size() == 4) {

            // Create the LatLng objects to use in the LatLngQuad
            LatLng latLng1 = new LatLng(boundsCirclePointList.get(0).latitude(),
                    boundsCirclePointList.get(0).longitude());
            LatLng latLng2 = new LatLng(boundsCirclePointList.get(1).latitude(),
                    boundsCirclePointList.get(1).longitude());
            LatLng latLng3 = new LatLng(boundsCirclePointList.get(2).latitude(),
                    boundsCirclePointList.get(2).longitude());
            LatLng latLng4 = new LatLng(boundsCirclePointList.get(3).latitude(),
                    boundsCirclePointList.get(3).longitude());
            quad = new LatLngQuad(latLng1, latLng2, latLng3, latLng4);

            // Launch the intent to open the device's image gallery picker
            Intent pickPhotoIntent = new Intent(Intent.ACTION_PICK,
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pickPhotoIntent.setType("image/*");
            startActivityForResult(pickPhotoIntent, PHOTO_PICK_CODE);
        }
        return true;
    }

    /**
     * Set up the CircleLayer source for showing LatLngQuad map click points
     */
    private void initCircleSource(@NonNull Style loadedMapStyle) {
        loadedMapStyle.addSource(
                new GeoJsonSource(CIRCLE_SOURCE_ID, FeatureCollection.fromFeatures(new Feature[]{}))
        );
    }

    /**
     * Set up the CircleLayer for showing LatLngQuad map click points
     */
    private void initCircleLayer(@NonNull Style loadedMapStyle) {
        loadedMapStyle.addLayer(new CircleLayer("circle-layer-bounds-corner-id",
                CIRCLE_SOURCE_ID).withProperties(
                circleRadius(8f),
                circleColor(Color.parseColor("#d004d3"))
        ));
    }

    protected final SparseArray<List<SourceLayer>> floorImageMap = new SparseArray<>();

    /**
     * Calling onActivityResult() to handle the return to the example from the device's image galleyr picker
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PHOTO_PICK_CODE && resultCode == Activity.RESULT_OK) {
            if (data == null) {
                //Display an error
                Toast.makeText(this, "Error receiving image data", Toast.LENGTH_LONG).show();
                return;
            }

            if (mapboxMap != null) {
                mapboxMap.getStyle(style -> {
                    Uri selectedImage = data.getData();
                    InputStream imageStream;
                    try {
                        if (selectedImage != null) {
                            imageStream = getContentResolver().openInputStream(selectedImage);

                            Bitmap bitmapOfSelectedImage = BitmapFactory.decodeStream(imageStream);

                            // Add the imageSource to the map
                            ImageSource source = new ImageSource(ID_IMAGE_SOURCE + imageCountIndex, quad, bitmapOfSelectedImage);
                            style.addSource(source);

                            // Create a raster layer and use the imageSource's ID as the layer's data// Add the layer to the map
                            RasterLayer layer = new RasterLayer(ID_IMAGE_LAYER + imageCountIndex,
                                    ID_IMAGE_SOURCE + imageCountIndex);
                            style.addLayer(layer);

                            // Append the source layer to this floor.
                            List<SourceLayer> sourceLayers = floorImageMap.get(getCurrentFloor(), new ArrayList<>());
                            sourceLayers.add(new SourceLayer(source, layer));
                            floorImageMap.put(getCurrentFloor(), sourceLayers);

                            // Reset lists in preparation for adding more images
                            boundsFeatureList = new ArrayList<>();
                            boundsCirclePointList = new ArrayList<>();

                            imageCountIndex++;

                            // Clear circles from CircleLayer
                            GeoJsonSource circleSource = style.getSourceAs(CIRCLE_SOURCE_ID);
                            if (circleSource != null) {
                                circleSource.setGeoJson(FeatureCollection.fromFeatures(boundsFeatureList));
                            }
                        }
                    } catch (FileNotFoundException exception) {
                        exception.printStackTrace();
                    }
                });
            }

        }
    }

    // Add the mapView lifecycle to the activity's lifecycle methods
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
        if (mapboxMap != null) {
            mapboxMap.removeOnMapClickListener(this);
        }
        mapView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }


    private void hideLevelButton() {
// When the user moves away from our bounding box region or zooms out far enough the floor level
// buttons are faded out and hidden.
        AlphaAnimation animation = new AlphaAnimation(1.0f, 0.0f);
        animation.setDuration(500);
        levelButtons.startAnimation(animation);
        levelButtons.setVisibility(View.GONE);
    }

    private void showLevelButton() {
// When the user moves inside our bounding box region or zooms in to a high enough zoom level,
// the floor level buttons are faded out and hidden.
        AlphaAnimation animation = new AlphaAnimation(0.0f, 1.0f);
        animation.setDuration(500);
        levelButtons.startAnimation(animation);
        levelButtons.setVisibility(View.VISIBLE);
    }

}