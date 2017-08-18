/*
 * Copyright (c) 2016 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration. All Rights Reserved.
 */

package gov.nasa.worldwind.draw;

import android.opengl.GLES10Ext;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.opengl.GLES31;
import android.opengl.GLES31Ext;
import android.opengl.GLES32;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import gov.nasa.worldwind.PickedObjectList;
import gov.nasa.worldwind.geom.Matrix4;
import gov.nasa.worldwind.geom.Vec2;
import gov.nasa.worldwind.geom.Vec3;
import gov.nasa.worldwind.geom.Viewport;
import gov.nasa.worldwind.render.BufferObject;
import gov.nasa.worldwind.render.Color;
import gov.nasa.worldwind.render.Framebuffer;
import gov.nasa.worldwind.render.Texture;

public class DrawContext {

    public Vec3 eyePoint = new Vec3();

    public Viewport viewport = new Viewport();

    public Matrix4 projection = new Matrix4();

    public Matrix4 modelview = new Matrix4();

    public Matrix4 modelviewProjection = new Matrix4();

    public Matrix4 infiniteProjection = new Matrix4();

    public Matrix4 screenProjection = new Matrix4();

    public DrawableQueue drawableQueue;

    public DrawableQueue drawableTerrain;

    public PickedObjectList pickedObjects;

    public Viewport pickViewport;

    public Vec2 pickPoint;

    public boolean pickMode;

    private int framebufferId;

    private int programId;

    private int textureUnit = GLES20.GL_TEXTURE0;

    private int[] textureId = new int[32];

    private int arrayBufferId;

    private int elementArrayBufferId;

    private Framebuffer scratchFramebuffer;

    private BufferObject unitCircleBuffer;

    private BufferObject unitSphereBuffer;

    private BufferObject unitSphereElements;

    private BufferObject unitSquareBuffer;

