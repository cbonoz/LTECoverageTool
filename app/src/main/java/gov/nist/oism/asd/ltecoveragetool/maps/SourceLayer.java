package gov.nist.oism.asd.ltecoveragetool.maps;

import com.mapbox.mapboxsdk.style.layers.RasterLayer;
import com.mapbox.mapboxsdk.style.sources.ImageSource;

public class SourceLayer {

    private final ImageSource imageSource;

    public ImageSource getImageSource() {
        return imageSource;
    }

    public RasterLayer getRasterLayer() {
        return rasterLayer;
    }

    private final RasterLayer rasterLayer;

    public SourceLayer(ImageSource imageSource, RasterLayer rasterLayer) {
        this.imageSource = imageSource;
        this.rasterLayer = rasterLayer;
    }
}
