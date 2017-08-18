/*
 * Copyright (c) 2017 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration. All Rights Reserved.
 */

package gov.nasa.worldwind.draw;

import android.opengl.GLES20;

import gov.nasa.worldwind.geom.Matrix4;
import gov.nasa.worldwind.geom.Vec3;
import gov.nasa.worldwind.render.Color;
import gov.nasa.worldwind.render.Framebuffer;
import gov.nasa.worldwind.render.SensorProgram;
import gov.nasa.worldwind.render.Texture;

public class DrawableOmnidirectionalSensor implements Drawable {

    public Matrix4 sensorTransform = new Matrix4();

    public double range;

    public Color visibleColor = new Color();

    public Color occludedColor = new Color();

    public SensorProgram program = null;

    private Matrix4 occludeProjection = new Matrix4();

    private Matrix4 occludeView = new Matrix4();

    private Matrix4 matrix = new Matrix4();

    private Matrix4[] cubeMapMatrix = {
        new Matrix4().setToRotation(0, 0, 1, -90).multiplyByRotation(1, 0, 0, 90), // positive X
        new Matrix4().setToRotation(0, 0, 1, 90).multiplyByRotation(1, 0, 0, 90), // negative X
        new Matrix4().setToRotation(1, 0, 0, 90), // positive Y
        new Matrix4().setToRotation(0, 0, 1, 180).multiplyByRotation(1, 0, 0, 90), // negative Y
        /*new Matrix4().setToRotation(1, 0, 0, 180),*/ // positive Z, intentionally omitted as terrain cannot be above the viewer
        new Matrix4() // negative Z
    };

    public DrawableOmnidirectionalSensor() {
    }

    @Override
    public void recycle() {

    }

    @Override
    public void draw(DrawContext dc) {
        if (this.program == null || !this.program.useProgram(dc)) {
            return; // program unspecified or failed to build
        }

        // Use the drawable's color.
        this.program.loadRange(this.range);
        this.program.loadColor(this.visibleColor, this.occludedColor);

        // Configure the depth projection matrix to capture a 90 degree portion of the sensor's range.
        this.occludeProjection.setToPerspectiveProjection(1, 1, 90, 1, this.range);

        for (int idx = 0, len = this.cubeMapMatrix.length; idx < len; idx++) {
            this.occludeView.set(this.sensorTransform);
            this.occludeView.multiplyByMatrix(this.cubeMapMatrix[idx]);
            this.occludeView.invertOrthonormal();

            // TODO accumulate only the visible terrain, which can be used in both passes
            // TODO give terrain a bounding box, test with a frustum set using depthviewProjection

            if (this.drawSceneDepth(dc)) {
                this.drawSceneOcclusion(dc);
            }
        }
    }

    protected boolean drawSceneDepth(DrawContext dc) {
        try {
            Framebuffer framebuffer = dc.scratchFramebuffer();
            if (!framebuffer.bindFramebuffer(dc)) {
                return false; // framebuffer failed to bind
            }

            // Clear the framebuffer.
            Texture depthTexture = framebuffer.getAttachedTexture(GLES20.GL_DEPTH_ATTACHMENT);
            GLES20.glViewport(0, 0, depthTexture.getWidth(), depthTexture.getHeight());
            GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT);

            // Draw only depth values offset slightly toward from the viewer.
            GLES20.glColorMask(false, false, false, false);
            GLES20.glEnable(GLES20.GL_POLYGON_OFFSET_FILL);
            GLES20.glPolygonOffset(4, 2);

            for (int idx = 0, len = dc.getDrawableTerrainCount(); idx < len; idx++) {
                // Get the drawable terrain associated with the draw context.
                DrawableTerrain terrain = dc.getDrawableTerrain(idx);
                Vec3 terrainOrigin = terrain.getVertexOrigin();

                // Use the terrain's vertex point attribute.
                if (!terrain.useVertexPointAttrib(dc, 0 /*vertexPoint*/)) {
                    continue; // vertex buffer failed to bind
                }

                // Use the depth texture's modelview projection matrix, transformed to terrain local coordinates.
                this.matrix.setToMultiply(this.occludeProjection, this.occludeView);
                this.matrix.multiplyByTranslation(terrainOrigin.x, terrainOrigin.y, terrainOrigin.z);
                this.program.loadModelviewProjection(this.matrix);

                // Draw the terrain as triangles.
                terrain.drawTriangles(dc);
            }
        } finally {
            // Restore the default World Wind OpenGL state.
            dc.bindFramebuffer(0);
            GLES20.glViewport(dc.viewport.x, dc.viewport.y, dc.viewport.width, dc.viewport.height);
            GLES20.glColorMask(true, true, true, true);
            GLES20.glDisable(GLES20.GL_POLYGON_OFFSET_FILL);
            GLES20.glPolygonOffset(0, 0);
        }

        return true;
    }

    protected void drawSceneOcclusion(DrawContext dc) {
        // Make multi-texture unit 0 active.
        dc.activeTextureUnit(GLES20.GL_TEXTURE0);

        Texture depthTexture = dc.scratchFramebuffer().getAttachedTexture(GLES20.GL_DEPTH_ATTACHMENT);
        if (!depthTexture.bindTexture(dc)) {
            return; // framebuffer texture failed to bind
        }

        for (int idx = 0, len = dc.getDrawableTerrainCount(); idx < len; idx++) {
            // Get the drawable terrain associated with the draw context.
            DrawableTerrain terrain = dc.getDrawableTerrain(idx);
            Vec3 terrainOrigin = terrain.getVertexOrigin();

            // Use the terrain's vertex point attribute.
            if (!terrain.useVertexPointAttrib(dc, 0 /*vertexPoint*/)) {
                continue; // vertex buffer failed to bind
            }

            // Use the draw context's modelview projection matrix, transformed to shape local coordinates.
            this.matrix.set(dc.modelviewProjection);
            this.matrix.multiplyByTranslation(terrainOrigin.x, terrainOrigin.y, terrainOrigin.z);
            this.program.loadModelviewProjection(this.matrix);

            // Use the depth texture's modelview projection matrix, transformed to shape local coordinates.
            this.matrix.set(this.occludeView);
            this.matrix.multiplyByTranslation(terrainOrigin.x, terrainOrigin.y, terrainOrigin.z);
            this.program.loadOccludeviewProjection(this.occludeProjection, this.matrix);

            // Draw the terrain as triangles.
            terrain.drawTriangles(dc);
        }
    }
}
