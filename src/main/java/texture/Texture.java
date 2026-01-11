package texture;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryUtil;

import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.io.IOException;
import java.io.InputStream;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.GL_DEPTH_COMPONENT32F;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;
import static org.lwjgl.system.MemoryStack.stackPush;

public class Texture {
    private int id;
    private int width;
    private int height;

    public Texture(String resourcePath) throws IOException {

        try (var stack = stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            // First try as file
            File file = new File(resourcePath);
            ByteBuffer image;

            if (file.exists()) {
                image = STBImage.stbi_load(file.getAbsolutePath(), w, h, channels, 4);
            } else {
                // Try as resource
                InputStream stream = getClass().getClassLoader().getResourceAsStream(resourcePath);
                if (stream == null) {
                    throw new IOException("Texture not found: " + resourcePath);
                }
                byte[] bytes = stream.readAllBytes();
                ByteBuffer buffer = MemoryUtil.memAlloc(bytes.length);
                buffer.put(bytes).flip();
                image = STBImage.stbi_load_from_memory(buffer, w, h, channels, 4);
                MemoryUtil.memFree(buffer);
            }

            if (image == null) {
                throw new IOException("Failed to load texture: " + resourcePath);
            }

            width = w.get();
            height = h.get();
            int comp = channels.get();

            int sampleX = width / 2;
            int sampleY = height / 2;
            int pixelIndex = (sampleY * width + sampleX) * comp;

            if (pixelIndex + 2 < width * height * comp) {
                int r = image.get(pixelIndex) & 0xFF;
                int g = image.get(pixelIndex + 1) & 0xFF;
                int b = image.get(pixelIndex + 2) & 0xFF;
                int a = comp > 3 ? image.get(pixelIndex + 3) & 0xFF : 255;
            }

            id = glGenTextures();

            glBindTexture(GL_TEXTURE_2D, id);

            // CRITICAL SETTINGS for texture atlas:
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

            if (comp == 4) {
                glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, image);
            } else if (comp == 3) {
                glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, width, height, 0, GL_RGB, GL_UNSIGNED_BYTE, image);
            }

            // Don't generate mipmaps for texture atlas
            // glGenerateMipmap(GL_TEXTURE_2D);

            glBindTexture(GL_TEXTURE_2D, 0);

            STBImage.stbi_image_free(image);

        }
    }

    public Texture(int width, int height) {
        this.width = width;
        this.height = height;

        id = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, id);

        // Allocate depth texture
        glTexImage2D(
                GL_TEXTURE_2D,
                0,
                GL_DEPTH_COMPONENT32F,
                width,
                height,
                0,
                GL_DEPTH_COMPONENT,
                GL_FLOAT,
                0
        );

        // Texture parameters (IMPORTANT)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public int getid() {
        return id;
    }

    public void bind() {
        glBindTexture(GL_TEXTURE_2D, id);
    }

    public void unbind() {
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public void cleanup() {
        glDeleteTextures(id);
    }
}
