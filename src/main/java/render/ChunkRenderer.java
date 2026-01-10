package render;

import mesh.ChunkMesh;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL13;
import shader.Shader;
import texture.Texture;
import camera.Camera;
import world.Chunk;
import world.WorldManager;

import java.io.IOException;
import java.util.*;

public class ChunkRenderer {
    private final Shader shader;
    private final Camera camera;
    private final WorldManager worldManager;
    private final ShadowManager shadowManager;
    private Shader debugShader;

    private int frameCount = 0;
    private long lastDebugOutput = 0;

    // Cache for chunk meshes
    private final Map<String, ChunkMesh> chunkMeshes = new HashMap<>();
    private final Set<String> previouslyLoadedChunks = new HashSet<>();

    // Texture cache
    private final Map<String, Texture> textureCache = new HashMap<>();
    private Texture defaultTexture;

    public ChunkRenderer(WorldManager world, Camera cam, ShadowManager shadow) throws IOException {
        this.worldManager = world;
        this.camera = cam;
        this.shadowManager = shadow;

        this.shader = new Shader("shader/cube/cube.vert", "shader/cube/cube.frag");
        this.debugShader = new Shader("shader/debug.vert", "shader/debug.frag");

        // Load default texture
        this.defaultTexture = loadTexture("textures/debug.png");
    }

    private Texture loadTexture(String path) throws IOException {
        return new Texture(path);
    }

    private Texture getTexture(String path) {
        if (path == null || path.isEmpty()) return defaultTexture;

        return textureCache.computeIfAbsent(path, p -> {
            try {
                return new Texture(p);
            } catch (IOException e) {
                e.printStackTrace();
                return defaultTexture;
            }
        });
    }

    private void cleanupUnusedMeshes() {
        // Get currently loaded chunk keys
        Set<String> currentlyLoaded = new HashSet<>();
        for (Chunk chunk : worldManager.getLoadedChunks()) {
            currentlyLoaded.add(getChunkKey(chunk.chunkX, chunk.chunkZ));
        }

        // Remove meshes for chunks that are no longer loaded
        Iterator<Map.Entry<String, ChunkMesh>> iterator = chunkMeshes.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, ChunkMesh> entry = iterator.next();
            if (!currentlyLoaded.contains(entry.getKey())) {
                entry.getValue().cleanup();
                iterator.remove();
            }
        }

        previouslyLoadedChunks.clear();
        previouslyLoadedChunks.addAll(currentlyLoaded);
    }

    private String getChunkKey(int x, int z) {
        return x + "_" + z;
    }

    public void render() {
        shader.bind();

        // --- Bind texture ---
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        defaultTexture.bind();
        shader.setUniform1i("u_Texture", 0);

        // --- Set lighting uniforms ---
        float time = (System.currentTimeMillis() % 120000) / 120000.0f;
        float sunAngle = time * 2.0f * (float)Math.PI;
        Vector3f sunDir = new Vector3f(
                (float)Math.sin(sunAngle),
                (float)Math.cos(sunAngle),
                -0.3f
        ).normalize();
        Vector3f sunColor = new Vector3f(1.0f, 0.95f, 0.85f);
        Vector3f globalIllum = new Vector3f(0.18f, 0.20f, 0.22f);

        shader.setUniform3f("u_LightDir", sunDir);
        shader.setUniform3f("u_LightColor", sunColor);
        shader.setUniform3f("u_Ambient", globalIllum);
        shader.setUniform1f("u_Sunlit", 1.0f);

        // --- Get camera matrices ---
        Matrix4f projection = camera.getProjection();
        Matrix4f view = camera.getView();

        // --- Get player position for frustum culling ---
        Vector3f playerPos = camera.getPosition();
        int playerChunkX = (int)Math.floor(playerPos.x / 16);
        int playerChunkZ = (int)Math.floor(playerPos.z / 16);

        int renderDistance = 8;
        int chunksRendered = 0;
        int verticesRendered = 0;

        // --- Clear rebuild flags at start of frame ---
        boolean anyChunksRebuilt = false;

        // --- Render visible chunks ---
        for (int dx = -renderDistance; dx <= renderDistance; dx++) {
            for (int dz = -renderDistance; dz <= renderDistance; dz++) {
                int chunkX = playerChunkX + dx;
                int chunkZ = playerChunkZ + dz;

                String chunkKey = getChunkKey(chunkX, chunkZ);

                // Get or create chunk
                Chunk chunk = worldManager.getChunkAt(chunkX, chunkZ);
                if (chunk == null) {
                    // Chunk not loaded, skip it
                    continue;
                }

                // Check if we need to rebuild this chunk's mesh
                ChunkMesh mesh = chunkMeshes.get(chunkKey);
                boolean needsRebuild = false;

                if (mesh == null) {
                    // First time seeing this chunk
                    needsRebuild = true;
                } else if (chunk.isModified()) {
                    // Chunk was modified (block broken/placed)
                    needsRebuild = true;
                    chunk.markClean(); // Reset modification flag
                } else if (!mesh.isValid()) {
                    // Mesh is invalid for some reason
                    needsRebuild = true;
                }

                // Rebuild mesh if needed
                if (needsRebuild) {
                    if (mesh != null) {
                        // Clean up old mesh
                        mesh.cleanup();
                    }

                    // Build new mesh
                    mesh = new ChunkMesh();
                    mesh.build(chunk);
                    chunkMeshes.put(chunkKey, mesh);

                    anyChunksRebuilt = true;
                }

                // Skip if mesh is empty
                if (mesh == null || mesh.getVertexCount() == 0) {
                    continue;
                }

                // --- Set transformation matrices ---
                Matrix4f model = new Matrix4f().translate(chunkX * 16, 0, chunkZ * 16);
                shader.setUniformMat4f("u_Model", model);

                Matrix4f mvp = new Matrix4f(projection).mul(view).mul(model);
                shader.setUniformMat4f("u_MVP", mvp);

                // --- Render chunk ---
                mesh.render();

                chunksRendered++;
                verticesRendered += mesh.getVertexCount();
            }
        }

        // --- Cleanup meshes for chunks that are no longer loaded ---
        if (frameCount % 60 == 0) { // Only check every second (assuming 60 FPS)
            cleanupUnusedMeshes();
        }
        frameCount++;

        // --- Debug output (limit to once per second) ---
        if (System.currentTimeMillis() - lastDebugOutput > 1000) {
            System.out.printf("Chunks: %d rendered, %d cached | Vertices: %d | Rebuilt: %s%n",
                    chunksRendered, chunkMeshes.size(), verticesRendered, anyChunksRebuilt ? "YES" : "NO");
            lastDebugOutput = System.currentTimeMillis();
        }

        // --- Unbind texture ---
        defaultTexture.unbind();
        shader.unbind();
    }

    public void cleanup() {
        for (ChunkMesh mesh : chunkMeshes.values()) {
            mesh.cleanup();
        }
        chunkMeshes.clear();
        shader.cleanup();
        defaultTexture.cleanup();

        for (Texture tex : textureCache.values()) {
            tex.cleanup();
        }
    }
}