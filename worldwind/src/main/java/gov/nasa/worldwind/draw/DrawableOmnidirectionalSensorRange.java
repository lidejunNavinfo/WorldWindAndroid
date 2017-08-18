/*
 * Copyright (c) 2017 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration. All Rights Reserved.
 */

package gov.nasa.worldwind.draw;

import android.opengl.GLES20;

import gov.nasa.worldwind.geom.Matrix4;
import gov.nasa.worldwind.render.BasicShaderProgram;
import gov.nasa.worldwind.render.BufferObject;
import gov.nasa.worldwind.render.Color;

public class DrawableOmnidirectionalSensorRange implements Drawable {

    public Matrix4 sensorTransform = new Matrix4();

    public double range;

    public Color visibleColor = new Color();

    public Color occludedColor = new Color();

    public BasicShaderProgram program = null;

    private Matrix4 matrix = new Matrix4();

    public DrawableOmnidirectionalSensorRange() {
    }

    @Override
    public void recycle() {

    }

    @Override
    public void draw(DrawContext dc) {
        if (this.program == null || !this.program.useProgram(dc)) {
            return; // program unspecified or failed to build
        }

        this.program.enablePickMode(false);
        this.program.enableTexture(false);

        // Use the drawable's color.
        this.program.loadColor(new Color(1, 1, 1, 0.5f));

        BufferObject vertexBuffer = dc.unitSphereBuffer();
        if (!vertexBuffer.bindBuffer(dc)) {
            return; // vertex buffer failed to bind
        }

        BufferObject elementBuffer = dc.unitSphereElements();
        if (!elementBuffer.bindBuffer(dc)) {
            return; // element buffer failed to bind
        }

        // Use the draw context's modelview projection matrix, transformed to shape local coordinates.
        this.matrix.set(dc.modelviewProjection);
        this.matrix.multiplyByMatrix(this.sensorTransform);
        this.matrix.multiplyByScale(this.range, this.range, this.range);
        this.program.loadModelviewProjection(this.matrix);

        GLES20.glDepthFunc(GLES20.GL_GREATER);

        // Draw the sensor geometry.
        GLES20.glVertexAttribPointer(0 /*vertexPoint*/, 3, GLES20.GL_FLOAT, false, 0 /*stride*/, 0 /*offset*/);
        GLES20.glDrawElements(GLES20.GL_TRIANGLE_STRIP, elementBuffer.getBufferLength(), GLES20.GL_UNSIGNED_SHORT, 0);

        GLES20.glDepthFunc(GLES20.GL_LEQUAL);
    }
}
