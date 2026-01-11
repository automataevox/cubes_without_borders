package render;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import mesh.CubeMesh;
import world.Block;
import world.Chunk;
import world.WorldManager;
import shader.Shader;
import texture.Texture;

import java.io.IOException;

public class ShadowManager {
    private final int SHADOW_WIDTH = 2048;  // Higher resolution = better shadows
    private final int SHADOW_HEIGHT = 2048;

    private int depthMapFBO;
    private Texture depthMapTexture;
    private Shader depthShader;
    private CubeMesh cubeMesh;  // For depth rendering
    private WorldManager worldManager;

    private Matrix4f lightSpaceMatrix;

    public ShadowManager(WorldManager worldManager) throws IOException {
        this.worldManager = worldManager;
        this.cubeMesh = new CubeMesh();
        this.depthShader = new Shader("shader/depth/depth.vert", "shader/depth/depth.frag");
        initShadowMap();
        computeLightSpaceMatrix();
    }

    private void initShadowMap() {
        // Create framebuffer for shadow map
        depthMapFBO = GL30.glGenFramebuffers();

        // Create depth texture
        depthMapTexture = new Texture(SHADOW_WIDTH, SHADOW_HEIGHT); // Add depth texture constructor

        // Attach depth texture to framebuffer
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, depthMapFBO);
        GL30.glFramebufferTexture2D(
                GL30.GL_FRAMEBUFFER,
                GL30.GL_DEPTH_ATTACHMENT,
                GL11.GL_TEXTURE_2D,
                depthMapTexture.getid(),
                0
        );

        // Disable color drawing (we only need depth)
        GL30.glDrawBuffer(GL11.GL_NONE);
        GL30.glReadBuffer(GL11.GL_NONE);

        // Check framebuffer completeness
        if (GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER) != GL30.GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("Framebuffer is not complete!");
        }

        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);

        System.out.println("Shadow map initialized: " + SHADOW_WIDTH + "x" + SHADOW_HEIGHT);
    }

    private void computeLightSpaceMatrix() {
        // Light position (sun position)
        Vector3f lightPos = new Vector3f(-50, 100, -50);
        Vector3f lightTarget = new Vector3f(0, 0, 0);
        Vector3f up = new Vector3f(0, 1, 0);

        // Orthographic projection for directional light
        float orthoSize = 100.0f; // How much area to cover
        Matrix4f lightProjection = new Matrix4f().ortho(
                -orthoSize, orthoSize,
                -orthoSize, orthoSize,
                0.1f, 200f
        );

        // View matrix from light's perspective
        Matrix4f lightView = new Matrix4f().lookAt(lightPos, lightTarget, up);

        // Combined light space matrix
        lightSpaceMatrix = new Matrix4f();
        lightProjection.mul(lightView, lightSpaceMatrix);
    }

    public void renderDepthMap() {
        // Set viewport to shadow map size
        GL11.glViewport(0, 0, SHADOW_WIDTH, SHADOW_HEIGHT);

        // Bind depth framebuffer
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, depthMapFBO);

        // Clear depth buffer
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);

        // Enable face culling for shadow pass (optional, improves performance)
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glCullFace(GL11.GL_FRONT); // Front face culling to reduce shadow acne

        // Use depth shader
        depthShader.bind();
        depthShader.setUniformMat4f("u_LightSpaceMatrix", lightSpaceMatrix);

        // Render all blocks from light's perspective
        for (Chunk chunk : worldManager.getLoadedChunks()) {
            renderChunkDepth(chunk);
        }

        // Restore settings
        GL11.glCullFace(GL11.GL_BACK);
        depthShader.unbind();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);

        // Restore viewport to window size
        GL11.glViewport(0, 0, 1280, 720); // Your window size
    }

    private void renderChunkDepth(Chunk chunk) {
        // Set chunk transformation
        Matrix4f model = new Matrix4f().translate(
                chunk.chunkX * Chunk.SIZE,
                0,
                chunk.chunkZ * Chunk.SIZE
        );

        depthShader.setUniformMat4f("u_Model", model);

        // Render chunk using its mesh or per-block
        // You'll need to implement this based on your rendering system
        renderBlocksDepth(chunk);
    }

    public int getShadowMapTexture() {
        return depthMapTexture.getid();  // Assuming Texture class has getid() method
    }

    public void bindShadowMap() {
        depthMapTexture.bind();  // Assuming Texture class has bind() method
    }

    public void unbindShadowMap() {
        depthMapTexture.unbind();  // Assuming Texture class has unbind() method
    }

    private void renderBlocksDepth(Chunk chunk) {
        // Simplified: render each block
        // In practice, you'd use chunk meshes
        for (int x = 0; x < Chunk.SIZE; x++) {
            for (int y = 0; y < Chunk.SIZE; y++) {
                for (int z = 0; z < Chunk.SIZE; z++) {
                    Block block = chunk.getBlock(x, y, z);
                    if (block != null) {
                        Matrix4f blockModel = new Matrix4f().translate(x, y, z);
                        depthShader.setUniformMat4f("u_Model", blockModel);
                        cubeMesh.renderAll(); // Render all faces
                    }
                }
            }
        }
    }

    public Matrix4f getLightSpaceMatrix() {
        return lightSpaceMatrix;
    }

    public Texture getDepthMapTexture() {
        return depthMapTexture;
    }

    public void cleanup() {
        depthShader.cleanup();
        cubeMesh.cleanup();
        depthMapTexture.cleanup();
        GL30.glDeleteFramebuffers(depthMapFBO);
    }
}