/*
 * Copyright (c) 2017 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration. All Rights Reserved.
 */

package gov.nasa.worldwind.shape;

import android.opengl.GLES20;

import gov.nasa.worldwind.draw.DrawContext;
import gov.nasa.worldwind.draw.Drawable;
import gov.nasa.worldwind.draw.DrawableTerrain;
import gov.nasa.worldwind.geom.Matrix3;
import gov.nasa.worldwind.geom.Matrix4;
import gov.nasa.worldwind.geom.Vec3;
import gov.nasa.worldwind.render.BasicShaderProgram;
import gov.nasa.worldwind.render.Color;
import gov.nasa.worldwind.render.Framebuffer;
import gov.nasa.worldwind.render.MockupProgram;
import gov.nasa.worldwind.render.Texture;

public class DrawableVisibilityMockup implements Drawable {

    public BasicShaderProgram program = null;

    public MockupProgram mockupProgram = null;

    public Matrix4 viewerMvpMatrix = new Matrix4();

    private Matrix4[] projMatrix = {new Matrix4(), new Matrix4()};

    private Matrix3 identityMatrix3 = new Matrix3();

    private Matrix4 unitSquareTransform = new Matrix4().multiplyByScale(512, 512, 1);

    public DrawableVisibilityMockup() {
    }

    @Override
    public void recycle() {

    }

    @Override
    public void draw(DrawContext dc) {
        if (this.program == null || !this.program.useProgram(dc)) {
            return; // program unspecified or failed to build
        }


        if (this.drawSceneToFramebuffer(dc)) {
            this.drawSceneVisibility(dc);
            this.drawFramebufferToScreen(dc);
        }
    }

    protected boolean drawSceneToFramebuffer(DrawContext dc) {
        try {
            if (this.program == null || !this.program.useProgram(dc)) {
                return false; // program unspecified or failed to build
            }

            Framebuffer framebuffer = dc.scratchFramebuffer();
            if (!framebuffer.bindFramebuffer(dc)) {
                return false; // framebuffer failed to bind
            }

            // Clear the framebuffer and disable rendering fragment colors.
            Texture depthAttachment = framebuffer.getAttachedTexture(GLES20.GL_DEPTH_ATTACHMENT);
            GLES20.glViewport(0, 0, depthAttachment.getWidth(), depthAttachment.getHeight());
            GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT);
            GLES20.glColorMask(false, false, false, false);

            // Disable texturing.
            this.program.enableTexture(false);

            for (int idx = 0, len = dc.getDrawableTerrainCount(); idx < len; idx++) {
                // Get the drawable terrain associated with the draw context.
                DrawableTerrain terrain = dc.getDrawableTerrain(idx);
                Vec3 terrainOrigin = terrain.getVertexOrigin();

                // Use the terrain's vertex point attribute.
                if (!terrain.useVertexPointAttrib(dc, 0 /*vertexPoint*/)) {
                    continue; // vertex buffer failed to bind
                }

                // Use the viewer's modelview projection matrix, transformed to terrain local coordinates.
                this.projMatrix[0].set(this.viewerMvpMatrix);
                this.projMatrix[0].multiplyByTranslation(terrainOrigin.x, terrainOrigin.y, terrainOrigin.z);
                this.program.loadModelviewProjection(this.projMatrix[0]);

                // Draw the terrain as triangles.
                terrain.drawTriangles(dc);
            }
        } finally {
            // Restore the default World Wind OpenGL state.
            dc.bindFramebuffer(0);
            GLES20.glViewport(dc.viewport.x, dc.viewport.y, dc.viewport.width, dc.viewport.height);
            GLES20.glColorMask(true, true, true, true);
        }

