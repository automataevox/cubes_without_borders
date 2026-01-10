#version 410 core

layout(location = 0) in vec3 aPos;
layout(location = 1) in vec2 aTexCoord;  // CHANGED: from vec3 aNormal to vec2 aTexCoord

out vec3 FragPos;
out vec3 Normal;
out vec2 TexCoord;

uniform mat4 u_Model;
uniform mat4 u_MVP;

void main() {
    FragPos = vec3(u_Model * vec4(aPos, 1.0));

    // Calculate normal from position (for cube faces)
    // Since cubes are centered at (0,0,0), position gives us the face direction
    vec3 absPos = abs(aPos);
    if (absPos.x > absPos.y && absPos.x > absPos.z) {
        Normal = vec3(sign(aPos.x), 0.0, 0.0);  // X face
    } else if (absPos.y > absPos.z) {
        Normal = vec3(0.0, sign(aPos.y), 0.0);  // Y face
    } else {
        Normal = vec3(0.0, 0.0, sign(aPos.z));  // Z face
    }

    TexCoord = aTexCoord;
    gl_Position = u_MVP * vec4(aPos, 1.0);
}