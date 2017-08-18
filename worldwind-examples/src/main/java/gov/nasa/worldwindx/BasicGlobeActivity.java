/*
 * Copyright (c) 2016 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration. All Rights Reserved.
 */

package gov.nasa.worldwindx;

import android.net.Uri;
import android.opengl.GLES20;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ZoomControls;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import gov.nasa.worldwind.NavigatorEvent;
import gov.nasa.worldwind.NavigatorListener;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.geom.LookAt;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.globe.BasicElevationCoverage;
import gov.nasa.worldwind.globe.Globe;
import gov.nasa.worldwind.layer.BackgroundLayer;
import gov.nasa.worldwind.layer.BlueMarbleLandsatLayer;
import gov.nasa.worldwind.layer.Layer;
import gov.nasa.worldwind.layer.LayerFactory;
import gov.nasa.worldwind.layer.LayerList;
import gov.nasa.worldwind.layer.RenderableLayer;
import gov.nasa.worldwind.layer.ShowTessellationLayer;
import gov.nasa.worldwind.ogc.Wcs100ElevationCoverage;
import gov.nasa.worldwind.ogc.wms.WmsCapabilities;
import gov.nasa.worldwind.ogc.wms.WmsLayer;
import gov.nasa.worldwind.render.Color;
import gov.nasa.worldwind.render.ImageSource;
import gov.nasa.worldwind.shape.OmnidirectionalSensor;
import gov.nasa.worldwind.shape.Placemark;
import gov.nasa.worldwindx.experimental.AtmosphereLayer;
import gov.nasa.worldwindx.support.LayerManager;

/**
 * Creates a simple view of a globe with touch navigation and a few layers.
 */
public class BasicGlobeActivity extends AbstractMainActivity implements NavigatorListener {

    protected final String EMULATOR_WMS = "http://10.0.2.2:8080/geoserver/ows";             // WMS on emulator

    protected final String EMULATOR_GWC = "http://10.0.2.2:8080/geoserver/gwc/service/wms"; // GeoWebCache (GWC) on emulator

    protected final String DEVICE_WCS = "http://10.0.1.7:8080/geoserver/wcs";             // WCS on device

    protected final String DEVICE_WMS = "http://10.0.1.7:8080/geoserver/ows";             // WMS on device

    protected final String DEVICE_GWC = "http://10.0.1.7:8080/geoserver/gwc/service/wms"; // GWC on device

    protected final String APACHE_WMS = "http://192.168.1.219:8080/geoserver/ows";             // WMS on apache

    protected final String APACHE_GWC = "http://192.168.1.219:8080/geoserver/gwc/service/wms"; // GeoWebCache (GWC) on apache

    protected final String COBRA_WCS = "http://192.168.1.222:8080/geoserver/wcs";             // WMS on cobra

    protected final String COBRA_WMS = "http://192.168.1.222:8080/geoserver/ows";             // WMS on cobra

    protected final String COBRA_GWC = "http://192.168.1.222:8080/geoserver/gwc/service/wms"; // GeoWebCache (GWC) on emulator

    String WCS_SERVER_ADDRESS = COBRA_WCS;

    String WMS_SERVER_ADDRESS = COBRA_WMS;

    /**
     * This protected member allows derived classes to override the resource used in setContentView.
     */
    protected int layoutResourceId = R.layout.activity_globe;

    /**
     * The WorldWindow (GLSurfaceView) maintained by this activity
     */
    protected WorldWindow wwd;

    /**
     * The Layer Manager is responsible for rendering the layer manager menu.
     */
    protected LayerManager layerManager;

    protected OmnidirectionalSensor sensor;

    protected Placemark placemark;

    protected LookAt lastLookAt;

