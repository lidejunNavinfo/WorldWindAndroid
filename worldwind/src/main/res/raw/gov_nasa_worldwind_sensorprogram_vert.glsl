/*
 * Copyright (c) 2017 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration. All Rights Reserved.
 */

uniform mat4 mvpMatrix;
uniform mat4 ovpMatrix[2];

attribute vec4 vertexPoint;

varying vec4 occludePosition;
varying float occludeDistance;

void main() {
    /* Transform the vertex position by the modelview-projection matrix. */
    gl_Position = mvpMatrix * vertexPoint;

    /* Transform the vertex position by the textureview-projection matrix. */
    vec4 occludeEyePosition = ovpMatrix[1] * vertexPoint;
    occludePosition = ovpMatrix[0] * occludeEyePosition;
    occludeDistance = length(occludeEyePosition);
}
