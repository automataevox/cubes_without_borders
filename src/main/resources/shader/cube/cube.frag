#version 410 core

// Inputs
in vec3 FragPos;
in vec3 Normal;
in vec2 TexCoord;
in vec4 FragPosLightSpace;

// Output
out vec4 FragColor;

// Texture uniforms
uniform sampler2D u_Texture;
uniform sampler2D u_ShadowMap;

// Lighting uniforms
uniform vec3 u_LightDir;
uniform vec3 u_LightColor;
uniform vec3 u_Ambient;
uniform vec3 u_ViewPos;

// Material properties
uniform float u_Roughness = 0.8;
uniform float u_Metallic = 0.0;

// Settings
uniform int u_UsePBR = 0;  // Use simple lighting by default
uniform int u_UseFog = 0;  // Disable fog for performance
uniform int u_UseShadows = 1;

// Constants
const float PI = 3.14159265359;
const float GAMMA = 2.2;

// ========== SIMPLE SHADOW CALCULATION ==========
float calculateShadow() {
    if (u_UseShadows == 0) return 0.0;

    // Perform perspective divide
    vec3 projCoords = FragPosLightSpace.xyz / FragPosLightSpace.w;

    // Transform to [0,1] range
    projCoords = projCoords * 0.5 + 0.5;

    // Clamp to texture bounds
    if (projCoords.x < 0.0 || projCoords.x > 1.0 ||
    projCoords.y < 0.0 || projCoords.y > 1.0 ||
    projCoords.z > 1.0) {
        return 0.0;
    }

    // Get depth values
    float closestDepth = texture(u_ShadowMap, projCoords.xy).r;
    float currentDepth = projCoords.z;

    // Simple bias
    float bias = 0.005;

    // Simple shadow (no PCF for performance)
    return currentDepth - bias > closestDepth ? 1.0 : 0.0;
}

// ========== SIMPLE LIGHTING ==========
vec3 calculateSimpleLighting(vec3 albedo, vec3 normal, float shadow) {
    vec3 N = normalize(normal);
    vec3 L = normalize(-u_LightDir);

    // Diffuse
    float diff = max(dot(N, L), 0.0);
    vec3 diffuse = u_LightColor * diff * albedo * (1.0 - shadow);

    // Ambient
    vec3 ambient = u_Ambient * albedo;

    // Simple specular (Blinn-Phong)
    if (u_UsePBR == 0) {
        vec3 V = normalize(u_ViewPos - FragPos);
        vec3 H = normalize(L + V);
        float spec = pow(max(dot(N, H), 0.0), 32.0 * (1.0 - u_Roughness));
        vec3 specular = u_LightColor * spec * u_Metallic;

        return ambient + diffuse + specular;
    }

    return ambient + diffuse;
}

// ========== SIMPLE PBR LIGHTING (if enabled) ==========
vec3 calculatePBRLighting(vec3 albedo, vec3 normal, float shadow) {
    if (u_UsePBR == 0) return calculateSimpleLighting(albedo, normal, shadow);

    vec3 N = normalize(normal);
    vec3 V = normalize(u_ViewPos - FragPos);
    vec3 L = normalize(-u_LightDir);
    vec3 H = normalize(V + L);

    // Base reflectivity
    vec3 F0 = mix(vec3(0.04), albedo, u_Metallic);

    // Simple Cook-Torrance approximation
    float NDF = 1.0 / (PI * u_Roughness * u_Roughness + 0.0001);
    float NdotH = max(dot(N, H), 0.0);
    float NdotV = max(dot(N, V), 0.0);
    float NdotL = max(dot(N, L), 0.0);

    // Fresnel
    vec3 F = F0 + (1.0 - F0) * pow(1.0 - NdotV, 5.0);

    // Geometry
    float G = 1.0 / ((NdotV * (1.0 - u_Roughness) + u_Roughness) *
    (NdotL * (1.0 - u_Roughness) + u_Roughness));

    // BRDF
    vec3 specular = (NDF * G * F) / (4.0 * NdotV * NdotL + 0.0001);

    // Diffuse
    vec3 kD = vec3(1.0) - F;
    kD *= 1.0 - u_Metallic;

    // Combine
    vec3 Lo = (kD * albedo / PI + specular) * u_LightColor * NdotL * (1.0 - shadow);
    vec3 ambient = u_Ambient * albedo;

    return ambient + Lo;
}

// ========== SIMPLE FOG ==========
vec3 applyFog(vec3 color, float distance) {
    if (u_UseFog == 0) return color;

    float fogAmount = 1.0 - exp(-distance * 0.005);
    vec3 fogColor = vec3(0.7, 0.8, 0.9);

    return mix(color, fogColor, fogAmount * 0.7);
}

// ========== MAIN FUNCTION ==========
void main() {
    // Sample texture
    vec4 texSample = texture(u_Texture, TexCoord);
    if (texSample.a < 0.1) discard;

    vec3 albedo = texSample.rgb;

    // Normal (no enhancement for performance)
    vec3 N = normalize(Normal);

    // Calculate shadow
    float shadow = calculateShadow();

    // Calculate lighting (simple or PBR based on setting)
    vec3 color;
    if (u_UsePBR == 1) {
        color = calculatePBRLighting(albedo, N, shadow);
    } else {
        color = calculateSimpleLighting(albedo, N, shadow);
    }

    // Apply fog if enabled
    if (u_UseFog == 1) {
        float distance = length(u_ViewPos - FragPos);
        color = applyFog(color, distance);
    }

    // Simple tone mapping and gamma correction
    color = color / (color + vec3(1.0));
    color = pow(color, vec3(1.0 / GAMMA));

    FragColor = vec4(color, 1.0);
}