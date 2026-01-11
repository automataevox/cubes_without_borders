package render;

import mesh.ChunkMesh;
import mesh.ChunkMeshData;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL13;
import shader.Shader;
import camera.Camera;
import texture.TextureAtlas;
import world.Chunk;
import world.WorldManager;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Future;

public class ChunkRenderer {
    private final Shader shader;
    private final Camera camera;
    private final WorldManager worldManager;
    private final TextureAtlas textureAtlas;
    private final ShadowManager shadowManager;

    // Performance settings
    private final int RENDER_DISTANCE = 8;
    private final int MAX_ASYNC_BUILDS_PER_FRAME = 3;
    private final int MAX_SYNC_BUILDS_PER_FRAME = 2;

    // Mesh cache
    private final Map<String, ChunkMesh> chunkMeshes = new HashMap<>();
    private final Map<String, Future<List<ChunkMeshData>>> pendingBuilds = new HashMap<>();

    // Priority chunks (block breaking)
    private final List<String> priorityChunks = new ArrayList<>();

    // Statistics
    private int frameCount = 0;
    private int asyncBuildsCompleted = 0;
    private int syncBuildsCompleted = 0;

    public ChunkRenderer(WorldManager world, Camera cam, ShadowManager shadow) throws IOException {
        this.worldManager = world;
        this.camera = cam;
        this.shadowManager = shadow;

        this.shader = new Shader("shader/cube/cube.vert", "shader/cube/cube.frag");
        this.textureAtlas = new TextureAtlas();
    }

    public void setPriorityChunks(List<String> priorityChunks) {
        this.priorityChunks.clear();
        this.priorityChunks.addAll(priorityChunks);
    }

    private String getChunkKey(int x, int z) {
        return x + "_" + z;
    }

    private List<Chunk> getVisibleChunks() {
        List<Chunk> visible = new ArrayList<>();
        Vector3f playerPos = camera.getPosition();
        int playerChunkX = (int)Math.floor(playerPos.x / 16);
        int playerChunkZ = (int)Math.floor(playerPos.z / 16);

        // Circular render distance
        for (int dx = -RENDER_DISTANCE; dx <= RENDER_DISTANCE; dx++) {
            for (int dz = -RENDER_DISTANCE; dz <= RENDER_DISTANCE; dz++) {
                if (dx * dx + dz * dz > RENDER_DISTANCE * RENDER_DISTANCE) {
                    continue;
                }

                int chunkX = playerChunkX + dx;
                int chunkZ = playerChunkZ + dz;

                Chunk chunk = worldManager.getChunkAt(chunkX, chunkZ);
                if (chunk != null && chunk.hasVisibleBlocks()) {
                    visible.add(chunk);
                }
            }
        }

        // Sort by distance (closest first)
        visible.sort((c1, c2) -> {
            float dist1 = (float)Math.pow(c1.chunkX - playerChunkX, 2) +
                    (float)Math.pow(c1.chunkZ - playerChunkZ, 2);
            float dist2 = (float)Math.pow(c2.chunkX - playerChunkX, 2) +
                    (float)Math.pow(c2.chunkZ - playerChunkZ, 2);
            return Float.compare(dist1, dist2);
        });

        return visible;
    }

    public void render() {
        shader.bind();

        // Setup textures and uniforms
        setupRenderState();

        // Get visible chunks
        List<Chunk> visibleChunks = getVisibleChunks();

        // Process async builds that have completed
        processCompletedAsyncBuilds();

        // Start new async builds for needed chunks
        startNewAsyncBuilds(visibleChunks);

        // Process priority chunks synchronously
        processPriorityChunks();

        // Render all available meshes
        renderChunks(visibleChunks);

        // Cleanup
        cleanupRenderState();

        frameCount++;
    }

    private void setupRenderState() {
        // Bind textures
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        textureAtlas.getTexture().bind();
        shader.setUniform1i("u_Texture", 0);

        // Bind shadow map
        if (shadowManager != null) {
            GL13.glActiveTexture(GL13.GL_TEXTURE1);
            shadowManager.bindShadowMap();
            shader.setUniform1i("u_ShadowMap", 1);
            shader.setUniformMat4f("u_LightSpaceMatrix", shadowManager.getLightSpaceMatrix());
        }

        // Set lighting uniforms
        setupLightingUniforms();
    }

    private void setupLightingUniforms() {
        float time = (System.currentTimeMillis() % 60000) / 60000.0f;
        float sunAngle = time * 2.0f * (float)Math.PI;
        Vector3f sunDir = new Vector3f(
                (float)Math.sin(sunAngle),
                (float)Math.cos(sunAngle),
                -0.3f
        ).normalize();

        shader.setUniform3f("u_LightDir", sunDir);
        shader.setUniform3f("u_LightColor", new Vector3f(1.0f, 0.95f, 0.85f));
        shader.setUniform3f("u_Ambient", new Vector3f(0.18f, 0.20f, 0.22f));
        shader.setUniform3f("u_ViewPos", camera.getPosition());
        shader.setUniform1f("u_Time", System.currentTimeMillis() / 1000.0f);
        shader.setUniform1i("u_UseFog", 0);
        shader.setUniform1i("u_UseWind", 0);
        shader.setUniform1i("u_UsePBR", 0);
    }

