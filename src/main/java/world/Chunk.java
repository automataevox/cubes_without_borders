package world;

import mesh.ChunkMesh;

public class Chunk {
    public static final int SIZE = 16;

    private final Block[] blocks;
    private ChunkMesh mesh;
    private boolean needsRebuild = true;
    public final int chunkX, chunkY, chunkZ;
    private boolean modified = false;  // Track if chunk was modified


    public Chunk(int cx, int cy, int cz) {
        this.chunkX = cx;
        this.chunkY = cy;
        this.chunkZ = cz;
        this.blocks = new Block[SIZE * SIZE * SIZE];
    }

    public void setBlock(int x, int y, int z, Block block) {
        blocks[index(x, y, z)] = block;
        modified = true;

        // Mark neighbor chunks for rebuild too
        if (x == 0) markNeighborForRebuild(chunkX - 1, chunkY, chunkZ);
        if (x == SIZE - 1) markNeighborForRebuild(chunkX + 1, chunkY, chunkZ);
        if (y == 0) markNeighborForRebuild(chunkX, chunkY - 1, chunkZ);
        if (y == SIZE - 1) markNeighborForRebuild(chunkX, chunkY + 1, chunkZ);
        if (z == 0) markNeighborForRebuild(chunkX, chunkY, chunkZ - 1);
        if (z == SIZE - 1) markNeighborForRebuild(chunkX, chunkY, chunkZ + 1);
    }

    private void markNeighborForRebuild(int cx, int cy, int cz) {
        // You'll need to implement this in WorldManager
    }

    public Block getBlock(int x, int y, int z) {
        return blocks[index(x, y, z)];
    }

    private int index(int x, int y, int z) {
        return x + y * SIZE + z * SIZE * SIZE;
    }

    public ChunkMesh getMesh() {
        return mesh;
    }

    public void setMesh(ChunkMesh mesh) {
        this.mesh = mesh;
    }

    public boolean isModified() {
        return modified;
    }

    public void markClean() {
        modified = false;
    }

    public boolean needsRebuild() {
        return needsRebuild;
    }

    public void markRebuilt() {
        needsRebuild = false;
    }

    public void cleanup() {
        if (mesh != null) {
            mesh.cleanup();
            mesh = null;
        }
    }
}