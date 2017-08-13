/*
 * Copyright (c) 2017 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration. All Rights Reserved.
 */

package gov.nasa.worldwind.shape;

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.geom.Matrix4;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.globe.Globe;
import gov.nasa.worldwind.render.AbstractRenderable;
import gov.nasa.worldwind.render.BasicShaderProgram;
import gov.nasa.worldwind.render.MockupProgram;
import gov.nasa.worldwind.render.RenderContext;
import gov.nasa.worldwind.util.Logger;

public class VisibilityMockup extends AbstractRenderable implements Movable {

    protected Position position = new Position();

    protected int altitudeMode = WorldWind.ABSOLUTE;

    protected double heading;

    protected double fieldOfView = 90;

    protected double nearDistance = 2.0e2;

    protected double farDistance = 2.0e4;

    private Matrix4 modelview = new Matrix4();

    private Matrix4 projection = new Matrix4();

    public VisibilityMockup(Position position) {
        if (position == null) {
            throw new IllegalArgumentException(
                Logger.logMessage(Logger.ERROR, "VisibilityMockup", "constructor", "missingPosition"));
        }

        this.position.set(position);
    }

    public Position getPosition() {
        return position;
    }

    public VisibilityMockup setPosition(Position position) {
        if (position == null) {
            throw new IllegalArgumentException(
                Logger.logMessage(Logger.ERROR, "VisibilityMockup", "setPosition", "missingPosition"));
        }

        this.position.set(position);
        return this;
    }

    @WorldWind.AltitudeMode
    public int getAltitudeMode() {
        return altitudeMode;
    }

    public VisibilityMockup setAltitudeMode(@WorldWind.AltitudeMode int altitudeMode) {
        this.altitudeMode = altitudeMode;
        return this;
    }

    public double getHeading() {
        return heading;
    }

    public VisibilityMockup setHeading(double heading) {
        this.heading = heading;
        return this;
    }

    @Override
    public Position getReferencePosition() {
        return this.getPosition();
    }

    @Override
    public void moveTo(Globe globe, Position position) {
        this.setPosition(position);
    }

    @Override
    protected void doRender(RenderContext rc) {
        if (rc.terrain.getSector().isEmpty()) {
            return; // no terrain surface to render on
        }

        // TODO
        // Enqueue a drawable for this shape

        // TODO
        // Draw the terrain into the scratch framebuffer using this shape's modelview-projection matrix
        // Initially capture color and depth, then draw the framebuffer colors in a quad in the corner of the screen

        DrawableVisibilityMockup drawable = new DrawableVisibilityMockup();

        // TODO interpret altitude mode other than absolute
        // Compute a modelview matrix for this shape's position and orientation.
        rc.globe.geographicToCartesianTransform(this.position.latitude, this.position.longitude, this.position.altitude, this.modelview);
        this.modelview.multiplyByRotation(0, 0, 1, -this.heading); // rotate clockwise about the Z axis
        this.modelview.multiplyByRotation(1, 0, 0, 90); // rotate counter-clockwise about the X axis
        this.modelview.invertOrthonormal();

        // Compute a perspective projection matrix for a square viewport, with this shape's field of view, far distance.
        this.projection.setToPerspectiveProjection(1.0, 1.0, this.fieldOfView, this.nearDistance, this.farDistance);

        // Compute the combined modelview-projection matrix.
        drawable.viewerMvpMatrix.setToMultiply(this.projection, this.modelview);

        drawable.program = (BasicShaderProgram) rc.getShaderProgram(BasicShaderProgram.KEY);
        if (drawable.program == null) {
            drawable.program = (BasicShaderProgram) rc.putShaderProgram(BasicShaderProgram.KEY, new BasicShaderProgram(rc.resources));
        }

        drawable.mockupProgram = (MockupProgram) rc.getShaderProgram(MockupProgram.KEY);
        if (drawable.mockupProgram == null) {
            drawable.mockupProgram = (MockupProgram) rc.putShaderProgram(MockupProgram.KEY, new MockupProgram(rc.resources));
        }

        rc.offerDrawable(drawable, WorldWind.SCREEN_DRAWABLE, 0 /*zOrder*/);
    }
}
