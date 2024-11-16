#version 120

uniform sampler2D DiffuseSampler;

varying vec2 texCoord;
varying vec2 oneTexel;

uniform float Radius;

void main(void) {
    vec4 centerCol = texture2D(DiffuseSampler, texCoord.xy);

    if (centerCol.a != 0) {
        discard;
    }

    vec4 finalColor = vec4(0.0);

    for (float x = -Radius; x <= Radius; x += 0.5) {
        for (float y = -Radius; y <= Radius; y += 0.5) {
            vec2 offset = vec2(oneTexel.x * x, oneTexel.y * y);
            vec4 currentColor = texture2D(DiffuseSampler, texCoord.xy + offset);

            float weight = smoothstep(0.0, 1.0, Radius - length(offset));
            finalColor += currentColor * weight;
        }
    }

    gl_FragColor = finalColor;
}
