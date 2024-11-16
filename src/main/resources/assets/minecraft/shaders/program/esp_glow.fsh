#version 150

// radius of the glow
#define Radius 3

// the final alpha of the area covering the entity
#define FinalAlpha 0.1


uniform sampler2D DiffuseSampler;
uniform sampler2D Sampler2;

in vec2 texCoord;
in vec2 oneTexel;

uniform vec2 BlurDir;

out vec4 fragColor;

vec2 oneStep = oneTexel * BlurDir;
float gradient = 1.0 / (Radius + 1.0);
bool isFinal = bool(BlurDir.y);
float finalAlpha = isFinal ? FinalAlpha : 1.0;
float additionalAlpha = isFinal ? gradient : 0.0;

void main() {
    // get the current pixel
    vec4 sampler = texture(DiffuseSampler, texCoord);
    if (sampler.a >= 1.0) {
        // if the pixel is opaque then
        fragColor = vec4(sampler.rgb, finalAlpha);
    } else {
        // accumulated RGB colors
        vec3 blurs;
        // number of pixels accumulated in blurs variable
        float totalColor;
        // the alpha value that should be the output
        float alpha = sampler.a;

        if (alpha > 0.0) {
            totalColor = 1;
            blurs = sampler.rgb;
        } else {
            totalColor = 0;
            blurs = vec3(0.0);
        }

        // iterate from far to close
        for (float r = Radius; r > 0; r--) {
            vec2 step = oneStep * r;
            vec4 sampleL = texture(DiffuseSampler, texCoord - step);
            vec4 sampleR = texture(DiffuseSampler, texCoord + step);
            if (sampleL.a > 0.0) {
                totalColor++;
                blurs += sampleL.rgb;
                alpha = max(alpha, sampleL.a - r * gradient);
            }
            if (sampleR.a > 0.0) {
                totalColor++;
                blurs += sampleR.rgb;
                alpha = max(alpha, sampleR.a - r * gradient);
            }
        }

        fragColor = alpha > 0.0 ? vec4(blurs / totalColor, alpha + additionalAlpha) : vec4(0.0);
    }
}
