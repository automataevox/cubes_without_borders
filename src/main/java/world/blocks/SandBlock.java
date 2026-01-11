package world.blocks;

import world.Block;

public class SandBlock extends Block {
    public SandBlock() {
        super("sand", new String[] {
                "sand",  // TOP
                "sand",  // BOTTOM
                "sand",  // FRONT
                "sand",  // BACK
                "sand",  // LEFT
                "sand"   // RIGHT
        });
    }

    @Override
    public boolean isTransparent() {
        return false;
    }
}