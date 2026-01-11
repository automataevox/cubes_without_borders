package world;

import java.util.Arrays;

public class Chunk {
    public static final int SIZE = 16;
    public final int chunkX, chunkY, chunkZ;
    private final Block[][][] blocks;
    private boolean modified = false;
    private int visibleBlockCount = 0; // Track how many blocks are visible

    public Chunk(int x, int y, int z) {
        this.chunkX = x;
        this.chunkY = y;
        this.chunkZ = z;
        this.blocks = new Block[SIZE][SIZE][SIZE];
    }

    public Block getBlock(int x, int y, int z) {
        if (x < 0 || y < 0 || z < 0 || x >= SIZE || y >= SIZE || z >= SIZE) {
            return null;
        }
        return blocks[x][y][z];
    }

    public void setBlock(int x, int y, int z, Block block) {
        if (x < 0 || y < 0 || z < 0 || x >= SIZE || y >= SIZE || z >= SIZE) {
            return;
        }

        Block oldBlock = blocks[x][y][z];
        if (oldBlock != null && oldBlock.isVisible()) {
            visibleBlockCount--;
        }

        blocks[x][y][z] = block;

        if (block != null && block.isVisible()) {
            visibleBlockCount++;
        }

        modified = true;
    }

    public boolean isModified() {
        return modified;
    }

    public void markClean() {
        modified = false;
    }

    public boolean hasVisibleBlocks() {
        return visibleBlockCount > 0;
    }

    public int getVisibleBlockCount() {
        return visibleBlockCount;
    }

    public void cleanup() {
        // Clear blocks array
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                Arrays.fill(blocks[x][y], null);
            }
        }
        visibleBlockCount = 0;
    }
}