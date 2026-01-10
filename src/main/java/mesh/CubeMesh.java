package mesh;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import org.lwjgl.system.MemoryUtil;

import face.Face;

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

    public static void addFace(FloatBuffer buffer, int x, int y, int z, Face face) {
        float[][] verts = face.getVertices();

        // CORRECT texture coordinates for a cube face
        float[][] texCoords = {
                {0.0f, 0.0f},  // bottom-left
                {1.0f, 0.0f},  // bottom-right
                {1.0f, 1.0f},  // top-right
                {1.0f, 1.0f},  // top-right (duplicate)
                {0.0f, 1.0f},  // top-left
                {0.0f, 0.0f}   // bottom-left (duplicate)
        };

        for (int i = 0; i < 6; i++) {
            // Position (3 floats)
            buffer.put(verts[i][0] + x);
            buffer.put(verts[i][1] + y);
            buffer.put(verts[i][2] + z);

            // Texture coordinates (2 floats) - MAKE SURE THESE ARE ADDED!
            buffer.put(texCoords[i][0]);  // u
            buffer.put(texCoords[i][1]);  // v
        }
    }


    public void cleanup() {
        glDeleteBuffers(vbo);
        glDeleteVertexArrays(vao);
    }
}