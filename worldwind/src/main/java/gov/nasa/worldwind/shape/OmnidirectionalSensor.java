/*
 * Copyright (c) 2017 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration. All Rights Reserved.
 */

package gov.nasa.worldwind.shape;

import gov.nasa.worldwind.draw.DrawableOmnidirectionalSensor;
import gov.nasa.worldwind.draw.DrawableOmnidirectionalSensorRange;
import gov.nasa.worldwind.geom.Matrix4;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Vec3;
import gov.nasa.worldwind.globe.Globe;
import gov.nasa.worldwind.render.BasicShaderProgram;
import gov.nasa.worldwind.render.Color;
import gov.nasa.worldwind.render.RenderContext;
import gov.nasa.worldwind.render.SensorProgram;
import gov.nasa.worldwind.util.Logger;

public class OmnidirectionalSensor extends AbstractShape implements Movable { // TODO sensor as

    protected Position position = new Position();

    protected double range;

    protected Vec3 point = new Vec3();

    protected Matrix4 transform = new Matrix4();

    public OmnidirectionalSensor(Position position, double range) {
        if (position == null) {
            throw new IllegalArgumentException(
                Logger.logMessage(Logger.ERROR, "OmnidirectionalSensor", "constructor", "missingPosition"));
        }

        if (range < 0) {
            throw new IllegalArgumentException(
                Logger.logMessage(Logger.ERROR, "OmnidirectionalSensor", "constructor", "invalidRange"));
        }

        this.position.set(position);
        this.range = range;
    }

    public OmnidirectionalSensor(Position position, ShapeAttributes attributes) {
        super(attributes);

        if (position == null) {
            throw new IllegalArgumentException(
                Logger.logMessage(Logger.ERROR, "OmnidirectionalSensor", "constructor", "missingPosition"));
        }

        this.position = position;
    }

    public OmnidirectionalSensor(Position position, double range, ShapeAttributes attributes) {
        super(attributes);

        if (position == null) {
            throw new IllegalArgumentException(
                Logger.logMessage(Logger.ERROR, "OmnidirectionalSensor", "constructor", "missingPosition"));
        }

        if (range < 0) {
            throw new IllegalArgumentException(
                Logger.logMessage(Logger.ERROR, "OmnidirectionalSensor", "constructor", "invalidRange"));
        }

        this.position = position;
        this.range = range;
    }

    public Position getPosition() {
        return position;
    }

    public OmnidirectionalSensor setPosition(Position position) {
        if (position == null) {
            throw new IllegalArgumentException(
                Logger.logMessage(Logger.ERROR, "OmnidirectionalSensor", "setPosition", "missingPosition"));
        }

        this.position.set(position);
        return this;
    }

    public double getRange() {
        return range;
    }

    public OmnidirectionalSensor setRange(double range) {
        if (range < 0) {
            throw new IllegalArgumentException(
                Logger.logMessage(Logger.ERROR, "OmnidirectionalSensor", "setRange", "invalidRange"));
        }

        this.range = range;
        return this;
    }

    /**
     * A position associated with the object that indicates its aggregate geographic position. For an
     * OmnidirectionalSensor, this is simply it's position property.
     *
     * @return {@link Label#getPosition()}
     */
    @Override
    public Position getReferencePosition() {
        return this.getPosition();
    }

    /**
     * Moves the shape over the globe's surface. For an OmnidirectionalSensor, this simply calls {@link
     * OmnidirectionalSensor#setPosition(Position)}.
     *
     * @param globe    not used.
     * @param position the new position of the shape's reference position.
     */
    @Override
    public void moveTo(Globe globe, Position position) {
        this.setPosition(position);
    }

    @Override
    protected void reset() {
        // Required method for subclasses of AbstractShape. Intentionally left blank since OmnidirectionalSensor is stateless.
    }

    protected void makeDrawable(RenderContext rc) {
        // TODO elevation model fallback doesn't work here - relative to ground point is offset relative to ellipsoid and geometry is occluded
        // TODO frustum culling with a stateless bounding sphere
        // Compute this sensor's position in Cartesian coordinates.
        rc.geographicToCartesian(this.position.latitude, this.position.longitude, this.position.altitude, this.altitudeMode, this.point);
        rc.globe.cartesianToLocalTransform(this.point.x, this.point.y, this.point.z, this.transform);
        double cameraDistance = rc.cameraPoint.distanceTo(this.point);

        // Compute the transform from sensor geometry coordinates to Cartesian coordinates.
        DrawableOmnidirectionalSensor drawable = new DrawableOmnidirectionalSensor();
        drawable.sensorTransform.set(this.transform);
        drawable.range = this.range;
        drawable.visibleColor.set(rc.pickMode ? this.pickColor : this.activeAttributes.interiorColor);
        drawable.occludedColor.set(rc.pickMode ? this.pickColor : new Color(1, 0, 0, 0.5f)); // TODO occluded attributes
        drawable.program = (SensorProgram) rc.getShaderProgram(SensorProgram.KEY);
        if (drawable.program == null) {
            drawable.program = (SensorProgram) rc.putShaderProgram(SensorProgram.KEY, new SensorProgram(rc.resources));
        }
        rc.offerSurfaceDrawable(drawable, 0 /*z-order*/);
    }
}
