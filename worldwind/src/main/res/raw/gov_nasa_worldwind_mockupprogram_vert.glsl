/*
 * Copyright (c) 2017 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration. All Rights Reserved.
 */

uniform mat4 projMatrix[2];

attribute vec4 vertexPoint;

varying vec4 projPosition;

void main() {
    /* Transform the vertex position by the modelview-projection matrix. */
    gl_Position = projMatrix[0] * vertexPoint;

    /* Transform the vertex position by texture-projection matrix. */
    projPosition = projMatrix[1] * vertexPoint;
}