    private void processCompletedAsyncBuilds() {
        asyncBuildsCompleted = 0;
        Iterator<Map.Entry<String, Future<List<ChunkMeshData>>>> iterator =
                pendingBuilds.entrySet().iterator();

        while (iterator.hasNext() && asyncBuildsCompleted < MAX_ASYNC_BUILDS_PER_FRAME) {
            Map.Entry<String, Future<List<ChunkMeshData>>> entry = iterator.next();

            if (entry.getValue().isDone()) {
                try {
                    List<ChunkMeshData> meshData = entry.getValue().get();
                    if (meshData != null && !meshData.isEmpty()) {
                        // Create mesh on main thread (OpenGL context available)
                        ChunkMesh mesh = new ChunkMesh();
                        mesh.buildFromData(meshData);
                        chunkMeshes.put(entry.getKey(), mesh);
                        asyncBuildsCompleted++;
                    }
                } catch (Exception e) {
                    System.err.println("Failed to get async mesh data: " + e.getMessage());
                }
                iterator.remove();
            }
        }
    }

    private void startNewAsyncBuilds(List<Chunk> visibleChunks) {
        int started = 0;

        for (Chunk chunk : visibleChunks) {
            if (started >= MAX_ASYNC_BUILDS_PER_FRAME) break;

            String key = getChunkKey(chunk.chunkX, chunk.chunkZ);

            // Skip if already have mesh, building, or is priority
            if (chunkMeshes.containsKey(key) ||
                    pendingBuilds.containsKey(key) ||
                    priorityChunks.contains(key)) {
                continue;
            }

            // Start async build
            Future<List<ChunkMeshData>> future = ChunkMeshBuilder.buildAsync(
                    chunk.chunkX, chunk.chunkZ, chunk, textureAtlas
            );
            pendingBuilds.put(key, future);
            started++;
        }
    }

    private void processPriorityChunks() {
        syncBuildsCompleted = 0;

        for (String chunkKey : priorityChunks) {
            if (syncBuildsCompleted >= MAX_SYNC_BUILDS_PER_FRAME) break;

            String[] parts = chunkKey.split("_");
            int chunkX = Integer.parseInt(parts[0]);
            int chunkZ = Integer.parseInt(parts[1]);

            Chunk chunk = worldManager.getChunkAt(chunkX, chunkZ);
            if (chunk != null) {
                // Cancel any pending async build
                Future<List<ChunkMeshData>> pending = pendingBuilds.remove(chunkKey);
                if (pending != null && !pending.isDone()) {
                    pending.cancel(true);
                }

                // Build synchronously
                ChunkMesh oldMesh = chunkMeshes.get(chunkKey);
                if (oldMesh != null) {
                    oldMesh.cleanup();
                }

                ChunkMesh newMesh = new ChunkMesh();
                newMesh.buildSync(chunk, textureAtlas);
                chunkMeshes.put(chunkKey, newMesh);
                chunk.markClean();

                syncBuildsCompleted++;
            }
        }

        priorityChunks.clear();
    }

    private void renderChunks(List<Chunk> visibleChunks) {
        Matrix4f projection = camera.getProjection();
        Matrix4f view = camera.getView();

        // Collect all textures from available meshes
        Set<String> allTextures = new HashSet<>();
        for (Chunk chunk : visibleChunks) {
            String key = getChunkKey(chunk.chunkX, chunk.chunkZ);
            ChunkMesh mesh = chunkMeshes.get(key);
            if (mesh != null && mesh.isValid()) {
                allTextures.addAll(mesh.getTextureTypes());
            }
        }

        // Render by texture type
        for (String textureName : allTextures) {
            for (Chunk chunk : visibleChunks) {
                String key = getChunkKey(chunk.chunkX, chunk.chunkZ);
                ChunkMesh mesh = chunkMeshes.get(key);

                if (mesh == null || !mesh.isValid() || !mesh.hasTexture(textureName)) {
                    continue;
                }

                // Set transformation
                Matrix4f model = new Matrix4f().translate(chunk.chunkX * 16, 0, chunk.chunkZ * 16);
                shader.setUniformMat4f("u_Model", model);

                Matrix4f mvp = new Matrix4f(projection).mul(view).mul(model);
                shader.setUniformMat4f("u_MVP", mvp);

                // Render
                mesh.render(textureName);
            }
        }
    }

    private void cleanupRenderState() {
        if (shadowManager != null) {
            GL13.glActiveTexture(GL13.GL_TEXTURE1);
            shadowManager.unbindShadowMap();
        }

        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        textureAtlas.getTexture().unbind();

        shader.unbind();
    }


    // === ADD THIS METHOD AT THE END (before cleanup()) ===
    public TextureAtlas getTextureAtlas() {
        return textureAtlas;
    }

    public void cleanup() {
        // Clean up meshes
        for (ChunkMesh mesh : chunkMeshes.values()) {
            mesh.cleanup();
        }
        chunkMeshes.clear();

        // Cancel pending builds
        ChunkMeshBuilder.cancelAll();
        pendingBuilds.clear();

        // Clear lists
        priorityChunks.clear();

        // Cleanup resources
        shader.cleanup();
        textureAtlas.getTexture().cleanup();
    }
}