        return true;
    }

    protected void drawSceneVisibility(DrawContext dc) {
        if (this.mockupProgram == null || !this.mockupProgram.useProgram(dc)) {
            return; // program unspecified or failed to build
        }

        // Make multi-texture unit 0 active.
        dc.activeTextureUnit(GLES20.GL_TEXTURE0);

        Texture depthAttachment = dc.scratchFramebuffer().getAttachedTexture(GLES20.GL_DEPTH_ATTACHMENT);
        if (!depthAttachment.bindTexture(dc)) {
            return; // framebuffer texture failed to bind
        }

        // Use the drawable's color.
        this.mockupProgram.loadColor(new Color(1, 1, 1, 1));

        for (int idx = 0, len = dc.getDrawableTerrainCount(); idx < len; idx++) {
            // Get the drawable terrain associated with the draw context.
            DrawableTerrain terrain = dc.getDrawableTerrain(idx);
            Vec3 terrainOrigin = terrain.getVertexOrigin();

            // Use the terrain's vertex point attribute.
            if (!terrain.useVertexPointAttrib(dc, 0 /*vertexPoint*/)) {
                continue; // vertex buffer failed to bind
            }

            // Use the draw context's modelview projection matrix, transformed to terrain local coordinates.
            this.projMatrix[0].set(dc.modelviewProjection);
            this.projMatrix[0].multiplyByTranslation(terrainOrigin.x, terrainOrigin.y, terrainOrigin.z);
            // Use the projector's modelview projection matrix, transformed to terrain local coordinates.
            this.projMatrix[1].set(
                0.5, 0.0, 0.0, 0.5,
                0.0, 0.5, 0.0, 0.5,
                0.0, 0.0, 0.5, 0.5,
                0.0, 0.0, 0.0, 1.0);
            this.projMatrix[1].multiplyByMatrix(this.viewerMvpMatrix);
            this.projMatrix[1].multiplyByTranslation(terrainOrigin.x, terrainOrigin.y, terrainOrigin.z);
            this.mockupProgram.loadProjectionMatrix(this.projMatrix);

            // Draw the terrain as triangles.
            terrain.drawTriangles(dc);
        }
    }

    protected void drawFramebufferToScreen(DrawContext dc) {
        if (this.program == null || !this.program.useProgram(dc)) {
            return; // program unspecified or failed to build
        }

        if (!dc.unitSquareBuffer().bindBuffer(dc)) {
            return; // vertex buffer failed to bind
        }

        // Make multi-texture unit 0 active.
        dc.activeTextureUnit(GLES20.GL_TEXTURE0);

        Texture depthAttachment = dc.scratchFramebuffer().getAttachedTexture(GLES20.GL_DEPTH_ATTACHMENT);
        if (!depthAttachment.bindTexture(dc)) {
            return; // framebuffer texture failed to bind
        }

        // Use the draw context's pick mode and use the drawable's color.
        this.program.enablePickMode(dc.pickMode);
        this.program.loadColor(new Color(1, 1, 1, 1));

        // Configure the program to draw texture fragments from the framebuffer.
        this.program.enableTexture(true);
        this.program.loadTexCoordMatrix(this.identityMatrix3);

        // Use a modelview-projection matrix that transforms the unit square to screen coordinates.
        this.projMatrix[0].setToMultiply(dc.screenProjection, this.unitSquareTransform);
        this.program.loadModelviewProjection(this.projMatrix[0]);

        // Use a unit square as the vertex point and vertex tex coord attributes.
        GLES20.glEnableVertexAttribArray(1 /*vertexTexCoord*/); // only vertexPoint is enabled by default
        GLES20.glVertexAttribPointer(0 /*vertexPoint*/, 2, GLES20.GL_FLOAT, false, 0, 0);
        GLES20.glVertexAttribPointer(1 /*vertexTexCoord*/, 2, GLES20.GL_FLOAT, false, 0, 0);

        // Disable depth testing.
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);

        // Draw the unit square as triangles.
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        // Restore the default World Wind OpenGL state.
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glDisableVertexAttribArray(1 /*vertexTexCoord*/); // only vertexPoint is enabled by default
    }
}
