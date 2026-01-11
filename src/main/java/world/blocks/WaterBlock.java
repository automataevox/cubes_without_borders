package world.blocks;

import world.Block;

public class WaterBlock extends Block {
    public WaterBlock() {
        super("water", new String[] {
                "water",  // TOP (transparent)
                "water",  // BOTTOM
                "water",  // FRONT
                "water",  // BACK
                "water",  // LEFT
                "water"   // RIGHT
        });
    }

    // Override to make water semi-transparent
    @Override
    public boolean isTransparent() {
        return true;
    }
}