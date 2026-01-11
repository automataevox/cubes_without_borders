package world.blocks;

import world.Block;

public class GrassBlock extends Block {
    public GrassBlock() {
        super("grass", new String[] {
                "grass_side",     // FRONT (0)
                "grass_side",     // BACK (1)
                "grass_side",     // LEFT (2)
                "grass_side",     // RIGHT (3)
                "grass_top",      // TOP (4)
                "dirt"            // BOTTOM (5)
        });
    }

    @Override
    public boolean isTransparent() {
        return false;
    }
}