package world.blocks;

import world.Block;

public class StoneBlock extends Block {
    public StoneBlock() {
        super("stone", new String[] {
                "stone",  // TOP
                "stone",  // BOTTOM
                "stone",  // FRONT
                "stone",  // BACK
                "stone",  // LEFT
                "stone"   // RIGHT
        });
    }

    @Override
    public boolean isTransparent() {
        return false;
    }
}