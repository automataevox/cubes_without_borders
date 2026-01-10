package world;

import world.blocks.GrassBlock;
import world.blocks.StoneBlock;

public class WorldGenerator {
    public static Block[][][] generateChunk(int size, int height) {
        Block[][][] blocks = new Block[size][height][size];
        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                for (int y = 0; y < height; y++) {
                    if (y == height - 1) {
                        blocks[x][y][z] = new GrassBlock();
                    } else {
                        blocks[x][y][z] = new StoneBlock();
                    }
                }
            }
        }
        return blocks;
    }
}
