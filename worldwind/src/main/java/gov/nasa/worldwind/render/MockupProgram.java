/*
 * Copyright (c) 2017 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration. All Rights Reserved.
 */

package gov.nasa.worldwind.render;

import android.content.res.Resources;
import android.opengl.GLES20;

import gov.nasa.worldwind.R;
import gov.nasa.worldwind.draw.DrawContext;
import gov.nasa.worldwind.geom.Matrix3;
import gov.nasa.worldwind.geom.Matrix4;
import gov.nasa.worldwind.util.Logger;
import gov.nasa.worldwind.util.WWUtil;

public class MockupProgram extends ShaderProgram {

    public static final Object KEY = MockupProgram.class;

    protected int projMatrixId;

    protected int texSamplerId;

    protected int colorId;

    private float[] array = new float[32];

    public MockupProgram(Resources resources) {
        try {
            String vs = WWUtil.readResourceAsText(resources, R.raw.gov_nasa_worldwind_mockupprogram_vert);
            String fs = WWUtil.readResourceAsText(resources, R.raw.gov_nasa_worldwind_mockupprogram_frag);
            this.setProgramSources(vs, fs);
            this.setAttribBindings("vertexPoint");
        } catch (Exception logged) {
            Logger.logMessage(Logger.ERROR, "MockupProgram", "constructor", "errorReadingProgramSource", logged);
        }
    }

    protected void initProgram(DrawContext dc) {
        this.projMatrixId = GLES20.glGetUniformLocation(this.programId, "projMatrix");
        GLES20.glUniformMatrix4fv(this.projMatrixId, 1, false, this.array, 0);

        this.texSamplerId = GLES20.glGetUniformLocation(this.programId, "texSampler");
        GLES20.glUniform1i(this.texSamplerId, 0); // GL_TEXTURE0

        this.colorId = GLES20.glGetUniformLocation(this.programId, "color");
        GLES20.glUniform4f(this.colorId, 1, 1, 1, 1);
    }

    public void loadProjectionMatrix(Matrix4... matrix) {
        matrix[0].transposeToArray(this.array, 0);
        matrix[1].transposeToArray(this.array, 16);
        GLES20.glUniformMatrix4fv(this.projMatrixId, 2, false, this.array, 0);
    }

    public void loadColor(Color color) {
        float alpha = color.alpha;
        GLES20.glUniform4f(this.colorId, color.red * alpha, color.green * alpha, color.blue * alpha, alpha);
    }
}