    /**
     * Creates and initializes the WorldWindow and adds it to the layout.
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Establish the activity content
        setContentView(this.layoutResourceId);
        setAboutBoxTitle("About the " + this.getResources().getText(R.string.title_basic_globe));
        setAboutBoxText("Demonstrates how to construct a WorldWindow with a few layers.\n" +
            "The globe uses the default navigation gestures: \n" +
            " - one-finger pan moves the camera,\n" +
            " - two-finger pinch-zoom adjusts the range to the look at position, \n" +
            " - two-finger rotate arcs the camera horizontally around the look at position,\n" +
            " - three-finger tilt arcs the camera vertically around the look at position.");

        // Create the World Window (a GLSurfaceView) which displays the globe.
        this.wwd = new WorldWindow(this) {
            @Override
            public void onSurfaceCreated(GL10 unused, EGLConfig config) {
                super.onSurfaceCreated(unused, config);

                String extensions = GLES20.glGetString(GLES20.GL_EXTENSIONS);
                String[] words = extensions.split("\\s");
                Arrays.sort(words);
                StringBuilder sb = new StringBuilder();
                for (String word : words) {
                    sb.append(word).append("\n");
                }
                setAboutBoxText(sb.toString());
            }
        };
        this.layerManager = new LayerManager(this, this.wwd);

        // Add the WorldWindow view object to the layout that was reserved for the globe.
        FrameLayout globeLayout = (FrameLayout) findViewById(R.id.globe);
        globeLayout.addView(this.wwd);

        initializeZoomControls();
        initializeTiltControls();

        // Setup the World Window's layers.
        this.wwd.getLayers().addLayer(new BackgroundLayer());
        this.wwd.getLayers().addLayer(new BlueMarbleLandsatLayer());
        this.wwd.getLayers().addLayer(new AtmosphereLayer());

        // Setup the World Window's elevation coverages.
        this.wwd.getGlobe().getElevationModel().addCoverage(new BasicElevationCoverage());

        Position pos = new Position(46.202, -122.190, 500);
        this.sensor = new OmnidirectionalSensor(pos, 10000);
        this.sensor.setAltitudeMode(WorldWind.RELATIVE_TO_GROUND);
        this.sensor.getAttributes().setInteriorColor(new Color(0, 1, 0, 0.5f));
        this.placemark = new Placemark(pos);
        this.placemark.setAltitudeMode(WorldWind.RELATIVE_TO_GROUND);
        this.placemark.getAttributes().setImageSource(ImageSource.fromResource(R.drawable.aircraft_fixwing));
        this.placemark.getAttributes().setImageScale(2);
        this.placemark.getAttributes().setDrawLeader(true);

        RenderableLayer mockupLayer = new RenderableLayer();
        mockupLayer.addRenderable(this.sensor);
        mockupLayer.addRenderable(this.placemark);
        this.wwd.getLayers().addLayer(mockupLayer);
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.wwd.onPause(); // pauses the rendering thread
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.wwd.onResume(); // resumes a paused rendering thread
    }

    /**
     * Gets the WorldWindow attached to this activity.
     *
     * @return The WorldWindow
     */
    @Override
    public WorldWindow getWorldWindow() {
        return this.wwd;
    }

    public LayerManager getLayerManager() {
        return this.layerManager;
    }

    /**
     * Adds the layers to the globe.
     */
    protected void initializeLayers() {

        // WCS Elevations
        this.wwd.getGlobe().getElevationModel().addCoverage(
            new Wcs100ElevationCoverage(
                sectorFromBBox(-180, -90, 180, 90),
                7, // num levels
                WCS_SERVER_ADDRESS,
                "gebco:gebco_2014"));
//            new Wcs100ElevationCoverage(
//                sectorFromBBox(-123.0005555556, 43.9994444441653, -117.99944444420979, 49.000555555555515),
//                13, // num levels
//                WCS_SERVER_ADDRESS,
//                "pnw:usgs_ned_10m"));

        // Default base layers
        getLayerManager().addLayer(new BackgroundLayer());
        getLayerManager().addLayer(new BlueMarbleLandsatLayer());
        //getLayerManager().addLayer(new AtmosphereLayer());
        getLayerManager().addLayer(new ShowTessellationLayer());
        new InitializeWmsLayersTask().execute();
    }

