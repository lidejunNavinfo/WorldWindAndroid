/*
 * Copyright (c) 2017 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration. All Rights Reserved.
 */

#ifdef GL_FRAGMENT_PRECISION_HIGH
precision highp float;
#else
precision mediump float;
#endif

uniform vec4 color;
uniform sampler2D texSampler;

varying vec4 projPosition;

const vec3 zero = vec3(0.0, 0.0, 0.0);
const vec3 one = vec3(1.0, 1.0, 1.0);

void main() {
    /* Sample the projective texture at the projected position. */
    vec4 texColor = texture2DProj(texSampler, projPosition);

    /* Compute a binary mask that's 1.0 if the projected position is inside the projected frustum, and 0.0 otherwise.*/
    vec3 ndcCoord = projPosition.xyz / projPosition.w;
    vec3 ndcCoordMask = step(zero, ndcCoord) * step(ndcCoord, one);
    float ndcMask = ndcCoordMask.x * ndcCoordMask.y * ndcCoordMask.z;

    /* Modulate the RGBA color with the texture color and the frustum mask. */
    gl_FragColor = color * texColor * ndcMask;
}