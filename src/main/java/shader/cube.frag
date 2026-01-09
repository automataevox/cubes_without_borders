#version 410 core

in vec3 FragPos;
in vec3 Normal;
in vec2 TexCoord;

out vec4 FragColor;

uniform sampler2D u_Texture;
uniform vec3 u_LightDir;
uniform vec3 u_LightColor;
uniform float u_AO;       // Ambient Occlusion factor 0..1
uniform vec3 u_Ambient;   // Ambient color

void main() {
    // Normalize
    vec3 norm = normalize(Normal);
    vec3 lightDir = normalize(-u_LightDir);

    // Diffuse
    float diff = max(dot(norm, lightDir), 0.0);

    // Texture color
    vec3 texColor = texture(u_Texture, TexCoord).rgb;

    // Ambient + AO
    vec3 ambient = u_Ambient * texColor * u_AO;

    // Final color
    vec3 result = ambient + texColor * u_LightColor * diff;

    FragColor = vec4(result, 1.0);
}
