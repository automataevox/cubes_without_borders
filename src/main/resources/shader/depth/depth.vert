#version 410 core

layout(location = 0) in vec3 aPos;

uniform mat4 u_LightSpaceMatrix;
uniform mat4 u_Model;

void main() {
    vec4 worldPos = u_Model * vec4(aPos, 1.0);
    gl_Position = u_LightSpaceMatrix * worldPos;
}