package world;

import face.Face;

public abstract class Block {
    private final String name;
    private final String[] faceTextures; // 6 textures: [top, bottom, front, back, left, right]

    public Block(String name, String[] faceTextures) {
        this.name = name;
        this.faceTextures = faceTextures;
    }

    public String getName() {
        return name;
    }

    // Get texture for specific face
    public String getTexture(Face face) {
        return faceTextures[face.ordinal()];
    }

    // Get all textures used by this block (for caching)
    public String[] getAllTextures() {
        return faceTextures;
    }

    // Add this method:
    public boolean isVisible() {
        // Air blocks should return false, all others true
        return !"air".equals(name) && !"empty".equals(name);
    }
}