    private Sector sectorFromBBox(double minX, double minY, double maxX, double maxY) {
        return Sector.fromDegrees(minY, minX, maxY - minY, maxX - minX);
    }

    protected class InitializeWmsLayersTask extends AsyncTask<Void, String, Void> {

        protected LayerList layers = new LayerList();

        @Override
        protected Void doInBackground(Void... notUsed) {
            // TIP: 10.0.2.2 is used to access the host development machine from emulator

            // Build a WMS server GetCapabilties request
            String serverAddress = WMS_SERVER_ADDRESS;
            Uri serviceUri = Uri.parse(serverAddress).buildUpon()
                .appendQueryParameter("VERSION", "1.3.0")
                .appendQueryParameter("SERVICE", "WMS")
                .appendQueryParameter("REQUEST", "GetCapabilities")
                .build();

            // Connect and read capabilities document
            InputStream inputStream = null;
            try {
                URLConnection conn = new URL(serviceUri.toString()).openConnection();
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(30000);
                inputStream = new BufferedInputStream(conn.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }

            // Parse the capabilities
            List<WmsLayer> namedLayers = null;
            try {
                WmsCapabilities capabilities = WmsCapabilities.getCapabilities(inputStream);
                namedLayers = capabilities.getNamedLayers();
            } catch (Exception ex) {
                Log.e("gov.nasa.worldwind", "Exception attempting to get WMS capabilities from: " + serviceUri.toString());
                return null;
            }

            // Setup the factory that will create WMS layers from the capabilities
            LayerFactory layerFactory = new LayerFactory();
            LayerFactory.Callback callback = new LayerFactory.Callback() {
                @Override
                public void creationSucceeded(LayerFactory factory, Layer layer) {
                    getLayerManager().addLayerBeforeNamed(AtmosphereLayer.LAYER_NAME, layer);
                }

                @Override
                public void creationFailed(LayerFactory factory, Layer layer, Throwable ex) {
                    Log.e("gov.nasa.worldwind", "WMS layer creation failed: " + layer.toString(), ex);
                }
            };

            // Create all the WMS layers
            for (WmsLayer layerCaps : namedLayers) {
                // The callback will add the layer to the layer list.
                Layer layer = layerFactory.createFromWmsLayerCapabilities(layerCaps, callback);
                layer.setDisplayName(">  " + layerCaps.getTitle());
                layer.putUserProperty("BBOX", layerCaps.getGeographicBoundingBox()); // TODO: use for highlighting layers in view
                layer.putUserProperty("MAX_SCALE_DENOM", layerCaps.getMaxScaleDenominator()); // TODO: use for sorting the layers
                layer.putUserProperty("MIN_SCALE_DENOM", layerCaps.getMinScaleDenominator()); // TODO: use for sorting the layers
                layer.setEnabled(false);
            }

            return null; // Void object
        }

        /**
         * Updates the WorldWindow layer list on the UI Thread.
         */
        @Override
        protected void onPostExecute(Void notUsed) {
            super.onPostExecute(notUsed);
            getLayerManager().addAllLayers(this.layers);
        }
    }

