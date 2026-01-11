package mesh;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import org.lwjgl.system.MemoryUtil;

import face.Face;
import texture.TextureAtlas;
import world.Block;

import java.nio.FloatBuffer;

public class CubeMesh {

    private final int vao, vbo;

    public static final int FACE_VERTICES = 6;

    // face offsets (in vertices, not floats)
    public static final int[] FACE_OFFSETS = {
            0,   // FRONT
            6,   // BACK
            12,  // LEFT
            18,  // RIGHT
            24,  // TOP
            30   // BOTTOM
    };

    private static final float[] VERTICES = {
            // Front face
            -0.5f, -0.5f,  0.5f,  0f, 0f, 1f,  0f, 0f,
            0.5f, -0.5f,  0.5f,  0f, 0f, 1f,  1f, 0f,
            0.5f,  0.5f,  0.5f,  0f, 0f, 1f,  1f, 1f,
            0.5f,  0.5f,  0.5f,  0f, 0f, 1f,  1f, 1f,
            -0.5f,  0.5f,  0.5f,  0f, 0f, 1f,  0f, 1f,
            -0.5f, -0.5f,  0.5f,  0f, 0f, 1f,  0f, 0f,

            // Back face
            -0.5f, -0.5f, -0.5f,  0f, 0f, -1f,  0f, 0f,
            -0.5f,  0.5f, -0.5f,  0f, 0f, -1f,  0f, 1f,
            0.5f,  0.5f, -0.5f,  0f, 0f, -1f,  1f, 1f,
            0.5f,  0.5f, -0.5f,  0f, 0f, -1f,  1f, 1f,
            0.5f, -0.5f, -0.5f,  0f, 0f, -1f,  1f, 0f,
            -0.5f, -0.5f, -0.5f,  0f, 0f, -1f,  0f, 0f,

            // Left face
            -0.5f, -0.5f, -0.5f, -1f, 0f, 0f,  0f, 0f,
            -0.5f, -0.5f,  0.5f, -1f, 0f, 0f,  1f, 0f,
            -0.5f,  0.5f,  0.5f, -1f, 0f, 0f,  1f, 1f,
            -0.5f,  0.5f,  0.5f, -1f, 0f, 0f,  1f, 1f,
            -0.5f,  0.5f, -0.5f, -1f, 0f, 0f,  0f, 1f,
            -0.5f, -0.5f, -0.5f, -1f, 0f, 0f,  0f, 0f,

            // Right face
            0.5f, -0.5f, -0.5f, 1f, 0f, 0f,  1f, 0f,
            0.5f,  0.5f, -0.5f, 1f, 0f, 0f,  1f, 1f,
            0.5f,  0.5f,  0.5f, 1f, 0f, 0f,  0f, 1f,
            0.5f,  0.5f,  0.5f, 1f, 0f, 0f,  0f, 1f,
            0.5f, -0.5f,  0.5f, 1f, 0f, 0f,  0f, 0f,
            0.5f, -0.5f, -0.5f, 1f, 0f, 0f,  1f, 0f,

            // Top face
            -0.5f,  0.5f, -0.5f, 0f, 1f, 0f,  0f, 1f,
            -0.5f,  0.5f,  0.5f, 0f, 1f, 0f,  0f, 0f,
            0.5f,  0.5f,  0.5f, 0f, 1f, 0f,  1f, 0f,
            0.5f,  0.5f,  0.5f, 0f, 1f, 0f,  1f, 0f,
            0.5f,  0.5f, -0.5f, 0f, 1f, 0f,  1f, 1f,
            -0.5f,  0.5f, -0.5f, 0f, 1f, 0f,  0f, 1f,

            // Bottom face
            -0.5f, -0.5f, -0.5f, 0f, -1f, 0f,  0f, 0f,
            0.5f, -0.5f, -0.5f, 0f, -1f, 0f,  1f, 0f,
            0.5f, -0.5f,  0.5f, 0f, -1f, 0f,  1f, 1f,
            0.5f, -0.5f,  0.5f, 0f, -1f, 0f,  1f, 1f,
            -0.5f, -0.5f,  0.5f, 0f, -1f, 0f,  0f, 1f,
            -0.5f, -0.5f, -0.5f, 0f, -1f, 0f,  0f, 0f
    };


    public CubeMesh() {
        vao = glGenVertexArrays();
        vbo = glGenBuffers();

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);

        FloatBuffer buf = MemoryUtil.memAllocFloat(VERTICES.length);
        buf.put(VERTICES).flip();
        glBufferData(GL_ARRAY_BUFFER, buf, GL_STATIC_DRAW);
        MemoryUtil.memFree(buf);

        int stride = 8 * Float.BYTES;
        glVertexAttribPointer(0,3,GL_FLOAT,false,stride,0);
        glVertexAttribPointer(1,3,GL_FLOAT,false,stride,3*Float.BYTES);
        glVertexAttribPointer(2,2,GL_FLOAT,false,stride,6*Float.BYTES);

        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);
        glEnableVertexAttribArray(2);

        glBindVertexArray(0);
    }

    public void renderFace(Face face) {
        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLES, FACE_OFFSETS[face.ordinal()], FACE_VERTICES);
        glBindVertexArray(0);
    }

    public void renderAll() {
        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLES, 0, VERTICES.length / 8); // 8 floats per vertex
        glBindVertexArray(0);
    }

    public static void addFace(FloatBuffer buffer, int x, int y, int z, Face face,
                               Block block, TextureAtlas atlas) {
        float[][] verts = face.getVertices();

        // Get UV coordinates for this specific face
        String textureName = block.getTexture(face);
        TextureAtlas.UVCoords uv = atlas.getUV(textureName);

        // Define vertices with proper UVs for this face
        // Cube faces are defined with vertices in specific order
        float[][] texCoords;

        switch (face) {
            case TOP:
                texCoords = new float[][] {
                        {uv.u1, uv.v2},  // bottom-left
                        {uv.u2, uv.v2},  // bottom-right
                        {uv.u2, uv.v1},  // top-right
                        {uv.u2, uv.v1},  // top-right (duplicate)
                        {uv.u1, uv.v1},  // top-left
                        {uv.u1, uv.v2}   // bottom-left (duplicate)
                };
                break;
            case BOTTOM:
                texCoords = new float[][] {
                        {uv.u1, uv.v1},
                        {uv.u2, uv.v1},
                        {uv.u2, uv.v2},
                        {uv.u2, uv.v2},
                        {uv.u1, uv.v2},
                        {uv.u1, uv.v1}
                };
                break;
            default: // Sides
                texCoords = new float[][] {
                        {uv.u1, uv.v2},
                        {uv.u2, uv.v2},
                        {uv.u2, uv.v1},
                        {uv.u2, uv.v1},
                        {uv.u1, uv.v1},
                        {uv.u1, uv.v2}
                };
        }

        for (int i = 0; i < 6; i++) {
            buffer.put(verts[i][0] + x);
            buffer.put(verts[i][1] + y);
            buffer.put(verts[i][2] + z);
            buffer.put(texCoords[i][0]);
            buffer.put(texCoords[i][1]);
        }
    }


    public void cleanup() {
        glDeleteBuffers(vbo);
        glDeleteVertexArrays(vao);
    }
}