    private ByteBuffer scratchBuffer = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder());

    private ArrayList<Object> scratchList = new ArrayList<>();

    private byte[] pixelArray = new byte[4];

    public DrawContext() {
    }

    public void reset() {
        this.eyePoint.set(0, 0, 0);
        this.viewport.setEmpty();
        this.projection.setToIdentity();
        this.modelview.setToIdentity();
        this.modelviewProjection.setToIdentity();
        this.screenProjection.setToIdentity();
        this.infiniteProjection.setToIdentity();
        this.drawableQueue = null;
        this.drawableTerrain = null;
        this.pickedObjects = null;
        this.pickViewport = null;
        this.pickPoint = null;
        this.pickMode = false;
        this.scratchBuffer.clear();
        this.scratchList.clear();
    }

    public void contextLost() {
        // Clear objects and values associated with the current OpenGL context.
        this.framebufferId = 0;
        this.programId = 0;
        this.textureUnit = GLES20.GL_TEXTURE0;
        this.arrayBufferId = 0;
        this.elementArrayBufferId = 0;
        this.scratchFramebuffer = null;
        this.unitCircleBuffer = null;
        this.unitSquareBuffer = null;
        Arrays.fill(this.textureId, 0);
    }

    public Drawable peekDrawable() {
        return (this.drawableQueue != null) ? this.drawableQueue.peekDrawable() : null;
    }

    public Drawable pollDrawable() {
        return (this.drawableQueue != null) ? this.drawableQueue.pollDrawable() : null;
    }

    public void rewindDrawables() {
        if (this.drawableQueue != null) {
            this.drawableQueue.rewindDrawables();
        }
    }

    public int getDrawableTerrainCount() {
        return (this.drawableTerrain != null) ? this.drawableTerrain.count() : 0;
    }

    public DrawableTerrain getDrawableTerrain(int index) {
        return (this.drawableTerrain != null) ? (DrawableTerrain) this.drawableTerrain.getDrawable(index) : null;
    }

    /**
     * Returns the name of the OpenGL framebuffer object that is currently active.
     *
     * @return the currently active framebuffer object, or 0 if no framebuffer object is active
     */
    public int currentFramebuffer() {
        return this.framebufferId;
    }

    /**
     * Makes an OpenGL framebuffer object active. The active framebuffer becomes the target of all OpenGL commands that
     * render to the framebuffer or read from the framebuffer. This has no effect if the specified framebuffer object is
     * already active. The default is framebuffer 0, indicating that the default framebuffer provided by the windowing
     * system is active.
     *
     * @param framebufferId the name of the OpenGL framebuffer object to make active, or 0 to make the default
     *                      framebuffer provided by the windowing system active
     */
    public void bindFramebuffer(int framebufferId) {
        if (this.framebufferId != framebufferId) {
            this.framebufferId = framebufferId;
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, framebufferId);
        }
    }

    public Framebuffer scratchFramebuffer() {
        if (this.scratchFramebuffer != null) {
            return this.scratchFramebuffer;
        }

        Framebuffer framebuffer = new Framebuffer();
        Texture colorAttachment = new Texture(1024, 1024, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE);
        Texture depthAttachment = new Texture(1024, 1024, GLES20.GL_DEPTH_COMPONENT, GLES20.GL_UNSIGNED_INT);
        // TODO the depth_component format could affect Texture's default parameters
        depthAttachment.setTexParameter(GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        depthAttachment.setTexParameter(GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
        framebuffer.attachTexture(this, colorAttachment, GLES20.GL_COLOR_ATTACHMENT0);
        framebuffer.attachTexture(this, depthAttachment, GLES20.GL_DEPTH_ATTACHMENT);

        return (this.scratchFramebuffer = framebuffer);
    }

    /**
     * Returns the name of the OpenGL program object that is currently active.
     *
     * @return the currently active program object, or 0 if no program object is active
     */
    public int currentProgram() {
        return this.programId;
    }

    /**
     * Makes an OpenGL program object active as part of current rendering state. This has no effect if the specified
     * program object is already active. The default is program 0, indicating that no program is active.
     *
     * @param programId the name of the OpenGL program object to make active, or 0 to make no program active
     */
    public void useProgram(int programId) {
        if (this.programId != programId) {
            this.programId = programId;
            GLES20.glUseProgram(programId);
        }
    }

    /**
     * Returns the OpenGL multitexture unit that is currently active. Returns a value from the GL_TEXTUREi enumeration,
     * where i ranges from 0 to 32.
     *
     * @return the currently active multitexture unit.
     */
    public int currentTextureUnit() {
        return this.textureUnit;
    }

    /**
     * Specifies the OpenGL multitexture unit to make active. This has no effect if the specified multitexture unit is
     * already active. The default is GL_TEXTURE0.
     *
     * @param textureUnit the multitexture unit, one of GL_TEXTUREi, where i ranges from 0 to 32.
     */
    public void activeTextureUnit(int textureUnit) {
        if (this.textureUnit != textureUnit) {
            this.textureUnit = textureUnit;
            GLES20.glActiveTexture(textureUnit);
        }
    }

    /**
     * Returns the name of the OpenGL texture 2D object currently bound to the active multitexture unit. The active
     * multitexture unit may be determined by calling currentTextureUnit.
     *
     * @return the currently bound texture 2D object, or 0 if no texture object is bound
     */
    public int currentTexture() {
        int textureUnitIndex = this.textureUnit - GLES20.GL_TEXTURE0;
        return this.textureId[textureUnitIndex];
    }

    /**
     * Returns the name of the OpenGL texture 2D object currently bound to the specified multitexture unit.
     *
     * @param textureUnit the multitexture unit, one of GL_TEXTUREi, where i ranges from 0 to 32.
     *
     * @return the currently bound texture 2D object, or 0 if no texture object is bound
     */
    public int currentTexture(int textureUnit) {
        int textureUnitIndex = textureUnit - GLES20.GL_TEXTURE0;
        return this.textureId[textureUnitIndex];
    }

    /**
     * Makes an OpenGL texture 2D object bound to the current multitexture unit. This has no effect if the specified
     * texture object is already bound. The default is texture 0, indicating that no texture is bound.
     *
     * @param textureId the name of the OpenGL texture 2D object to make active, or 0 to make no texture active
     */
    public void bindTexture(int textureId) {
        int textureUnitIndex = this.textureUnit - GLES20.GL_TEXTURE0;
        if (this.textureId[textureUnitIndex] != textureId) {
            this.textureId[textureUnitIndex] = textureId;
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        }
    }

    /**
     * Returns the name of the OpenGL buffer object bound to the specified target buffer.
     *
     * @param target the target buffer, either GL_ARRAY_BUFFER or GL_ELEMENT_ARRAY_BUFFER
     *
     * @return the currently bound buffer object, or 0 if no buffer object is bound
     */
    public int currentBuffer(int target) {
        if (target == GLES20.GL_ARRAY_BUFFER) {
            return this.arrayBufferId;
        } else if (target == GLES20.GL_ELEMENT_ARRAY_BUFFER) {
            return this.elementArrayBufferId;
        } else {
            return 0;
        }
    }

    /**
     * Makes an OpenGL buffer object bound to a specified target buffer. This has no effect if the specified buffer
     * object is already bound. The default is buffer 0, indicating that no buffer object is bound.
     *
     * @param target   the target buffer, either GL_ARRAY_BUFFER or GL_ELEMENT_ARRAY_BUFFER
     * @param bufferId the name of the OpenGL buffer object to make active
     */
    public void bindBuffer(int target, int bufferId) {
        if (target == GLES20.GL_ARRAY_BUFFER && this.arrayBufferId != bufferId) {
            this.arrayBufferId = bufferId;
            GLES20.glBindBuffer(target, bufferId);
        } else if (target == GLES20.GL_ELEMENT_ARRAY_BUFFER && this.elementArrayBufferId != bufferId) {
            this.elementArrayBufferId = bufferId;
            GLES20.glBindBuffer(target, bufferId);
        } else {
            GLES20.glBindBuffer(target, bufferId);
        }
    }

    public BufferObject unitCircleBuffer() {
        if (this.unitCircleBuffer != null) {
            return this.unitCircleBuffer;
        }

        int slices = 360;
        double angle = 0;
        double deltaAngle = 2 * Math.PI / slices;
        float[] points = new float[slices * 2 + 4];
        int pos = 2;

        for (int idx = 0, len = points.length - 4; idx < len; angle += deltaAngle) {
            points[pos++] = (float) Math.cos(angle);
            points[pos++] = (float) Math.sin(angle);
        }

        points[pos++] = points[2];
        points[pos++] = points[3];

        int size = points.length * 4;
        FloatBuffer buffer = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder()).asFloatBuffer();
        buffer.put(points).rewind();

        BufferObject bufferObject = new BufferObject(GLES20.GL_ARRAY_BUFFER, size, buffer);

        return (this.unitCircleBuffer = bufferObject);
    }

    public BufferObject unitSphereBuffer() {
        if (this.unitSphereBuffer != null) {
            return this.unitSphereBuffer;
        }

        int numLat = 120;
        int numLon = 120;
        double deltaLat = Math.PI / (numLat - 1);
        double deltaLon = 2 * Math.PI / (numLon - 1);
        float[] points = new float[numLat * numLon * 3];
        int pos = 0;

        for (double latIndex = 0, lat = -Math.PI / 2; latIndex < numLat; latIndex++, lat += deltaLat) {
            double cosLat = Math.cos(lat);
            double sinLat = Math.sin(lat);

            for (double lonIndex = 0, lon = -Math.PI; lonIndex < numLon; lonIndex++, lon += deltaLon) {
                double cosLon = Math.cos(lon);
                double sinLon = Math.sin(lon);

                points[pos++] = (float) (cosLat * sinLon);
                points[pos++] = (float) (sinLat);
                points[pos++] = (float) (cosLat * cosLon);
            }
        }

        int size = points.length * 4;
        FloatBuffer buffer = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder()).asFloatBuffer();
        buffer.put(points).rewind();

        return (this.unitSphereBuffer = new BufferObject(GLES20.GL_ARRAY_BUFFER, size, buffer));
    }

    public BufferObject unitSphereElements() {
        if (this.unitSphereElements != null) {
            return this.unitSphereElements;
        }

        // Allocate a buffer to hold the indices.
        int numLat = 120;
        int numLon = 120;
        int count = ((numLat - 1) * numLon + (numLat - 2)) * 2;
        short[] elements = new short[count];
        int pos = 0, vertex = 0;

        for (int latIndex = 0; latIndex < numLat - 1; latIndex++) {
            // Create a triangle strip joining each adjacent column of vertices, starting in the bottom left corner and
            // proceeding to the right. The first vertex starts with the left row of vertices and moves right to create
            // a counterclockwise winding order.
            for (int lonIndex = 0; lonIndex < numLon; lonIndex++) {
                vertex = lonIndex + latIndex * numLon;
                elements[pos++] = (short) (vertex + numLon);
                elements[pos++] = (short) vertex;
            }

            // Insert indices to create 2 degenerate triangles:
            // - one for the end of the current row, and
            // - one for the beginning of the next row
            if (latIndex < numLat - 2) {
                elements[pos++] = (short) vertex;
                elements[pos++] = (short) ((latIndex + 2) * numLon);
            }
        }

        int size = elements.length * 2;
        ShortBuffer buffer = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder()).asShortBuffer();
        buffer.put(elements).rewind();

        return (this.unitSphereElements = new BufferObject(GLES20.GL_ELEMENT_ARRAY_BUFFER, size, buffer));
    }

    /**
     * Returns an OpenGL buffer object containing a unit square expressed as four vertices at (0, 1), (0, 0), (1, 1) and
     * (1, 0). Each vertex is stored as two 32-bit floating point coordinates. The four vertices are in the order
     * required by a triangle strip.
     * <p/>
     * The OpenGL buffer object is created on first use and cached. Subsequent calls to this method return the cached
     * buffer object.
     */
    public BufferObject unitSquareBuffer() {
        if (this.unitSquareBuffer != null) {
            return this.unitSquareBuffer;
        }

        float[] points = new float[]{
            0, 1,   // upper left corner
            0, 0,   // lower left corner
            1, 1,   // upper right corner
            1, 0};  // lower right corner

        int size = points.length * 4;
        FloatBuffer buffer = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder()).asFloatBuffer();
        buffer.put(points).rewind();

        BufferObject bufferObject = new BufferObject(GLES20.GL_ARRAY_BUFFER, size, buffer);

        return (this.unitSquareBuffer = bufferObject);
    }

    /**
     * Reads the fragment color at a screen point in the currently active OpenGL frame buffer. The X and Y components
     * indicate OpenGL screen coordinates, which originate in the frame buffer's lower left corner.
     *
     * @param x      the screen point's X component
     * @param y      the screen point's Y component
     * @param result an optional pre-allocated Color in which to return the fragment color, or null to return a new
     *               color
     *
     * @return the result argument set to the fragment color, or a new color if the result is null
     */
    public Color readPixelColor(int x, int y, Color result) {
        if (result == null) {
            result = new Color();
        }

        // Read the fragment pixel as an RGBA 8888 color.
        ByteBuffer pixelBuffer = (ByteBuffer) this.scratchBuffer(4).clear();
        GLES20.glReadPixels(x, y, 1, 1, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, pixelBuffer);
        pixelBuffer.get(this.pixelArray, 0, 4);

        // Convert the RGBA 8888 color to a World Wind color.
        result.red = (this.pixelArray[0] & 0xFF) / (float) 0xFF;
        result.green = (this.pixelArray[1] & 0xFF) / (float) 0xFF;
        result.blue = (this.pixelArray[2] & 0xFF) / (float) 0xFF;
        result.alpha = (this.pixelArray[3] & 0xFF) / (float) 0xFF;

        return result;
    }

    /**
     * Reads the unique fragment colors within a screen rectangle in the currently active OpenGL frame buffer. The
     * components indicate OpenGL screen coordinates, which originate in the frame buffer's lower left corner.
     *
     * @param x      the screen rectangle's X component
     * @param y      the screen rectangle's Y component
     * @param width  the screen rectangle's width
     * @param height the screen rectangle's height
     *
     * @return a set containing the unique fragment colors
     */
    public Set<Color> readPixelColors(int x, int y, int width, int height) {
        // Read the fragment pixels as a tightly packed array of RGBA 8888 colors.
        int pixelCount = width * height;
        ByteBuffer pixelBuffer = (ByteBuffer) this.scratchBuffer(pixelCount * 4).clear();
        GLES20.glReadPixels(x, y, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, pixelBuffer);

        HashSet<Color> resultSet = new HashSet<>();
        Color result = new Color();

        for (int idx = 0; idx < pixelCount; idx++) {
            // Copy each RGBA 888 color from the NIO buffer a heap array in bulk to reduce buffer access overhead.
            pixelBuffer.get(this.pixelArray, 0, 4);

            // Convert the RGBA 8888 color to a World Wind color.
            result.red = (this.pixelArray[0] & 0xFF) / (float) 0xFF;
            result.green = (this.pixelArray[1] & 0xFF) / (float) 0xFF;
            result.blue = (this.pixelArray[2] & 0xFF) / (float) 0xFF;
            result.alpha = (this.pixelArray[3] & 0xFF) / (float) 0xFF;

            // Accumulate the unique colors in a set.
            if (resultSet.add(result)) {
                result = new Color();
            }
        }

        return resultSet;
    }

    /**
     * Returns a scratch NIO buffer suitable for use during drawing. The returned buffer has capacity at least equal to
     * the specified capacity. The buffer is cleared before each frame, otherwise its contents, position, limit and mark
     * are undefined.
     *
     * @param capacity the buffer's minimum capacity in bytes
     *
     * @return the draw context's scratch buffer
     */
    public ByteBuffer scratchBuffer(int capacity) {
        if (this.scratchBuffer.capacity() < capacity) {
            this.scratchBuffer = ByteBuffer.allocateDirect(capacity).order(ByteOrder.nativeOrder());
        }

        return this.scratchBuffer;
    }

    /**
     * Returns a scratch list suitable for accumulating entries during drawing. The list is cleared before each frame,
     * otherwise its contents are undefined.
     *
     * @return the draw context's scratch list
     */
    public ArrayList<Object> scratchList() {
        return this.scratchList;
    }
}
