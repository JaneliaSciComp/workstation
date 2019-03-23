#version 330

uniform vec4 outlineColor = vec4(0, 0, 0, 1);

in float quad_y; // outer edge antialiasing parameter
in vec4 testColor;

out vec4 fragColor;

// Outline/silhouette fragment shader

void main() 
{
    // Round off sharp edge corners

    // antialias
    float d = quad_y;
    float tipLength = 2.0 * fwidth(d);
    const float halfWidth = 1.0;
    float alpha = outlineColor.a;
    if (d > halfWidth - tipLength)
        alpha *= (1.0 - (d - halfWidth + tipLength) / tipLength);

    fragColor = vec4(outlineColor.rgb, alpha);
}
