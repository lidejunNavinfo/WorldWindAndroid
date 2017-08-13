/*
 * Copyright (c) 2016 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration. All Rights Reserved.
 */

package gov.nasa.worldwindx;

import android.os.Bundle;
import android.widget.FrameLayout;

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
import gov.nasa.worldwind.shape.Placemark;
import gov.nasa.worldwind.shape.VisibilityMockup;
import gov.nasa.worldwindx.experimental.AtmosphereLayer;

/**
 * Creates a simple view of a globe with touch navigation and a few layers.
 */
public class BasicGlobeActivity extends AbstractMainActivity {

    /**
     * This protected member allows derived classes to override the resource used in setContentView.
     */
    protected int layoutResourceId = R.layout.activity_globe;

    /**
     * The WorldWindow (GLSurfaceView) maintained by this activity
     */
    protected WorldWindow wwd;

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
        this.wwd = new WorldWindow(this);

        // Add the WorldWindow view object to the layout that was reserved for the globe.
        FrameLayout globeLayout = (FrameLayout) findViewById(R.id.globe);
        globeLayout.addView(this.wwd);

        // Setup the World Window's layers.
        this.wwd.getLayers().addLayer(new BackgroundLayer());
        this.wwd.getLayers().addLayer(new BlueMarbleLandsatLayer());
        this.wwd.getLayers().addLayer(new AtmosphereLayer());

        // Setup the World Window's elevation coverages.
        this.wwd.getGlobe().getElevationModel().addCoverage(new BasicElevationCoverage());

        final RenderableLayer mockupLayer = new RenderableLayer();
        final VisibilityMockup mockupRenderable = new VisibilityMockup(new Position());
        final Placemark placemark = new Placemark(new Position());
        placemark.getAttributes().setImageScale(10);
        mockupLayer.addRenderable(mockupRenderable);
        mockupLayer.addRenderable(placemark);
        this.wwd.getLayers().addLayer(mockupLayer);

        this.wwd.addNavigatorListener(new NavigatorListener() {
            @Override
            public void onNavigatorEvent(WorldWindow wwd, NavigatorEvent event) {
                LookAt lookAt = event.getNavigator().getAsLookAt(wwd.getGlobe(), new LookAt());
                mockupRenderable.getPosition().set(lookAt.latitude, lookAt.longitude, 2500);
                placemark.getPosition().set(mockupRenderable.getPosition());
                wwd.requestRedraw();
            }
        });
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
}
