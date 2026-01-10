package render;

import org.joml.Matrix4f;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL13;
import shader.Shader;
import mesh.CubeMesh;
import texture.Texture;
import camera.Camera;
import face.Face;
import world.Block;
import world.Chunk;
import world.WorldManager;

import java.io.IOException;
import java.util.*;

public class RenderManager {

    private final CubeMesh cubeMesh;
    private final Shader shader;
    private final Camera camera;
    private final WorldManager worldManager;
    private final ShadowManager shadowManager;
    private final ChunkRenderer chunkRenderer; // ADD THIS

    // Cache textures to avoid reloading every frame
    private final Map<String, Texture> textureCache = new HashMap<>();

    public RenderManager(WorldManager world, Camera cam, ShadowManager shadow) throws IOException {
        this.worldManager = world;
        this.camera = cam;
        this.shadowManager = shadow;

        this.cubeMesh = new CubeMesh();
        this.shader = new Shader(
                "shader/cube/cube.vert",
                "shader/cube/cube.frag"
        );
        this.chunkRenderer = new ChunkRenderer(world, cam, shadow); // ADD THIS

    }

    private Texture getTexture(String path) {
        return textureCache.computeIfAbsent(path, p -> {
            try {
                return new Texture(p);
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException("Failed to load texture: " + p);
            }
        });
    }

    public void render() {
        // OPTION 1: Use chunk rendering (FAST - 100+ FPS)
        chunkRenderer.render();

        // OPTION 2: Use old per-block rendering (SLOW - for debugging only)
        //renderPerBlock(); // Comment this out for production
    }

    private void renderPerBlock() {
        shader.bind();
        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        shadowManager.getDepthTexture().bind();

        // --- Sun movement ---
        float time = (System.currentTimeMillis() % 120000) / 120000.0f;
        float sunAngle = time * 2.0f * (float)Math.PI;
        Vector3f sunDir = new Vector3f((float)Math.sin(sunAngle), (float)Math.cos(sunAngle), -0.3f).normalize();
        Vector3f sunColor = new Vector3f(1.0f, 0.95f, 0.85f);
        Vector3f globalIllum = new Vector3f(0.18f, 0.20f, 0.22f);

        shader.setUniform3f("u_LightDir", sunDir);
        shader.setUniform3f("u_LightColor", sunColor);
        shader.setUniform3f("u_Ambient", globalIllum);
        shader.setUniform1i("shadowMap", 1);
        shader.setUniformMat4f("u_LightSpaceMatrix", shadowManager.getLightSpaceMatrix());

        // Group blocks by texture FIRST to reduce texture binds
        Map<String, List<Vector3f>> blocksByTexture = new HashMap<>();

        for (Vector3f cubePos : worldManager.getRenderList()) {
            Block block = worldManager.getBlock(cubePos);
            if (block == null) continue;

            blocksByTexture
                    .computeIfAbsent(block.getTexture(), k -> new ArrayList<>())
                    .add(cubePos);
        }

        // Render all blocks of the same texture together
        for (Map.Entry<String, List<Vector3f>> entry : blocksByTexture.entrySet()) {
            String texturePath = entry.getKey();
            if (texturePath.isEmpty()) continue;

            Texture texture = getTexture(texturePath);
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            texture.bind();
            shader.setUniform1i("u_Texture", 0);

            for (Vector3f cubePos : entry.getValue()) {
                EnumSet<Face> visibleFaces = getVisibleFaces(cubePos);
                for (Face face : visibleFaces) {
                    renderFace(cubePos, face);
                }
            }

            texture.unbind();
        }

        shader.unbind();
    }

    private void renderFace(Vector3f cubePos, Face face) {
        shader.setUniform1f("u_AO", getAmbientOcclusion(face));

        // Check if face is sunlit (no block above in sun direction)
        float sunlit = 1.0f;
        // Only check for top face (y+)
        if (face == Face.TOP) {
            int aboveX = (int) cubePos.x;
            int aboveY = (int) cubePos.y + 1;
            int aboveZ = (int) cubePos.z;
            if (worldManager.hasCube(aboveX, aboveY, aboveZ)) {
                sunlit = 0.0f;
            }
        }
        shader.setUniform1f("u_Sunlit", sunlit);

        Matrix4f model = new Matrix4f().translate(cubePos.x, cubePos.y, cubePos.z);
        shader.setUniformMat4f("u_Model", model);

        Matrix4f mvp = new Matrix4f();
        camera.getProjection().mul(camera.getView(), mvp);
        mvp.mul(model);
        shader.setUniformMat4f("u_MVP", mvp);

        cubeMesh.renderFace(face);
    }

    private float getAmbientOcclusion(Face face) {
        return switch (face) {
            case BOTTOM -> 0.5f;
            case LEFT, RIGHT -> 0.7f;
            default -> 1.0f;
        };
    }



    private EnumSet<Face> getVisibleFaces(Vector3f pos) {
        EnumSet<Face> faces = EnumSet.allOf(Face.class);

        for (Face f : Face.values()) {
            int nx = (int) pos.x + f.dx;
            int ny = (int) pos.y + f.dy;
            int nz = (int) pos.z + f.dz;

            if (worldManager.hasCube(nx, ny, nz)) {
                faces.remove(f);
            }
        }

        return faces;
    }

    public void cleanup() {
        chunkRenderer.cleanup(); // Clean up chunk renderer
        shader.cleanup();
        for (Texture tex : textureCache.values()) {
            tex.cleanup();
        }
    }
}
