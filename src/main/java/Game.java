import camera.Camera;
import mesh.ChunkMesh;
import org.lwjgl.opengl.GL;
import physics.RaycastManager;
import player.Player;
import render.ChunkMeshBuilder;
import render.HighlightManager;
import render.RenderManager;
import render.ShadowManager;
import texture.TextureAtlasGenerator;
import world.Chunk;
import world.WorldManager;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFWErrorCallback;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Game {
    private Window window;
    private Camera camera;
    private Player player;
    private WorldManager worldManager;
    private RenderManager renderManager;
    private HighlightManager highlightManager;
    private RaycastManager raycastManager;
    private ShadowManager shadowManager;

    // FPS optimization variables
    private int frameCount = 0;
    private float fpsTimer = 0;
    private float chunkUpdateTimer = 0;
    private float lastFPS = 0;

    // Shadow update optimization
    private int shadowUpdateCounter = 0;
    public boolean shadowsEnabled = false;

    public void run() {
        init();
        loop();
        cleanup();
    }

    // --- Initialization ---
    private void init() {
        GLFWErrorCallback.createPrint(System.err).set();
        window = new Window(1280, 720, "Minecraft Clone");
        window.init();
        initOpenGL();
        initManagers();
        setupCallbacks();
    }

    private void initOpenGL() {
        GL.createCapabilities();
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        glClearColor(0.53f, 0.81f, 0.92f, 1.0f);
    }

    private void initManagers() {
        try {
            ensureTextureAtlasExists();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        camera = new Camera(window.getHandle());
        worldManager = new WorldManager();

        try {
            shadowManager = new ShadowManager(worldManager);
            renderManager = new RenderManager(worldManager, camera, shadowManager);
            highlightManager = new HighlightManager(camera);
            raycastManager = new RaycastManager(worldManager);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize shaders or textures");
        }

        player = new Player(camera, worldManager);
        Vector3f spawn = worldManager.getSpawnPoint();
        camera.setPosition(spawn);

        // TEST: Force generate and check a chunk
        worldManager.generateChunksAround(spawn);

        // Immediately test mesh building
        testMeshBuilding();
    }

    private void testMeshBuilding() {
        System.out.println("=== Testing Mesh Building ===");

        Vector3f spawn = worldManager.getSpawnPoint();
        int chunkX = (int)Math.floor(spawn.x / 16);
        int chunkZ = (int)Math.floor(spawn.z / 16);

        Chunk chunk = worldManager.getChunkAt(chunkX, chunkZ);
        if (chunk == null) {
            System.out.println("No chunk found at spawn!");
            return;
        }

        System.out.println("Chunk has " + chunk.getVisibleBlockCount() + " visible blocks");

        if (chunk.getVisibleBlockCount() > 0) {
            try {
                // Force build a mesh synchronously
                mesh.ChunkMesh testMesh = new mesh.ChunkMesh();
                testMesh.buildSync(chunk, renderManager.getTextureAtlas()); // Add getter to RenderManager
                System.out.println("Test mesh built: " + testMesh.isValid() +
                        " (" + testMesh.getVertexCount() + " vertices)");
                testMesh.cleanup();
            } catch (Exception e) {
                System.err.println("Failed to build mesh: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("Chunk has no visible blocks to render!");
        }
    }

    private void setupCallbacks() {
        // Set up window resize callback
        glfwSetFramebufferSizeCallback(window.getHandle(), (windowHandle, width, height) -> {
            // Update OpenGL viewport
            glViewport(0, 0, width, height);
        });

        // Optional: Add F11 key for fullscreen toggle
        glfwSetKeyCallback(window.getHandle(), (windowHandle, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_F11 && action == GLFW_PRESS) {
                toggleFullscreen();
            }
            // Optional: Toggle shadows with F10
            if (key == GLFW_KEY_F10 && action == GLFW_PRESS) {
                shadowsEnabled = !shadowsEnabled;
                System.out.println("Shadows " + (shadowsEnabled ? "enabled" : "disabled"));
            }
        });
    }

    private void toggleFullscreen() {
        // Simple fullscreen toggle implementation
        long monitor = glfwGetPrimaryMonitor();
        var vidmode = glfwGetVideoMode(monitor);

        if (glfwGetWindowMonitor(window.getHandle()) == 0) {
            // Go to fullscreen
            glfwSetWindowMonitor(
                    window.getHandle(),
                    monitor,
                    0, 0,
                    vidmode.width(),
                    vidmode.height(),
                    vidmode.refreshRate()
            );
        } else {
            // Go back to windowed
            glfwSetWindowMonitor(
                    window.getHandle(),
                    0,
                    100, 100,
                    1280, 720,
                    GLFW_DONT_CARE
            );
        }
    }

    private void ensureTextureAtlasExists() throws IOException {
        File atlasFile = new File("src/main/resources/textures/atlas.png");
        if (!atlasFile.exists()) {
            System.out.println("Texture atlas not found, generating...");
            TextureAtlasGenerator.generateAtlas(atlasFile.getPath());
        }
    }

    private InputState getInputState() {
        return new InputState(
                glfwGetKey(window.getHandle(), GLFW_KEY_W) == GLFW_PRESS,
                glfwGetKey(window.getHandle(), GLFW_KEY_S) == GLFW_PRESS,
                glfwGetKey(window.getHandle(), GLFW_KEY_A) == GLFW_PRESS,
                glfwGetKey(window.getHandle(), GLFW_KEY_D) == GLFW_PRESS,
                glfwGetKey(window.getHandle(), GLFW_KEY_SPACE) == GLFW_PRESS
        );
    }

    private void loop() {
        float lastTime = (float) glfwGetTime();

        // Track modified chunks
        List<String> modifiedChunks = new ArrayList<>();

        // Chunk generation rate limiting
        float chunkGenTimer = 0;
        final float CHUNK_GEN_INTERVAL = 0.1f; // Generate chunks every 0.1 seconds
        boolean firstFrame = true;
        while (!window.shouldClose()) {
            if (firstFrame) {
                firstFrame = false;
                Vector3f spawn = worldManager.getSpawnPoint();
                int chunkX = (int)Math.floor(spawn.x / 16);
                int chunkZ = (int)Math.floor(spawn.z / 16);

                // Force this chunk to be priority
                modifiedChunks.add(chunkX + "_" + chunkZ);
                System.out.println("Forcing priority build of chunk " + chunkX + "_" + chunkZ);
            }
            float currentTime = (float) glfwGetTime();
            float deltaTime = currentTime - lastTime;
            lastTime = currentTime;

            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            // Update FPS counter
            updateFPSCounter(deltaTime);

            // --- Get input ---
            InputState input = getInputState();

            // --- Player Movement ---
            camera.updateRotation();
            player.update(deltaTime, input.forward, input.backward, input.left, input.right, input.jump);

            // --- Raycasting / Highlighting ---
            Vector3f hoveredCube = raycastManager.raycastBlock(
                    player.getCamera().getPosition(),
                    player.getCamera().getFront()
            );
            highlightManager.setHoveredCube(hoveredCube);

            // --- Handle block breaking ---
            if (hoveredCube != null && glfwGetMouseButton(window.getHandle(), GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS) {
                int chunkX = (int)Math.floor(hoveredCube.x / 16);
                int chunkZ = (int)Math.floor(hoveredCube.z / 16);
                String chunkKey = chunkX + "_" + chunkZ;

                if (!modifiedChunks.contains(chunkKey)) {
                    modifiedChunks.add(chunkKey);
                }

                worldManager.breakBlock(hoveredCube);
            }

            // --- Update shadows (less frequently) ---
            shadowUpdateCounter++;
            if (shadowsEnabled && shadowUpdateCounter % 10 == 0) { // Every 10 frames
                shadowManager.renderDepthMap();
            }

            // --- Generate chunks (rate limited) ---
            chunkGenTimer += deltaTime;
            if (chunkGenTimer >= CHUNK_GEN_INTERVAL) {
                worldManager.generateChunksAround(player.getCamera().getPosition());
                chunkGenTimer = 0;
            }

            // --- Render World ---
            renderManager.setPriorityChunks(modifiedChunks);
            renderManager.render();
            modifiedChunks.clear();

            // Render highlights
            highlightManager.render();

            window.swapBuffers();
            window.pollEvents();

            // Limit FPS to reduce CPU usage
            limitFPS(deltaTime, 60);
        }
    }

    private void limitFPS(float deltaTime, int targetFPS) {
        float targetFrameTime = 1.0f / targetFPS;
        if (deltaTime < targetFrameTime) {
            try {
                Thread.sleep((long) ((targetFrameTime - deltaTime) * 1000));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // FPS counter method
    private void updateFPSCounter(float deltaTime) {
        frameCount++;
        fpsTimer += deltaTime;

        if (fpsTimer >= 1.0f) {
            lastFPS = frameCount;
            System.out.printf("FPS: %.1f | Chunks: %d | Blocks: %d | Shadows: %s%n",
                    lastFPS,
                    worldManager.getLoadedChunks().size(),
                    worldManager.getRenderList().size(),
                    shadowsEnabled ? "ON" : "OFF");
            frameCount = 0;
            fpsTimer = 0;
        }
    }

    // --- Cleanup ---
    private void cleanup() {
        renderManager.cleanup();
        highlightManager.cleanup();
        raycastManager.cleanup();

        // IMPORTANT: Shutdown async builder
        ChunkMeshBuilder.shutdown();

        if (shadowManager != null) {
            shadowManager.cleanup();
        }

        worldManager.cleanup();
        window.cleanup();
    }

    // Helper class to hold input state
    private static class InputState {
        final boolean forward;
        final boolean backward;
        final boolean left;
        final boolean right;
        final boolean jump;

        InputState(boolean forward, boolean backward, boolean left, boolean right, boolean jump) {
            this.forward = forward;
            this.backward = backward;
            this.left = left;
            this.right = right;
            this.jump = jump;
        }
    }
}