package world.blocks;

import world.Block;

public class DebugBlock extends Block {
    public DebugBlock() {
        super("debug", new String[] {
                "debug",  // TOP
                "debug",  // BOTTOM
                "debug",  // FRONT
                "debug",  // BACK
                "debug",  // LEFT
                "debug"   // RIGHT
        });
    }

    @Override
    public boolean isTransparent() {
        return false;
    }
}