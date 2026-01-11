#version 410 core

layout(location = 0) in vec3 aPos;
layout(location = 1) in vec2 aTexCoord;

out vec3 FragPos;
out vec2 TexCoord;
out vec4 FragPosLightSpace;  // Add this

uniform mat4 u_Model;
uniform mat4 u_MVP;
uniform mat4 u_LightSpaceMatrix;  // Add this

void main() {
    FragPos = vec3(u_Model * vec4(aPos, 1.0));
    TexCoord = aTexCoord;

    // Calculate fragment position in light space
    FragPosLightSpace = u_LightSpaceMatrix * vec4(FragPos, 1.0);

    gl_Position = u_MVP * vec4(aPos, 1.0);
}