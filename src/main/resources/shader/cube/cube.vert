#version 330 core

layout(location = 0) in vec3 aPos;
layout(location = 1) in vec3 aNormal;
layout(location = 2) in vec2 aTexCoord;

uniform mat4 u_Model;
uniform mat4 u_MVP;
uniform mat4 u_LightSpaceMatrix;

out vec3 FragPos;
out vec3 Normal;
out vec2 TexCoord;
out vec4 FragPosLightSpace;

void main() {
    FragPos = vec3(u_Model * vec4(aPos, 1.0));
    Normal = mat3(transpose(inverse(u_Model))) * aNormal;
    TexCoord = aTexCoord;
    FragPosLightSpace = u_LightSpaceMatrix * vec4(FragPos, 1.0);

    gl_Position = u_MVP * vec4(FragPos, 1.0);
}
