/*
 * Copyright (c) 2017 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration. All Rights Reserved.
 */

#ifdef GL_FRAGMENT_PRECISION_HIGH
precision highp float;
#else
precision mediump float;
#endif

uniform float range;
uniform vec4 color[2];
uniform sampler2D occludeSampler;

varying vec4 occludePosition;
varying float occludeDistance;

const vec3 minusOne = vec3(-1.0, -1.0, -1.0);
const vec3 plusOne = vec3(1.0, 1.0, 1.0);

void main() {
    /* Compute a mask that's on when the position is inside the occlusion projection, and off otherwise. Transform the
       position to clip coordinates, where values between -1.0 and 1.0 are in the frustum. */
    vec3 clipCoord = occludePosition.xyz / occludePosition.w;
    vec3 clipCoordMask = step(minusOne, clipCoord) * step(clipCoord, plusOne);
    float clipMask = clipCoordMask.x * clipCoordMask.y * clipCoordMask.z;

    /* Compute a mask that's on when the position is inside the sensor range, and off otherwise.*/
    float rangeMask = step(occludeDistance, range);

    /* Compute a mask that's on when the object's depth is less than the scene's depth. The texture contains the
       scene's minimum depth at each position. */
    vec3 objectCoord = clipCoord * 0.5 + 0.5;
    float occludeDepth = texture2D(occludeSampler, objectCoord.xy);
    float occludeMask = step(occludeDepth, objectCoord.z);

    /* Modulate the RGBA color with the computed masks to display fragments visible from the texture projection. */
    gl_FragColor = mix(color[0], color[1], occludeMask) * clipMask * rangeMask;
}