    /**
     * Adds the zoom controls to the layout.
     */
    protected void initializeZoomControls() {
        ZoomControls zoomControls = (ZoomControls) findViewById(R.id.zoom_controls);
        zoomControls.setOnZoomInClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adjustZoom(-0.6);
            }
        });
        zoomControls.setOnZoomOutClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adjustZoom(0.6);
            }
        });
        zoomControls.setZoomSpeed(50);  // repeat every 50 ms after a long press
    }

    /**
     * Adds the zoom controls to the layout.
     */
    protected void initializeTiltControls() {
        Button tiltUp = (Button) findViewById(R.id.btn_tilt_up);
        tiltUp.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                adjustTilt(-1.0);
                return true;
            }
        });

        Button tiltDown = (Button) findViewById(R.id.btn_tilt_down);
        tiltDown.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                adjustTilt(1.0);
                return true;
            }
        });
    }

    /**
     * Synchronizes the state of the Zoom Controls menu item to the controls widget. This is called right before the
     * menu is shown, every time it is shown.
     *
     * @param menu The options menu as last shown or first initialized by onCreateOptionsMenu().
     *
     * @return You must return true for the menu to be displayed; if you return false it will not be shown.
     *
     * @see #onCreateOptionsMenu
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        ZoomControls zoomControls = (ZoomControls) findViewById(R.id.zoom_controls);
        MenuItem menuItem = menu.findItem(R.id.action_zoom_controls);
        menuItem.setChecked(zoomControls.isShown());

        return super.onPrepareOptionsMenu(menu);
    }

    /**
     * Zooms the WorldWindow by the given zoom step.
     *
     * @param amount Zoom step; negative values zoom in.
     */
    protected void adjustZoom(double amount) {
        Globe globe = this.wwd.getGlobe();
        LookAt lookAt = this.wwd.getNavigator().getAsLookAt(globe, new LookAt());
        double range = lookAt.range;
        double coeff = 0.05;
        double change = coeff * amount;
        double logRange = range != 0 ? Math.log(range) : 0;
        // Zoom changes are treated as logarithmic values. This accomplishes two things:
        // 1) Zooming is slow near the globe, and fast at great distances.
        // 2) Zooming in and then immediately zooming out returns the viewer to the same range value.
        lookAt.range = Math.exp(logRange + change);

        this.wwd.getNavigator().setAsLookAt(globe, lookAt);
        this.wwd.requestRedraw();
    }

    protected void adjustTilt(double amount) {
        Globe globe = this.wwd.getGlobe();
        LookAt lookAt = this.wwd.getNavigator().getAsLookAt(globe, new LookAt());
        lookAt.tilt = Math.max(0, Math.min(90, lookAt.tilt += amount));
        this.wwd.getNavigator().setAsLookAt(globe, lookAt);
        this.wwd.requestRedraw();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (item.getItemId() == R.id.action_zoom_controls) {
            // Toggle the selection
            item.setChecked(!item.isChecked());
            ZoomControls zoomControls = (ZoomControls) findViewById(R.id.zoom_controls);
            if (item.isChecked()) {
                zoomControls.show();
            } else {
                zoomControls.hide();
            }
            return true;
        } else if (item.getItemId() == R.id.action_layer_manager) {
            this.drawerLayout.openDrawer(GravityCompat.END); /*Opens the Right Drawer*/
            return true;
        } else if (id == R.id.action_set) {
            wwd.removeNavigatorListener(this);
            LookAt navLookAt = wwd.getNavigator().getAsLookAt(wwd.getGlobe(), new LookAt());
            Position pos = new Position();
            pos.latitude = navLookAt.latitude;
            pos.longitude = navLookAt.longitude;
            sensor.setPosition(pos);
            placemark.setPosition(pos);
            wwd.requestRedraw();
            return true;
        } else if (id == R.id.action_track) {
            if (lastLookAt == null) {
                wwd.addNavigatorListener(this);
                lastLookAt = wwd.getNavigator().getAsLookAt(wwd.getGlobe(), new LookAt());
            } else {
                wwd.removeNavigatorListener(this);
                lastLookAt = null;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onNavigatorEvent(WorldWindow wwd, NavigatorEvent event) {
        LookAt navLookAt = wwd.getNavigator().getAsLookAt(wwd.getGlobe(), new LookAt());

        Position pos = sensor.getPosition();
        pos.latitude += navLookAt.latitude - lastLookAt.latitude;
        pos.longitude += navLookAt.longitude - lastLookAt.longitude;
        sensor.setPosition(pos);
        placemark.setPosition(pos);

        pos.altitude += navLookAt.range - lastLookAt.range;
        sensor.setPosition(pos);
        placemark.setPosition(pos);
        //double range = sensor.getRange();
        //range *= navLookAt.range / lastLookAt.range;
        //sensor.setRange(range);

        lastLookAt.set(navLookAt);
        wwd.requestRedraw();
    }
}
