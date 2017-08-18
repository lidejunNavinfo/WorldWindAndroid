/*
 * Copyright (c) 2016 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration. All Rights Reserved.
 */

package gov.nasa.worldwindx;

import android.opengl.GLES20;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.FrameLayout;

import java.util.Arrays;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import gov.nasa.worldwind.NavigatorEvent;
import gov.nasa.worldwind.NavigatorListener;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.geom.LookAt;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.globe.BasicElevationCoverage;
import gov.nasa.worldwind.layer.BackgroundLayer;
import gov.nasa.worldwind.layer.BlueMarbleLandsatLayer;
import gov.nasa.worldwind.layer.RenderableLayer;
import gov.nasa.worldwind.render.Color;
import gov.nasa.worldwind.render.ImageSource;
import gov.nasa.worldwind.shape.OmnidirectionalSensor;
import gov.nasa.worldwind.shape.Placemark;
import gov.nasa.worldwindx.experimental.AtmosphereLayer;

/**
 * Creates a simple view of a globe with touch navigation and a few layers.
 */
public class BasicGlobeActivity extends AbstractMainActivity implements NavigatorListener {

    /**
     * This protected member allows derived classes to override the resource used in setContentView.
     */
    protected int layoutResourceId = R.layout.activity_globe;

    /**
     * The WorldWindow (GLSurfaceView) maintained by this activity
     */
    protected WorldWindow wwd;

    protected OmnidirectionalSensor sensor;

    protected Placemark placemark;

    protected LookAt lastLookAt;

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

        // Add the WorldWindow view object to the layout that was reserved for the globe.
        FrameLayout globeLayout = (FrameLayout) findViewById(R.id.globe);
        globeLayout.addView(this.wwd);

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

    @Override
    public WorldWindow getWorldWindow() {
        return this.wwd;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_set) {
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
