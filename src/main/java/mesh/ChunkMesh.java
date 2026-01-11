package mesh;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import org.lwjgl.system.MemoryUtil;

import render.ChunkMeshBuilder;
import texture.TextureAtlas;
import world.Chunk;

import java.nio.FloatBuffer;
import java.util.*;

public class ChunkMesh {
    private final Map<String, MeshPart> meshParts;
    private boolean built = false;
    private int totalVertexCount = 0;

    private static class MeshPart {
        int vao, vbo;
        int vertexCount;

        void cleanup() {
            if (vbo != 0) glDeleteBuffers(vbo);
            if (vao != 0) glDeleteVertexArrays(vao);
            vbo = 0;
            vao = 0;
        }
    }

    // FIXED CONSTRUCTOR - Initialize the map!
    public ChunkMesh() {
        this.meshParts = new HashMap<>();
        this.built = false;
        this.totalVertexCount = 0;
    }

    // Build from pre-generated mesh data (MAIN THREAD - OpenGL operations)
    public void buildFromData(List<ChunkMeshData> meshDataList) {
        cleanup(); // Clear any existing mesh

        if (meshDataList == null || meshDataList.isEmpty()) {
            System.out.println("Warning: No mesh data provided to buildFromData");
            return;
        }

        totalVertexCount = 0;

        for (ChunkMeshData data : meshDataList) {
            if (data.vertices == null || data.vertices.length == 0) {
                System.out.println("Warning: Empty vertex data for texture: " + data.textureName);
                continue;
            }

            MeshPart part = new MeshPart();
            createMeshPart(part, data.vertices, data.textureName);
            meshParts.put(data.textureName, part);
            totalVertexCount += part.vertexCount;
        }

        built = !meshParts.isEmpty();
    }

    // Build synchronously (for priority chunks)
    public void buildSync(Chunk chunk, TextureAtlas atlas) {
        // Generate data and build immediately
        System.out.println("Building chunk mesh synchronously...");
        List<ChunkMeshData> meshData = ChunkMeshBuilder.generateMeshData(chunk, atlas);

        if (meshData == null || meshData.isEmpty()) {
            System.out.println("Warning: No mesh data generated for chunk");
            return;
        }

        buildFromData(meshData);
    }

    private void createMeshPart(MeshPart part, float[] vertices, String textureName) {
        try {
            // This runs on MAIN THREAD - OpenGL context is available
            part.vao = glGenVertexArrays();
            part.vbo = glGenBuffers();

            if (part.vao == 0 || part.vbo == 0) {
                throw new RuntimeException("Failed to generate OpenGL objects");
            }

            FloatBuffer buffer = MemoryUtil.memAllocFloat(vertices.length);
            buffer.put(vertices).flip();

            glBindVertexArray(part.vao);
            glBindBuffer(GL_ARRAY_BUFFER, part.vbo);
            glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);
            MemoryUtil.memFree(buffer);

            // 5 floats per vertex: position(3) + texcoord(2)
            int stride = 5 * Float.BYTES;
            glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
            glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 3 * Float.BYTES);

            glEnableVertexAttribArray(0);
            glEnableVertexAttribArray(1);

            glBindVertexArray(0);

            part.vertexCount = vertices.length / 5;
        } catch (Exception e) {
            System.err.println("Error creating mesh part for texture '" + textureName + "': " + e.getMessage());
            part.cleanup();
            throw e;
        }
    }

    // Render methods
    public void render(String textureName) {
        if (!built) {
            System.out.println("Warning: Mesh not built, cannot render");
            return;
        }

        MeshPart part = meshParts.get(textureName);
        if (part == null || part.vertexCount == 0) {
            System.out.println("Warning: No mesh part for texture: " + textureName);
            return;
        }

        glBindVertexArray(part.vao);
        glDrawArrays(GL_TRIANGLES, 0, part.vertexCount);
        glBindVertexArray(0);
    }

    public void renderAll() {
        if (!built || meshParts.isEmpty()) {
            System.out.println("Warning: Mesh not built or empty, cannot renderAll");
            return;
        }

        for (Map.Entry<String, MeshPart> entry : meshParts.entrySet()) {
            MeshPart part = entry.getValue();
            if (part.vertexCount == 0) continue;

            glBindVertexArray(part.vao);
            glDrawArrays(GL_TRIANGLES, 0, part.vertexCount);
            glBindVertexArray(0);
        }
    }

    // Getters
    public Set<String> getTextureTypes() {
        return meshParts.keySet();
    }

    public int getVertexCount() {
        return totalVertexCount;
    }

    public int getVertexCountForTexture(String textureName) {
        MeshPart part = meshParts.get(textureName);
        return part != null ? part.vertexCount : 0;
    }

    public int getTextureCount() {
        return meshParts.size();
    }

    public boolean hasTexture(String textureName) {
        return meshParts.containsKey(textureName);
    }

    public boolean isBuilt() {
        return built;
    }

    public boolean isValid() {
        return built && !meshParts.isEmpty();
    }

    public void cleanup() {
        if (meshParts != null) {
            for (MeshPart part : meshParts.values()) {
                part.cleanup();
            }
            meshParts.clear();
        }
        totalVertexCount = 0;
        built = false;
    }
}